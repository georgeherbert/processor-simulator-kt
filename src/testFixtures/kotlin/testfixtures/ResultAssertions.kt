package testfixtures

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
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
