package web

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.ProcessorError
import types.TemporaryPathCreateFailed
import java.nio.file.Path

data class StubTemporaryDirectoryFactory(
    private val directory: Path?,
    private val failure: ProcessorError?
) : TemporaryDirectoryFactory {

    constructor(directory: Path) : this(directory, null)

    constructor(failure: ProcessorError) : this(null, failure)

    override fun create(prefix: String) =
        when {
            failure != null -> failure.asFailure()
            directory != null -> directory.asSuccess()
            else -> TemporaryPathCreateFailed(prefix).asFailure()
        }
}
