package reorderbuffer

import commondatabus.CommonDataBus
import decoder.StoreOperation
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

interface ReorderBuffer {
    fun freeSlotCount(): Int
    fun entryCount(): Int
    fun enqueueRegisterWrite(
        destinationRegisterAddress: RegisterAddress,
        category: RegisterWriteReorderBufferEntryCategory
    ): ProcessorResult<ReorderBufferAllocationResult>

    fun enqueueJump(
        destinationRegisterAddress: RegisterAddress,
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress
    ): ProcessorResult<ReorderBufferAllocationResult>

    fun enqueueBranch(
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress
    ): ProcessorResult<ReorderBufferAllocationResult>

    fun enqueueStore(
        operation: StoreOperation,
        valueOperand: Operand
    ): ProcessorResult<ReorderBufferAllocationResult>

    fun acceptCommonDataBus(commonDataBus: CommonDataBus): ReorderBuffer
    fun recordStoreAddress(robId: RobId, address: DataAddress): ReorderBuffer
    fun recordBranchActualNextInstructionAddress(
        robId: RobId,
        actualNextInstructionAddress: InstructionAddress
    ): ReorderBuffer

    fun resolveOperand(operand: PendingOperand): Operand
    fun hasResolvedValue(robId: RobId): Boolean
    fun valueFor(robId: RobId): ProcessorResult<Word>
    fun hasEarlierStore(robId: RobId, address: DataAddress): Boolean
    fun commitReadyHeadIfPossible(): ReorderBufferCommitReadyHeadOutcome
    fun commitReadyHead(): ProcessorResult<ReorderBufferCommitHeadResult>
    fun clear(): ReorderBuffer
}

data class ReorderBufferAllocationResult(
    val reorderBuffer: ReorderBuffer,
    val robId: RobId
)

data class ReorderBufferCommitHeadResult(
    val reorderBuffer: ReorderBuffer,
    val entry: ReorderBufferEntry
) : ReorderBufferCommitReadyHeadOutcome

sealed interface ReorderBufferCommitReadyHeadOutcome

data object ReorderBufferCommitReadyHeadUnavailable : ReorderBufferCommitReadyHeadOutcome

sealed interface ReorderBufferEntry {
    val robId: RobId
    fun isReady(): Boolean
}

sealed interface RegisterWriteReorderBufferEntryCategory
data object ArithmeticLogicRegisterWriteReorderBufferEntryCategory : RegisterWriteReorderBufferEntryCategory
data object LoadRegisterWriteReorderBufferEntryCategory : RegisterWriteReorderBufferEntryCategory

data class RegisterWriteReorderBufferEntry(
    override val robId: RobId,
    val destinationRegisterAddress: RegisterAddress,
    val category: RegisterWriteReorderBufferEntryCategory,
    val valueOperand: Operand
) : ReorderBufferEntry {
    override fun isReady() = valueOperand is ReadyOperand
}

data class JumpReorderBufferEntry(
    override val robId: RobId,
    val destinationRegisterAddress: RegisterAddress,
    val instructionAddress: InstructionAddress,
    val predictedNextInstructionAddress: InstructionAddress,
    val actualNextInstructionAddress: InstructionAddress?,
    val valueOperand: Operand
) : ReorderBufferEntry {
    override fun isReady() =
        valueOperand is ReadyOperand && actualNextInstructionAddress != null
}

data class BranchReorderBufferEntry(
    override val robId: RobId,
    val instructionAddress: InstructionAddress,
    val predictedNextInstructionAddress: InstructionAddress,
    val actualNextInstructionAddress: InstructionAddress?
) : ReorderBufferEntry {
    override fun isReady() = actualNextInstructionAddress != null
}

data class StoreReorderBufferEntry(
    override val robId: RobId,
    val operation: StoreOperation,
    val address: DataAddress?,
    val valueOperand: Operand
) : ReorderBufferEntry {
    override fun isReady() =
        address != null && valueOperand is ReadyOperand
}

@ConsistentCopyVisibility
data class RealReorderBuffer private constructor(
    private val capacity: Int,
    private val nextRobIdValue: Int,
    private val entries: List<ReorderBufferEntry>
) : ReorderBuffer {

    constructor(size: Size) : this(size.value, 1, emptyList())

    override fun freeSlotCount() = capacity - entries.size

    override fun entryCount() = entries.size

    override fun enqueueRegisterWrite(
        destinationRegisterAddress: RegisterAddress,
        category: RegisterWriteReorderBufferEntryCategory
    ) =
        allocate(RegisterWriteReorderBufferEntry(nextRobId(), destinationRegisterAddress, category, PendingOperand(nextRobId())))

    override fun enqueueJump(
        destinationRegisterAddress: RegisterAddress,
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress
    ) =
        allocate(
            JumpReorderBufferEntry(
                nextRobId(),
                destinationRegisterAddress,
                instructionAddress,
                predictedNextInstructionAddress,
                null,
                PendingOperand(nextRobId())
            )
        )

    override fun enqueueBranch(
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress
    ) =
        allocate(BranchReorderBufferEntry(nextRobId(), instructionAddress, predictedNextInstructionAddress, null))

    override fun enqueueStore(
        operation: StoreOperation,
        valueOperand: Operand
    ) =
        allocate(StoreReorderBufferEntry(nextRobId(), operation, null, valueOperand))

    override fun acceptCommonDataBus(commonDataBus: CommonDataBus) =
        copy(entries = entries.map { entry -> entry.withResolvedOperands(commonDataBus) })

    override fun recordStoreAddress(robId: RobId, address: DataAddress) =
        copy(
            entries = entries.map { entry ->
                when (entry) {
                    is StoreReorderBufferEntry ->
                        when (entry.robId == robId) {
                            true -> entry.copy(address = address)
                            false -> entry
                        }

                    else -> entry
                }
            }
        )

    override fun recordBranchActualNextInstructionAddress(
        robId: RobId,
        actualNextInstructionAddress: InstructionAddress
    ) =
        copy(
            entries = entries.map { entry ->
                when (entry) {
                    is JumpReorderBufferEntry ->
                        when (entry.robId == robId) {
                            true -> entry.copy(actualNextInstructionAddress = actualNextInstructionAddress)
                            false -> entry
                        }

                    is BranchReorderBufferEntry ->
                        when (entry.robId == robId) {
                            true -> entry.copy(actualNextInstructionAddress = actualNextInstructionAddress)
                            false -> entry
                        }

                    else -> entry
                }
            }
        )

    override fun resolveOperand(operand: PendingOperand) =
        when (val resolvedValue = entryFor(operand.robId)?.resolvedValue()) {
            null -> operand
            else -> ReadyOperand(resolvedValue)
        }

    override fun hasResolvedValue(robId: RobId) =
        entryFor(robId)?.resolvedValue() != null

    override fun valueFor(robId: RobId) =
        when (val resolvedValue = entryFor(robId)?.resolvedValue()) {
            null -> ReorderBufferValueNotReady(robId).asFailure()
            else -> resolvedValue.asSuccess()
        }

    override fun hasEarlierStore(robId: RobId, address: DataAddress) =
        entries
            .takeWhile { entry -> entry.robId != robId }
            .filterIsInstance<StoreReorderBufferEntry>()
            .any { entry -> entry.address == address }

    override fun commitReadyHeadIfPossible() =
        when {
            entries.isEmpty() -> ReorderBufferCommitReadyHeadUnavailable
            !entries.first().isReady() -> ReorderBufferCommitReadyHeadUnavailable
            else ->
                ReorderBufferCommitHeadResult(
                    copy(entries = entries.drop(1)),
                    entries.first()
                )
        }

    override fun commitReadyHead() =
        when (val commitReadyHeadOutcome = commitReadyHeadIfPossible()) {
            is ReorderBufferCommitHeadResult -> commitReadyHeadOutcome.asSuccess()
            ReorderBufferCommitReadyHeadUnavailable ->
                when {
                    entries.isEmpty() -> ReorderBufferEmpty.asFailure()
                    else -> ReorderBufferHeadNotReady.asFailure()
                }
        }

    override fun clear() = copy(entries = emptyList())

    private fun allocate(entry: ReorderBufferEntry) =
        when (entries.size >= capacity) {
            true -> ReorderBufferFull.asFailure()
            false -> ReorderBufferAllocationResult(copy(entries = entries + entry, nextRobIdValue = nextRobIdValue + 1), entry.robId).asSuccess()
        }

    private fun nextRobId() = RobId(nextRobIdValue)

    private fun entryFor(robId: RobId) =
        entries.firstOrNull { entry -> entry.robId == robId }

    private fun ReorderBufferEntry.withResolvedOperands(commonDataBus: CommonDataBus): ReorderBufferEntry =
        when (this) {
            is RegisterWriteReorderBufferEntry -> copy(valueOperand = valueOperand.resolved(commonDataBus))
            is JumpReorderBufferEntry -> copy(valueOperand = valueOperand.resolved(commonDataBus))
            is StoreReorderBufferEntry -> copy(valueOperand = valueOperand.resolved(commonDataBus))
            is BranchReorderBufferEntry -> this
        }

    private fun ReorderBufferEntry.resolvedValue() =
        when (this) {
            is RegisterWriteReorderBufferEntry ->
                when (valueOperand) {
                    is ReadyOperand -> valueOperand.value
                    is PendingOperand -> null
                }

            is JumpReorderBufferEntry ->
                when (valueOperand) {
                    is ReadyOperand -> valueOperand.value
                    is PendingOperand -> null
                }

            is BranchReorderBufferEntry -> null
            is StoreReorderBufferEntry -> null
        }

    private fun Operand.resolved(commonDataBus: CommonDataBus): Operand =
        when (this) {
            is ReadyOperand -> this
            is PendingOperand -> commonDataBus.resolveOperand(this)
        }
}
