package web

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import kotlinx.serialization.Serializable
import types.BenchmarkProgramMaterializationFailed
import types.BenchmarkProgramNotFound
import types.BenchmarkProgramResourceMissing
import types.BenchmarkProgramSourceReadFailed
import types.ProcessorResult
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

interface BenchmarkProgramCatalog {
    fun programs(): List<BenchmarkProgram>
    fun source(programName: String): ProcessorResult<String>
    fun materializeSource(programName: String, targetDirectory: Path): ProcessorResult<Path>
}

@Serializable
data class BenchmarkProgram(
    val name: String,
    val label: String
)

data object RealBenchmarkProgramCatalog : BenchmarkProgramCatalog {

    private val programs = listOf(
        BenchmarkProgram("arithmetic.c", "Arithmetic"),
        BenchmarkProgram("bubble_sort.c", "Bubble Sort"),
        BenchmarkProgram("divide.c", "Divide"),
        BenchmarkProgram("euclidean.c", "Euclidean"),
        BenchmarkProgram("factorial.c", "Factorial"),
        BenchmarkProgram("fibonacci.c", "Fibonacci"),
        BenchmarkProgram("matrix_multiply.c", "Matrix Multiply")
    )

    override fun programs() = programs

    override fun source(programName: String) =
        when (programs.any { program -> program.name == programName }) {
            false -> BenchmarkProgramNotFound(programName).asFailure()
            true ->
                resourceStreamFor(programName)
                    ?.use { sourceStream ->
                        try {
                            sourceStream.readBytes().decodeToString().asSuccess()
                        } catch (_: IOException) {
                            BenchmarkProgramSourceReadFailed(programName).asFailure()
                        }
                    }
                    ?: BenchmarkProgramResourceMissing(programName).asFailure()
        }

    override fun materializeSource(programName: String, targetDirectory: Path) =
        when (programs.any { program -> program.name == programName }) {
            false -> BenchmarkProgramNotFound(programName).asFailure()
            true -> writeResourceToFile(programName, targetDirectory.resolve(programName))
        }

    private fun writeResourceToFile(programName: String, targetPath: Path) =
        resourceStreamFor(programName)
            ?.use { sourceStream ->
                try {
                    Files.copy(sourceStream, targetPath).let { targetPath.asSuccess() }
                } catch (_: IOException) {
                    BenchmarkProgramMaterializationFailed(programName, targetPath.toString()).asFailure()
                }
            }
            ?: BenchmarkProgramResourceMissing(programName).asFailure()

    private fun resourceStreamFor(programName: String) =
        javaClass.classLoader.getResourceAsStream("benchmark_kernels/$programName")
}
