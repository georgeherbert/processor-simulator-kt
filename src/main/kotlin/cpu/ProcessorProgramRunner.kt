package cpu

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import mainmemory.MainMemoryProgramLoader
import mainmemory.RealMainMemoryProgramLoader
import types.InstructionAddress
import types.ProcessorCycleLimitExceeded
import types.ProcessorResult
import types.Size
import java.nio.file.Path

interface ProcessorProgramRunner {
    fun run(
        programFilePath: Path,
        mainMemorySize: Size,
        initialInstructionAddress: InstructionAddress,
        maxCycleCount: Int,
        configuration: ProcessorConfiguration
    ): ProcessorResult<ProcessorProgramRunResult>
}

data class ProcessorProgramRunResult(
    val finalState: ProcessorState
)

data class RealProcessorProgramRunner(
    private val mainMemoryProgramLoader: MainMemoryProgramLoader,
    private val processorFactory: ProcessorFactory
) : ProcessorProgramRunner {

    constructor() : this(
        RealMainMemoryProgramLoader,
        RealProcessorFactory
    )

    override fun run(
        programFilePath: Path,
        mainMemorySize: Size,
        initialInstructionAddress: InstructionAddress,
        maxCycleCount: Int,
        configuration: ProcessorConfiguration
    ) =
        mainMemoryProgramLoader
            .load(programFilePath, mainMemorySize)
            .flatMap { mainMemory ->
                processorFactory.create(
                    configuration,
                    mainMemory,
                    mainMemorySize,
                    initialInstructionAddress
                )
            }
            .flatMap { initialState ->
                runUntilCompletion(
                    RealProcessor(configuration),
                    initialState,
                    maxCycleCount
                )
            }

    private fun runUntilCompletion(
        processor: Processor,
        currentState: ProcessorState,
        maxCycleCount: Int
    ): ProcessorResult<ProcessorProgramRunResult> {
        val initialResult: ProcessorResult<ProcessorState> = currentState.asSuccess()

        return (0 until maxCycleCount)
            .fold(initialResult) { stateResult, _ ->
                stateResult.flatMap { activeState ->
                    when (activeState.halted) {
                        true -> activeState.asSuccess()
                        false -> processor.step(activeState)
                    }
                }
            }
            .flatMap { finalState ->
                when (finalState.halted) {
                    true -> ProcessorProgramRunResult(finalState).asSuccess()
                    false -> ProcessorCycleLimitExceeded(maxCycleCount).asFailure()
                }
            }
    }
}
