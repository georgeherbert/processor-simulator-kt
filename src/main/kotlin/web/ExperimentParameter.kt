package web

import kotlinx.serialization.Serializable

@Serializable
enum class ExperimentParameter(
    val label: String,
    val fieldName: String,
    private val readValue: (ExperimentConfigurationSnapshot) -> Int,
    private val updateValue: (ExperimentConfigurationSnapshot, Int) -> ExperimentConfigurationSnapshot
) {
    FetchWidth(
        "Fetch Width",
        "fetchWidth",
        { configuration -> configuration.fetchWidth },
        { configuration, value -> configuration.copy(fetchWidth = value) }
    ),
    IssueWidth(
        "Issue Width",
        "issueWidth",
        { configuration -> configuration.issueWidth },
        { configuration, value -> configuration.copy(issueWidth = value) }
    ),
    CommitWidth(
        "Commit Width",
        "commitWidth",
        { configuration -> configuration.commitWidth },
        { configuration, value -> configuration.copy(commitWidth = value) }
    ),
    InstructionQueueSize(
        "Instruction Queue Size",
        "instructionQueueSize",
        { configuration -> configuration.instructionQueueSize },
        { configuration, value -> configuration.copy(instructionQueueSize = value) }
    ),
    ReorderBufferSize(
        "Reorder Buffer Size",
        "reorderBufferSize",
        { configuration -> configuration.reorderBufferSize },
        { configuration, value -> configuration.copy(reorderBufferSize = value) }
    ),
    BranchTargetBufferSize(
        "Branch Target Buffer Size",
        "branchTargetBufferSize",
        { configuration -> configuration.branchTargetBufferSize },
        { configuration, value -> configuration.copy(branchTargetBufferSize = value) }
    ),
    BranchOutcomeCounterBitWidth(
        "Branch Predictor Bit Width",
        "branchOutcomeCounterBitWidth",
        { configuration -> configuration.branchOutcomeCounterBitWidth },
        { configuration, value -> configuration.copy(branchOutcomeCounterBitWidth = value) }
    ),
    ArithmeticLogicReservationStationCount(
        "ALU Reservation Stations",
        "arithmeticLogicReservationStationCount",
        { configuration -> configuration.arithmeticLogicReservationStationCount },
        { configuration, value ->
            configuration.copy(arithmeticLogicReservationStationCount = value)
        }
    ),
    BranchReservationStationCount(
        "Branch Reservation Stations",
        "branchReservationStationCount",
        { configuration -> configuration.branchReservationStationCount },
        { configuration, value ->
            configuration.copy(branchReservationStationCount = value)
        }
    ),
    MemoryBufferCount(
        "Memory Buffer Count",
        "memoryBufferCount",
        { configuration -> configuration.memoryBufferCount },
        { configuration, value -> configuration.copy(memoryBufferCount = value) }
    ),
    ArithmeticLogicUnitCount(
        "ALU Unit Count",
        "arithmeticLogicUnitCount",
        { configuration -> configuration.arithmeticLogicUnitCount },
        { configuration, value -> configuration.copy(arithmeticLogicUnitCount = value) }
    ),
    BranchUnitCount(
        "Branch Unit Count",
        "branchUnitCount",
        { configuration -> configuration.branchUnitCount },
        { configuration, value -> configuration.copy(branchUnitCount = value) }
    ),
    AddressUnitCount(
        "Address Unit Count",
        "addressUnitCount",
        { configuration -> configuration.addressUnitCount },
        { configuration, value -> configuration.copy(addressUnitCount = value) }
    ),
    MemoryUnitCount(
        "Memory Unit Count",
        "memoryUnitCount",
        { configuration -> configuration.memoryUnitCount },
        { configuration, value -> configuration.copy(memoryUnitCount = value) }
    ),
    AddLatency(
        "ADD Latency",
        "addLatency",
        { configuration -> configuration.addLatency },
        { configuration, value -> configuration.copy(addLatency = value) }
    ),
    LoadUpperImmediateLatency(
        "LUI Latency",
        "loadUpperImmediateLatency",
        { configuration -> configuration.loadUpperImmediateLatency },
        { configuration, value -> configuration.copy(loadUpperImmediateLatency = value) }
    ),
    AddUpperImmediateToProgramCounterLatency(
        "AUIPC Latency",
        "addUpperImmediateToProgramCounterLatency",
        { configuration -> configuration.addUpperImmediateToProgramCounterLatency },
        { configuration, value ->
            configuration.copy(addUpperImmediateToProgramCounterLatency = value)
        }
    ),
    SubtractLatency(
        "SUB Latency",
        "subtractLatency",
        { configuration -> configuration.subtractLatency },
        { configuration, value -> configuration.copy(subtractLatency = value) }
    ),
    ShiftLeftLogicalLatency(
        "SLL Latency",
        "shiftLeftLogicalLatency",
        { configuration -> configuration.shiftLeftLogicalLatency },
        { configuration, value -> configuration.copy(shiftLeftLogicalLatency = value) }
    ),
    SetLessThanSignedLatency(
        "SLT Latency",
        "setLessThanSignedLatency",
        { configuration -> configuration.setLessThanSignedLatency },
        { configuration, value -> configuration.copy(setLessThanSignedLatency = value) }
    ),
    SetLessThanUnsignedLatency(
        "SLTU Latency",
        "setLessThanUnsignedLatency",
        { configuration -> configuration.setLessThanUnsignedLatency },
        { configuration, value -> configuration.copy(setLessThanUnsignedLatency = value) }
    ),
    ExclusiveOrLatency(
        "XOR Latency",
        "exclusiveOrLatency",
        { configuration -> configuration.exclusiveOrLatency },
        { configuration, value -> configuration.copy(exclusiveOrLatency = value) }
    ),
    ShiftRightLogicalLatency(
        "SRL Latency",
        "shiftRightLogicalLatency",
        { configuration -> configuration.shiftRightLogicalLatency },
        { configuration, value -> configuration.copy(shiftRightLogicalLatency = value) }
    ),
    ShiftRightArithmeticLatency(
        "SRA Latency",
        "shiftRightArithmeticLatency",
        { configuration -> configuration.shiftRightArithmeticLatency },
        { configuration, value -> configuration.copy(shiftRightArithmeticLatency = value) }
    ),
    OrLatency(
        "OR Latency",
        "orLatency",
        { configuration -> configuration.orLatency },
        { configuration, value -> configuration.copy(orLatency = value) }
    ),
    AndLatency(
        "AND Latency",
        "andLatency",
        { configuration -> configuration.andLatency },
        { configuration, value -> configuration.copy(andLatency = value) }
    ),
    JumpAndLinkLatency(
        "JAL Latency",
        "jumpAndLinkLatency",
        { configuration -> configuration.jumpAndLinkLatency },
        { configuration, value -> configuration.copy(jumpAndLinkLatency = value) }
    ),
    JumpAndLinkRegisterLatency(
        "JALR Latency",
        "jumpAndLinkRegisterLatency",
        { configuration -> configuration.jumpAndLinkRegisterLatency },
        { configuration, value -> configuration.copy(jumpAndLinkRegisterLatency = value) }
    ),
    BranchEqualLatency(
        "BEQ Latency",
        "branchEqualLatency",
        { configuration -> configuration.branchEqualLatency },
        { configuration, value -> configuration.copy(branchEqualLatency = value) }
    ),
    BranchNotEqualLatency(
        "BNE Latency",
        "branchNotEqualLatency",
        { configuration -> configuration.branchNotEqualLatency },
        { configuration, value -> configuration.copy(branchNotEqualLatency = value) }
    ),
    BranchLessThanSignedLatency(
        "BLT Latency",
        "branchLessThanSignedLatency",
        { configuration -> configuration.branchLessThanSignedLatency },
        { configuration, value -> configuration.copy(branchLessThanSignedLatency = value) }
    ),
    BranchLessThanUnsignedLatency(
        "BLTU Latency",
        "branchLessThanUnsignedLatency",
        { configuration -> configuration.branchLessThanUnsignedLatency },
        { configuration, value -> configuration.copy(branchLessThanUnsignedLatency = value) }
    ),
    BranchGreaterThanOrEqualSignedLatency(
        "BGE Latency",
        "branchGreaterThanOrEqualSignedLatency",
        { configuration -> configuration.branchGreaterThanOrEqualSignedLatency },
        { configuration, value ->
            configuration.copy(branchGreaterThanOrEqualSignedLatency = value)
        }
    ),
    BranchGreaterThanOrEqualUnsignedLatency(
        "BGEU Latency",
        "branchGreaterThanOrEqualUnsignedLatency",
        { configuration -> configuration.branchGreaterThanOrEqualUnsignedLatency },
        { configuration, value ->
            configuration.copy(branchGreaterThanOrEqualUnsignedLatency = value)
        }
    ),
    AddressLatency(
        "Address Latency",
        "addressLatency",
        { configuration -> configuration.addressLatency },
        { configuration, value -> configuration.copy(addressLatency = value) }
    ),
    LoadWordLatency(
        "LW Latency",
        "loadWordLatency",
        { configuration -> configuration.loadWordLatency },
        { configuration, value -> configuration.copy(loadWordLatency = value) }
    ),
    LoadHalfWordLatency(
        "LH Latency",
        "loadHalfWordLatency",
        { configuration -> configuration.loadHalfWordLatency },
        { configuration, value -> configuration.copy(loadHalfWordLatency = value) }
    ),
    LoadHalfWordUnsignedLatency(
        "LHU Latency",
        "loadHalfWordUnsignedLatency",
        { configuration -> configuration.loadHalfWordUnsignedLatency },
        { configuration, value -> configuration.copy(loadHalfWordUnsignedLatency = value) }
    ),
    LoadByteLatency(
        "LB Latency",
        "loadByteLatency",
        { configuration -> configuration.loadByteLatency },
        { configuration, value -> configuration.copy(loadByteLatency = value) }
    ),
    LoadByteUnsignedLatency(
        "LBU Latency",
        "loadByteUnsignedLatency",
        { configuration -> configuration.loadByteUnsignedLatency },
        { configuration, value -> configuration.copy(loadByteUnsignedLatency = value) }
    );

    fun valueIn(configuration: ExperimentConfigurationSnapshot) =
        readValue(configuration)

    fun update(configuration: ExperimentConfigurationSnapshot, value: Int) =
        updateValue(configuration, value)
}

@Serializable
data class ExperimentParameterOption(
    val key: ExperimentParameter,
    val label: String
)
