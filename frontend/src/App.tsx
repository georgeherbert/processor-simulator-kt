import { useEffect, useState } from "react";
import { fetchBenchmarkSource, fetchBenchmarks, fetchExperimentDefaults, runExperiment } from "./api";
import { ConfigurationEditor } from "./components/ConfigurationEditor";
import { BenchmarkSourcePanel } from "./components/BenchmarkSourcePanel";
import { ExperimentControls } from "./components/ExperimentControls";
import { ExperimentResultsPanel } from "./components/ExperimentResultsPanel";
import { configurationFieldKeyForParameter } from "./configurationSections";
import { labelForParameter, parseInteger, validateExperimentInputs } from "./experimentUtils";
import { BenchmarkProgram, ExperimentConfiguration, ExperimentParameter, ExperimentParameterOption, ExperimentRunResponse } from "./types";

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
            <ExperimentControls
              benchmarks={benchmarks}
              selectedProgram={selectedProgram}
              selectedParameter={selectedParameter}
              parameterOptions={parameterOptions}
              startValueText={startValueText}
              endValueText={endValueText}
              incrementText={incrementText}
              cycleLimitText={cycleLimitText}
              busy={busy}
              configurationReady={configuration !== null}
              onProgramChange={handleSelectedProgramChange}
              onParameterChange={handleParameterChange}
              onStartValueChange={setStartValueText}
              onEndValueChange={setEndValueText}
              onIncrementChange={setIncrementText}
              onCycleLimitChange={setCycleLimitText}
              onRunExperiment={handleRunExperiment}
            />

            <ConfigurationEditor
              configuration={configuration}
              defaultConfiguration={defaultConfiguration}
              selectedParameter={selectedParameter}
              parameterOptions={parameterOptions}
              busy={busy}
              onReset={handleResetConfiguration}
              onConfigurationChange={handleConfigurationChange}
            />
          </div>

          <div className="resultsColumn">
            <BenchmarkSourcePanel
              selectedProgram={selectedProgram}
              sourceCode={sourceCode}
              sourceError={sourceError}
              sourceBusy={sourceBusy}
            />

            <ExperimentResultsPanel
              result={result}
              parameterOptions={parameterOptions}
            />
          </div>
        </div>
      </main>
    </div>
  );
}
