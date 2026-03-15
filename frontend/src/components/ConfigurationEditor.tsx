import { configurationSections } from "../configurationSections";
import { labelForParameter } from "../experimentUtils";
import { ExperimentConfiguration, ExperimentParameter, ExperimentParameterOption } from "../types";

type Props = {
  configuration: ExperimentConfiguration | null;
  defaultConfiguration: ExperimentConfiguration | null;
  selectedParameter: ExperimentParameter;
  parameterOptions: ExperimentParameterOption[];
  busy: boolean;
  onReset(): void;
  onConfigurationChange(field: keyof ExperimentConfiguration, text: string): void;
};

export function ConfigurationEditor(props: Props) {
  return (
    <section className="section">
      <div className="sectionHeader">
        <h2>Base Configuration</h2>
        <span className="sectionMeta">
          {labelForParameter(props.selectedParameter, props.parameterOptions)} varies in the experiment
        </span>
      </div>

      {props.configuration === null ? (
        <p className="emptyText">Loading configuration...</p>
      ) : (
        <>
          <p className="helperText">
            The selected sweep parameter is controlled by the experiment range, not by the base configuration.
          </p>
          <div className="configurationActions">
            <button
              className="actionButton"
              onClick={props.onReset}
              disabled={props.busy || props.defaultConfiguration === null}
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
                    const sweepControlled = field.parameter === props.selectedParameter;

                    return (
                      <label
                        className={`field compactField${sweepControlled ? " sweepField" : ""}`}
                        key={field.key}
                      >
                        <span>{sweepControlled ? `${field.label} (sweep controlled)` : field.label}</span>
                        <input
                          value={String(props.configuration[field.key])}
                          onChange={(event) => props.onConfigurationChange(field.key, event.target.value)}
                          disabled={props.busy || sweepControlled}
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
  );
}
