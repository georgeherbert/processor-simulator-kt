package mainmemory

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import java.io.IOException
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import types.MainMemoryProgramFileEmpty
import types.MainMemoryProgramFileNotFound
import types.MainMemoryProgramFileReadFailed
import types.MainMemoryProgramFileTooLarge
import types.ProcessorResult
import types.Size

interface MainMemoryProgramLoader {
    fun load(programFilePath: Path, mainMemorySize: Size): ProcessorResult<MainMemory>
}

data object RealMainMemoryProgramLoader : MainMemoryProgramLoader {

    override fun load(programFilePath: Path, mainMemorySize: Size) =
        readProgramBytes(programFilePath)
            .flatMap { programBytes -> validateProgramBytes(programFilePath, programBytes, mainMemorySize) }
            .map { programBytes -> loadIntoMainMemory(mainMemorySize, programBytes) }

    private fun readProgramBytes(programFilePath: Path) =
        try {
            Files.readAllBytes(programFilePath).asSuccess()
        } catch (_: NoSuchFileException) {
            MainMemoryProgramFileNotFound(programFilePath.toString()).asFailure()
        } catch (_: IOException) {
            MainMemoryProgramFileReadFailed(programFilePath.toString()).asFailure()
        }

    private fun validateProgramBytes(
        programFilePath: Path,
        programBytes: ByteArray,
        mainMemorySize: Size
    ) =
        when {
            programBytes.isEmpty() -> MainMemoryProgramFileEmpty(programFilePath.toString()).asFailure()
            programBytes.size > mainMemorySize.value ->
                MainMemoryProgramFileTooLarge(
                    programFilePath.toString(),
                    programBytes.size,
                    mainMemorySize.value
                ).asFailure()

            else -> programBytes.asSuccess()
        }

    private fun loadIntoMainMemory(mainMemorySize: Size, programBytes: ByteArray): MainMemory =
        RealMainMemory.fromProgramBytes(mainMemorySize, programBytes)
}
