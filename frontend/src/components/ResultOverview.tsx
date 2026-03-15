import { formatInstructionsPerCycle } from "../experimentUtils";
import { ExperimentPoint } from "../types";

type Props = {
  points: ExperimentPoint[];
};

export function ResultOverview(props: Props) {
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
