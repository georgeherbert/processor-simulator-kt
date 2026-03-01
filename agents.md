# Agents Guide

## Purpose
This repository is a Kotlin/JVM processor simulator with an immutable-domain-model style.
Use this file as the default operating guide for contributors and coding agents.

## Stack and Baseline
- Kotlin JVM project (Gradle Kotlin DSL)
- JDK toolchain: 21
- Test stack: JUnit 5 + Strikt
- Build command of record: `./gradlew clean build`

## Day-to-Day Workflow
- Run `./pre-commit` before committing.
- `pre-commit` intentionally runs `./gradlew clean build` to reduce local/CI drift.
- For quick feedback during development, `./gradlew test` is fine, but pre-commit remains the stricter gate.

## Architecture Rule
- Every concrete class must implement an interface.
- Design for testability by default:
  - Production code depends on interfaces, not implementations.
  - Add stubs/fakes in `src/testFixtures` only when a real test seam needs them.

## Reference Scope (C Implementation)
- Reference source is under `reference/src_superscalar/`.
- Reference architecture is documented in `reference/presentation.pdf`.
- The Kotlin implementation target includes all major components from the reference:
  - CPU orchestration
  - Fetch unit
  - Decode unit
  - Instruction queue
  - Issue unit
  - ALU reservation stations
  - Branch reservation stations
  - Memory buffers
  - ALU units
  - Branch units
  - Address units
  - Memory/load units
  - Common data bus (CDB)
  - Reorder buffer (ROB)
  - Commit unit
  - Register file
  - Main memory
  - Branch target buffer (BTB) with saturating counters
  - RV32I decode/operation model
  - Pipeline control state (for example PC source and mispredict redirect)

## Target Kotlin Architecture (Required Shape)
- Keep the same logical processor decomposition as the reference, but not the same coding style.
- Model the simulator as explicit cycle transitions: `currentState -> nextState`.
- Prefer immutable state snapshots and pure transition functions over in-place mutation.
- Encapsulate each subsystem behind an interface and provide at least:
  - Production implementation in `src/main`.
  - Stub/fake implementation in `src/testFixtures` when test seams are needed.
- Keep composition explicit in a top-level CPU coordinator that wires interfaces, not concrete classes.
- Preserve configurable widths/counts/latencies as typed configuration data instead of C preprocessor macros.
- Represent domain concepts with types (addresses, instruction words, ROB ids, station ids, etc.), not raw primitives where avoidable.
- Do not terminate execution with runtime exceptions for expected situations; model recoverable outcomes explicitly (for example as result/value types).
- Runtime behavior should stay deterministic and testable at stage and full-pipeline levels.

## Component Mapping Guidance
- Fetch: read instruction words from memory, apply BTB prediction, emit instruction+PC+predicted-NPC bundles.
- Decode: decode RV32I into typed instruction/domain operation models.
- Issue: perform dependency tracking, allocate ROB entries, and dispatch to ALU/branch/memory scheduling structures.
- Reservation stations and memory buffers: maintain ready/waiting operands and enforce per-cycle dispatch constraints.
- Execute units (ALU/Branch/Address/Memory): consume ready work, apply operation latencies, and publish completion on CDB.
- ROB: preserve program order for commit, track readiness, values, branch metadata, and store metadata.
- Commit: retire in order up to commit width, update architectural state, handle branch resolution/mispredict recovery, and update BTB.
- Control: represent redirect/flush behavior with typed control-state transitions.

## Kotlin Code Style (Project Standard)
- Do not use default parameter values in constructors or functions.
- Never use hard casts with `as`.
- Do not use `var` unless mutation is strictly required and cannot be expressed immutably.
- Do not use trailing commas.
- Avoid named arguments by default; use them only when required for disambiguation or readability in ambiguous call sites.
- Use explicit constructors for fixed initialization requirements (for example explicit zero-initialization).
- Prefer immutable models:
  - `data class ... private constructor(...)` plus controlled secondary constructors.
  - Return new instances on mutation operations instead of mutating state.
- Use focused domain types:
  - `@JvmInline value class` wrappers for primitive/data-unit semantics.
  - Unsigned numeric types (`UByte`, `UShort`, `UInt`) where bit-precision matters.
  - Prefer domain value types (`Word`, `InstructionAddress`, etc.) over primitives at component boundaries.
- Keep APIs explicit and small:
  - Clear method names (`loadByte`, `storeWord`, etc.).
  - All failure cases must be modeled with `ProcessorResult<T>`.
  - Do not use exceptions or nullable return values to represent failure.
  - Do not add explicit return types on override methods when the interface already defines the return type.
- When no stronger domain name is available, use `Real` as the concrete production implementation prefix for interface implementations.
- Keep logic readable:
  - Prefer expression-bodied functions (`fun foo() = ...`) instead of block bodies when possible.
  - In expression-bodied functions, always place the expression on the next line after `=`.
  - Avoid unnecessary explicit return types when type inference is clear and signatures already communicate intent.
  - Avoid temporary locals that only feed a `when`; prefer `when (val value = ...)` or direct expression forms.
  - For bitfield/immediate assembly, align one bit contribution per line at the same indentation level.
  - Model instruction format fields faithfully; do not add fields to a format model that are not defined by that format in the ISA.
  - Keep helper extensions/private helpers near call sites.
  - Never use implicit lambda parameters (`it`); always name lambda variables explicitly.
  - Avoid acronyms in Kotlin names where practical (`commonDataBus` over `cdb`).

## State Modeling Rule
- Model cycle/tick transition semantics at processor-state orchestration level by default.
- Avoid embedding cycle-latch (`current`/`next`) mechanics into low-level components unless there is a clear need.

## Testing Standard
- Use JUnit 5 tests with descriptive backtick method names.
- Use Strikt assertions (`expectThat`, `expectCatching`).
- Prefer simple expressions inside `expectThat(...)`.
- Prefer chained assertions on separate lines for readability.
- Keep assertions top-level in test bodies; do not place assertions inside `let` blocks.
- Use explicit intermediate variables in tests when they improve readability.
- Separate consecutive assertion blocks with a blank line.
- For `ProcessorResult`, use Strikt assertion-builder extensions (`expectThat(result).isSuccess()` / `isFailure()`) from `src/testFixtures`.
- Prefer behavior-focused test names and edge-case coverage (bounds, endian behavior, shift semantics).
- Tests must cover all code paths, including edge and failure cases, for each component.
- Use `src/testFixtures` for reusable predictor/test doubles.
