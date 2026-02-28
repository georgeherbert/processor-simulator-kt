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
  - Provide stubs/fakes in `src/testFixtures` and wire those into tests.

## Kotlin Code Style (Project Standard)
- Prefer immutable models:
  - `data class ... private constructor(...)` plus controlled secondary constructors.
  - Return new instances on mutation operations instead of mutating state.
- Use focused domain types:
  - `@JvmInline value class` wrappers for primitive/data-unit semantics.
  - Unsigned numeric types (`UByte`, `UShort`, `UInt`) where bit-precision matters.
- Keep APIs explicit and small:
  - Clear method names (`loadByte`, `storeWord`, etc.).
  - Do not throw exceptions that can manifest during normal runtime behavior.
- Keep logic readable:
  - Use expression bodies where concise.
  - Keep helper extensions/private helpers near call sites.
  - Never use implicit lambda parameters (`it`); always name lambda variables explicitly.

## Testing Standard
- Use JUnit 5 tests with descriptive backtick method names.
- Use Strikt assertions (`expectThat`, `expectCatching`).
- Prefer behavior-focused test names and edge-case coverage (bounds, endian behavior, shift semantics).
- Use `src/testFixtures` for reusable predictor/test doubles.
