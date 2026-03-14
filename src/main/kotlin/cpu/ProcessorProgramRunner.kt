package cpu

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
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
    private val processorFactory: ProcessorFactory,
    private val processorCreator: ProcessorCreator
) : ProcessorProgramRunner {

    constructor() : this(
        RealMainMemoryProgramLoader,
        RealProcessorFactory,
        RealProcessorCreator
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
                    processorCreator.create(configuration),
                    initialState,
                    maxCycleCount,
                    maxCycleCount
                )
            }

    private tailrec fun runUntilCompletion(
        processor: Processor,
        currentState: ProcessorState,
        remainingCycleBudget: Int,
        maxCycleCount: Int
    ): ProcessorResult<ProcessorProgramRunResult> =
        when {
            currentState.halted -> ProcessorProgramRunResult(currentState).asSuccess()
            remainingCycleBudget <= 0 -> ProcessorCycleLimitExceeded(maxCycleCount).asFailure()
            else ->
                when (val nextStateResult = processor.step(currentState)) {
                    is Failure -> nextStateResult.reason.asFailure()
                    is Success ->
                        runUntilCompletion(
                            processor,
                            nextStateResult.value,
                            remainingCycleBudget - 1,
                            maxCycleCount
                        )
                }
        }
}
