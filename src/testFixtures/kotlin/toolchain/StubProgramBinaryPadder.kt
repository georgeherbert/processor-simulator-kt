package toolchain

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import java.nio.file.Path
import types.ProcessorError
import types.ProgramBinaryFileWriteFailed

data class StubProgramBinaryPadder(
    private val paddedBinaryPath: Path?,
    private val failure: ProcessorError?
) : ProgramBinaryPadder {

    constructor(paddedBinaryPath: Path) : this(paddedBinaryPath, null)

    constructor(failure: ProcessorError) : this(null, failure)

    override fun pad(binaryFilePath: Path, paddedBinaryFilePath: Path) =
        when {
            failure != null -> failure.asFailure()
            paddedBinaryPath != null -> paddedBinaryPath.asSuccess()
            else -> ProgramBinaryFileWriteFailed(paddedBinaryFilePath.toString()).asFailure()
        }
}
