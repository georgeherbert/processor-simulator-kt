package web

import cpu.ArithmeticLogicLatencies
import cpu.BranchLatencies
import cpu.CommitWidth
import cpu.IssueWidth
import cpu.MemoryLatencies
import cpu.ProcessorConfiguration
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import fetch.FetchWidth
import kotlinx.serialization.Serializable
import types.*

@Serializable
data class ExperimentConfigurationSnapshot(
    val fetchWidth: Int,
    val issueWidth: Int,
    val commitWidth: Int,
    val instructionQueueSize: Int,
    val reorderBufferSize: Int,
    val branchTargetBufferSize: Int,
    val branchOutcomeCounterBitWidth: Int,
    val arithmeticLogicReservationStationCount: Int,
    val branchReservationStationCount: Int,
    val memoryBufferCount: Int,
    val arithmeticLogicUnitCount: Int,
    val branchUnitCount: Int,
    val addressUnitCount: Int,
    val memoryUnitCount: Int,
    val addLatency: Int,
    val loadUpperImmediateLatency: Int,
    val addUpperImmediateToProgramCounterLatency: Int,
    val subtractLatency: Int,
    val shiftLeftLogicalLatency: Int,
    val setLessThanSignedLatency: Int,
    val setLessThanUnsignedLatency: Int,
    val exclusiveOrLatency: Int,
    val shiftRightLogicalLatency: Int,
    val shiftRightArithmeticLatency: Int,
    val orLatency: Int,
    val andLatency: Int,
    val jumpAndLinkLatency: Int,
    val jumpAndLinkRegisterLatency: Int,
    val branchEqualLatency: Int,
    val branchNotEqualLatency: Int,
    val branchLessThanSignedLatency: Int,
    val branchLessThanUnsignedLatency: Int,
    val branchGreaterThanOrEqualSignedLatency: Int,
    val branchGreaterThanOrEqualUnsignedLatency: Int,
    val addressLatency: Int,
    val loadWordLatency: Int,
    val loadHalfWordLatency: Int,
    val loadHalfWordUnsignedLatency: Int,
    val loadByteLatency: Int,
    val loadByteUnsignedLatency: Int
)

fun ExperimentConfigurationSnapshot.withParameter(parameter: ExperimentParameter, value: Int) =
    parameter.update(this, value)

fun ExperimentConfigurationSnapshot.toProcessorConfiguration(): ProcessorResult<ProcessorConfiguration> {
    val invalidEntry =
        positiveFields()
            .firstOrNull { entry -> entry.second <= 0 }

    return when (invalidEntry) {
        null ->
            ProcessorConfiguration(
                FetchWidth(fetchWidth),
                IssueWidth(issueWidth),
                CommitWidth(commitWidth),
                Size(instructionQueueSize),
                Size(reorderBufferSize),
                Size(branchTargetBufferSize),
                BitWidth(branchOutcomeCounterBitWidth),
                Size(arithmeticLogicReservationStationCount),
                Size(branchReservationStationCount),
                Size(memoryBufferCount),
                Size(arithmeticLogicUnitCount),
                Size(branchUnitCount),
                Size(addressUnitCount),
                Size(memoryUnitCount),
                ArithmeticLogicLatencies(
                    CycleCount(addLatency),
                    CycleCount(loadUpperImmediateLatency),
                    CycleCount(addUpperImmediateToProgramCounterLatency),
                    CycleCount(subtractLatency),
                    CycleCount(shiftLeftLogicalLatency),
                    CycleCount(setLessThanSignedLatency),
                    CycleCount(setLessThanUnsignedLatency),
                    CycleCount(exclusiveOrLatency),
                    CycleCount(shiftRightLogicalLatency),
                    CycleCount(shiftRightArithmeticLatency),
                    CycleCount(orLatency),
                    CycleCount(andLatency)
                ),
                BranchLatencies(
                    CycleCount(jumpAndLinkLatency),
                    CycleCount(jumpAndLinkRegisterLatency),
                    CycleCount(branchEqualLatency),
                    CycleCount(branchNotEqualLatency),
                    CycleCount(branchLessThanSignedLatency),
                    CycleCount(branchLessThanUnsignedLatency),
                    CycleCount(branchGreaterThanOrEqualSignedLatency),
                    CycleCount(branchGreaterThanOrEqualUnsignedLatency)
                ),
                CycleCount(addressLatency),
                MemoryLatencies(
                    CycleCount(loadWordLatency),
                    CycleCount(loadHalfWordLatency),
                    CycleCount(loadHalfWordUnsignedLatency),
                    CycleCount(loadByteLatency),
                    CycleCount(loadByteUnsignedLatency)
                )
            ).asSuccess()

        else -> ExperimentConfigurationValueInvalid(invalidEntry.first, invalidEntry.second).asFailure()
    }
}

fun ProcessorConfiguration.toExperimentConfigurationSnapshot() =
    ExperimentConfigurationSnapshot(
        fetchWidth.value,
        issueWidth.value,
        commitWidth.value,
        instructionQueueSize.value,
        reorderBufferSize.value,
        branchTargetBufferSize.value,
        branchOutcomeCounterBitWidth.value,
        arithmeticLogicReservationStationCount.value,
        branchReservationStationCount.value,
        memoryBufferCount.value,
        arithmeticLogicUnitCount.value,
        branchUnitCount.value,
        addressUnitCount.value,
        memoryUnitCount.value,
        arithmeticLogicLatencies.add.value,
        arithmeticLogicLatencies.loadUpperImmediate.value,
        arithmeticLogicLatencies.addUpperImmediateToProgramCounter.value,
        arithmeticLogicLatencies.subtract.value,
        arithmeticLogicLatencies.shiftLeftLogical.value,
        arithmeticLogicLatencies.setLessThanSigned.value,
        arithmeticLogicLatencies.setLessThanUnsigned.value,
        arithmeticLogicLatencies.exclusiveOr.value,
        arithmeticLogicLatencies.shiftRightLogical.value,
        arithmeticLogicLatencies.shiftRightArithmetic.value,
        arithmeticLogicLatencies.or.value,
        arithmeticLogicLatencies.and.value,
        branchLatencies.jumpAndLink.value,
        branchLatencies.jumpAndLinkRegister.value,
        branchLatencies.branchEqual.value,
        branchLatencies.branchNotEqual.value,
        branchLatencies.branchLessThanSigned.value,
        branchLatencies.branchLessThanUnsigned.value,
        branchLatencies.branchGreaterThanOrEqualSigned.value,
        branchLatencies.branchGreaterThanOrEqualUnsigned.value,
        addressLatency.value,
        memoryLatencies.loadWord.value,
        memoryLatencies.loadHalfWord.value,
        memoryLatencies.loadHalfWordUnsigned.value,
        memoryLatencies.loadByte.value,
        memoryLatencies.loadByteUnsigned.value
    )

private fun ExperimentConfigurationSnapshot.positiveFields() =
    ExperimentParameter.entries.map { parameter ->
        parameter.fieldName to parameter.valueIn(this)
    }
