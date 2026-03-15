package commit

import cpu.CommitWidth
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import reorderbuffer.BranchReorderBufferEntry
import reorderbuffer.JumpReorderBufferEntry
import reorderbuffer.RegisterWriteReorderBufferEntry
import reorderbuffer.ReorderBuffer
import reorderbuffer.ReorderBufferCommitHeadResult
import reorderbuffer.ReorderBufferCommitReadyHeadUnavailable
import reorderbuffer.StoreReorderBufferEntry
import types.CommitEntryActualNextInstructionAddressUnavailable
import types.CommitEntryAddressUnavailable
import types.CommitEntryValueUnavailable
import types.DataAddress
import types.InstructionAddress
import types.Operand
import types.PendingOperand
import types.ProcessorResult
import types.ReadyOperand
import types.RobId

data class RealCommitUnit(private val commitWidth: CommitWidth) : CommitUnit {

    override fun nextCycleDelta(reorderBuffer: ReorderBuffer) =
        commitReadyEntries(
            commitWidth.value,
            reorderBuffer,
            CommitCycleDelta.none()
        )

    private fun commitReadyEntries(
        remainingCommitSlots: Int,
        reorderBuffer: ReorderBuffer,
        cycleDelta: CommitCycleDelta
    ): ProcessorResult<CommitCycleDelta> =
        when {
            remainingCommitSlots == 0 -> cycleDelta.asSuccess()
            cycleDelta.controlEvent is RedirectCommitControlEvent -> cycleDelta.asSuccess()
            cycleDelta.halted -> cycleDelta.asSuccess()
            else ->
                when (val headCommitOutcome = reorderBuffer.commitReadyHeadIfPossible()) {
                    ReorderBufferCommitReadyHeadUnavailable -> cycleDelta.asSuccess()
                    is ReorderBufferCommitHeadResult ->
                        nextDeltaFor(headCommitOutcome.entry).flatMap { nextCycleDelta ->
                            commitReadyEntries(
                                remainingCommitSlots - 1,
                                headCommitOutcome.reorderBuffer,
                                cycleDelta.mergedWith(nextCycleDelta)
                            )
                        }
                }
        }

    private fun nextDeltaFor(entry: reorderbuffer.ReorderBufferEntry): ProcessorResult<CommitCycleDelta> =
        when (entry) {
            is RegisterWriteReorderBufferEntry -> registerWriteDeltaFor(entry)
            is JumpReorderBufferEntry -> jumpDeltaFor(entry)
            is BranchReorderBufferEntry -> branchDeltaFor(entry)
            is StoreReorderBufferEntry -> storeDeltaFor(entry)
        }

    private fun registerWriteDeltaFor(
        entry: RegisterWriteReorderBufferEntry
    ) =
        entry.value().map { value ->
            CommitCycleDelta.registerWrite(
                entry.destinationRegisterAddress,
                value,
                entry.robId
            )
        }

    private fun jumpDeltaFor(entry: JumpReorderBufferEntry) =
        entry.actualNextInstructionAddress()
            .flatMap { actualNextInstructionAddress ->
                entry.value().map { value ->
                    CommitCycleDelta.jump(
                        entry.destinationRegisterAddress,
                        value,
                        entry.robId,
                        entry.instructionAddress,
                        actualNextInstructionAddress,
                        controlEventFor(
                            entry.predictedNextInstructionAddress,
                            actualNextInstructionAddress
                        ),
                        actualNextInstructionAddress.value == 0
                    )
                }
            }

    private fun branchDeltaFor(entry: BranchReorderBufferEntry) =
        entry.actualNextInstructionAddress()
            .map { actualNextInstructionAddress ->
                CommitCycleDelta.branch(
                    entry.instructionAddress,
                    actualNextInstructionAddress,
                    controlEventFor(
                        entry.predictedNextInstructionAddress,
                        actualNextInstructionAddress
                    )
                )
            }

    private fun storeDeltaFor(entry: StoreReorderBufferEntry) =
        entry.addressValue()
            .flatMap { address ->
                entry.value().map { value ->
                    CommitCycleDelta.store(
                        entry.operation,
                        address,
                        value
                    )
                }
            }

    private fun controlEventFor(
        predictedNextInstructionAddress: InstructionAddress,
        actualNextInstructionAddress: InstructionAddress
    ) =
        when (predictedNextInstructionAddress == actualNextInstructionAddress) {
            true -> NoCommitControlEvent
            false -> RedirectCommitControlEvent(actualNextInstructionAddress)
        }

    private fun RegisterWriteReorderBufferEntry.value() =
        valueOperand.readyValue(robId)

    private fun JumpReorderBufferEntry.value() =
        valueOperand.readyValue(robId)

    private fun JumpReorderBufferEntry.actualNextInstructionAddress() =
        actualNextInstructionAddress.readyInstructionAddress(robId)

    private fun BranchReorderBufferEntry.actualNextInstructionAddress() =
        actualNextInstructionAddress.readyInstructionAddress(robId)

    private fun StoreReorderBufferEntry.value() =
        valueOperand.readyValue(robId)

    private fun StoreReorderBufferEntry.addressValue() =
        address.readyDataAddress(robId)

    private fun Operand.readyValue(robId: RobId) =
        when (this) {
            is ReadyOperand -> value.asSuccess()
            is PendingOperand -> CommitEntryValueUnavailable(robId).asFailure()
        }

    private fun InstructionAddress?.readyInstructionAddress(robId: RobId) =
        this?.asSuccess()
            ?: CommitEntryActualNextInstructionAddressUnavailable(robId).asFailure()

    private fun DataAddress?.readyDataAddress(robId: RobId) =
        this?.asSuccess()
            ?: CommitEntryAddressUnavailable(robId).asFailure()
}
