package web

import cpu.benchmarkConfiguration
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.ExperimentConfigurationValueInvalid

class ExperimentViewTest {

    @Test
    fun `default experiment response exposes the baseline configuration and parameters`() {
        val defaults = defaultExperimentResponse(benchmarkConfiguration(), 100_000)

        expectThat(defaults.configuration.fetchWidth)
            .isEqualTo(8)

        expectThat(defaults.parameters.map { option -> option.key })
            .contains(ExperimentParameter.FetchWidth)

        expectThat(defaults.parameters.map { option -> option.key })
            .contains(ExperimentParameter.LoadByteUnsignedLatency)
    }

    @Test
    fun `withParameter updates each supported variable parameter`() {
        val baseConfiguration = benchmarkConfiguration().toExperimentConfigurationSnapshot()

        ExperimentParameter.entries.forEachIndexed { index, parameter ->
            val nextValue = index + 2
            val updatedConfiguration = baseConfiguration.withParameter(parameter, nextValue)

            expectThat(parameter.valueIn(updatedConfiguration))
                .isEqualTo(nextValue)
        }
    }

    @Test
    fun `toProcessorConfiguration rejects non positive values`() {
        expectThat(
            benchmarkConfiguration()
                .toExperimentConfigurationSnapshot()
                .copy(issueWidth = 0)
                .toProcessorConfiguration()
        )
            .isFailure()
            .isEqualTo(ExperimentConfigurationValueInvalid("issueWidth", 0))
    }

    @Test
    fun `toProcessorConfiguration recreates a processor configuration from the snapshot`() {
        val configurationResult =
            benchmarkConfiguration()
                .toExperimentConfigurationSnapshot()
                .copy(
                    branchTargetBufferSize = 5,
                    branchOutcomeCounterBitWidth = 3,
                    addLatency = 4,
                    jumpAndLinkRegisterLatency = 6,
                    loadByteUnsignedLatency = 7
                )
                .toProcessorConfiguration()

        expectThat(configurationResult)
            .isSuccess()
            .get { branchTargetBufferSize.value }
            .isEqualTo(5)

        expectThat(configurationResult)
            .isSuccess()
            .get { branchOutcomeCounterBitWidth.value }
            .isEqualTo(3)

        expectThat(configurationResult)
            .isSuccess()
            .get { arithmeticLogicLatencies.add.value }
            .isEqualTo(4)

        expectThat(configurationResult)
            .isSuccess()
            .get { branchLatencies.jumpAndLinkRegister.value }
            .isEqualTo(6)

        expectThat(configurationResult)
            .isSuccess()
            .get { memoryLatencies.loadByteUnsigned.value }
            .isEqualTo(7)
    }
}
