package memorybuffer

import commondatabus.CommonDataBus
import decoder.LoadOperation
import decoder.StoreOperation
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import reorderbuffer.ReorderBuffer
import types.*

interface MemoryBufferQueue {
    fun freeSlotCount(): Int
    fun entryCount(): Int
    fun enqueue(
        operation: MemoryBufferOperation,
        baseOperand: Operand,
        valueOperand: Operand,
        immediate: Word,
        robId: RobId
    ): ProcessorResult<MemoryBufferQueue>

    fun dispatchAddressComputations(maxCount: Int): MemoryBufferAddressDispatchResult
    fun recordComputedAddress(memoryBufferId: MemoryBufferId, address: DataAddress): MemoryBufferQueue
    fun removeEntry(memoryBufferId: MemoryBufferId): MemoryBufferQueue
    fun dispatchMemoryLoads(maxCount: Int, reorderBuffer: ReorderBuffer): MemoryBufferLoadDispatchResult
    fun acceptCommonDataBus(commonDataBus: CommonDataBus): MemoryBufferQueue
    fun clear(): MemoryBufferQueue
}

sealed interface MemoryBufferOperation

data class LoadMemoryBufferOperation(val operation: LoadOperation) : MemoryBufferOperation

data class StoreMemoryBufferOperation(val operation: StoreOperation) : MemoryBufferOperation

sealed interface MemoryBufferState
data object QueuedForAddressMemoryBufferState : MemoryBufferState
data object AddressInFlightMemoryBufferState : MemoryBufferState
data class AddressReadyMemoryBufferState(val address: DataAddress) : MemoryBufferState

data class MemoryBufferEntry(
    val memoryBufferId: MemoryBufferId,
    val operation: MemoryBufferOperation,
    val baseOperand: Operand,
    val valueOperand: Operand,
    val immediate: Word,
    val robId: RobId,
    val state: MemoryBufferState
)

sealed interface AddressComputationWork

data class LoadAddressComputationWork(
    val memoryBufferId: MemoryBufferId,
    val operation: LoadOperation,
    val baseValue: Word,
    val immediate: Word,
    val robId: RobId
) : AddressComputationWork

data class StoreAddressComputationWork(
    val memoryBufferId: MemoryBufferId,
    val operation: StoreOperation,
    val baseValue: Word,
    val immediate: Word,
    val robId: RobId
) : AddressComputationWork

data class MemoryBufferAddressDispatchResult(
    val memoryBufferQueue: MemoryBufferQueue,
    val workItems: List<AddressComputationWork>
)

data class MemoryBufferLoadWork(
    val memoryBufferId: MemoryBufferId,
    val operation: LoadOperation,
    val address: DataAddress,
    val robId: RobId
)

data class MemoryBufferLoadDispatchResult(
    val memoryBufferQueue: MemoryBufferQueue,
    val workItems: List<MemoryBufferLoadWork>
)

@ConsistentCopyVisibility
data class RealMemoryBufferQueue private constructor(
    private val capacity: Int,
    private val nextMemoryBufferIdValue: Int,
    private val entries: List<MemoryBufferEntry>
) : MemoryBufferQueue {

    constructor(size: Size) : this(size.value, 1, emptyList())

    override fun freeSlotCount() = capacity - entries.size

    override fun entryCount() = entries.size

    override fun enqueue(
        operation: MemoryBufferOperation,
        baseOperand: Operand,
        valueOperand: Operand,
        immediate: Word,
        robId: RobId
    ) =
        when (entries.size >= capacity) {
            true -> MemoryBufferFull.asFailure()
            false ->
                copy(
                    entries = entries + MemoryBufferEntry(
                        MemoryBufferId(nextMemoryBufferIdValue),
                        operation,
                        baseOperand,
                        valueOperand,
                        immediate,
                        robId,
                        QueuedForAddressMemoryBufferState
                    ),
                    nextMemoryBufferIdValue = nextMemoryBufferIdValue + 1
                ).asSuccess()
        }

    override fun dispatchAddressComputations(maxCount: Int): MemoryBufferAddressDispatchResult {
        val dispatchState = entries.fold(
            AddressDispatchState(
                emptySet(),
                emptyList(),
                false
            )
        ) { currentState, entry ->
            currentState.updatedWith(entry, maxCount)
        }

        val dispatchedIds = dispatchState.selectedEntryIds
        val nextEntries = entries.map { entry ->
            when (entry.memoryBufferId in dispatchedIds) {
                true -> entry.copy(state = AddressInFlightMemoryBufferState)
                false -> entry
            }
        }

        return MemoryBufferAddressDispatchResult(copy(entries = nextEntries), dispatchState.workItems)
    }

    override fun recordComputedAddress(memoryBufferId: MemoryBufferId, address: DataAddress) =
        copy(
            entries = entries.map { entry ->
                when (entry.memoryBufferId == memoryBufferId) {
                    true -> entry.copy(state = AddressReadyMemoryBufferState(address))
                    false -> entry
                }
            }
        )

    override fun removeEntry(memoryBufferId: MemoryBufferId) =
        copy(entries = entries.filter { entry -> entry.memoryBufferId != memoryBufferId })

    override fun dispatchMemoryLoads(maxCount: Int, reorderBuffer: ReorderBuffer): MemoryBufferLoadDispatchResult {
        val dispatchState = entries.fold(
            LoadDispatchState(
                emptySet(),
                emptyList(),
                false
            )
        ) { currentState, entry ->
            currentState.updatedWith(entry, maxCount, reorderBuffer)
        }

        return MemoryBufferLoadDispatchResult(
            copy(entries = entries.filter { entry -> entry.memoryBufferId !in dispatchState.selectedIds }),
            dispatchState.workItems
        )
    }

    override fun acceptCommonDataBus(commonDataBus: CommonDataBus) =
        copy(
            entries = entries.map { entry ->
                entry.copy(
                    baseOperand = entry.baseOperand.resolved(commonDataBus),
                    valueOperand = entry.valueOperand.resolved(commonDataBus)
                )
            }
        )

    override fun clear() = copy(entries = emptyList())

    private fun Operand.resolved(commonDataBus: CommonDataBus): Operand =
        when (this) {
            is ReadyOperand -> this
            is PendingOperand ->
                when (commonDataBus.isValueReady(robId)) {
                    true ->
                        commonDataBus.valueFor(robId)
                            .map { value -> ReadyOperand(value) }
                            .recover { this@resolved }

                    false -> this
                }
        }

    private fun AddressDispatchState.updatedWith(
        entry: MemoryBufferEntry,
        maxCount: Int
    ) =
        when (entry.state) {
            QueuedForAddressMemoryBufferState ->
                when (entry.operation) {
                    is StoreMemoryBufferOperation ->
                        when (entry.baseOperand) {
                            is ReadyOperand ->
                                when (workItems.size < maxCount) {
                                    true ->
                                        copy(
                                            selectedEntryIds = selectedEntryIds + entry.memoryBufferId,
                                            workItems = workItems + StoreAddressComputationWork(
                                                entry.memoryBufferId,
                                                entry.operation.operation,
                                                entry.baseOperand.value,
                                                entry.immediate,
                                                entry.robId
                                            )
                                        )

                                    false -> this
                                }

                            is PendingOperand -> copy(unresolvedEarlierStore = true)
                        }

                    is LoadMemoryBufferOperation ->
                        when {
                            unresolvedEarlierStore -> this
                            entry.baseOperand !is ReadyOperand -> this
                            workItems.size >= maxCount -> this
                            else ->
                                copy(
                                    selectedEntryIds = selectedEntryIds + entry.memoryBufferId,
                                    workItems = workItems + LoadAddressComputationWork(
                                        entry.memoryBufferId,
                                        entry.operation.operation,
                                        entry.baseOperand.value,
                                        entry.immediate,
                                        entry.robId
                                    )
                                )
                        }
                }

            AddressInFlightMemoryBufferState,
            is AddressReadyMemoryBufferState -> this
        }

    private fun LoadDispatchState.updatedWith(
        entry: MemoryBufferEntry,
        maxCount: Int,
        reorderBuffer: ReorderBuffer
    ) =
        when (entry.operation) {
            is StoreMemoryBufferOperation ->
                copy(blockedByEarlierStore = true)

            is LoadMemoryBufferOperation ->
                when {
                    workItems.size >= maxCount -> this
                    blockedByEarlierStore -> this
                    entry.state !is AddressReadyMemoryBufferState -> this
                    reorderBuffer.hasEarlierStore(entry.robId, entry.state.address) -> this
                    else ->
                        copy(
                            selectedIds = selectedIds + entry.memoryBufferId,
                            workItems = workItems + MemoryBufferLoadWork(
                                entry.memoryBufferId,
                                entry.operation.operation,
                                entry.state.address,
                                entry.robId
                            )
                        )
                }
        }

    private data class AddressDispatchState(
        val selectedEntryIds: Set<MemoryBufferId>,
        val workItems: List<AddressComputationWork>,
        val unresolvedEarlierStore: Boolean
    )

    private data class LoadDispatchState(
        val selectedIds: Set<MemoryBufferId>,
        val workItems: List<MemoryBufferLoadWork>,
        val blockedByEarlierStore: Boolean
    )
}
