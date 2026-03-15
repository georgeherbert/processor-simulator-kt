package web

import cpu.benchmarkConfiguration
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import types.BenchmarkProgramNotFound

class SimulationHttpApiTest {

    @Test
    fun `benchmarks endpoint returns the configured benchmark list`() =
        testApplication {
            application {
                processorSimulatorWebModule(
                    RealSimulationHttpApi(
                        StubSimulationService(
                            listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                            benchmarkSourceFixture(),
                            experimentDefaultsFixture(),
                            experimentRunFixture()
                        )
                    )
                )
            }

            val response = client.get("/api/benchmarks")

            expectThat(response.status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.bodyAsText())
                .contains("arithmetic.c")
    }

    @Test
    fun `experiment run endpoint maps processor errors to http responses`() =
        testApplication {
            application {
                processorSimulatorWebModule(
                    RealSimulationHttpApi(
                        StubSimulationService(
                            listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                            BenchmarkProgramNotFound("missing.c"),
                            experimentDefaultsFixture(),
                            BenchmarkProgramNotFound("missing.c")
                        )
                    )
                )
            }

            val response = client.post("/api/experiment/run") {
                contentType(ContentType.Application.Json)
                setBody(
                    Json.encodeToString(
                        experimentRequestFixture().copy(programName = "missing.c")
                    )
                )
            }

            expectThat(response.status)
                .isEqualTo(HttpStatusCode.NotFound)

            expectThat(response.bodyAsText())
                .contains("Unknown benchmark program")
    }

    @Test
    fun `benchmark source endpoint returns the selected benchmark source`() =
        testApplication {
            application {
                processorSimulatorWebModule(
                    RealSimulationHttpApi(
                        StubSimulationService(
                            listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                            benchmarkSourceFixture(),
                            experimentDefaultsFixture(),
                            experimentRunFixture()
                        )
                    )
                )
            }

            val response = client.get("/api/benchmarks/arithmetic.c/source")

            expectThat(response.status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.bodyAsText())
                .contains("sourceCode")
                .contains("int32_t main")
        }

    @Test
    fun `experiment defaults endpoint returns the baseline configuration`() =
        testApplication {
            application {
                processorSimulatorWebModule(
                    RealSimulationHttpApi(
                        StubSimulationService(
                            listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                            benchmarkSourceFixture(),
                            experimentDefaultsFixture(),
                            experimentRunFixture()
                        )
                    )
                )
            }

            val response = client.get("/api/experiment/defaults")

            expectThat(response.status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.bodyAsText())
                .contains("issueWidth")
        }

    @Test
    fun `experiment run endpoint returns plotted points`() =
        testApplication {
            application {
                processorSimulatorWebModule(
                    RealSimulationHttpApi(
                        StubSimulationService(
                            listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                            benchmarkSourceFixture(),
                            experimentDefaultsFixture(),
                            experimentRunFixture()
                        )
                    )
                )
            }

            val response = client.post("/api/experiment/run") {
                contentType(ContentType.Application.Json)
                setBody(Json.encodeToString(experimentRequestFixture()))
            }

            expectThat(response.status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.bodyAsText())
                .contains("instructionsPerCycle")
        }

    @Test
    fun `root path serves the frontend shell`() =
        testApplication {
            application {
                processorSimulatorWebModule(
                    RealSimulationHttpApi(
                        StubSimulationService(
                            listOf(BenchmarkProgram("arithmetic.c", "Arithmetic")),
                            benchmarkSourceFixture(),
                            experimentDefaultsFixture(),
                            experimentRunFixture()
                        )
                    )
                )
            }

            val response = client.get("/")

            expectThat(response.status)
                .isEqualTo(HttpStatusCode.OK)

            expectThat(response.bodyAsText())
                .contains("""<div id="root"></div>""")
        }

    private fun experimentDefaultsFixture() =
        ExperimentDefaultsResponse(
            benchmarkConfiguration().toExperimentConfigurationSnapshot(),
            experimentParameterOptions(),
            100_000
        )

    private fun benchmarkSourceFixture() =
        BenchmarkProgramSourceResponse(
            "arithmetic.c",
            "#include <stdint.h>\n\nint32_t main() {\n    return 1;\n}\n"
        )

    private fun experimentRequestFixture() =
        RunExperimentRequest(
            "arithmetic.c",
            benchmarkConfiguration().toExperimentConfigurationSnapshot(),
            ExperimentParameter.IssueWidth,
            1,
            3,
            1,
            1000
        )

    private fun experimentRunFixture() =
        ExperimentRunResponse(
            "arithmetic.c",
            ExperimentParameter.IssueWidth,
            listOf(
                ExperimentPoint(1, 0.75),
                ExperimentPoint(2, 1.0),
                ExperimentPoint(3, 1.25)
            )
        )
}
