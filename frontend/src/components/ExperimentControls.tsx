import { ExperimentParameter, ExperimentParameterOption } from "../types";
import { parseExperimentParameter } from "../experimentUtils";

type Props = {
  benchmarks: { name: string; label: string }[];
  selectedProgram: string;
  selectedParameter: ExperimentParameter;
  parameterOptions: ExperimentParameterOption[];
  startValueText: string;
  endValueText: string;
  incrementText: string;
  cycleLimitText: string;
  busy: boolean;
  configurationReady: boolean;
  onProgramChange(nextProgram: string): void;
  onParameterChange(nextParameter: ExperimentParameter): void;
  onStartValueChange(nextValue: string): void;
  onEndValueChange(nextValue: string): void;
  onIncrementChange(nextValue: string): void;
  onCycleLimitChange(nextValue: string): void;
  onRunExperiment(): void;
};

export function ExperimentControls(props: Props) {
  return (
    <section className="section">
      <div className="sectionHeader">
        <h2>Experiment</h2>
        <span className="status neutral">{props.busy ? "running" : "ready"}</span>
      </div>

      <label className="field">
        <span>Benchmark</span>
        <select
          value={props.selectedProgram}
          onChange={(event) => props.onProgramChange(event.target.value)}
          disabled={props.busy}
        >
          {props.benchmarks.map((benchmark) => (
            <option key={benchmark.name} value={benchmark.name}>
              {benchmark.label}
            </option>
          ))}
        </select>
      </label>

      <label className="field">
        <span>Variable Parameter</span>
        <select
          value={props.selectedParameter}
          onChange={(event) => {
            const nextParameter = parseExperimentParameter(
              event.target.value,
              props.parameterOptions
            );

            if (nextParameter !== null) {
              props.onParameterChange(nextParameter);
            }
          }}
          disabled={props.busy}
        >
          {props.parameterOptions.map((option) => (
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
            value={props.startValueText}
            onChange={(event) => props.onStartValueChange(event.target.value)}
            disabled={props.busy}
          />
        </label>

        <label className="field">
          <span>End</span>
          <input
            value={props.endValueText}
            onChange={(event) => props.onEndValueChange(event.target.value)}
            disabled={props.busy}
          />
        </label>

        <label className="field">
          <span>Increment</span>
          <input
            value={props.incrementText}
            onChange={(event) => props.onIncrementChange(event.target.value)}
            disabled={props.busy}
          />
        </label>
      </div>

      <label className="field">
        <span>Cycle Limit</span>
        <input
          value={props.cycleLimitText}
          onChange={(event) => props.onCycleLimitChange(event.target.value)}
          disabled={props.busy}
        />
      </label>

      <button
        className="actionButton primary"
        onClick={props.onRunExperiment}
        disabled={props.busy || !props.configurationReady || props.selectedProgram.length === 0}
      >
        Run Experiment
      </button>
    </section>
  );
}
