package toolchain

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import types.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

interface Rv32iCCompiler {
    fun compile(cSourceFilePath: Path, outputDirectory: Path): ProcessorResult<Path>
}

data class RealRv32iCCompiler(
    private val compilerExecutable: String,
    private val objcopyExecutable: String
) : Rv32iCCompiler {

    override fun compile(cSourceFilePath: Path, outputDirectory: Path) =
        ensureOutputDirectoryExists(outputDirectory)
            .flatMap { compileToElf(cSourceFilePath, outputDirectory.resolve("program.elf")) }
            .flatMap { elfFilePath ->
                convertElfToBinary(elfFilePath, outputDirectory.resolve("program.bin"))
            }

    private fun ensureOutputDirectoryExists(outputDirectory: Path) =
        try {
            Files.createDirectories(outputDirectory).asSuccess()
        } catch (_: IOException) {
            TemporaryPathCreateFailed(outputDirectory.toString()).asFailure()
        }

    private fun compileToElf(cSourceFilePath: Path, elfFilePath: Path) =
        runProcess(
            compilerExecutable,
            listOf(
                compilerExecutable,
                "-nostdlib",
                "-march=rv32i",
                "-mabi=ilp32",
                "-O2",
                "-fno-toplevel-reorder",
                "-Wl,-Ttext=0",
                "-Wl,--no-relax",
                cSourceFilePath.toString(),
                "-lgcc",
                "-o",
                elfFilePath.toString()
            )
        ).flatMap { elfFilePath.asSuccess() }

    private fun convertElfToBinary(elfFilePath: Path, binaryFilePath: Path) =
        runProcess(
            objcopyExecutable,
            listOf(
                objcopyExecutable,
                "-O",
                "binary",
                elfFilePath.toString(),
                binaryFilePath.toString()
            )
        ).flatMap { binaryFilePath.asSuccess() }

    private fun runProcess(
        executable: String,
        command: List<String>
    ): ProcessorResult<Unit> =
        try {
            ProcessBuilder(command)
                .redirectErrorStream(false)
                .start()
                .let { process ->
                    val exitCode = process.waitFor()
                    val standardError = process.errorReader().readText()

                    when (exitCode == 0) {
                        true -> Unit.asSuccess()
                        false -> ExternalToolExecutionFailed(executable, exitCode, standardError).asFailure()
                    }
                }
        } catch (_: IOException) {
            ExternalToolProcessLaunchFailed(executable).asFailure()
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            ExternalToolExecutionInterrupted(executable).asFailure()
        }
}
