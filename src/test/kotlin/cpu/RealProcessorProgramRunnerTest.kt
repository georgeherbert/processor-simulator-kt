package cpu

import fetch.FetchWidth
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import toolchain.RealRv32iCCompiler
import types.*
import java.nio.file.Files
import java.nio.file.Path

class RealProcessorProgramRunnerTest {

    private val compiler = RealRv32iCCompiler("riscv64-unknown-elf-gcc", "riscv64-unknown-elf-objcopy")
    private val runner = RealProcessorProgramRunner()
    private val mainMemorySize = Size(32 * 1024)

    @Test
    fun `arithmetic program runs to expected completion`(@TempDir temporaryDirectory: Path) {
        assertBenchmarkKernel(temporaryDirectory, "arithmetic.c", 18818u, 20_000_000)
    }

    @Test
    fun `bubble sort program runs to expected completion`(@TempDir temporaryDirectory: Path) {
        assertBenchmarkKernel(temporaryDirectory, "bubble_sort.c", 0u, 100_000_000)
    }

    @Test
    fun `divide program runs to expected completion`(@TempDir temporaryDirectory: Path) {
        assertBenchmarkKernel(temporaryDirectory, "divide.c", 42u, 2_000_000)
    }

    @Test
    fun `euclidean program runs to expected completion`(@TempDir temporaryDirectory: Path) {
        assertBenchmarkKernel(temporaryDirectory, "euclidean.c", 343u, 200_000_000)
    }

    @Test
    fun `factorial program runs to expected completion`(@TempDir temporaryDirectory: Path) {
        assertBenchmarkKernel(temporaryDirectory, "factorial.c", 362880u, 50_000_000)
    }

    @Test
    fun `fibonacci program runs to expected completion`(@TempDir temporaryDirectory: Path) {
        assertBenchmarkKernel(temporaryDirectory, "fibonacci.c", 1836311903u, 50_000_000)
    }

    @Test
    fun `matrix multiply program runs to expected completion`(@TempDir temporaryDirectory: Path) {
        assertBenchmarkKernel(temporaryDirectory, "matrix_multiply.c", 1425u, 200_000_000)
    }

    @Test
    fun `fails when the program file does not exist`(@TempDir temporaryDirectory: Path) {
        val missingProgramPath = temporaryDirectory.resolve("missing.bin")

        expectThat(
            runner.run(
                missingProgramPath,
                mainMemorySize,
                InstructionAddress(0),
                32,
                benchmarkProcessorConfiguration()
            )
        )
            .isFailure()
            .isEqualTo(MainMemoryProgramFileNotFound(missingProgramPath.toString()))
    }

    @Test
    fun `fails when the program does not halt before the cycle limit`(@TempDir temporaryDirectory: Path) {
        val compiledProgramPath = compileInlineProgram(
            temporaryDirectory,
            """
            #include <stdint.h>

            uint32_t _start()
            {
                volatile uint32_t counter = 0;
                while (1)
                {
                    counter += 1;
                }
            }
            """.trimIndent()
        )

        expectThat(
            runner.run(
                compiledProgramPath,
                mainMemorySize,
                InstructionAddress(0),
                4,
                benchmarkProcessorConfiguration()
            )
        )
            .isFailure()
            .isEqualTo(ProcessorCycleLimitExceeded(4))
    }

    private fun assertBenchmarkKernel(
        temporaryDirectory: Path,
        fileName: String,
        expectedReturnValue: UInt,
        maxCycleCount: Int
    ) {
        val sourceFilePath = benchmarkSourceFilePath(fileName)
        expectThat(Files.exists(sourceFilePath))
            .isTrue()

        val binaryFilePath = expectThat(
            compiler.compile(sourceFilePath, temporaryDirectory.resolve(fileName.removeSuffix(".c")))
        )
            .isSuccess()
            .subject
        val paddedBinaryFilePath = paddedBinaryFilePath(binaryFilePath, temporaryDirectory, fileName.removeSuffix(".c"))

        val runResult = expectThat(
            runner.run(
                paddedBinaryFilePath,
                mainMemorySize,
                InstructionAddress(0),
                maxCycleCount,
                benchmarkProcessorConfiguration()
            )
        )
            .isSuccess()
            .subject

        expectThat(runResult.finalState.halted)
            .isTrue()

        expectThat(runResult.finalState.registerFile.readCommitted(RegisterAddress(10)))
            .isEqualTo(Word(expectedReturnValue))

        expectThat(runResult.finalState.statistics.committedInstructionCount)
            .isGreaterThan(0)
    }

    private fun compileInlineProgram(
        temporaryDirectory: Path,
        cSource: String
    ): Path {
        val sourceFilePath = temporaryDirectory.resolve("program.c")
        Files.writeString(sourceFilePath, cSource)

        val binaryFilePath = expectThat(
            compiler.compile(sourceFilePath, temporaryDirectory.resolve("program"))
        )
            .isSuccess()
            .subject

        return paddedBinaryFilePath(binaryFilePath, temporaryDirectory, "program")
    }

    private fun benchmarkSourceFilePath(fileName: String) =
        Path.of("")
            .toAbsolutePath()
            .resolve("src/testFixtures/resources/benchmark_kernels")
            .resolve(fileName)

    private fun benchmarkProcessorConfiguration() =
        testProcessorConfiguration().copy(
            fetchWidth = FetchWidth(8),
            issueWidth = IssueWidth(8),
            commitWidth = CommitWidth(8),
            instructionQueueSize = Size(64),
            reorderBufferSize = Size(128),
            arithmeticLogicReservationStationCount = Size(64),
            branchReservationStationCount = Size(16),
            memoryBufferCount = Size(64),
            arithmeticLogicUnitCount = Size(4),
            branchUnitCount = Size(2),
            addressUnitCount = Size(2),
            memoryUnitCount = Size(2)
        )

    private fun paddedBinaryFilePath(
        binaryFilePath: Path,
        temporaryDirectory: Path,
        outputName: String
    ): Path {
        val paddedFilePath = temporaryDirectory.resolve(outputName + "-padded.bin")
        val nopInstruction = byteArrayOf(0x13, 0x00, 0x00, 0x00)
        val nopPadding = ByteArray(512 * nopInstruction.size) { byteIndex ->
            nopInstruction[byteIndex % nopInstruction.size]
        }

        Files.write(
            paddedFilePath,
            Files.readAllBytes(binaryFilePath) + nopPadding
        )

        return paddedFilePath
    }
}
