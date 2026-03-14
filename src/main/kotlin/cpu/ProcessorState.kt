package cpu

import address.AddressUnitSet
import arithmeticlogic.ArithmeticLogicOperation
import arithmeticlogic.ArithmeticLogicUnitSet
import branchlogic.BranchOperation
import branchlogic.BranchUnitSet
import branchpredictor.DynamicBranchTargetPredictor
import commondatabus.CommonDataBus
import control.ControlState
import instructionqueue.InstructionQueue
import mainmemory.MainMemory
import memoryaccess.MemoryUnitSet
import memorybuffer.MemoryBufferQueue
import registerfile.RegisterFile
import reorderbuffer.ReorderBuffer
import reservationstation.ReservationStationBank

data class ProcessorState(
    val controlState: ControlState,
    val statistics: ProcessorStatistics,
    val halted: Boolean,
    val branchTargetPredictor: DynamicBranchTargetPredictor,
    val commonDataBus: CommonDataBus,
    val mainMemory: MainMemory,
    val instructionQueue: InstructionQueue,
    val registerFile: RegisterFile,
    val reorderBuffer: ReorderBuffer,
    val arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
    val branchReservationStations: ReservationStationBank<BranchOperation>,
    val memoryBufferQueue: MemoryBufferQueue,
    val arithmeticLogicUnitSet: ArithmeticLogicUnitSet,
    val branchUnitSet: BranchUnitSet,
    val addressUnitSet: AddressUnitSet,
    val memoryUnitSet: MemoryUnitSet
)
