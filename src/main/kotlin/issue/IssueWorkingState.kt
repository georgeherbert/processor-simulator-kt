package issue

import arithmeticlogic.ArithmeticLogicOperation
import branchlogic.BranchOperation
import memorybuffer.MemoryBufferQueue
import registerfile.RegisterFile
import reorderbuffer.ReorderBuffer
import reservationstation.ReservationStationBank

internal data class IssueWorkingState(
    val registerFile: RegisterFile,
    val reorderBuffer: ReorderBuffer,
    val arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
    val branchReservationStations: ReservationStationBank<BranchOperation>,
    val memoryBufferQueue: MemoryBufferQueue,
    val cycleChanges: IssueCycleDelta
)
