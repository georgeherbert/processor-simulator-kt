import { useEffect, useState } from "react";
import { fetchBenchmarkSource, fetchBenchmarks, fetchExperimentDefaults, runExperiment } from "./api";
import {
  BenchmarkProgram,
  ExperimentConfiguration,
  ExperimentParameter,
  ExperimentParameterOption,
  ExperimentPoint,
  ExperimentRunResponse
} from "./types";

type ConfigurationFieldDefinition = {
  key: keyof ExperimentConfiguration;
  label: string;
  parameter: ExperimentParameter;
};

type ConfigurationSectionDefinition = {
  title: string;
  fields: ConfigurationFieldDefinition[];
};

const configurationSections: ConfigurationSectionDefinition[] = [
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

function configurationFieldKeyForParameter(
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

export default function App() {
  const [benchmarks, setBenchmarks] = useState<BenchmarkProgram[]>([]);
  const [parameterOptions, setParameterOptions] = useState<ExperimentParameterOption[]>([]);
  const [selectedProgram, setSelectedProgram] = useState("");
  const [selectedParameter, setSelectedParameter] = useState<ExperimentParameter>("FetchWidth");
  const [defaultConfiguration, setDefaultConfiguration] = useState<ExperimentConfiguration | null>(null);
  const [configuration, setConfiguration] = useState<ExperimentConfiguration | null>(null);
  const [startValueText, setStartValueText] = useState("1");
  const [endValueText, setEndValueText] = useState("1");
  const [incrementText, setIncrementText] = useState("1");
  const [cycleLimitText, setCycleLimitText] = useState("100000");
  const [defaultCycleLimit, setDefaultCycleLimit] = useState(100000);
  const [result, setResult] = useState<ExperimentRunResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [sourceCode, setSourceCode] = useState("");
  const [sourceError, setSourceError] = useState<string | null>(null);
  const [sourceBusy, setSourceBusy] = useState(false);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    void loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedProgram.length === 0) {
      setSourceCode("");
      setSourceError(null);
      setSourceBusy(false);
      return;
    }

    let cancelled = false;

    async function loadSource() {
      setSourceBusy(true);
      setSourceError(null);

      const sourceResult = await fetchBenchmarkSource(selectedProgram);

      if (cancelled) {
        return;
      }

      if (sourceResult.kind === "failure") {
        setSourceCode("");
        setSourceError(sourceResult.error);
        setSourceBusy(false);
        return;
      }

      setSourceCode(sourceResult.value.sourceCode);
      setSourceBusy(false);
    }

    void loadSource();

    return () => {
      cancelled = true;
    };
  }, [selectedProgram]);

  async function loadInitialData() {
    setBusy(true);
    setError(null);

    const benchmarksResult = await fetchBenchmarks();
    if (benchmarksResult.kind === "failure") {
      setBusy(false);
      setError(benchmarksResult.error);
      return;
    }

    const defaultsResult = await fetchExperimentDefaults();
    if (defaultsResult.kind === "failure") {
      setBusy(false);
      setError(defaultsResult.error);
      return;
    }

    setBenchmarks(benchmarksResult.value.benchmarks);
    setParameterOptions(defaultsResult.value.parameters);
    setDefaultConfiguration(defaultsResult.value.configuration);
    setConfiguration(defaultsResult.value.configuration);
    setSelectedProgram(benchmarksResult.value.benchmarks[0]?.name ?? "");
    setSelectedParameter(defaultsResult.value.parameters[0]?.key ?? "FetchWidth");
    setStartValueText("1");
    setEndValueText(String(defaultsResult.value.configuration.fetchWidth));
    setIncrementText("1");
    setDefaultCycleLimit(defaultsResult.value.defaultCycleLimit);
    setCycleLimitText(String(defaultsResult.value.defaultCycleLimit));
    setBusy(false);
  }

  function handleParameterChange(nextParameter: ExperimentParameter) {
    setSelectedParameter(nextParameter);

    if (configuration === null) {
      return;
    }

    const nextField = configurationFieldKeyForParameter(nextParameter);

    if (nextField === null) {
      return;
    }

    setStartValueText("1");
    setEndValueText(String(configuration[nextField]));
    setIncrementText("1");
  }

  function handleSelectedProgramChange(nextProgram: string) {
    setSelectedProgram(nextProgram);
    setResult(null);
    setSourceCode("");
    setSourceError(null);
  }

  function handleConfigurationChange(
    field: keyof ExperimentConfiguration,
    text: string
  ) {
    if (configuration === null) {
      return;
    }

    const parsedValue = Number.parseInt(text, 10);
    const nextValue = Number.isNaN(parsedValue) ? 0 : parsedValue;
    const nextConfiguration = {
      ...configuration,
      [field]: nextValue
    };

    setConfiguration(nextConfiguration);

    const selectedField = configurationFieldKeyForParameter(selectedParameter);

    if (field === selectedField) {
      setEndValueText(String(nextValue));
    }
  }

  function handleResetConfiguration() {
    if (defaultConfiguration === null) {
      return;
    }

    const nextField = configurationFieldKeyForParameter(selectedParameter);

    if (nextField === null) {
      return;
    }

    setConfiguration(defaultConfiguration);
    setStartValueText("1");
    setEndValueText(String(defaultConfiguration[nextField]));
    setIncrementText("1");
    setCycleLimitText(String(defaultCycleLimit));
    setError(null);
  }

  async function handleRunExperiment() {
    if (configuration === null || selectedProgram.length === 0) {
      return;
    }

    const localValidationError = validateExperimentInputs(
      configuration,
      parseInteger(startValueText),
      parseInteger(endValueText),
      parseInteger(incrementText),
      parseInteger(cycleLimitText)
    );

    if (localValidationError !== null) {
      setError(localValidationError);
      return;
    }

    setBusy(true);
    setError(null);

    const experimentResult = await runExperiment(
      selectedProgram,
      configuration,
      selectedParameter,
      parseInteger(startValueText),
      parseInteger(endValueText),
      parseInteger(incrementText),
      parseInteger(cycleLimitText)
    );

    if (experimentResult.kind === "failure") {
      setBusy(false);
      setError(experimentResult.error);
      return;
    }

    setResult(experimentResult.value);
    setBusy(false);
  }

  return (
    <div className="page">
      <header className="siteHeader">
        <nav className="siteNav" aria-label="Site">
          <span className="siteNavCurrent">Processor IPC Experiments</span>
        </nav>
      </header>

      <main className="pageBody">
        <section className="intro">
          <p className="eyebrow">Processor Simulator</p>
          <h1>Benchmark Sweep Runner</h1>
          <p className="heroCopy">
            Sweep one processor width, structure, predictor, or latency parameter across a range and compare the
            average instructions per cycle achieved while finishing the selected benchmark.
          </p>
        </section>

        {error !== null ? (
          <section className="section errorSection" role="alert">
            <div className="sectionHeader">
              <h2>Error</h2>
            </div>
            <p>{error}</p>
          </section>
        ) : null}

        <div className="layout">
          <div className="controlColumn">
            <section className="section">
              <div className="sectionHeader">
                <h2>Experiment</h2>
                <span className="status neutral">{busy ? "running" : "ready"}</span>
              </div>

              <label className="field">
                <span>Benchmark</span>
                <select
                  value={selectedProgram}
                  onChange={(event) => handleSelectedProgramChange(event.target.value)}
                  disabled={busy}
                >
                  {benchmarks.map((benchmark) => (
                    <option key={benchmark.name} value={benchmark.name}>
                      {benchmark.label}
                    </option>
                  ))}
                </select>
              </label>

              <label className="field">
                <span>Variable Parameter</span>
                <select
                  value={selectedParameter}
                  onChange={(event) => {
                    const nextParameter = parseExperimentParameter(
                      event.target.value,
                      parameterOptions
                    );

                    if (nextParameter !== null) {
                      handleParameterChange(nextParameter);
                    }
                  }}
                  disabled={busy}
                >
                  {parameterOptions.map((option) => (
                    <option key={option.key} value={option.key}>
                      {option.label}
                    </option>
                  ))}
                </select>
              </label>

              <div className="inlineFields">
                <label className="field">
                  <span>Start</span>
                  <input
                    value={startValueText}
                    onChange={(event) => setStartValueText(event.target.value)}
                    disabled={busy}
                  />
                </label>

                <label className="field">
                  <span>End</span>
                  <input
                    value={endValueText}
                    onChange={(event) => setEndValueText(event.target.value)}
                    disabled={busy}
                  />
                </label>

                <label className="field">
                  <span>Increment</span>
                  <input
                    value={incrementText}
                    onChange={(event) => setIncrementText(event.target.value)}
                    disabled={busy}
                  />
                </label>
              </div>

              <label className="field">
                <span>Cycle Limit</span>
                <input
                  value={cycleLimitText}
                  onChange={(event) => setCycleLimitText(event.target.value)}
                  disabled={busy}
                />
              </label>

              <button
                className="actionButton primary"
                onClick={handleRunExperiment}
                disabled={busy || configuration === null || selectedProgram.length === 0}
              >
                Run Experiment
              </button>
            </section>

            <section className="section">
              <div className="sectionHeader">
                <h2>Base Configuration</h2>
                <span className="sectionMeta">
                  {labelForParameter(selectedParameter, parameterOptions)} varies in the experiment
                </span>
              </div>

              {configuration === null ? (
                <p className="emptyText">Loading configuration...</p>
              ) : (
                <>
                  <p className="helperText">
                    The selected sweep parameter is controlled by the experiment range, not by the base configuration.
                  </p>
                  <div className="configurationActions">
                    <button
                      className="actionButton"
                      onClick={handleResetConfiguration}
                      disabled={busy || defaultConfiguration === null}
                    >
                      Reset Baseline
                    </button>
                  </div>
                  <div className="configurationSections">
                    {configurationSections.map((section) => (
                      <section className="configurationSection" key={section.title}>
                        <h3>{section.title}</h3>
                        <div className="configurationGrid">
                          {section.fields.map((field) => {
                            const sweepControlled = field.parameter === selectedParameter;

                            return (
                              <label
                                className={`field compactField${sweepControlled ? " sweepField" : ""}`}
                                key={field.key}
                              >
                                <span>{sweepControlled ? `${field.label} (sweep controlled)` : field.label}</span>
                                <input
                                  value={String(configuration[field.key])}
                                  onChange={(event) => handleConfigurationChange(field.key, event.target.value)}
                                  disabled={busy || sweepControlled}
                                  inputMode="numeric"
                                />
                              </label>
                            );
                          })}
                        </div>
                      </section>
                    ))}
                  </div>
                </>
              )}
            </section>
          </div>

          <div className="resultsColumn">
            <section className="section">
              <div className="sectionHeader">
                <h2>Selected Benchmark</h2>
                <span className="sectionMeta">{selectedProgram.length === 0 ? "No selection" : selectedProgram}</span>
              </div>

              {selectedProgram.length === 0 ? (
                <p className="emptyText">Choose a benchmark to inspect the source that will be compiled and executed.</p>
              ) : sourceBusy ? (
                <p className="emptyText">Loading source...</p>
              ) : sourceError !== null ? (
                <p className="emptyText">{sourceError}</p>
              ) : (
                <pre className="sourceCodeBlock" aria-label="Selected benchmark source code">
                  <code>{sourceCode}</code>
                </pre>
              )}
            </section>

            <section className="section">
              <div className="sectionHeader">
                <h2>IPC Plot</h2>
                <span className="sectionMeta">{result === null ? "No data yet" : `${result.points.length} run(s)`}</span>
              </div>

              {result === null ? (
                <p className="emptyText">
                  Run an experiment to see how the selected parameter affects average instructions per cycle.
                </p>
              ) : (
                <>
                  <div className="resultSummary">
                    <strong>{result.programName}</strong>
                    <span>{labelForParameter(result.variableParameter, parameterOptions)}</span>
                  </div>
                  <ResultOverview points={result.points} />
                  <LineChart
                    points={result.points}
                    xLabel={labelForParameter(result.variableParameter, parameterOptions)}
                    yLabel="Average IPC"
                  />
                  <div className="resultTable">
                    {result.points.map((point) => (
                      <div className="resultRow" key={`${point.parameterValue}-${point.instructionsPerCycle}`}>
                        <span>{point.parameterValue}</span>
                        <strong>{formatInstructionsPerCycle(point.instructionsPerCycle)} IPC</strong>
                      </div>
                    ))}
                  </div>
                </>
              )}
            </section>
          </div>
        </div>
      </main>
    </div>
  );
}

function LineChart(props: {
  points: ExperimentPoint[];
  xLabel: string;
  yLabel: string;
}) {
  const [hoveredPoint, setHoveredPoint] = useState<ExperimentPoint | null>(null);
  const width = 760;
  const height = 320;
  const paddingLeft = 64;
  const paddingRight = 24;
  const paddingTop = 20;
  const paddingBottom = 48;
  const tooltipWidth = 196;
  const tooltipHeight = 44;
  const xValues = props.points.map((point) => point.parameterValue);
  const yValues = props.points.map((point) => point.instructionsPerCycle);
  const minimumX = Math.min(...xValues);
  const maximumX = Math.max(...xValues);
  const maximumY = Math.max(...yValues);
  const plotWidth = width - paddingLeft - paddingRight;
  const plotHeight = height - paddingTop - paddingBottom;

  const xAt = (value: number) =>
    minimumX === maximumX
      ? paddingLeft + plotWidth / 2
      : paddingLeft + ((value - minimumX) / (maximumX - minimumX)) * plotWidth;

  const yAt = (value: number) =>
    paddingTop + plotHeight - (value / Math.max(maximumY, 1)) * plotHeight;

  const linePoints = props.points
    .map((point) => `${xAt(point.parameterValue)},${yAt(point.instructionsPerCycle)}`)
    .join(" ");

  function pointLabel(point: ExperimentPoint) {
    return `${props.xLabel}: ${point.parameterValue}, ${props.yLabel}: ${formatInstructionsPerCycle(point.instructionsPerCycle)}`;
  }

  function tooltipX(point: ExperimentPoint) {
    const preferredLeft = xAt(point.parameterValue) + 10;
    const maximumLeft = width - paddingRight - tooltipWidth;

    return Math.max(paddingLeft, Math.min(preferredLeft, maximumLeft));
  }

  function tooltipY(point: ExperimentPoint) {
    const preferredAbove = yAt(point.instructionsPerCycle) - tooltipHeight - 10;

    if (preferredAbove >= paddingTop) {
      return preferredAbove;
    }

    return Math.min(
      yAt(point.instructionsPerCycle) + 12,
      height - paddingBottom - tooltipHeight
    );
  }

  return (
    <div className="chartCard">
      <svg viewBox={`0 0 ${width} ${height}`} className="chart" role="img" aria-label="Experiment line chart">
        <line x1={paddingLeft} y1={paddingTop} x2={paddingLeft} y2={height - paddingBottom} className="axisLine" />
        <line
          x1={paddingLeft}
          y1={height - paddingBottom}
          x2={width - paddingRight}
          y2={height - paddingBottom}
          className="axisLine"
        />
        <text x={paddingLeft} y={paddingTop - 6} className="axisText">
          {props.yLabel}
        </text>
        <text x={width - paddingRight} y={height - 12} textAnchor="end" className="axisText">
          {props.xLabel}
        </text>
        <text x={paddingLeft - 10} y={yAt(maximumY)} textAnchor="end" className="tickText">
          {formatInstructionsPerCycle(maximumY)}
        </text>
        <text x={paddingLeft - 10} y={height - paddingBottom} textAnchor="end" className="tickText">
          {formatInstructionsPerCycle(0)}
        </text>
        <polyline points={linePoints} className="chartLine" />
        {hoveredPoint !== null ? (
          <g
            className="chartTooltip"
            transform={`translate(${tooltipX(hoveredPoint)},${tooltipY(hoveredPoint)})`}
            aria-hidden="true"
          >
            <rect width={tooltipWidth} height={tooltipHeight} className="chartTooltipBox" />
            <text x={10} y={17} className="chartTooltipText">
              {props.xLabel}: {hoveredPoint.parameterValue}
            </text>
            <text x={10} y={33} className="chartTooltipText">
              {props.yLabel}: {formatInstructionsPerCycle(hoveredPoint.instructionsPerCycle)}
            </text>
          </g>
        ) : null}
        {props.points.map((point) => (
          <g key={`${point.parameterValue}-${point.instructionsPerCycle}`}>
            <circle
              cx={xAt(point.parameterValue)}
              cy={yAt(point.instructionsPerCycle)}
              r={4}
              className="chartPoint"
              tabIndex={0}
              aria-label={pointLabel(point)}
              onMouseEnter={() => setHoveredPoint(point)}
              onMouseLeave={() => setHoveredPoint(null)}
              onFocus={() => setHoveredPoint(point)}
              onBlur={() => setHoveredPoint(null)}
            />
            <title>{pointLabel(point)}</title>
            <text x={xAt(point.parameterValue)} y={height - paddingBottom + 18} textAnchor="middle" className="tickText">
              {point.parameterValue}
            </text>
          </g>
        ))}
      </svg>
    </div>
  );
}

function parseInteger(text: string) {
  const parsedValue = Number.parseInt(text, 10);

  return Number.isNaN(parsedValue) ? 0 : parsedValue;
}

function labelForParameter(
  parameter: ExperimentParameter,
  options: ExperimentParameterOption[]
) {
  const matchingOption = options.find((option) => option.key === parameter);

  return matchingOption === undefined ? parameter : matchingOption.label;
}

function parseExperimentParameter(
  value: string,
  options: ExperimentParameterOption[]
) {
  const matchingOption = options.find((option) => option.key === value);

  return matchingOption === undefined ? null : matchingOption.key;
}

function ResultOverview(props: {
  points: ExperimentPoint[];
}) {
  const fastestPoint = props.points.reduce((bestPoint, point) =>
    point.instructionsPerCycle > bestPoint.instructionsPerCycle ? point : bestPoint
  );
  const slowestPoint = props.points.reduce((worstPoint, point) =>
    point.instructionsPerCycle < worstPoint.instructionsPerCycle ? point : worstPoint
  );

  return (
    <div className="overviewStrip">
      <div className="metric">
        <span>Runs</span>
        <strong>{props.points.length}</strong>
      </div>
      <div className="metric">
        <span>Highest IPC</span>
        <strong>{formatInstructionsPerCycle(fastestPoint.instructionsPerCycle)} IPC</strong>
      </div>
      <div className="metric">
        <span>At Value</span>
        <strong>{fastestPoint.parameterValue}</strong>
      </div>
      <div className="metric">
        <span>Lowest IPC</span>
        <strong>{formatInstructionsPerCycle(slowestPoint.instructionsPerCycle)} IPC</strong>
      </div>
      <div className="metric">
        <span>Spread</span>
        <strong>
          {formatInstructionsPerCycle(fastestPoint.instructionsPerCycle - slowestPoint.instructionsPerCycle)} IPC
        </strong>
      </div>
    </div>
  );
}

function validateExperimentInputs(
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

function formatInstructionsPerCycle(value: number) {
  return value.toFixed(2);
}
