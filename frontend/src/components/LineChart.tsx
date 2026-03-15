import { useState } from "react";
import { formatInstructionsPerCycle } from "../experimentUtils";
import { ExperimentPoint } from "../types";

type Props = {
  points: ExperimentPoint[];
  xLabel: string;
  yLabel: string;
};

export function LineChart(props: Props) {
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
