package commit

import reorderbuffer.ReorderBuffer
import types.ProcessorResult

interface CommitUnit {
    fun nextCycleDelta(reorderBuffer: ReorderBuffer): ProcessorResult<CommitCycleDelta>
}
