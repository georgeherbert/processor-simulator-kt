package issue

import arithmeticlogic.ArithmeticLogicOperation
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import reorderbuffer.ArithmeticLogicRegisterWriteReorderBufferEntryCategory
import reorderbuffer.ReorderBufferAllocationResult
import reorderbuffer.ReorderBufferAllocationUnavailable
import reservationstation.ReservationStationEnqueueResult
import reservationstation.ReservationStationEnqueueUnavailable
import types.InstructionAddress
import types.Operand
import types.ProcessorResult
import types.RegisterAddress
import types.RobId
import types.Word

internal data class ArithmeticIssueRequest(
    val operation: ArithmeticLogicOperation,
    val leftOperand: Operand,
    val rightOperand: Operand,
    val instructionAddress: InstructionAddress,
    val destinationRegisterAddress: RegisterAddress
) : IssueRequest {

    override fun applyTo(workingState: IssueWorkingState): ProcessorResult<IssueAttemptOutcome> =
        workingState.reorderBuffer.enqueueRegisterWrite(
            destinationRegisterAddress,
            ArithmeticLogicRegisterWriteReorderBufferEntryCategory
        ).flatMap { allocationOutcome ->
            when (allocationOutcome) {
                ReorderBufferAllocationUnavailable -> IssueBackpressured.asSuccess()
                is ReorderBufferAllocationResult ->
                    workingState.arithmeticLogicReservationStations.enqueue(
                        operation,
                        leftOperand,
                        rightOperand,
                        zeroWord(),
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
                                        arithmeticLogicReservationStations = enqueueOutcome.reservationStationBank,
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
                IssueCycleDelta.RegisterWriteAllocation(
                    destinationRegisterAddress,
                    ArithmeticLogicRegisterWriteReorderBufferEntryCategory
                )
            ),
            listOf(
                IssueCycleDelta.ReservationStationEnqueue(
                    operation,
                    leftOperand,
                    rightOperand,
                    zeroWord(),
                    robId,
                    instructionAddress
                )
            ),
            emptyList(),
            emptyList()
        )
}

private fun zeroWord() = Word(0u)
