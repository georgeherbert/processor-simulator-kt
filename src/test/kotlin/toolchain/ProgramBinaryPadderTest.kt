package toolchain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.ProgramBinaryFileNotFound
import types.ProgramBinaryFileReadFailed
import types.ProgramBinaryFileWriteFailed
import java.nio.file.Files
import java.nio.file.Path

class ProgramBinaryPadderTest {

    @Test
    fun `pad appends a nop sled to the binary image`(@TempDir temporaryDirectory: Path) {
        val binaryFilePath = temporaryDirectory.resolve("program.bin")
        Files.write(binaryFilePath, byteArrayOf(0x6f, 0x00, 0x00, 0x00))
        val paddedBinaryFilePath = temporaryDirectory.resolve("program-padded.bin")

        val resultPath = expectThat(
            RealProgramBinaryPadder.pad(binaryFilePath, paddedBinaryFilePath)
        )
            .isSuccess()
            .subject

        expectThat(Files.exists(resultPath))
            .isTrue()

        expectThat(Files.readAllBytes(resultPath).take(8))
            .isEqualTo(byteArrayOf(0x6f, 0x00, 0x00, 0x00, 0x13, 0x00, 0x00, 0x00).toList())
    }

    @Test
    fun `pad fails when the input binary does not exist`(@TempDir temporaryDirectory: Path) {
        val binaryFilePath = temporaryDirectory.resolve("missing.bin")

        expectThat(
            RealProgramBinaryPadder.pad(binaryFilePath, temporaryDirectory.resolve("program-padded.bin"))
        )
            .isFailure()
            .isEqualTo(ProgramBinaryFileNotFound(binaryFilePath.toString()))
    }

    @Test
    fun `pad fails when the input binary cannot be read`(@TempDir temporaryDirectory: Path) {
        val binaryFilePath = temporaryDirectory.resolve("directory")
        Files.createDirectory(binaryFilePath)

        expectThat(
            RealProgramBinaryPadder.pad(binaryFilePath, temporaryDirectory.resolve("program-padded.bin"))
        )
            .isFailure()
            .isEqualTo(ProgramBinaryFileReadFailed(binaryFilePath.toString()))
    }

    @Test
    fun `pad fails when the padded binary cannot be written`(@TempDir temporaryDirectory: Path) {
        val binaryFilePath = temporaryDirectory.resolve("program.bin")
        Files.write(binaryFilePath, byteArrayOf(0x6f, 0x00, 0x00, 0x00))
        val paddedBinaryFilePath = temporaryDirectory.resolve("missing").resolve("program-padded.bin")

        expectThat(
            RealProgramBinaryPadder.pad(binaryFilePath, paddedBinaryFilePath)
        )
            .isFailure()
            .isEqualTo(ProgramBinaryFileWriteFailed(paddedBinaryFilePath.toString()))
    }
}
