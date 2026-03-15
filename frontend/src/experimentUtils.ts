import { ExperimentConfiguration, ExperimentParameter, ExperimentParameterOption } from "./types";

export function parseInteger(text: string) {
  const parsedValue = Number.parseInt(text, 10);

  return Number.isNaN(parsedValue) ? 0 : parsedValue;
}

export function labelForParameter(
  parameter: ExperimentParameter,
  options: ExperimentParameterOption[]
) {
  const matchingOption = options.find((option) => option.key === parameter);

  return matchingOption === undefined ? parameter : matchingOption.label;
}

export function parseExperimentParameter(
  value: string,
  options: ExperimentParameterOption[]
) {
  const matchingOption = options.find((option) => option.key === value);

  return matchingOption === undefined ? null : matchingOption.key;
}

export function validateExperimentInputs(
  configuration: ExperimentConfiguration,
  startValue: number,
  endValue: number,
  increment: number,
  cycleLimit: number
) {
  const invalidConfigurationEntry = Object.entries(configuration).find((entry) => entry[1] <= 0);

  if (invalidConfigurationEntry !== undefined) {
    return `${invalidConfigurationEntry[0]} must be positive`;
  }

  if (increment <= 0 || startValue > endValue) {
    return `Invalid experiment range start=${startValue} end=${endValue} increment=${increment}`;
  }

  if (cycleLimit <= 0) {
    return `Cycle count must be positive: ${cycleLimit}`;
  }

  return null;
}

export function formatInstructionsPerCycle(value: number) {
  return value.toFixed(2);
}
