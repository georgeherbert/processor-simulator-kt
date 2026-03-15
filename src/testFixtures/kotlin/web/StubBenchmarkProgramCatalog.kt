package web

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.ProcessorError
import types.TemporaryPathCreateFailed
import java.nio.file.Path

data class StubBenchmarkProgramCatalog(
    private val programsToReturn: List<BenchmarkProgram>,
    private val materializedSourcePath: Path?,
    private val sourceCodeToReturn: String?,
    private val failure: ProcessorError?
) : BenchmarkProgramCatalog {

    constructor(programsToReturn: List<BenchmarkProgram>, materializedSourcePath: Path) : this(
        programsToReturn,
        materializedSourcePath,
        DEFAULT_SOURCE_CODE,
        null
    )

    constructor(programsToReturn: List<BenchmarkProgram>, failure: ProcessorError) : this(
        programsToReturn,
        null,
        null,
        failure
    )

    override fun programs() = programsToReturn

    override fun source(programName: String) =
        when {
            failure != null -> failure.asFailure()
            sourceCodeToReturn != null -> sourceCodeToReturn.asSuccess()
            else -> TemporaryPathCreateFailed(programName).asFailure()
        }

    override fun materializeSource(programName: String, targetDirectory: Path) =
        when {
            failure != null -> failure.asFailure()
            materializedSourcePath != null -> materializedSourcePath.asSuccess()
            else -> TemporaryPathCreateFailed(targetDirectory.toString()).asFailure()
        }

    private companion object {
        const val DEFAULT_SOURCE_CODE = "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"
    }
}
