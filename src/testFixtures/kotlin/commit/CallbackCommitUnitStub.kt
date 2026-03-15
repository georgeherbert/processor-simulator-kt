package commit

import reorderbuffer.ReorderBuffer
import types.ProcessorResult

typealias CommitUnitNextCycleDeltaCallback = (ReorderBuffer) -> ProcessorResult<CommitCycleDelta>

data class CallbackCommitUnitStub(
    private val nextCycleDeltaCallback: CommitUnitNextCycleDeltaCallback
) : CommitUnit {
    override fun nextCycleDelta(reorderBuffer: ReorderBuffer) =
        nextCycleDeltaCallback(reorderBuffer)
}
