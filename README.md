# processor-simulator-kt

Immutable Kotlin/JVM RV32I processor simulator with a Ktor backend and a React frontend for benchmark sweep experiments.

## Prerequisites

- JDK 21
- Node.js and npm
- `riscv64-unknown-elf-gcc`
- `riscv64-unknown-elf-objcopy`

The backend compiles the bundled benchmark C programs on demand, so the RISC-V toolchain needs to be on `PATH`.

## Run The App

### Simplest path

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

### Frontend dev mode

If you want hot reload for the React app:

Terminal 1:

```bash
./gradlew run
```

Terminal 2:

```bash
cd frontend
npm ci
npm run dev
```

Then open:

```text
http://localhost:5173
```

The Vite dev server proxies `/api` to `http://127.0.0.1:8080`.

## Useful Commands

Run the full project checks:

```bash
./gradlew clean build
```

Run the stricter local gate:

```bash
./pre-commit
```

Run only the frontend tests:

```bash
./gradlew frontendTest
```

Run only the frontend build:

```bash
./gradlew frontendBuild
```

## Backend Structure

- `src/main/kotlin/web` contains the Ktor server, API wiring, simulation service, and snapshot mapping.
- `frontend/` contains the React/Vite UI.
- `src/main/resources/benchmark_kernels` contains the bundled C examples that can be compiled and run through the simulator.
