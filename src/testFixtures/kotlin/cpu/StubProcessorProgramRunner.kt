package cpu

import dev.forkhandles.result4k.asSuccess
import types.InstructionAddress
import types.ProcessorResult
import types.Size
import java.nio.file.Path

data class StubProcessorProgramRunner(
    private val resultFactory: (
        Path,
        Size,
        InstructionAddress,
        Int,
        ProcessorConfiguration
    ) -> ProcessorResult<ProcessorProgramRunResult>
) : ProcessorProgramRunner {

    constructor(result: ProcessorProgramRunResult) : this(
        { _, _, _, _, _ -> result.asSuccess() }
    )

    override fun run(
        programFilePath: Path,
        mainMemorySize: Size,
        initialInstructionAddress: InstructionAddress,
        maxCycleCount: Int,
        configuration: ProcessorConfiguration
    ) =
        resultFactory(
            programFilePath,
            mainMemorySize,
            initialInstructionAddress,
            maxCycleCount,
            configuration
        )
}
