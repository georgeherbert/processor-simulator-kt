package web

import cpu.RealProcessorFactory
import cpu.RealProcessorProgramRunner
import cpu.benchmarkConfiguration
import mainmemory.RealMainMemoryProgramLoader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.hasSize
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import testfixtures.isSuccess
import toolchain.RealProgramBinaryPadder
import toolchain.RealRv32iCCompiler
import types.InstructionAddress
import types.Size
import java.nio.file.Path

class RealSimulationServiceIntegrationTest {

    @Test
    fun `runExperiment executes the arithmetic benchmark through the real web service path`(
        @TempDir temporaryDirectory: Path
    ) {
        val service = RealSimulationService(
            RealBenchmarkProgramCatalog,
            RealRv32iCCompiler("riscv64-unknown-elf-gcc", "riscv64-unknown-elf-objcopy"),
            RealProgramBinaryPadder,
            StubTemporaryDirectoryFactory(temporaryDirectory),
            RealProcessorProgramRunner(RealMainMemoryProgramLoader, RealProcessorFactory),
            benchmarkConfiguration(),
            Size(32 * 1024),
            InstructionAddress(0)
        )

        val result = expectThat(
            service.runExperiment(
                RunExperimentRequest(
                    "arithmetic.c",
                    benchmarkConfiguration().toExperimentConfigurationSnapshot(),
                    ExperimentParameter.IssueWidth,
                    benchmarkConfiguration().issueWidth.value,
                    benchmarkConfiguration().issueWidth.value,
                    1,
                    20_000_000
                )
            )
        )
            .isSuccess()
            .subject

        expectThat(result.points)
            .hasSize(1)

        expectThat(result.points.first().parameterValue)
            .isEqualTo(benchmarkConfiguration().issueWidth.value)

        expectThat(result.points.first().instructionsPerCycle)
            .isGreaterThan(0.0)
    }
}
