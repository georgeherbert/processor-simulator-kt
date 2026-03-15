import {
  ApiResult,
  BenchmarkProgramsResponse,
  BenchmarkProgramSourceResponse,
  ErrorResponse,
  ExperimentConfiguration,
  ExperimentDefaultsResponse,
  ExperimentParameter,
  ExperimentRunResponse
} from "./types";

async function request<T>(
  path: string,
  init: RequestInit | undefined
): Promise<ApiResult<T>> {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json"
    },
    ...init
  });

  const body = await parseJson<T | ErrorResponse>(response);

  if (response.ok) {
    return {
      kind: "success",
      value: body as T
    };
  }

  return {
    kind: "failure",
    error:
      body !== null && "message" in body && typeof body.message === "string"
        ? body.message
        : `Request failed with status ${response.status}`
  };
}

async function parseJson<T>(response: Response): Promise<T | null> {
  const text = await response.text();

  return text.length === 0 ? null : (JSON.parse(text) as T);
}

export function fetchBenchmarks() {
  return request<BenchmarkProgramsResponse>("/api/benchmarks", undefined);
}

export function fetchBenchmarkSource(programName: string) {
  return request<BenchmarkProgramSourceResponse>(`/api/benchmarks/${encodeURIComponent(programName)}/source`, undefined);
}

export function fetchExperimentDefaults() {
  return request<ExperimentDefaultsResponse>("/api/experiment/defaults", undefined);
}

export function runExperiment(
  programName: string,
  baseConfiguration: ExperimentConfiguration,
  variableParameter: ExperimentParameter,
  startValue: number,
  endValue: number,
  increment: number,
  cycleLimit: number
) {
  return request<ExperimentRunResponse>("/api/experiment/run", {
    method: "POST",
    body: JSON.stringify({
      programName,
      baseConfiguration,
      variableParameter,
      startValue,
      endValue,
      increment,
      cycleLimit
    })
  });
}
