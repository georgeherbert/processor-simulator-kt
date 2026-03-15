import { ExperimentConfiguration, ExperimentParameter } from "./types";

export type ConfigurationFieldDefinition = {
  key: keyof ExperimentConfiguration;
  label: string;
  parameter: ExperimentParameter;
};

export type ConfigurationSectionDefinition = {
  title: string;
  fields: ConfigurationFieldDefinition[];
};

export const configurationSections: ConfigurationSectionDefinition[] = [
  {
    title: "Widths and Queues",
    fields: [
      { key: "fetchWidth", label: "Fetch Width", parameter: "FetchWidth" },
      { key: "issueWidth", label: "Issue Width", parameter: "IssueWidth" },
      { key: "commitWidth", label: "Commit Width", parameter: "CommitWidth" },
      { key: "instructionQueueSize", label: "Instruction Queue Size", parameter: "InstructionQueueSize" },
      { key: "reorderBufferSize", label: "Reorder Buffer Size", parameter: "ReorderBufferSize" }
    ]
  },
  {
    title: "Predictor and Structures",
    fields: [
      { key: "branchTargetBufferSize", label: "Branch Target Buffer Size", parameter: "BranchTargetBufferSize" },
      {
        key: "branchOutcomeCounterBitWidth",
        label: "Branch Predictor Bit Width",
        parameter: "BranchOutcomeCounterBitWidth"
      },
      {
        key: "arithmeticLogicReservationStationCount",
        label: "ALU Reservation Stations",
        parameter: "ArithmeticLogicReservationStationCount"
      },
      {
        key: "branchReservationStationCount",
        label: "Branch Reservation Stations",
        parameter: "BranchReservationStationCount"
      },
      { key: "memoryBufferCount", label: "Memory Buffer Count", parameter: "MemoryBufferCount" }
    ]
  },
  {
    title: "Execution Units",
    fields: [
      { key: "arithmeticLogicUnitCount", label: "ALU Unit Count", parameter: "ArithmeticLogicUnitCount" },
      { key: "branchUnitCount", label: "Branch Unit Count", parameter: "BranchUnitCount" },
      { key: "addressUnitCount", label: "Address Unit Count", parameter: "AddressUnitCount" },
      { key: "memoryUnitCount", label: "Memory Unit Count", parameter: "MemoryUnitCount" }
    ]
  },
  {
    title: "ALU Latencies",
    fields: [
      { key: "addLatency", label: "ADD Latency", parameter: "AddLatency" },
      { key: "loadUpperImmediateLatency", label: "LUI Latency", parameter: "LoadUpperImmediateLatency" },
      {
        key: "addUpperImmediateToProgramCounterLatency",
        label: "AUIPC Latency",
        parameter: "AddUpperImmediateToProgramCounterLatency"
      },
      { key: "subtractLatency", label: "SUB Latency", parameter: "SubtractLatency" },
      { key: "shiftLeftLogicalLatency", label: "SLL Latency", parameter: "ShiftLeftLogicalLatency" },
      { key: "setLessThanSignedLatency", label: "SLT Latency", parameter: "SetLessThanSignedLatency" },
      { key: "setLessThanUnsignedLatency", label: "SLTU Latency", parameter: "SetLessThanUnsignedLatency" },
      { key: "exclusiveOrLatency", label: "XOR Latency", parameter: "ExclusiveOrLatency" },
      { key: "shiftRightLogicalLatency", label: "SRL Latency", parameter: "ShiftRightLogicalLatency" },
      { key: "shiftRightArithmeticLatency", label: "SRA Latency", parameter: "ShiftRightArithmeticLatency" },
      { key: "orLatency", label: "OR Latency", parameter: "OrLatency" },
      { key: "andLatency", label: "AND Latency", parameter: "AndLatency" }
    ]
  },
  {
    title: "Branch Latencies",
    fields: [
      { key: "jumpAndLinkLatency", label: "JAL Latency", parameter: "JumpAndLinkLatency" },
      { key: "jumpAndLinkRegisterLatency", label: "JALR Latency", parameter: "JumpAndLinkRegisterLatency" },
      { key: "branchEqualLatency", label: "BEQ Latency", parameter: "BranchEqualLatency" },
      { key: "branchNotEqualLatency", label: "BNE Latency", parameter: "BranchNotEqualLatency" },
      { key: "branchLessThanSignedLatency", label: "BLT Latency", parameter: "BranchLessThanSignedLatency" },
      { key: "branchLessThanUnsignedLatency", label: "BLTU Latency", parameter: "BranchLessThanUnsignedLatency" },
      {
        key: "branchGreaterThanOrEqualSignedLatency",
        label: "BGE Latency",
        parameter: "BranchGreaterThanOrEqualSignedLatency"
      },
      {
        key: "branchGreaterThanOrEqualUnsignedLatency",
        label: "BGEU Latency",
        parameter: "BranchGreaterThanOrEqualUnsignedLatency"
      }
    ]
  },
  {
    title: "Address and Memory Latencies",
    fields: [
      { key: "addressLatency", label: "Address Latency", parameter: "AddressLatency" },
      { key: "loadWordLatency", label: "LW Latency", parameter: "LoadWordLatency" },
      { key: "loadHalfWordLatency", label: "LH Latency", parameter: "LoadHalfWordLatency" },
      {
        key: "loadHalfWordUnsignedLatency",
        label: "LHU Latency",
        parameter: "LoadHalfWordUnsignedLatency"
      },
      { key: "loadByteLatency", label: "LB Latency", parameter: "LoadByteLatency" },
      { key: "loadByteUnsignedLatency", label: "LBU Latency", parameter: "LoadByteUnsignedLatency" }
    ]
  }
];

export function configurationFieldKeyForParameter(
  parameter: ExperimentParameter
) {
  for (const section of configurationSections) {
    const matchingField = section.fields.find((field) => field.parameter === parameter);

    if (matchingField !== undefined) {
      return matchingField.key;
    }
  }

  return null;
}
