# processor-simulator-kt

Immutable Kotlin/JVM RV32I processor simulator with a Ktor backend and a React frontend for benchmark sweep experiments.

The repository ships the Kotlin simulator, web app, and bundled benchmark programs directly.

## Prerequisites

- JDK 21
- Node.js and npm
- `riscv64-unknown-elf-gcc`
- `riscv64-unknown-elf-objcopy`

The backend compiles the bundled benchmark C programs on demand, so the RISC-V toolchain needs to be on `PATH`.

## Run The App

Run the backend and use the frontend it serves:

```bash
./gradlew run
```

Then open:

```text
http://localhost:8080
```

The Ktor server serves both:

- the JSON API under `/api`
- the built frontend under `/`

The frontend lets you:

- choose a bundled benchmark program
- configure a baseline processor
- sweep one processor parameter across a numeric range
- plot parameter value on the X axis against average instructions per cycle on the Y axis

## Useful Commands

```bash
./pre-commit
```

```bash
./gradlew run
```

## Backend Structure

- `src/main/kotlin/web` contains the Ktor server, API wiring, experiment service, and benchmark-source endpoints.
- `frontend/` contains the React/Vite UI.
- `src/main/resources/benchmark_kernels` contains the bundled C examples that can be compiled and run through the simulator.
