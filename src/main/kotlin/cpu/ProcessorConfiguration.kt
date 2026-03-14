package cpu

import fetch.FetchWidth
import types.BitWidth
import types.CycleCount
import types.Size

data class ProcessorConfiguration(
    val fetchWidth: FetchWidth,
    val issueWidth: IssueWidth,
    val commitWidth: CommitWidth,
    val instructionQueueSize: Size,
    val reorderBufferSize: Size,
    val branchTargetBufferSize: Size,
    val branchOutcomeCounterBitWidth: BitWidth,
    val arithmeticLogicReservationStationCount: Size,
    val branchReservationStationCount: Size,
    val memoryBufferCount: Size,
    val arithmeticLogicUnitCount: Size,
    val branchUnitCount: Size,
    val addressUnitCount: Size,
    val memoryUnitCount: Size,
    val arithmeticLogicLatencies: ArithmeticLogicLatencies,
    val branchLatencies: BranchLatencies,
    val addressLatency: CycleCount,
    val memoryLatencies: MemoryLatencies
)

data class ArithmeticLogicLatencies(
    val add: CycleCount,
    val loadUpperImmediate: CycleCount,
    val addUpperImmediateToProgramCounter: CycleCount,
    val subtract: CycleCount,
    val shiftLeftLogical: CycleCount,
    val setLessThanSigned: CycleCount,
    val setLessThanUnsigned: CycleCount,
    val exclusiveOr: CycleCount,
    val shiftRightLogical: CycleCount,
    val shiftRightArithmetic: CycleCount,
    val or: CycleCount,
    val and: CycleCount
)

data class BranchLatencies(
    val jumpAndLink: CycleCount,
    val jumpAndLinkRegister: CycleCount,
    val branchEqual: CycleCount,
    val branchNotEqual: CycleCount,
    val branchLessThanSigned: CycleCount,
    val branchLessThanUnsigned: CycleCount,
    val branchGreaterThanOrEqualSigned: CycleCount,
    val branchGreaterThanOrEqualUnsigned: CycleCount
)

data class MemoryLatencies(
    val loadWord: CycleCount,
    val loadHalfWord: CycleCount,
    val loadHalfWordUnsigned: CycleCount,
    val loadByte: CycleCount,
    val loadByteUnsigned: CycleCount
)
