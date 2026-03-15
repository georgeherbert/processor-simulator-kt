package issue

import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import reorderbuffer.ReorderBufferAllocationResult
import reorderbuffer.ReorderBufferAllocationUnavailable
import reservationstation.ReservationStationEnqueueResult
import reservationstation.ReservationStationEnqueueUnavailable
import types.InstructionAddress
import types.Operand
import types.ProcessorResult
import types.ReadyOperand
import types.RegisterAddress
import types.RobId
import types.Word

internal data class JumpIssueRequest(
    val operation: branchlogic.BranchOperation,
    val leftOperand: Operand,
    val immediate: Word,
    val instructionAddress: InstructionAddress,
    val predictedNextInstructionAddress: InstructionAddress,
    val destinationRegisterAddress: RegisterAddress
) : IssueRequest {

    override fun applyTo(workingState: IssueWorkingState): ProcessorResult<IssueAttemptOutcome> =
        workingState.reorderBuffer.enqueueJump(
            destinationRegisterAddress,
            instructionAddress,
            predictedNextInstructionAddress
        ).flatMap { allocationOutcome ->
            when (allocationOutcome) {
                ReorderBufferAllocationUnavailable -> IssueBackpressured.asSuccess()
                is ReorderBufferAllocationResult ->
                    workingState.branchReservationStations.enqueue(
                        operation,
                        leftOperand,
                        ReadyOperand(zeroWord()),
                        immediate,
                        allocationOutcome.robId,
                        instructionAddress
                    ).map { enqueueOutcome ->
                        when (enqueueOutcome) {
                            ReservationStationEnqueueUnavailable -> IssueBackpressured
                            is ReservationStationEnqueueResult ->
                                IssueApplied(
                                    workingState.copy(
                                        registerFile = workingState.registerFile.reserveDestination(
                                            destinationRegisterAddress,
                                            allocationOutcome.robId
                                        ),
                                        reorderBuffer = allocationOutcome.reorderBuffer,
                                        branchReservationStations = enqueueOutcome.reservationStationBank,
                                        cycleChanges = workingState.cycleChanges.mergedWith(cycleDeltaFor(allocationOutcome.robId))
                                    )
                                )
                        }
                    }
            }
        }

    private fun cycleDeltaFor(robId: RobId) =
        IssueCycleDelta(
            1,
            listOf(IssueCycleDelta.IssueRegisterReservation(destinationRegisterAddress, robId)),
            listOf(
                IssueCycleDelta.JumpAllocation(
                    destinationRegisterAddress,
                    instructionAddress,
                    predictedNextInstructionAddress
                )
            ),
            emptyList(),
            listOf(
                IssueCycleDelta.ReservationStationEnqueue(
                    operation,
                    leftOperand,
                    ReadyOperand(zeroWord()),
                    immediate,
                    robId,
                    instructionAddress
                )
            ),
            emptyList()
        )
}

internal data class BranchIssueRequest(
    val operation: branchlogic.BranchOperation,
    val leftOperand: Operand,
    val rightOperand: Operand,
    val immediate: Word,
    val instructionAddress: InstructionAddress,
    val predictedNextInstructionAddress: InstructionAddress
) : IssueRequest {

    override fun applyTo(workingState: IssueWorkingState): ProcessorResult<IssueAttemptOutcome> =
        workingState.reorderBuffer.enqueueBranch(
            instructionAddress,
            predictedNextInstructionAddress
        ).flatMap { allocationOutcome ->
            when (allocationOutcome) {
                ReorderBufferAllocationUnavailable -> IssueBackpressured.asSuccess()
                is ReorderBufferAllocationResult ->
                    workingState.branchReservationStations.enqueue(
                        operation,
                        leftOperand,
                        rightOperand,
                        immediate,
                        allocationOutcome.robId,
                        instructionAddress
                    ).map { enqueueOutcome ->
                        when (enqueueOutcome) {
                            ReservationStationEnqueueUnavailable -> IssueBackpressured
                            is ReservationStationEnqueueResult ->
                                IssueApplied(
                                    workingState.copy(
                                        reorderBuffer = allocationOutcome.reorderBuffer,
                                        branchReservationStations = enqueueOutcome.reservationStationBank,
                                        cycleChanges = workingState.cycleChanges.mergedWith(cycleDeltaFor(allocationOutcome.robId))
                                    )
                                )
                        }
                    }
            }
        }

    private fun cycleDeltaFor(robId: RobId) =
        IssueCycleDelta(
            1,
            emptyList(),
            listOf(
                IssueCycleDelta.BranchAllocation(
                    instructionAddress,
                    predictedNextInstructionAddress
                )
            ),
            emptyList(),
            listOf(
                IssueCycleDelta.ReservationStationEnqueue(
                    operation,
                    leftOperand,
                    rightOperand,
                    immediate,
                    robId,
                    instructionAddress
                )
            ),
            emptyList()
        )
}

private fun zeroWord() = Word(0u)
