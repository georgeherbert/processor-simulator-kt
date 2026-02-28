package types

import dev.forkhandles.result4k.Result

typealias ProcessorResult<T> = Result<T, ProcessorError>

sealed interface ProcessorError

sealed interface CommonDataBusError : ProcessorError
data object CommonDataBusFull : CommonDataBusError
data object CommonDataBusValueNotPresent : CommonDataBusError

sealed interface MainMemoryError : ProcessorError
data class MainMemoryAddressOutOfBounds(val address: Int) : MainMemoryError
