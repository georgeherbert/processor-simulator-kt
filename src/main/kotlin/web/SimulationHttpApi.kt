package web

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondResource
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.http.content.staticResources
import kotlinx.serialization.json.Json
import types.*

interface SimulationHttpApi {
    fun install(routing: Routing)
}

data class RealSimulationHttpApi(
    private val simulationService: SimulationService
) : SimulationHttpApi {

    override fun install(routing: Routing) {
        routing.route("/api") {
            get("/benchmarks") {
                call.respond(BenchmarkProgramsResponse(simulationService.benchmarks()))
            }

            get("/benchmarks/{programName}/source") {
                call.respondBenchmarkSourceResult(
                    simulationService.benchmarkSource(
                        call.parameters["programName"] ?: ""
                    )
                )
            }

            get("/experiment/defaults") {
                call.respond(simulationService.experimentDefaults())
            }

            post("/experiment/run") {
                val request = call.receive<RunExperimentRequest>()
                call.respondExperimentResult(simulationService.runExperiment(request))
            }
        }
    }

    private suspend fun io.ktor.server.application.ApplicationCall.respondBenchmarkSourceResult(
        result: ProcessorResult<BenchmarkProgramSourceResponse>
    ) =
        when (result) {
            is Success -> respond(result.value)
            is Failure -> respondProcessorError(result.reason)
        }

    private suspend fun io.ktor.server.application.ApplicationCall.respondExperimentResult(
        result: ProcessorResult<ExperimentRunResponse>
    ) =
        when (result) {
            is Success -> respond(result.value)
            is Failure -> respondProcessorError(result.reason)
        }

    private suspend fun io.ktor.server.application.ApplicationCall.respondProcessorError(error: ProcessorError) =
        respond(
            statusFor(error),
            ErrorResponse(
                error::class.simpleName ?: "ProcessorError",
                messageFor(error)
            )
        )

    private fun statusFor(error: ProcessorError) =
        when (error) {
            is BenchmarkProgramNotFound -> HttpStatusCode.NotFound
            is BenchmarkProgramResourceMissing -> HttpStatusCode.InternalServerError
            is BenchmarkProgramSourceReadFailed -> HttpStatusCode.InternalServerError
            is SimulationCycleCountInvalid -> HttpStatusCode.BadRequest
            is ExperimentRangeInvalid -> HttpStatusCode.BadRequest
            is ExperimentConfigurationValueInvalid -> HttpStatusCode.BadRequest
            is ValidationError -> HttpStatusCode.BadRequest
            else -> HttpStatusCode.InternalServerError
        }

    private fun messageFor(error: ProcessorError) =
        when (error) {
            is BenchmarkProgramNotFound -> "Unknown benchmark program: ${error.programName}"
            is BenchmarkProgramResourceMissing -> "Benchmark resource is missing: ${error.programName}"
            is BenchmarkProgramMaterializationFailed -> "Failed to materialize benchmark: ${error.programName}"
            is BenchmarkProgramSourceReadFailed -> "Failed to read benchmark source: ${error.programName}"
            is SimulationCycleCountInvalid -> "Cycle count must be positive: ${error.cycleCount}"
            is ExperimentRangeInvalid ->
                "Invalid experiment range start=${error.startValue} end=${error.endValue} increment=${error.increment}"

            is ExperimentConfigurationValueInvalid ->
                "Invalid experiment configuration value ${error.fieldName}=${error.value}"
            is MainMemoryAddressOutOfBounds -> "Memory address is out of bounds: ${error.address}"
            is MainMemoryProgramFileNotFound -> "Program file not found: ${error.filePath}"
            is MainMemoryProgramFileReadFailed -> "Program file could not be read: ${error.filePath}"
            is MainMemoryProgramFileEmpty -> "Program file is empty: ${error.filePath}"
            is MainMemoryProgramFileTooLarge -> "Program file is too large: ${error.filePath}"
            ProcessorAlreadyHalted -> "Processor is already halted"
            is ProcessorCycleLimitExceeded -> "Processor exceeded cycle limit ${error.maxCycleCount}"
            CommonDataBusFull -> "Common data bus is full"
            InstructionQueueFull -> "Instruction queue is full"
            InstructionQueueEmpty -> "Instruction queue is empty"
            is InstructionQueueSlotCountInvalid -> "Instruction queue slot count is invalid: ${error.count}"
            ReorderBufferFull -> "Reorder buffer is full"
            ReorderBufferEmpty -> "Reorder buffer is empty"
            ReorderBufferHeadNotReady -> "Reorder buffer head is not ready"
            ReservationStationFull -> "Reservation station is full"
            MemoryBufferFull -> "Memory buffer is full"
            is CommitEntryValueUnavailable -> "Commit value unavailable for ROB ${error.robId.value}"
            is CommitEntryAddressUnavailable -> "Commit address unavailable for ROB ${error.robId.value}"
            is CommitEntryActualNextInstructionAddressUnavailable ->
                "Commit next instruction address unavailable for ROB ${error.robId.value}"
            is BranchTargetBufferSizeInvalid -> "Branch target buffer size is invalid: ${error.size}"
            is BranchOutcomeBufferSizeInvalid -> "Branch outcome buffer size is invalid: ${error.size}"
            is BranchOutcomePredictorBitWidthInvalid -> "Branch predictor bit width is invalid: ${error.bitWidth}"
            is ExternalToolExecutionFailed ->
                "External tool ${error.executable} failed with exit code ${error.exitCode}"
            is ExternalToolProcessLaunchFailed -> "Failed to launch external tool: ${error.executable}"
            is ExternalToolExecutionInterrupted -> "External tool interrupted: ${error.executable}"
            is TemporaryPathCreateFailed -> "Failed to create temporary path: ${error.pathDescription}"
            is ProgramBinaryFileNotFound -> "Program binary not found: ${error.filePath}"
            is ProgramBinaryFileReadFailed -> "Program binary could not be read: ${error.filePath}"
            is ProgramBinaryFileWriteFailed -> "Program binary could not be written: ${error.filePath}"
            is DecoderUnknownOpcode -> "Unknown opcode ${error.opcode}"
            is DecoderUnknownFunct3 -> "Unknown funct3 ${error.funct3} for opcode ${error.opcode}"
            is DecoderUnknownFunct7 ->
                "Unknown funct7 ${error.funct7} for opcode ${error.opcode} funct3 ${error.funct3}"
        }
}

fun Application.processorSimulatorWebModule(simulationHttpApi: SimulationHttpApi) {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = false
                ignoreUnknownKeys = true
            }
        )
    }
    install(CORS) {
        anyHost()
        allowHeader("Content-Type")
    }
    install(CallLogging)

    routing {
        simulationHttpApi.install(this)
        staticResources("/assets", "web/assets")
        get("/") {
            call.respondResource("web/index.html")
        }
        get("/{...}") {
            call.respondResource("web/index.html")
        }
    }
}
