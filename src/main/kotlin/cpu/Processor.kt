package cpu

import commit.CommitCycleDelta
import commit.CommitUnit
import commit.RealCommitUnit
import commit.RedirectCommitControlEvent
import decoder.InstructionDecoder
import decoder.RealInstructionDecoder
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.flatMap
import issue.IssueUnit
import issue.RealIssueUnit
import types.ProcessorAlreadyHalted
import types.ProcessorResult

interface Processor {
    fun step(state: ProcessorState): ProcessorResult<ProcessorState>
}

data class RealProcessor(
    private val configuration: ProcessorConfiguration,
    private val instructionDecoder: InstructionDecoder,
    private val issueUnit: IssueUnit,
    private val commitUnit: CommitUnit
) : Processor {

    constructor(configuration: ProcessorConfiguration) : this(
        configuration,
        RealInstructionDecoder,
        RealIssueUnit(configuration.issueWidth),
        RealCommitUnit(configuration.commitWidth)
    )

    override fun step(state: ProcessorState) =
        when (state.halted) {
            true -> ProcessorAlreadyHalted.asFailure()
            false -> stepActiveProcessor(state)
        }

    private fun stepActiveProcessor(state: ProcessorState) =
        commitUnit
            .nextCycleDelta(
                state.reorderBuffer
            )
            .flatMap { commitChanges ->
                val updatedStatistics = state.statistics.updatedWith(commitChanges.statisticsDelta)
                when {
                    commitChanges.halted ->
                        buildTerminalState(
                            state,
                            commitChanges,
                            updatedStatistics,
                            haltedControlState(),
                            true
                        )

                    commitChanges.controlEvent is RedirectCommitControlEvent ->
                        buildTerminalState(
                            state,
                            commitChanges,
                            updatedStatistics,
                            redirectedControlState(commitChanges.controlEvent.targetInstructionAddress),
                            false
                        )

                    else ->
                        continueActiveCycle(
                            state,
                            commitChanges,
                            updatedStatistics
                        )
                }
            }

    private fun continueActiveCycle(
        state: ProcessorState,
        commitChanges: CommitCycleDelta,
        updatedStatistics: ProcessorStatistics
    ) =
        collectCurrentCycleActivity(state)
            .flatMap { currentCycleActivity ->
                collectNextCycleInputs(
                    state,
                    configuration,
                    instructionDecoder,
                    issueUnit
                )
                    .flatMap { nextCycleInputs ->
                        buildContinuingState(
                            state,
                            commitChanges,
                            updatedStatistics,
                            currentCycleActivity,
                            nextCycleInputs
                        )
                    }
            }
}
