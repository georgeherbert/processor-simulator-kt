package web

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.ProcessorResult
import types.TemporaryPathCreateFailed
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

interface TemporaryDirectoryFactory {
    fun create(prefix: String): ProcessorResult<Path>
}

data object RealTemporaryDirectoryFactory : TemporaryDirectoryFactory {
    override fun create(prefix: String) =
        try {
            Files.createTempDirectory(prefix).asSuccess()
        } catch (_: IOException) {
            TemporaryPathCreateFailed(prefix).asFailure()
        }
}
