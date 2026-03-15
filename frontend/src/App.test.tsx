import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import App from "./App";

function successResponse(body: unknown) {
  return Promise.resolve(
    new Response(JSON.stringify(body), {
      status: 200,
      headers: {
        "Content-Type": "application/json"
      }
    })
  );
}

function failureResponse(message: string, status: number) {
  return Promise.resolve(
    new Response(
      JSON.stringify({
        code: "ProcessorError",
        message
      }),
      {
        status,
        headers: {
          "Content-Type": "application/json"
        }
      }
    )
  );
}

function defaultsFixture() {
  return {
    configuration: {
      fetchWidth: 2,
      issueWidth: 2,
      commitWidth: 2,
      instructionQueueSize: 8,
      reorderBufferSize: 8,
      branchTargetBufferSize: 8,
      branchOutcomeCounterBitWidth: 2,
      arithmeticLogicReservationStationCount: 8,
      branchReservationStationCount: 8,
      memoryBufferCount: 8,
      arithmeticLogicUnitCount: 1,
      branchUnitCount: 1,
      addressUnitCount: 1,
      memoryUnitCount: 1,
      addLatency: 1,
      loadUpperImmediateLatency: 1,
      addUpperImmediateToProgramCounterLatency: 1,
      subtractLatency: 1,
      shiftLeftLogicalLatency: 1,
      setLessThanSignedLatency: 1,
      setLessThanUnsignedLatency: 1,
      exclusiveOrLatency: 1,
      shiftRightLogicalLatency: 1,
      shiftRightArithmeticLatency: 1,
      orLatency: 1,
      andLatency: 1,
      jumpAndLinkLatency: 1,
      jumpAndLinkRegisterLatency: 1,
      branchEqualLatency: 1,
      branchNotEqualLatency: 1,
      branchLessThanSignedLatency: 1,
      branchLessThanUnsignedLatency: 1,
      branchGreaterThanOrEqualSignedLatency: 1,
      branchGreaterThanOrEqualUnsignedLatency: 1,
      addressLatency: 1,
      loadWordLatency: 1,
      loadHalfWordLatency: 1,
      loadHalfWordUnsignedLatency: 1,
      loadByteLatency: 1,
      loadByteUnsignedLatency: 1
    },
    parameters: [
      {
        key: "FetchWidth",
        label: "Fetch Width"
      },
      {
        key: "IssueWidth",
        label: "Issue Width"
      }
    ],
    defaultCycleLimit: 100000
  };
}

function experimentResultFixture() {
  return {
    programName: "arithmetic.c",
    variableParameter: "IssueWidth",
    points: [
      {
        parameterValue: 1,
        instructionsPerCycle: 0.75
      },
      {
        parameterValue: 2,
        instructionsPerCycle: 1.0
      }
    ]
  };
}

function sourceFixture(programName: string, body: string) {
  return {
    programName,
    sourceCode: body
  };
}

describe("App", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("loads the experiment controls on startup", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockImplementationOnce(() =>
          successResponse({
            benchmarks: [
              {
                name: "arithmetic.c",
                label: "Arithmetic"
              }
            ]
          })
        )
        .mockImplementationOnce(() => successResponse(defaultsFixture()))
        .mockImplementationOnce(() =>
          successResponse(sourceFixture("arithmetic.c", "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"))
        )
    );

    render(<App />);

    expect(await screen.findByRole("button", { name: "Run Experiment" })).toBeInTheDocument();
    expect(await screen.findByRole("option", { name: "Arithmetic" })).toBeInTheDocument();
    expect(await screen.findByDisplayValue("100000")).toBeInTheDocument();
    expect(await screen.findByLabelText("Selected benchmark source code")).toHaveTextContent("int32_t main");
  });

  it("marks the selected sweep parameter as sweep controlled in the base configuration", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockImplementationOnce(() =>
          successResponse({
            benchmarks: [
              {
                name: "arithmetic.c",
                label: "Arithmetic"
              }
            ]
          })
        )
        .mockImplementationOnce(() => successResponse(defaultsFixture()))
        .mockImplementationOnce(() =>
          successResponse(sourceFixture("arithmetic.c", "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"))
        )
    );

    render(<App />);

    expect(await screen.findByLabelText("Fetch Width (sweep controlled)")).toBeDisabled();
  });

  it("runs an experiment and renders the resulting ipc plot", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockImplementationOnce(() =>
          successResponse({
            benchmarks: [
              {
                name: "arithmetic.c",
                label: "Arithmetic"
              }
            ]
          })
        )
        .mockImplementationOnce(() => successResponse(defaultsFixture()))
        .mockImplementationOnce(() =>
          successResponse(sourceFixture("arithmetic.c", "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"))
        )
        .mockImplementationOnce(() => successResponse(experimentResultFixture()))
    );

    render(<App />);
    await userEvent.click(await screen.findByRole("button", { name: "Run Experiment" }));

    expect(await screen.findAllByText("arithmetic.c")).toHaveLength(2);
    expect(await screen.findAllByText("0.75 IPC")).toHaveLength(2);
    expect(await screen.findByRole("img", { name: "Experiment line chart" })).toBeInTheDocument();
  });

  it("shows point coordinates when hovering a chart point", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockImplementationOnce(() =>
          successResponse({
            benchmarks: [
              {
                name: "arithmetic.c",
                label: "Arithmetic"
              }
            ]
          })
        )
        .mockImplementationOnce(() => successResponse(defaultsFixture()))
        .mockImplementationOnce(() =>
          successResponse(sourceFixture("arithmetic.c", "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"))
        )
        .mockImplementationOnce(() => successResponse(experimentResultFixture()))
    );

    render(<App />);
    await userEvent.click(await screen.findByRole("button", { name: "Run Experiment" }));

    const firstPoint = await screen.findByLabelText("Issue Width: 1, Average IPC: 0.75");
    fireEvent.mouseEnter(firstPoint);

    expect(await screen.findByText("Issue Width: 1")).toBeInTheDocument();
    expect(await screen.findByText("Average IPC: 0.75")).toBeInTheDocument();
  });

  it("shows local validation errors before sending the experiment request", async () => {
    const fetchMock = vi
      .fn()
      .mockImplementationOnce(() =>
        successResponse({
          benchmarks: [
            {
              name: "arithmetic.c",
              label: "Arithmetic"
            }
          ]
        })
      )
      .mockImplementationOnce(() => successResponse(defaultsFixture()))
      .mockImplementationOnce(() =>
        successResponse(sourceFixture("arithmetic.c", "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"))
      );

    vi.stubGlobal("fetch", fetchMock);

    render(<App />);

    const startInput = await screen.findByLabelText("Start");
    const endInput = screen.getByLabelText("End");

    await userEvent.clear(startInput);
    await userEvent.type(startInput, "4");
    await userEvent.clear(endInput);
    await userEvent.type(endInput, "2");
    await userEvent.click(screen.getByRole("button", { name: "Run Experiment" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid experiment range");
    expect(fetchMock).toHaveBeenCalledTimes(3);
  });

  it("shows backend errors from experiment runs", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockImplementationOnce(() =>
          successResponse({
            benchmarks: [
              {
                name: "arithmetic.c",
                label: "Arithmetic"
              }
            ]
          })
        )
        .mockImplementationOnce(() => successResponse(defaultsFixture()))
        .mockImplementationOnce(() =>
          successResponse(sourceFixture("arithmetic.c", "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"))
        )
        .mockImplementationOnce(() => failureResponse("Invalid experiment range start=4 end=2 increment=1", 400))
    );

    render(<App />);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: "Run Experiment" })).toBeInTheDocument();
    });

    const startInput = screen.getByLabelText("Start");
    const endInput = screen.getByLabelText("End");

    await userEvent.clear(startInput);
    await userEvent.type(startInput, "4");
    await userEvent.clear(endInput);
    await userEvent.type(endInput, "2");
    await userEvent.click(screen.getByRole("button", { name: "Run Experiment" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Invalid experiment range");
  });

  it("loads the source for the newly selected benchmark", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn()
        .mockImplementationOnce(() =>
          successResponse({
            benchmarks: [
              {
                name: "arithmetic.c",
                label: "Arithmetic"
              },
              {
                name: "fibonacci.c",
                label: "Fibonacci"
              }
            ]
          })
        )
        .mockImplementationOnce(() => successResponse(defaultsFixture()))
        .mockImplementationOnce(() =>
          successResponse(sourceFixture("arithmetic.c", "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"))
        )
        .mockImplementationOnce(() =>
          successResponse(sourceFixture("fibonacci.c", "#include <stdint.h>\n\nint32_t main() {\n    return 2;\n}\n"))
        )
    );

    render(<App />);

    const benchmarkSelect = await screen.findByLabelText("Benchmark");
    await userEvent.selectOptions(benchmarkSelect, "fibonacci.c");

    expect(await screen.findByLabelText("Selected benchmark source code")).toHaveTextContent("return 2;");
  });
});
