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

            expectThat(
                valueForParameter(
                    baseConfiguration.withParameter(parameter, nextValue),
                    parameter
                )
            )
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
        expectThat(
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
        )
            .isSuccess()
            .get { branchTargetBufferSize.value }
            .isEqualTo(5)

        expectThat(
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
        )
            .isSuccess()
            .get { branchOutcomeCounterBitWidth.value }
            .isEqualTo(3)

        expectThat(
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
        )
            .isSuccess()
            .get { arithmeticLogicLatencies.add.value }
            .isEqualTo(4)

        expectThat(
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
        )
            .isSuccess()
            .get { branchLatencies.jumpAndLinkRegister.value }
            .isEqualTo(6)

        expectThat(
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
        )
            .isSuccess()
            .get { memoryLatencies.loadByteUnsigned.value }
            .isEqualTo(7)
    }

    private fun valueForParameter(
        configuration: ExperimentConfigurationSnapshot,
        parameter: ExperimentParameter
    ) =
        when (parameter) {
            ExperimentParameter.FetchWidth -> configuration.fetchWidth
            ExperimentParameter.IssueWidth -> configuration.issueWidth
            ExperimentParameter.CommitWidth -> configuration.commitWidth
            ExperimentParameter.InstructionQueueSize -> configuration.instructionQueueSize
            ExperimentParameter.ReorderBufferSize -> configuration.reorderBufferSize
            ExperimentParameter.BranchTargetBufferSize -> configuration.branchTargetBufferSize
            ExperimentParameter.BranchOutcomeCounterBitWidth -> configuration.branchOutcomeCounterBitWidth
            ExperimentParameter.ArithmeticLogicReservationStationCount ->
                configuration.arithmeticLogicReservationStationCount

            ExperimentParameter.BranchReservationStationCount ->
                configuration.branchReservationStationCount

            ExperimentParameter.MemoryBufferCount -> configuration.memoryBufferCount
            ExperimentParameter.ArithmeticLogicUnitCount -> configuration.arithmeticLogicUnitCount
            ExperimentParameter.BranchUnitCount -> configuration.branchUnitCount
            ExperimentParameter.AddressUnitCount -> configuration.addressUnitCount
            ExperimentParameter.MemoryUnitCount -> configuration.memoryUnitCount
            ExperimentParameter.AddLatency -> configuration.addLatency
            ExperimentParameter.LoadUpperImmediateLatency -> configuration.loadUpperImmediateLatency
            ExperimentParameter.AddUpperImmediateToProgramCounterLatency ->
                configuration.addUpperImmediateToProgramCounterLatency

            ExperimentParameter.SubtractLatency -> configuration.subtractLatency
            ExperimentParameter.ShiftLeftLogicalLatency -> configuration.shiftLeftLogicalLatency
            ExperimentParameter.SetLessThanSignedLatency -> configuration.setLessThanSignedLatency
            ExperimentParameter.SetLessThanUnsignedLatency -> configuration.setLessThanUnsignedLatency
            ExperimentParameter.ExclusiveOrLatency -> configuration.exclusiveOrLatency
            ExperimentParameter.ShiftRightLogicalLatency -> configuration.shiftRightLogicalLatency
            ExperimentParameter.ShiftRightArithmeticLatency -> configuration.shiftRightArithmeticLatency
            ExperimentParameter.OrLatency -> configuration.orLatency
            ExperimentParameter.AndLatency -> configuration.andLatency
            ExperimentParameter.JumpAndLinkLatency -> configuration.jumpAndLinkLatency
            ExperimentParameter.JumpAndLinkRegisterLatency -> configuration.jumpAndLinkRegisterLatency
            ExperimentParameter.BranchEqualLatency -> configuration.branchEqualLatency
            ExperimentParameter.BranchNotEqualLatency -> configuration.branchNotEqualLatency
            ExperimentParameter.BranchLessThanSignedLatency -> configuration.branchLessThanSignedLatency
            ExperimentParameter.BranchLessThanUnsignedLatency -> configuration.branchLessThanUnsignedLatency
            ExperimentParameter.BranchGreaterThanOrEqualSignedLatency ->
                configuration.branchGreaterThanOrEqualSignedLatency

            ExperimentParameter.BranchGreaterThanOrEqualUnsignedLatency ->
                configuration.branchGreaterThanOrEqualUnsignedLatency

            ExperimentParameter.AddressLatency -> configuration.addressLatency
            ExperimentParameter.LoadWordLatency -> configuration.loadWordLatency
            ExperimentParameter.LoadHalfWordLatency -> configuration.loadHalfWordLatency
            ExperimentParameter.LoadHalfWordUnsignedLatency -> configuration.loadHalfWordUnsignedLatency
            ExperimentParameter.LoadByteLatency -> configuration.loadByteLatency
            ExperimentParameter.LoadByteUnsignedLatency -> configuration.loadByteUnsignedLatency
        }
}
