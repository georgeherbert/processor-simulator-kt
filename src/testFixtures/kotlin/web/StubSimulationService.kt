package web

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.ProcessorError
import types.ProcessorResult

data class StubSimulationService(
    private val benchmarksToReturn: List<BenchmarkProgram>,
    private val benchmarkSourceResult: ProcessorResult<BenchmarkProgramSourceResponse>,
    private val experimentDefaultsToReturn: ExperimentDefaultsResponse,
    private val experimentRunResult: ProcessorResult<ExperimentRunResponse>
) : SimulationService {

    constructor(
        benchmarksToReturn: List<BenchmarkProgram>,
        benchmarkSource: BenchmarkProgramSourceResponse,
        experimentDefaults: ExperimentDefaultsResponse,
        experimentRunResponse: ExperimentRunResponse
    ) : this(
        benchmarksToReturn,
        benchmarkSource.asSuccess(),
        experimentDefaults,
        experimentRunResponse.asSuccess()
    )

    constructor(
        benchmarksToReturn: List<BenchmarkProgram>,
        benchmarkSourceFailure: ProcessorError,
        experimentDefaults: ExperimentDefaultsResponse,
        failure: ProcessorError
    ) : this(
        benchmarksToReturn,
        benchmarkSourceFailure.asFailure(),
        experimentDefaults,
        failure.asFailure()
    )

    override fun benchmarks() = benchmarksToReturn

    override fun benchmarkSource(programName: String) = benchmarkSourceResult

    override fun experimentDefaults() = experimentDefaultsToReturn

    override fun runExperiment(request: RunExperimentRequest) = experimentRunResult
}
