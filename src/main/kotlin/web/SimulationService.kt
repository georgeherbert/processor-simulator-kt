package web

import cpu.ProcessorConfiguration
import cpu.ProcessorProgramRunner
import cpu.ProcessorProgramRunResult
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import toolchain.ProgramBinaryPadder
import toolchain.Rv32iCCompiler
import types.ExperimentRangeInvalid
import types.InstructionAddress
import types.ProcessorResult
import types.Size
import types.SimulationCycleCountInvalid

interface SimulationService {
    fun benchmarks(): List<BenchmarkProgram>
    fun benchmarkSource(programName: String): ProcessorResult<BenchmarkProgramSourceResponse>
    fun experimentDefaults(): ExperimentDefaultsResponse
    fun runExperiment(request: RunExperimentRequest): ProcessorResult<ExperimentRunResponse>
}

data class RealSimulationService(
    private val benchmarkProgramCatalog: BenchmarkProgramCatalog,
    private val compiler: Rv32iCCompiler,
    private val programBinaryPadder: ProgramBinaryPadder,
    private val temporaryDirectoryFactory: TemporaryDirectoryFactory,
    private val processorProgramRunner: ProcessorProgramRunner,
    private val configuration: ProcessorConfiguration,
    private val mainMemorySize: Size,
    private val initialInstructionAddress: InstructionAddress
) : SimulationService {

    override fun benchmarks() = benchmarkProgramCatalog.programs()

    override fun benchmarkSource(programName: String) =
        benchmarkProgramCatalog.source(programName)
            .map { sourceCode ->
                BenchmarkProgramSourceResponse(
                    programName,
                    sourceCode
                )
            }

    override fun experimentDefaults() =
        defaultExperimentResponse(configuration, DEFAULT_EXPERIMENT_CYCLE_LIMIT)

    override fun runExperiment(request: RunExperimentRequest) =
        validateExperimentRequest(request)
            .flatMap {
                compilePaddedBenchmark(request.programName)
            }
            .flatMap { binaryFilePath ->
                runExperimentSeries(binaryFilePath, request)
            }

    private fun compilePaddedBenchmark(programName: String) =
        temporaryDirectoryFactory
            .create("processor-simulator-")
            .flatMap { temporaryDirectory ->
                benchmarkProgramCatalog.materializeSource(programName, temporaryDirectory)
                    .flatMap { sourceFilePath ->
                        val buildDirectory = temporaryDirectory.resolve("build")

                        compiler.compile(sourceFilePath, buildDirectory)
                            .flatMap { binaryFilePath ->
                                programBinaryPadder.pad(
                                    binaryFilePath,
                                    buildDirectory.resolve("program-padded.bin")
                                )
                            }
                    }
            }

    private fun validateExperimentRequest(request: RunExperimentRequest) =
        when {
            request.cycleLimit <= 0 -> SimulationCycleCountInvalid(request.cycleLimit).asFailure()
            request.increment <= 0 || request.startValue > request.endValue ->
                ExperimentRangeInvalid(request.startValue, request.endValue, request.increment).asFailure()

            else -> request.asSuccess()
        }

    private fun runExperimentSeries(
        binaryFilePath: java.nio.file.Path,
        request: RunExperimentRequest
    ): ProcessorResult<ExperimentRunResponse> {
        val initialResult: ProcessorResult<List<ExperimentPoint>> = emptyList<ExperimentPoint>().asSuccess()

        return experimentValues(request)
            .fold(initialResult) { pointsResult, parameterValue ->
                pointsResult.flatMap { points ->
                    request.baseConfiguration
                        .withParameter(request.variableParameter, parameterValue)
                        .toProcessorConfiguration()
                        .flatMap { experimentConfiguration ->
                            processorProgramRunner.run(
                                binaryFilePath,
                                mainMemorySize,
                                initialInstructionAddress,
                                request.cycleLimit,
                                experimentConfiguration
                            )
                        }
                        .map { runResult ->
                            points + ExperimentPoint(
                                parameterValue,
                                instructionsPerCycle(runResult)
                            )
                        }
                }
            }
            .map { points ->
                ExperimentRunResponse(
                    request.programName,
                    request.variableParameter,
                    points
                )
            }
    }

    private fun experimentValues(request: RunExperimentRequest) =
        generateSequence(request.startValue) { currentValue ->
            (currentValue + request.increment)
                .takeIf { nextValue -> nextValue <= request.endValue }
        }.toList()

    private fun instructionsPerCycle(runResult: ProcessorProgramRunResult): Double {
        val statistics = runResult.finalState.statistics

        return when (statistics.cycleCount == 0) {
            true -> 0.0
            false -> statistics.committedInstructionCount.toDouble() / statistics.cycleCount.toDouble()
        }
    }

    private companion object {
        const val DEFAULT_EXPERIMENT_CYCLE_LIMIT = 100_000
    }
}
