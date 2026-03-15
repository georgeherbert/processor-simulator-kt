package issue

import arithmeticlogic.ArithmeticLogicOperation
import cpu.IssueWidth
import decoder.DecodedInstruction
import decoder.InstructionDecoder
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import instructionqueue.InstructionQueue
import instructionqueue.InstructionQueueDequeueResult
import instructionqueue.InstructionQueueDequeueUnavailable
import memorybuffer.MemoryBufferQueue
import registerfile.RegisterFile
import reorderbuffer.ReorderBuffer
import reservationstation.ReservationStationBank
import types.InstructionAddress
import types.ProcessorResult
import types.Word

data class RealIssueUnit(private val issueWidth: IssueWidth) : IssueUnit {

    override fun nextCycleDelta(
        instructionQueue: InstructionQueue,
        instructionDecoder: InstructionDecoder,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<branchlogic.BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ) =
        issueInstructions(
            issueWidth.value,
            instructionQueue,
            instructionDecoder,
            IssueWorkingState(
                registerFile,
                reorderBuffer,
                arithmeticLogicReservationStations,
                branchReservationStations,
                memoryBufferQueue,
                IssueCycleDelta.none()
            )
        )

    private fun issueInstructions(
        remainingIssueSlots: Int,
        instructionQueue: InstructionQueue,
        instructionDecoder: InstructionDecoder,
        workingState: IssueWorkingState
    ): ProcessorResult<IssueCycleDelta> =
        when {
            remainingIssueSlots == 0 -> workingState.cycleChanges.asSuccess()
            else ->
                when (val dequeueOutcome = instructionQueue.dequeueIfPresent()) {
                    InstructionQueueDequeueUnavailable -> workingState.cycleChanges.asSuccess()
                    is InstructionQueueDequeueResult ->
                        issueDequeuedInstruction(
                            remainingIssueSlots,
                            dequeueOutcome,
                            instructionDecoder,
                            workingState
                        )
                }
        }

    private fun issueDequeuedInstruction(
        remainingIssueSlots: Int,
        dequeueResult: InstructionQueueDequeueResult,
        instructionDecoder: InstructionDecoder,
        workingState: IssueWorkingState
    ) =
        instructionDecoder.decode(
            dequeueResult.entry.instruction,
            dequeueResult.entry.instructionAddress.asWord(),
            dequeueResult.entry.predictedNextInstructionAddress.asWord()
        ).flatMap { decodedInstruction ->
            issueDecodedInstruction(decodedInstruction, workingState)
                .flatMap { attemptOutcome ->
                    when (attemptOutcome) {
                        IssueBackpressured -> workingState.cycleChanges.asSuccess()
                        is IssueApplied ->
                            issueInstructions(
                                remainingIssueSlots - 1,
                                dequeueResult.instructionQueue,
                                instructionDecoder,
                                attemptOutcome.workingState
                            )
                    }
                }
        }

    private fun issueDecodedInstruction(
        decodedInstruction: DecodedInstruction,
        workingState: IssueWorkingState
    ): ProcessorResult<IssueAttemptOutcome> =
        decodedInstruction.toIssueRequest(workingState).applyTo(workingState)

    private fun InstructionAddress.asWord() = Word(value.toUInt())
}
