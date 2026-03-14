package reorderbuffer

import commondatabus.CommonDataBus
import decoder.StoreOperation
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

data class StubCommitHeadReorderBuffer(
    private val entries: List<ReorderBufferEntry>
) : ReorderBuffer {

    constructor(entry: ReorderBufferEntry) : this(listOf(entry))

    override fun freeSlotCount() = 0

    override fun entryCount() = entries.size

    override fun enqueueRegisterWrite(
        destinationRegisterAddress: RegisterAddress,
        category: RegisterWriteReorderBufferEntryCategory
    ) =
        ReorderBufferFull.asFailure()

    override fun enqueueJump(
        destinationRegisterAddress: RegisterAddress,
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress
    ) =
        ReorderBufferFull.asFailure()

    override fun enqueueBranch(
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress
    ) =
        ReorderBufferFull.asFailure()

    override fun enqueueStore(
        operation: StoreOperation,
        valueOperand: Operand
    ) =
        ReorderBufferFull.asFailure()

    override fun acceptCommonDataBus(commonDataBus: CommonDataBus) = this

    override fun recordStoreAddress(robId: RobId, address: DataAddress) = this

    override fun recordBranchActualNextInstructionAddress(
        robId: RobId,
        actualNextInstructionAddress: InstructionAddress
    ) = this

    override fun hasResolvedValue(robId: RobId) = false

    override fun valueFor(robId: RobId) =
        ReorderBufferValueNotReady(robId).asFailure()

    override fun hasEarlierStore(robId: RobId, address: DataAddress) = false

    override fun commitReadyHead() =
        when (entries.isEmpty()) {
            true -> ReorderBufferEmpty.asFailure()
            false -> ReorderBufferCommitHeadResult(copy(entries = entries.drop(1)), entries.first()).asSuccess()
        }

    override fun clear() = StubCommitHeadReorderBuffer(emptyList())
}
