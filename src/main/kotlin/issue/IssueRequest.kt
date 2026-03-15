package issue

import types.ProcessorResult

internal sealed interface IssueRequest {
    fun applyTo(workingState: IssueWorkingState): ProcessorResult<IssueAttemptOutcome>
}

internal sealed interface IssueAttemptOutcome

internal data class IssueApplied(val workingState: IssueWorkingState) : IssueAttemptOutcome

internal data object IssueBackpressured : IssueAttemptOutcome
