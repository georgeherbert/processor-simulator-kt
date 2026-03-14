package commit

import branchpredictor.DynamicBranchTargetPredictor
import mainmemory.MainMemory
import registerfile.RegisterFile
import reorderbuffer.ReorderBuffer
import types.ProcessorResult

typealias CommitUnitNextCycleDeltaCallback = (
    ReorderBuffer,
    RegisterFile,
    MainMemory,
    DynamicBranchTargetPredictor
) -> ProcessorResult<CommitCycleDelta>

data class CallbackCommitUnitStub(
    private val nextCycleDeltaCallback: CommitUnitNextCycleDeltaCallback
) : CommitUnit {
    override fun nextCycleDelta(
        reorderBuffer: ReorderBuffer,
        registerFile: RegisterFile,
        mainMemory: MainMemory,
        branchTargetPredictor: DynamicBranchTargetPredictor
    ) =
        nextCycleDeltaCallback(
            reorderBuffer,
            registerFile,
            mainMemory,
            branchTargetPredictor
        )
}
