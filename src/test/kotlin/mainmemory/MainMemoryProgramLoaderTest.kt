package mainmemory

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.Byte
import types.MainMemoryProgramFileEmpty
import types.MainMemoryProgramFileNotFound
import types.MainMemoryProgramFileReadFailed
import types.MainMemoryProgramFileTooLarge
import types.Size

class MainMemoryProgramLoaderTest {

    private val loader = RealMainMemoryProgramLoader

    @Test
    fun `loads program bytes into memory and zero fills remaining bytes`(@TempDir temporaryDirectory: Path) {
        val programFilePath = temporaryDirectory.resolve("program.bin")
        Files.write(programFilePath, byteArrayOf(0x01, 0x23, 0xFF.toByte()))

        val loadedMainMemory = expectThat(loader.load(programFilePath, Size(8)))
            .isSuccess()
            .subject

        expectThat(loadedMainMemory.loadByte(0))
            .isSuccess()
            .isEqualTo(Byte(0x01u))

        expectThat(loadedMainMemory.loadByte(1))
            .isSuccess()
            .isEqualTo(Byte(0x23u))

        expectThat(loadedMainMemory.loadByte(2))
            .isSuccess()
            .isEqualTo(Byte(0xFFu))

        expectThat(loadedMainMemory.loadByte(3))
            .isSuccess()
            .isEqualTo(Byte(0x00u))

        expectThat(loadedMainMemory.loadByte(7))
            .isSuccess()
            .isEqualTo(Byte(0x00u))
    }

    @Test
    fun `loads program bytes when program exactly fits main memory`(@TempDir temporaryDirectory: Path) {
        val programFilePath = temporaryDirectory.resolve("exact-fit.bin")
        Files.write(programFilePath, byteArrayOf(0x10, 0x20, 0x30, 0x40))

        val loadedMainMemory = expectThat(loader.load(programFilePath, Size(4)))
            .isSuccess()
            .subject

        expectThat(loadedMainMemory.loadByte(0))
            .isSuccess()
            .isEqualTo(Byte(0x10u))

        expectThat(loadedMainMemory.loadByte(1))
            .isSuccess()
            .isEqualTo(Byte(0x20u))

        expectThat(loadedMainMemory.loadByte(2))
            .isSuccess()
            .isEqualTo(Byte(0x30u))

        expectThat(loadedMainMemory.loadByte(3))
            .isSuccess()
            .isEqualTo(Byte(0x40u))
    }

    @Test
    fun `fails when program file does not exist`(@TempDir temporaryDirectory: Path) {
        val missingFilePath = temporaryDirectory.resolve("missing.bin")

        expectThat(loader.load(missingFilePath, Size(8)))
            .isFailure()
            .isEqualTo(MainMemoryProgramFileNotFound(missingFilePath.toString()))
    }

    @Test
    fun `fails when program file is empty`(@TempDir temporaryDirectory: Path) {
        val emptyProgramFilePath = temporaryDirectory.resolve("empty.bin")
        Files.write(emptyProgramFilePath, byteArrayOf())

        expectThat(loader.load(emptyProgramFilePath, Size(8)))
            .isFailure()
            .isEqualTo(MainMemoryProgramFileEmpty(emptyProgramFilePath.toString()))
    }

    @Test
    fun `fails when program file is larger than main memory`(@TempDir temporaryDirectory: Path) {
        val oversizedProgramFilePath = temporaryDirectory.resolve("oversized.bin")
        Files.write(oversizedProgramFilePath, byteArrayOf(0x00, 0x01, 0x02, 0x03, 0x04))

        expectThat(loader.load(oversizedProgramFilePath, Size(4)))
            .isFailure()
            .isEqualTo(
                MainMemoryProgramFileTooLarge(
                    oversizedProgramFilePath.toString(),
                    5,
                    4
                )
            )
    }

    @Test
    fun `fails when main memory has zero capacity and program contains bytes`(@TempDir temporaryDirectory: Path) {
        val programFilePath = temporaryDirectory.resolve("one-byte.bin")
        Files.write(programFilePath, byteArrayOf(0x7F))

        expectThat(loader.load(programFilePath, Size(0)))
            .isFailure()
            .isEqualTo(
                MainMemoryProgramFileTooLarge(
                    programFilePath.toString(),
                    1,
                    0
                )
            )
    }

    @Test
    fun `fails when path is not a readable file`(@TempDir temporaryDirectory: Path) {
        val directoryPath = temporaryDirectory.resolve("directory")
        Files.createDirectory(directoryPath)

        expectThat(loader.load(directoryPath, Size(8)))
            .isFailure()
            .isEqualTo(MainMemoryProgramFileReadFailed(directoryPath.toString()))
    }
}
