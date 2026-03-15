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

interface IssueUnit {
    fun nextCycleDelta(
        instructionQueue: InstructionQueue,
        instructionDecoder: InstructionDecoder,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ): ProcessorResult<IssueCycleDelta>
}
