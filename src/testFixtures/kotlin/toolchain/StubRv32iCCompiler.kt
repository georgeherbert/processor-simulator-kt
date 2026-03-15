package toolchain

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.ProcessorError
import types.TemporaryPathCreateFailed
import java.nio.file.Path

data class StubRv32iCCompiler(
    private val compiledBinaryPath: Path?,
    private val failure: ProcessorError?
) : Rv32iCCompiler {

    constructor(compiledBinaryPath: Path) : this(compiledBinaryPath, null)

    constructor(failure: ProcessorError) : this(null, failure)

    override fun compile(cSourceFilePath: Path, outputDirectory: Path) =
        when {
            failure != null -> failure.asFailure()
            compiledBinaryPath != null -> compiledBinaryPath.asSuccess()
            else -> TemporaryPathCreateFailed(outputDirectory.toString()).asFailure()
        }
}
