package web

import cpu.*
import dev.forkhandles.result4k.asSuccess
import mainmemory.RealMainMemory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import toolchain.StubProgramBinaryPadder
import toolchain.StubRv32iCCompiler
import types.*
import java.nio.file.Files
import java.nio.file.Path

class SimulationServiceTest {

    @Test
    fun `benchmarks delegates to the catalog`(@TempDir temporaryDirectory: Path) {
        val service = service(
            temporaryDirectory,
            StubBenchmarkProgramCatalog(
                listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                temporaryDirectory.resolve("program.c")
            ),
            StubRv32iCCompiler(unpaddedProgramBinaryPath(temporaryDirectory)),
            StubProgramBinaryPadder(paddedProgramBinaryPath(temporaryDirectory))
        )

        expectThat(service.benchmarks())
            .isEqualTo(listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")))
    }

    @Test
    fun `benchmarkSource returns the bundled benchmark code`(@TempDir temporaryDirectory: Path) {
        val service = service(
            temporaryDirectory,
            StubBenchmarkProgramCatalog(
                listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                temporaryDirectory.resolve("program.c")
            ),
            StubRv32iCCompiler(unpaddedProgramBinaryPath(temporaryDirectory)),
            StubProgramBinaryPadder(paddedProgramBinaryPath(temporaryDirectory))
        )

        expectThat(service.benchmarkSource("arithmetic.c"))
            .isSuccess()
            .get { programName }
            .isEqualTo("arithmetic.c")

        expectThat(service.benchmarkSource("arithmetic.c"))
            .isSuccess()
            .get { sourceCode.contains("int32_t main") }
            .isEqualTo(true)
    }

    @Test
    fun `experimentDefaults exposes the baseline benchmark configuration`(@TempDir temporaryDirectory: Path) {
        val service = service(
            temporaryDirectory,
            StubBenchmarkProgramCatalog(
                listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                temporaryDirectory.resolve("program.c")
            ),
            StubRv32iCCompiler(unpaddedProgramBinaryPath(temporaryDirectory)),
            StubProgramBinaryPadder(paddedProgramBinaryPath(temporaryDirectory))
        )

        val defaults = service.experimentDefaults()

        expectThat(defaults.configuration.issueWidth)
            .isEqualTo(2)

        expectThat(defaults.parameters.first().key)
            .isEqualTo(ExperimentParameter.FetchWidth)
    }

    @Test
    fun `runExperiment rejects non positive cycle limits`(@TempDir temporaryDirectory: Path) {
        val service = service(
            temporaryDirectory,
            StubBenchmarkProgramCatalog(
                listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                temporaryDirectory.resolve("program.c")
            ),
            StubRv32iCCompiler(unpaddedProgramBinaryPath(temporaryDirectory)),
            StubProgramBinaryPadder(paddedProgramBinaryPath(temporaryDirectory))
        )

        expectThat(
            service.runExperiment(
                RunExperimentRequest(
                    "arithmetic.c",
                    testProcessorConfiguration().toExperimentConfigurationSnapshot(),
                    ExperimentParameter.IssueWidth,
                    1,
                    3,
                    1,
                    0
                )
            )
        )
            .isFailure()
            .isEqualTo(SimulationCycleCountInvalid(0))
    }

    @Test
    fun `runExperiment sweeps the selected parameter and reports instructions per cycle`(@TempDir temporaryDirectory: Path) {
        val service = service(
            temporaryDirectory,
            StubBenchmarkProgramCatalog(
                listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                temporaryDirectory.resolve("program.c")
            ),
            StubRv32iCCompiler(unpaddedProgramBinaryPath(temporaryDirectory)),
            StubProgramBinaryPadder(paddedProgramBinaryPath(temporaryDirectory)),
            StubProcessorProgramRunner { _, _, _, _, configuration ->
                programRunResult(2, configuration.issueWidth.value).asSuccess()
            }
        )

        val result = expectThat(
            service.runExperiment(
                RunExperimentRequest(
                    "arithmetic.c",
                    testProcessorConfiguration().toExperimentConfigurationSnapshot(),
                    ExperimentParameter.IssueWidth,
                    1,
                    3,
                    1,
                    1_000
                )
            )
        )
            .isSuccess()
            .subject

        expectThat(result.points.map { point -> point.parameterValue })
            .isEqualTo(listOf(1, 2, 3))

        expectThat(result.points.map { point -> point.instructionsPerCycle })
            .isEqualTo(listOf(0.5, 1.0, 1.5))
    }

    @Test
    fun `runExperiment rejects invalid ranges`(@TempDir temporaryDirectory: Path) {
        val service = service(
            temporaryDirectory,
            StubBenchmarkProgramCatalog(
                listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                temporaryDirectory.resolve("program.c")
            ),
            StubRv32iCCompiler(unpaddedProgramBinaryPath(temporaryDirectory)),
            StubProgramBinaryPadder(paddedProgramBinaryPath(temporaryDirectory))
        )

        expectThat(
            service.runExperiment(
                RunExperimentRequest(
                    "arithmetic.c",
                    testProcessorConfiguration().toExperimentConfigurationSnapshot(),
                    ExperimentParameter.IssueWidth,
                    4,
                    2,
                    1,
                    1_000
                )
            )
        )
            .isFailure()
            .isEqualTo(ExperimentRangeInvalid(4, 2, 1))
    }

    @Test
    fun `runExperiment propagates temp directory creation failures`(@TempDir temporaryDirectory: Path) {
        val service = RealSimulationService(
            StubBenchmarkProgramCatalog(
                listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                temporaryDirectory.resolve("program.c")
            ),
            StubRv32iCCompiler(unpaddedProgramBinaryPath(temporaryDirectory)),
            StubProgramBinaryPadder(paddedProgramBinaryPath(temporaryDirectory)),
            StubTemporaryDirectoryFactory(TemporaryPathCreateFailed("processor-simulator-")),
            StubProcessorProgramRunner(programRunResult(1, 1)),
            testProcessorConfiguration(),
            Size(64),
            InstructionAddress(0)
        )

        expectThat(
            service.runExperiment(
                RunExperimentRequest(
                    "arithmetic.c",
                    testProcessorConfiguration().toExperimentConfigurationSnapshot(),
                    ExperimentParameter.IssueWidth,
                    1,
                    1,
                    1,
                    1_000
                )
            )
        )
            .isFailure()
            .isEqualTo(TemporaryPathCreateFailed("processor-simulator-"))
    }

    @Test
    fun `runExperiment propagates binary padding failures`(@TempDir temporaryDirectory: Path) {
        val service = RealSimulationService(
            StubBenchmarkProgramCatalog(
                listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                temporaryDirectory.resolve("program.c")
            ),
            StubRv32iCCompiler(unpaddedProgramBinaryPath(temporaryDirectory)),
            StubProgramBinaryPadder(ProgramBinaryFileWriteFailed("program-padded.bin")),
            StubTemporaryDirectoryFactory(temporaryDirectory),
            StubProcessorProgramRunner(programRunResult(1, 1)),
            testProcessorConfiguration(),
            Size(64),
            InstructionAddress(0)
        )

        expectThat(
            service.runExperiment(
                RunExperimentRequest(
                    "arithmetic.c",
                    testProcessorConfiguration().toExperimentConfigurationSnapshot(),
                    ExperimentParameter.IssueWidth,
                    1,
                    1,
                    1,
                    1_000
                )
            )
        )
            .isFailure()
            .isEqualTo(ProgramBinaryFileWriteFailed("program-padded.bin"))
    }

    private fun service(
        temporaryDirectory: Path,
        benchmarkProgramCatalog: BenchmarkProgramCatalog,
        compiler: toolchain.Rv32iCCompiler,
        programBinaryPadder: toolchain.ProgramBinaryPadder,
        processorProgramRunner: ProcessorProgramRunner
    ) =
        RealSimulationService(
            benchmarkProgramCatalog,
            compiler,
            programBinaryPadder,
            StubTemporaryDirectoryFactory(temporaryDirectory),
            processorProgramRunner,
            testProcessorConfiguration(),
            Size(64),
            InstructionAddress(0)
        )

    private fun service(
        temporaryDirectory: Path,
        benchmarkProgramCatalog: BenchmarkProgramCatalog,
        compiler: toolchain.Rv32iCCompiler,
        programBinaryPadder: toolchain.ProgramBinaryPadder
    ) =
        service(
            temporaryDirectory,
            benchmarkProgramCatalog,
            compiler,
            programBinaryPadder,
            StubProcessorProgramRunner(programRunResult(1, 1))
        )

    private fun unpaddedProgramBinaryPath(temporaryDirectory: Path): Path =
        temporaryDirectory.resolve("program.bin")

    private fun paddedProgramBinaryPath(temporaryDirectory: Path): Path =
        temporaryDirectory.resolve("program-padded.bin").also { binaryPath ->
            Files.write(binaryPath, byteArrayOf(0x6f, 0x00, 0x00, 0x00))
        }

    private fun programRunResult(cycleCount: Int, committedInstructionCount: Int) =
        expectThat(
            RealProcessorFactory.create(
                testProcessorConfiguration(),
                RealMainMemory.fromProgramBytes(Size(64), byteArrayOf(0x13, 0x00, 0x00, 0x00)),
                Size(64),
                InstructionAddress(0)
            )
        )
            .isSuccess()
            .subject
            .let { initialState ->
                ProcessorProgramRunResult(
                    initialState.copy(
                        statistics = ProcessorStatistics(cycleCount, committedInstructionCount, 0, 0),
                        halted = true
                    )
                )
            }
}
