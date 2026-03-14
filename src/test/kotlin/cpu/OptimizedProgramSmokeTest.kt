package cpu

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import testfixtures.isSuccess
import toolchain.RealRv32iCCompiler
import types.InstructionAddress
import types.RegisterAddress
import types.Size
import types.Word
import java.nio.file.Files
import java.nio.file.Path

class OptimizedProgramSmokeTest {

    private val compiler = RealRv32iCCompiler("riscv64-unknown-elf-gcc", "riscv64-unknown-elf-objcopy")
    private val runner = RealProcessorProgramRunner()

    @Test
    fun `optimized direct function call returns the expected value`(@TempDir temporaryDirectory: Path) {
        val binaryFilePath = compileProgram(
            temporaryDirectory,
            """
            #include <stdint.h>

            uint32_t identity(uint32_t value);

            uint32_t _start()
            {
                return identity(42);
            }

            uint32_t identity(uint32_t value)
            {
                return value;
            }
            """.trimIndent()
        )

        val runResult = expectThat(
            runner.run(
                binaryFilePath,
                Size(32 * 1024),
                InstructionAddress(0),
                2_000_000,
                benchmarkProcessorConfiguration()
            )
        )
            .isSuccess()
            .subject

        expectThat(runResult.finalState.halted)
            .isTrue()

        expectThat(runResult.finalState.registerFile.readCommitted(RegisterAddress(10)))
            .isEqualTo(Word(42u))
    }

    @Test
    fun `optimized runtime multiply returns the expected value`(@TempDir temporaryDirectory: Path) {
        val binaryFilePath = compileProgram(
            temporaryDirectory,
            """
            #include <stdint.h>

            uint32_t _start()
            {
                volatile uint32_t left = 6;
                volatile uint32_t right = 7;
                return left * right;
            }
            """.trimIndent()
        )

        val runResult = expectThat(
            runner.run(
                binaryFilePath,
                Size(32 * 1024),
                InstructionAddress(0),
                2_000_000,
                benchmarkProcessorConfiguration()
            )
        )
            .isSuccess()
            .subject

        expectThat(runResult.finalState.halted)
            .isTrue()

        expectThat(runResult.finalState.registerFile.readCommitted(RegisterAddress(10)))
            .isEqualTo(Word(42u))
    }

    private fun compileProgram(temporaryDirectory: Path, source: String): Path {
        val sourceFilePath = temporaryDirectory.resolve("program.c")
        Files.writeString(sourceFilePath, source)

        val binaryFilePath = expectThat(
            compiler.compile(sourceFilePath, temporaryDirectory.resolve("out"))
        )
            .isSuccess()
            .subject

        return paddedBinaryFilePath(binaryFilePath, temporaryDirectory)
    }

    private fun benchmarkProcessorConfiguration() =
        testProcessorConfiguration().copy(
            fetchWidth = fetch.FetchWidth(8),
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

    private fun paddedBinaryFilePath(binaryFilePath: Path, temporaryDirectory: Path): Path {
        val paddedFilePath = temporaryDirectory.resolve("program-padded.bin")
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
