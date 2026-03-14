package issue

import arithmeticlogic.ArithmeticLogicOperation
import branchlogic.BranchOperation
import decoder.InstructionDecoder
import instructionqueue.InstructionQueue
import memorybuffer.MemoryBufferQueue
import registerfile.RegisterFile
import reorderbuffer.ReorderBuffer
import reservationstation.ReservationStationBank
import types.ProcessorResult

typealias IssueUnitNextCycleDeltaCallback = (
    InstructionQueue,
    InstructionDecoder,
    RegisterFile,
    ReorderBuffer,
    ReservationStationBank<ArithmeticLogicOperation>,
    ReservationStationBank<BranchOperation>,
    MemoryBufferQueue
) -> ProcessorResult<IssueCycleDelta>

data class CallbackIssueUnitStub(
    private val nextCycleDeltaCallback: IssueUnitNextCycleDeltaCallback
) : IssueUnit {
    override fun nextCycleDelta(
        instructionQueue: InstructionQueue,
        instructionDecoder: InstructionDecoder,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ) =
        nextCycleDeltaCallback(
            instructionQueue,
            instructionDecoder,
            registerFile,
            reorderBuffer,
            arithmeticLogicReservationStations,
            branchReservationStations,
            memoryBufferQueue
        )
}
