package web

import kotlinx.serialization.Serializable

@Serializable
data class BenchmarkProgramsResponse(
    val benchmarks: List<BenchmarkProgram>
)

@Serializable
data class BenchmarkProgramSourceResponse(
    val programName: String,
    val sourceCode: String
)

@Serializable
data class ErrorResponse(
    val code: String,
    val message: String
)
