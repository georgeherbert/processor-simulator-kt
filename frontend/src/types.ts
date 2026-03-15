export type BenchmarkProgram = {
  name: string;
  label: string;
};

export type BenchmarkProgramsResponse = {
  benchmarks: BenchmarkProgram[];
};

export type BenchmarkProgramSourceResponse = {
  programName: string;
  sourceCode: string;
};

export type ExperimentParameter =
  | "FetchWidth"
  | "IssueWidth"
  | "CommitWidth"
  | "InstructionQueueSize"
  | "ReorderBufferSize"
  | "BranchTargetBufferSize"
  | "BranchOutcomeCounterBitWidth"
  | "ArithmeticLogicReservationStationCount"
  | "BranchReservationStationCount"
  | "MemoryBufferCount"
  | "ArithmeticLogicUnitCount"
  | "BranchUnitCount"
  | "AddressUnitCount"
  | "MemoryUnitCount"
  | "AddLatency"
  | "LoadUpperImmediateLatency"
  | "AddUpperImmediateToProgramCounterLatency"
  | "SubtractLatency"
  | "ShiftLeftLogicalLatency"
  | "SetLessThanSignedLatency"
  | "SetLessThanUnsignedLatency"
  | "ExclusiveOrLatency"
  | "ShiftRightLogicalLatency"
  | "ShiftRightArithmeticLatency"
  | "OrLatency"
  | "AndLatency"
  | "JumpAndLinkLatency"
  | "JumpAndLinkRegisterLatency"
  | "BranchEqualLatency"
  | "BranchNotEqualLatency"
  | "BranchLessThanSignedLatency"
  | "BranchLessThanUnsignedLatency"
  | "BranchGreaterThanOrEqualSignedLatency"
  | "BranchGreaterThanOrEqualUnsignedLatency"
  | "AddressLatency"
  | "LoadWordLatency"
  | "LoadHalfWordLatency"
  | "LoadHalfWordUnsignedLatency"
  | "LoadByteLatency"
  | "LoadByteUnsignedLatency";

export type ExperimentParameterOption = {
  key: ExperimentParameter;
  label: string;
};

export type ExperimentConfiguration = {
  fetchWidth: number;
  issueWidth: number;
  commitWidth: number;
  instructionQueueSize: number;
  reorderBufferSize: number;
  branchTargetBufferSize: number;
  branchOutcomeCounterBitWidth: number;
  arithmeticLogicReservationStationCount: number;
  branchReservationStationCount: number;
  memoryBufferCount: number;
  arithmeticLogicUnitCount: number;
  branchUnitCount: number;
  addressUnitCount: number;
  memoryUnitCount: number;
  addLatency: number;
  loadUpperImmediateLatency: number;
  addUpperImmediateToProgramCounterLatency: number;
  subtractLatency: number;
  shiftLeftLogicalLatency: number;
  setLessThanSignedLatency: number;
  setLessThanUnsignedLatency: number;
  exclusiveOrLatency: number;
  shiftRightLogicalLatency: number;
  shiftRightArithmeticLatency: number;
  orLatency: number;
  andLatency: number;
  jumpAndLinkLatency: number;
  jumpAndLinkRegisterLatency: number;
  branchEqualLatency: number;
  branchNotEqualLatency: number;
  branchLessThanSignedLatency: number;
  branchLessThanUnsignedLatency: number;
  branchGreaterThanOrEqualSignedLatency: number;
  branchGreaterThanOrEqualUnsignedLatency: number;
  addressLatency: number;
  loadWordLatency: number;
  loadHalfWordLatency: number;
  loadHalfWordUnsignedLatency: number;
  loadByteLatency: number;
  loadByteUnsignedLatency: number;
};

export type ExperimentDefaultsResponse = {
  configuration: ExperimentConfiguration;
  parameters: ExperimentParameterOption[];
  defaultCycleLimit: number;
};

export type ExperimentPoint = {
  parameterValue: number;
  instructionsPerCycle: number;
};

export type ExperimentRunResponse = {
  programName: string;
  variableParameter: ExperimentParameter;
  points: ExperimentPoint[];
};

export type ErrorResponse = {
  code: string;
  message: string;
};

export type ApiResult<T> =
  | {
      kind: "success";
      value: T;
    }
  | {
      kind: "failure";
      error: string;
    };
