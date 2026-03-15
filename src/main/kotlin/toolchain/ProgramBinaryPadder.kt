package toolchain

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import types.ProcessorResult
import types.ProgramBinaryFileNotFound
import types.ProgramBinaryFileReadFailed
import types.ProgramBinaryFileWriteFailed

interface ProgramBinaryPadder {
    fun pad(binaryFilePath: Path, paddedBinaryFilePath: Path): ProcessorResult<Path>
}

data object RealProgramBinaryPadder : ProgramBinaryPadder {

    override fun pad(binaryFilePath: Path, paddedBinaryFilePath: Path) =
        readBinaryFile(binaryFilePath)
            .flatMap { binaryBytes -> writePaddedBinary(paddedBinaryFilePath, binaryBytes + nopPadding()) }

    private fun readBinaryFile(binaryFilePath: Path) =
        try {
            Files.readAllBytes(binaryFilePath).asSuccess()
        } catch (_: NoSuchFileException) {
            ProgramBinaryFileNotFound(binaryFilePath.toString()).asFailure()
        } catch (_: IOException) {
            ProgramBinaryFileReadFailed(binaryFilePath.toString()).asFailure()
        }

    private fun writePaddedBinary(paddedBinaryFilePath: Path, binaryBytes: ByteArray) =
        try {
            Files.write(paddedBinaryFilePath, binaryBytes)
            paddedBinaryFilePath.asSuccess()
        } catch (_: IOException) {
            ProgramBinaryFileWriteFailed(paddedBinaryFilePath.toString()).asFailure()
        }

    private fun nopPadding() =
        ByteArray(NOP_INSTRUCTION_COUNT * NOP_INSTRUCTION_BYTES.size) { byteIndex ->
            NOP_INSTRUCTION_BYTES[byteIndex % NOP_INSTRUCTION_BYTES.size]
        }

    private val NOP_INSTRUCTION_BYTES = byteArrayOf(0x13, 0x00, 0x00, 0x00)
    private const val NOP_INSTRUCTION_COUNT = 512
}
