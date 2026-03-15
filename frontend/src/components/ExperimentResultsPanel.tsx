import { labelForParameter, formatInstructionsPerCycle } from "../experimentUtils";
import { ExperimentParameterOption, ExperimentRunResponse } from "../types";
import { LineChart } from "./LineChart";
import { ResultOverview } from "./ResultOverview";

type Props = {
  result: ExperimentRunResponse | null;
  parameterOptions: ExperimentParameterOption[];
};

export function ExperimentResultsPanel(props: Props) {
  return (
    <section className="section">
      <div className="sectionHeader">
        <h2>IPC Plot</h2>
        <span className="sectionMeta">{props.result === null ? "No data yet" : `${props.result.points.length} run(s)`}</span>
      </div>

      {props.result === null ? (
        <p className="emptyText">
          Run an experiment to see how the selected parameter affects average instructions per cycle.
        </p>
      ) : (
        <>
          <div className="resultSummary">
            <strong>{props.result.programName}</strong>
            <span>{labelForParameter(props.result.variableParameter, props.parameterOptions)}</span>
          </div>
          <ResultOverview points={props.result.points} />
          <LineChart
            points={props.result.points}
            xLabel={labelForParameter(props.result.variableParameter, props.parameterOptions)}
            yLabel="Average IPC"
          />
          <div className="resultTable">
            {props.result.points.map((point) => (
              <div className="resultRow" key={`${point.parameterValue}-${point.instructionsPerCycle}`}>
                <span>{point.parameterValue}</span>
                <strong>{formatInstructionsPerCycle(point.instructionsPerCycle)} IPC</strong>
              </div>
            ))}
          </div>
        </>
      )}
    </section>
  );
}
