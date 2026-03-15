package web

import cpu.ProcessorConfiguration
import kotlinx.serialization.Serializable

@Serializable
data class ExperimentDefaultsResponse(
    val configuration: ExperimentConfigurationSnapshot,
    val parameters: List<ExperimentParameterOption>,
    val defaultCycleLimit: Int
)

@Serializable
data class RunExperimentRequest(
    val programName: String,
    val baseConfiguration: ExperimentConfigurationSnapshot,
    val variableParameter: ExperimentParameter,
    val startValue: Int,
    val endValue: Int,
    val increment: Int,
    val cycleLimit: Int
)

@Serializable
data class ExperimentPoint(
    val parameterValue: Int,
    val instructionsPerCycle: Double
)

@Serializable
data class ExperimentRunResponse(
    val programName: String,
    val variableParameter: ExperimentParameter,
    val points: List<ExperimentPoint>
)

fun defaultExperimentResponse(configuration: ProcessorConfiguration, defaultCycleLimit: Int) =
    ExperimentDefaultsResponse(
        configuration.toExperimentConfigurationSnapshot(),
        experimentParameterOptions(),
        defaultCycleLimit
    )

fun experimentParameterOptions() =
    ExperimentParameter.entries.map { parameter ->
        ExperimentParameterOption(parameter, parameter.label)
    }
