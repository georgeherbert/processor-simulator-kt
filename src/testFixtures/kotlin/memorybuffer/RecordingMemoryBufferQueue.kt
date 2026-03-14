package memorybuffer

import commondatabus.CommonDataBus
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import reorderbuffer.ReorderBuffer
import types.*

data class MemoryBufferEnqueueCall(
    val operation: MemoryBufferOperation,
    val baseOperand: Operand,
    val valueOperand: Operand,
    val immediate: Word,
    val robId: RobId
)

@ConsistentCopyVisibility
data class RecordingMemoryBufferQueue private constructor(
    private val enqueueFailure: ProcessorError?,
    val enqueueCalls: List<MemoryBufferEnqueueCall>
) : MemoryBufferQueue {

    constructor() : this(null, emptyList())

    constructor(enqueueFailure: ProcessorError) : this(enqueueFailure, emptyList())

    override fun freeSlotCount() = Int.MAX_VALUE

    override fun entryCount() = enqueueCalls.size

    override fun enqueue(
        operation: MemoryBufferOperation,
        baseOperand: Operand,
        valueOperand: Operand,
        immediate: Word,
        robId: RobId
    ) =
        enqueueFailure
            ?.asFailure()
            ?: copy(
                enqueueCalls = enqueueCalls + MemoryBufferEnqueueCall(
                    operation,
                    baseOperand,
                    valueOperand,
                    immediate,
                    robId
                )
            ).asSuccess()

    override fun dispatchAddressComputations(maxCount: Int) =
        MemoryBufferAddressDispatchResult(
            this,
            enqueueCalls
                .mapIndexedNotNull { index, enqueueCall ->
                    enqueueCall.toAddressComputationWorkOrNull(index + 1)
                }
                .take(maxCount)
        )

    override fun recordComputedAddress(memoryBufferId: MemoryBufferId, address: DataAddress) = this

    override fun removeEntry(memoryBufferId: MemoryBufferId) = this

    override fun dispatchMemoryLoads(maxCount: Int, reorderBuffer: ReorderBuffer) =
        MemoryBufferLoadDispatchResult(this, emptyList())

    override fun acceptCommonDataBus(commonDataBus: CommonDataBus) =
        copy(
            enqueueCalls = enqueueCalls.map { enqueueCall ->
                enqueueCall.copy(
                    baseOperand = enqueueCall.baseOperand.resolved(commonDataBus),
                    valueOperand = enqueueCall.valueOperand.resolved(commonDataBus)
                )
            }
        )

    override fun clear() = RecordingMemoryBufferQueue()

    private fun MemoryBufferEnqueueCall.toAddressComputationWorkOrNull(memoryBufferIdValue: Int) =
        when (operation) {
            is LoadMemoryBufferOperation ->
                when (baseOperand) {
                    is ReadyOperand ->
                        LoadAddressComputationWork(
                            MemoryBufferId(memoryBufferIdValue),
                            operation.operation,
                            baseOperand.value,
                            immediate,
                            robId
                        )

                    is PendingOperand -> null
                }

            is StoreMemoryBufferOperation ->
                when (baseOperand) {
                    is ReadyOperand ->
                        StoreAddressComputationWork(
                            MemoryBufferId(memoryBufferIdValue),
                            operation.operation,
                            baseOperand.value,
                            immediate,
                            robId
                        )

                    is PendingOperand -> null
                }
        }

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
}
