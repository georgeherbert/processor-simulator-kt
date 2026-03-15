package types

import dev.forkhandles.result4k.Result

typealias ProcessorResult<T> = Result<T, ProcessorError>

sealed interface ProcessorError

sealed interface ValidationError : ProcessorError
data class BranchTargetBufferSizeInvalid(val size: Int) : ValidationError
data class BranchOutcomeBufferSizeInvalid(val size: Int) : ValidationError
data class BranchOutcomePredictorBitWidthInvalid(val bitWidth: Int) : ValidationError

sealed interface CommonDataBusError : ProcessorError
data object CommonDataBusFull : CommonDataBusError

sealed interface MainMemoryError : ProcessorError
data class MainMemoryAddressOutOfBounds(val address: Int) : MainMemoryError
data class MainMemoryProgramFileNotFound(val filePath: String) : MainMemoryError
data class MainMemoryProgramFileReadFailed(val filePath: String) : MainMemoryError
data class MainMemoryProgramFileEmpty(val filePath: String) : MainMemoryError
data class MainMemoryProgramFileTooLarge(
    val filePath: String,
    val byteCount: Int,
    val capacityByteCount: Int
) : MainMemoryError

sealed interface InstructionQueueError : ProcessorError
data object InstructionQueueFull : InstructionQueueError
data object InstructionQueueEmpty : InstructionQueueError
data class InstructionQueueSlotCountInvalid(val count: Int) : InstructionQueueError

sealed interface ReorderBufferError : ProcessorError
data object ReorderBufferFull : ReorderBufferError
data object ReorderBufferEmpty : ReorderBufferError
data object ReorderBufferHeadNotReady : ReorderBufferError

sealed interface ReservationStationError : ProcessorError
data object ReservationStationFull : ReservationStationError

sealed interface MemoryBufferError : ProcessorError
data object MemoryBufferFull : MemoryBufferError

sealed interface CommitError : ProcessorError
data class CommitEntryValueUnavailable(val robId: RobId) : CommitError
data class CommitEntryAddressUnavailable(val robId: RobId) : CommitError
data class CommitEntryActualNextInstructionAddressUnavailable(val robId: RobId) : CommitError

sealed interface ProcessorExecutionError : ProcessorError
data object ProcessorAlreadyHalted : ProcessorExecutionError
data class ProcessorCycleLimitExceeded(val maxCycleCount: Int) : ProcessorExecutionError

sealed interface SimulationError : ProcessorError
data class SimulationCycleCountInvalid(val cycleCount: Int) : SimulationError
data class BenchmarkProgramNotFound(val programName: String) : SimulationError
data class BenchmarkProgramResourceMissing(val programName: String) : SimulationError
data class BenchmarkProgramMaterializationFailed(
    val programName: String,
    val targetPath: String
) : SimulationError
data class BenchmarkProgramSourceReadFailed(val programName: String) : SimulationError
data class ExperimentRangeInvalid(
    val startValue: Int,
    val endValue: Int,
    val increment: Int
) : SimulationError
data class ExperimentConfigurationValueInvalid(
    val fieldName: String,
    val value: Int
) : SimulationError

sealed interface ToolchainError : ProcessorError
data class ProgramBinaryFileNotFound(val filePath: String) : ToolchainError
data class ProgramBinaryFileReadFailed(val filePath: String) : ToolchainError
data class ProgramBinaryFileWriteFailed(val filePath: String) : ToolchainError
data class ExternalToolExecutionFailed(
    val executable: String,
    val exitCode: Int,
    val standardError: String
) : ToolchainError
data class ExternalToolProcessLaunchFailed(val executable: String) : ToolchainError
data class ExternalToolExecutionInterrupted(val executable: String) : ToolchainError
data class TemporaryPathCreateFailed(val pathDescription: String) : ToolchainError

sealed interface DecoderError : ProcessorError
data class DecoderUnknownOpcode(val opcode: Int) : DecoderError
data class DecoderUnknownFunct3(val opcode: Int, val funct3: Int) : DecoderError
data class DecoderUnknownFunct7(val opcode: Int, val funct3: Int, val funct7: Int) : DecoderError
