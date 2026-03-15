type Props = {
  selectedProgram: string;
  sourceCode: string;
  sourceError: string | null;
  sourceBusy: boolean;
};

export function BenchmarkSourcePanel(props: Props) {
  return (
    <section className="section">
      <div className="sectionHeader">
        <h2>Selected Benchmark</h2>
        <span className="sectionMeta">{props.selectedProgram.length === 0 ? "No selection" : props.selectedProgram}</span>
      </div>

      {props.selectedProgram.length === 0 ? (
        <p className="emptyText">Choose a benchmark to inspect the source that will be compiled and executed.</p>
      ) : props.sourceBusy ? (
        <p className="emptyText">Loading source...</p>
      ) : props.sourceError !== null ? (
        <p className="emptyText">{props.sourceError}</p>
      ) : (
        <pre className="sourceCodeBlock" aria-label="Selected benchmark source code">
          <code>{props.sourceCode}</code>
        </pre>
      )}
    </section>
  );
}
