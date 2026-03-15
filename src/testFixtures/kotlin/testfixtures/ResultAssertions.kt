package testfixtures

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import memorybuffer.MemoryBufferEnqueueOutcome
import memorybuffer.MemoryBufferEnqueueResult
import reorderbuffer.ReorderBufferAllocationOutcome
import reorderbuffer.ReorderBufferAllocationResult
import reservationstation.ReservationStationEnqueueOutcome
import reservationstation.ReservationStationEnqueueResult
import strikt.api.Assertion
import strikt.assertions.isA
import types.ProcessorError
import types.ProcessorResult

fun <T : Any, E : ProcessorError> Assertion.Builder<Result4k<T, E>>.isSuccess() =
    this
        .isA<Success<T>>()
        .get { value }

fun <T : Any, E : ProcessorError> Assertion.Builder<Result4k<T, E>>.isFailure() =
    this
        .isA<Failure<ProcessorError>>()
        .get { reason }

fun Assertion.Builder<ReorderBufferAllocationOutcome>.isAllocationResult() =
    this
        .isA<ReorderBufferAllocationResult>()

fun <T> Assertion.Builder<ReservationStationEnqueueOutcome<T>>.isReservationStationEnqueueResult() =
    this
        .isA<ReservationStationEnqueueResult<T>>()
        .get { reservationStationBank }

fun Assertion.Builder<MemoryBufferEnqueueOutcome>.isMemoryBufferEnqueueResult() =
    this
        .isA<MemoryBufferEnqueueResult>()
        .get { memoryBufferQueue }
