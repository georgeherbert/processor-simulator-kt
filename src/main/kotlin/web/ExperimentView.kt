package web

import cpu.*
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import fetch.FetchWidth
import kotlinx.serialization.Serializable
import types.*

@Serializable
enum class ExperimentParameter {
    FetchWidth,
    IssueWidth,
    CommitWidth,
    InstructionQueueSize,
    ReorderBufferSize,
    BranchTargetBufferSize,
    BranchOutcomeCounterBitWidth,
    ArithmeticLogicReservationStationCount,
    BranchReservationStationCount,
    MemoryBufferCount,
    ArithmeticLogicUnitCount,
    BranchUnitCount,
    AddressUnitCount,
    MemoryUnitCount,
    AddLatency,
    LoadUpperImmediateLatency,
    AddUpperImmediateToProgramCounterLatency,
    SubtractLatency,
    ShiftLeftLogicalLatency,
    SetLessThanSignedLatency,
    SetLessThanUnsignedLatency,
    ExclusiveOrLatency,
    ShiftRightLogicalLatency,
    ShiftRightArithmeticLatency,
    OrLatency,
    AndLatency,
    JumpAndLinkLatency,
    JumpAndLinkRegisterLatency,
    BranchEqualLatency,
    BranchNotEqualLatency,
    BranchLessThanSignedLatency,
    BranchLessThanUnsignedLatency,
    BranchGreaterThanOrEqualSignedLatency,
    BranchGreaterThanOrEqualUnsignedLatency,
    AddressLatency,
    LoadWordLatency,
    LoadHalfWordLatency,
    LoadHalfWordUnsignedLatency,
    LoadByteLatency,
    LoadByteUnsignedLatency
}

@Serializable
data class ExperimentParameterOption(
    val key: ExperimentParameter,
    val label: String
)

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

@Serializable
data class ExperimentDefaultsResponse(
    val configuration: ExperimentConfigurationSnapshot,
    val parameters: List<ExperimentParameterOption>,
    val defaultCycleLimit: Int
)

@Serializable
data class RunExperimentRequest(
    val programName: String,
    val baseConfiguration: ExperimentConfigurationSnapshot,
    val variableParameter: ExperimentParameter,
    val startValue: Int,
    val endValue: Int,
    val increment: Int,
    val cycleLimit: Int
)

@Serializable
data class ExperimentPoint(
    val parameterValue: Int,
    val instructionsPerCycle: Double
)

@Serializable
data class ExperimentRunResponse(
    val programName: String,
    val variableParameter: ExperimentParameter,
    val points: List<ExperimentPoint>
)

fun defaultExperimentResponse(configuration: ProcessorConfiguration, defaultCycleLimit: Int) =
    ExperimentDefaultsResponse(
        configuration.toExperimentConfigurationSnapshot(),
        experimentParameterOptions(),
        defaultCycleLimit
    )

fun experimentParameterOptions() =
    listOf(
        ExperimentParameterOption(ExperimentParameter.FetchWidth, "Fetch Width"),
        ExperimentParameterOption(ExperimentParameter.IssueWidth, "Issue Width"),
        ExperimentParameterOption(ExperimentParameter.CommitWidth, "Commit Width"),
        ExperimentParameterOption(ExperimentParameter.InstructionQueueSize, "Instruction Queue Size"),
        ExperimentParameterOption(ExperimentParameter.ReorderBufferSize, "Reorder Buffer Size"),
        ExperimentParameterOption(ExperimentParameter.BranchTargetBufferSize, "Branch Target Buffer Size"),
        ExperimentParameterOption(ExperimentParameter.BranchOutcomeCounterBitWidth, "Branch Predictor Bit Width"),
        ExperimentParameterOption(
            ExperimentParameter.ArithmeticLogicReservationStationCount,
            "ALU Reservation Stations"
        ),
        ExperimentParameterOption(
            ExperimentParameter.BranchReservationStationCount,
            "Branch Reservation Stations"
        ),
        ExperimentParameterOption(ExperimentParameter.MemoryBufferCount, "Memory Buffer Count"),
        ExperimentParameterOption(ExperimentParameter.ArithmeticLogicUnitCount, "ALU Unit Count"),
        ExperimentParameterOption(ExperimentParameter.BranchUnitCount, "Branch Unit Count"),
        ExperimentParameterOption(ExperimentParameter.AddressUnitCount, "Address Unit Count"),
        ExperimentParameterOption(ExperimentParameter.MemoryUnitCount, "Memory Unit Count"),
        ExperimentParameterOption(ExperimentParameter.AddLatency, "ADD Latency"),
        ExperimentParameterOption(ExperimentParameter.LoadUpperImmediateLatency, "LUI Latency"),
        ExperimentParameterOption(
            ExperimentParameter.AddUpperImmediateToProgramCounterLatency,
            "AUIPC Latency"
        ),
        ExperimentParameterOption(ExperimentParameter.SubtractLatency, "SUB Latency"),
        ExperimentParameterOption(ExperimentParameter.ShiftLeftLogicalLatency, "SLL Latency"),
        ExperimentParameterOption(ExperimentParameter.SetLessThanSignedLatency, "SLT Latency"),
        ExperimentParameterOption(ExperimentParameter.SetLessThanUnsignedLatency, "SLTU Latency"),
        ExperimentParameterOption(ExperimentParameter.ExclusiveOrLatency, "XOR Latency"),
        ExperimentParameterOption(ExperimentParameter.ShiftRightLogicalLatency, "SRL Latency"),
        ExperimentParameterOption(ExperimentParameter.ShiftRightArithmeticLatency, "SRA Latency"),
        ExperimentParameterOption(ExperimentParameter.OrLatency, "OR Latency"),
        ExperimentParameterOption(ExperimentParameter.AndLatency, "AND Latency"),
        ExperimentParameterOption(ExperimentParameter.JumpAndLinkLatency, "JAL Latency"),
        ExperimentParameterOption(ExperimentParameter.JumpAndLinkRegisterLatency, "JALR Latency"),
        ExperimentParameterOption(ExperimentParameter.BranchEqualLatency, "BEQ Latency"),
        ExperimentParameterOption(ExperimentParameter.BranchNotEqualLatency, "BNE Latency"),
        ExperimentParameterOption(ExperimentParameter.BranchLessThanSignedLatency, "BLT Latency"),
        ExperimentParameterOption(ExperimentParameter.BranchLessThanUnsignedLatency, "BLTU Latency"),
        ExperimentParameterOption(
            ExperimentParameter.BranchGreaterThanOrEqualSignedLatency,
            "BGE Latency"
        ),
        ExperimentParameterOption(
            ExperimentParameter.BranchGreaterThanOrEqualUnsignedLatency,
            "BGEU Latency"
        ),
        ExperimentParameterOption(ExperimentParameter.AddressLatency, "Address Latency"),
        ExperimentParameterOption(ExperimentParameter.LoadWordLatency, "LW Latency"),
        ExperimentParameterOption(ExperimentParameter.LoadHalfWordLatency, "LH Latency"),
        ExperimentParameterOption(ExperimentParameter.LoadHalfWordUnsignedLatency, "LHU Latency"),
        ExperimentParameterOption(ExperimentParameter.LoadByteLatency, "LB Latency"),
        ExperimentParameterOption(ExperimentParameter.LoadByteUnsignedLatency, "LBU Latency")
    )

fun ExperimentConfigurationSnapshot.withParameter(parameter: ExperimentParameter, value: Int) =
    when (parameter) {
        ExperimentParameter.FetchWidth -> copy(fetchWidth = value)
        ExperimentParameter.IssueWidth -> copy(issueWidth = value)
        ExperimentParameter.CommitWidth -> copy(commitWidth = value)
        ExperimentParameter.InstructionQueueSize -> copy(instructionQueueSize = value)
        ExperimentParameter.ReorderBufferSize -> copy(reorderBufferSize = value)
        ExperimentParameter.BranchTargetBufferSize -> copy(branchTargetBufferSize = value)
        ExperimentParameter.BranchOutcomeCounterBitWidth -> copy(branchOutcomeCounterBitWidth = value)
        ExperimentParameter.ArithmeticLogicReservationStationCount ->
            copy(arithmeticLogicReservationStationCount = value)

        ExperimentParameter.BranchReservationStationCount ->
            copy(branchReservationStationCount = value)

        ExperimentParameter.MemoryBufferCount -> copy(memoryBufferCount = value)
        ExperimentParameter.ArithmeticLogicUnitCount -> copy(arithmeticLogicUnitCount = value)
        ExperimentParameter.BranchUnitCount -> copy(branchUnitCount = value)
        ExperimentParameter.AddressUnitCount -> copy(addressUnitCount = value)
        ExperimentParameter.MemoryUnitCount -> copy(memoryUnitCount = value)
        ExperimentParameter.AddLatency -> copy(addLatency = value)
        ExperimentParameter.LoadUpperImmediateLatency -> copy(loadUpperImmediateLatency = value)
        ExperimentParameter.AddUpperImmediateToProgramCounterLatency ->
            copy(addUpperImmediateToProgramCounterLatency = value)

        ExperimentParameter.SubtractLatency -> copy(subtractLatency = value)
        ExperimentParameter.ShiftLeftLogicalLatency -> copy(shiftLeftLogicalLatency = value)
        ExperimentParameter.SetLessThanSignedLatency -> copy(setLessThanSignedLatency = value)
        ExperimentParameter.SetLessThanUnsignedLatency -> copy(setLessThanUnsignedLatency = value)
        ExperimentParameter.ExclusiveOrLatency -> copy(exclusiveOrLatency = value)
        ExperimentParameter.ShiftRightLogicalLatency -> copy(shiftRightLogicalLatency = value)
        ExperimentParameter.ShiftRightArithmeticLatency -> copy(shiftRightArithmeticLatency = value)
        ExperimentParameter.OrLatency -> copy(orLatency = value)
        ExperimentParameter.AndLatency -> copy(andLatency = value)
        ExperimentParameter.JumpAndLinkLatency -> copy(jumpAndLinkLatency = value)
        ExperimentParameter.JumpAndLinkRegisterLatency -> copy(jumpAndLinkRegisterLatency = value)
        ExperimentParameter.BranchEqualLatency -> copy(branchEqualLatency = value)
        ExperimentParameter.BranchNotEqualLatency -> copy(branchNotEqualLatency = value)
        ExperimentParameter.BranchLessThanSignedLatency -> copy(branchLessThanSignedLatency = value)
        ExperimentParameter.BranchLessThanUnsignedLatency -> copy(branchLessThanUnsignedLatency = value)
        ExperimentParameter.BranchGreaterThanOrEqualSignedLatency ->
            copy(branchGreaterThanOrEqualSignedLatency = value)

        ExperimentParameter.BranchGreaterThanOrEqualUnsignedLatency ->
            copy(branchGreaterThanOrEqualUnsignedLatency = value)

        ExperimentParameter.AddressLatency -> copy(addressLatency = value)
        ExperimentParameter.LoadWordLatency -> copy(loadWordLatency = value)
        ExperimentParameter.LoadHalfWordLatency -> copy(loadHalfWordLatency = value)
        ExperimentParameter.LoadHalfWordUnsignedLatency -> copy(loadHalfWordUnsignedLatency = value)
        ExperimentParameter.LoadByteLatency -> copy(loadByteLatency = value)
        ExperimentParameter.LoadByteUnsignedLatency -> copy(loadByteUnsignedLatency = value)
    }

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
    listOf(
        "fetchWidth" to fetchWidth,
        "issueWidth" to issueWidth,
        "commitWidth" to commitWidth,
        "instructionQueueSize" to instructionQueueSize,
        "reorderBufferSize" to reorderBufferSize,
        "branchTargetBufferSize" to branchTargetBufferSize,
        "branchOutcomeCounterBitWidth" to branchOutcomeCounterBitWidth,
        "arithmeticLogicReservationStationCount" to arithmeticLogicReservationStationCount,
        "branchReservationStationCount" to branchReservationStationCount,
        "memoryBufferCount" to memoryBufferCount,
        "arithmeticLogicUnitCount" to arithmeticLogicUnitCount,
        "branchUnitCount" to branchUnitCount,
        "addressUnitCount" to addressUnitCount,
        "memoryUnitCount" to memoryUnitCount,
        "addLatency" to addLatency,
        "loadUpperImmediateLatency" to loadUpperImmediateLatency,
        "addUpperImmediateToProgramCounterLatency" to addUpperImmediateToProgramCounterLatency,
        "subtractLatency" to subtractLatency,
        "shiftLeftLogicalLatency" to shiftLeftLogicalLatency,
        "setLessThanSignedLatency" to setLessThanSignedLatency,
        "setLessThanUnsignedLatency" to setLessThanUnsignedLatency,
        "exclusiveOrLatency" to exclusiveOrLatency,
        "shiftRightLogicalLatency" to shiftRightLogicalLatency,
        "shiftRightArithmeticLatency" to shiftRightArithmeticLatency,
        "orLatency" to orLatency,
        "andLatency" to andLatency,
        "jumpAndLinkLatency" to jumpAndLinkLatency,
        "jumpAndLinkRegisterLatency" to jumpAndLinkRegisterLatency,
        "branchEqualLatency" to branchEqualLatency,
        "branchNotEqualLatency" to branchNotEqualLatency,
        "branchLessThanSignedLatency" to branchLessThanSignedLatency,
        "branchLessThanUnsignedLatency" to branchLessThanUnsignedLatency,
        "branchGreaterThanOrEqualSignedLatency" to branchGreaterThanOrEqualSignedLatency,
        "branchGreaterThanOrEqualUnsignedLatency" to branchGreaterThanOrEqualUnsignedLatency,
        "addressLatency" to addressLatency,
        "loadWordLatency" to loadWordLatency,
        "loadHalfWordLatency" to loadHalfWordLatency,
        "loadHalfWordUnsignedLatency" to loadHalfWordUnsignedLatency,
        "loadByteLatency" to loadByteLatency,
        "loadByteUnsignedLatency" to loadByteUnsignedLatency
    )
