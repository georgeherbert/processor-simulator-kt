package testfixtures

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import strikt.api.Assertion
import strikt.assertions.isA
import types.ProcessorError
import types.ProcessorResult

fun <T : Any> Assertion.Builder<ProcessorResult<T>>.isSuccess() =
    this
        .isA<Success<T>>()
        .get { value }

fun <T : Any> Assertion.Builder<ProcessorResult<T>>.isFailure() =
    this
        .isA<Failure<ProcessorError>>()
        .get { reason }
