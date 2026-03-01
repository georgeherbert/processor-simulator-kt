package types

import dev.forkhandles.result4k.Result

typealias ProcessorResult<T> = Result<T, ProcessorError>

sealed interface ProcessorError

sealed interface CommonDataBusError : ProcessorError
data object CommonDataBusFull : CommonDataBusError
data object CommonDataBusValueNotPresent : CommonDataBusError

sealed interface MainMemoryError : ProcessorError
data class MainMemoryAddressOutOfBounds(val address: Int) : MainMemoryError

sealed interface InstructionQueueError : ProcessorError
data object InstructionQueueFull : InstructionQueueError
data object InstructionQueueEmpty : InstructionQueueError

sealed interface DecoderError : ProcessorError
data class DecoderUnknownOpcode(val opcode: Int) : DecoderError
data class DecoderUnknownFunct3(val opcode: Int, val funct3: Int) : DecoderError
data class DecoderUnknownFunct7(val opcode: Int, val funct3: Int, val funct7: Int) : DecoderError
