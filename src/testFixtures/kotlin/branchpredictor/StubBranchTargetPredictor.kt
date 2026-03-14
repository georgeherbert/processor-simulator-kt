package branchpredictor

import types.InstructionAddress
import types.next

class StubBranchTargetPredictor(
    private val predictions: Map<InstructionAddress, InstructionAddress>,
) : BranchTargetPredictor {

    override fun predict(instructionAddress: InstructionAddress) =
        predictions[instructionAddress]
            ?: instructionAddress.next
}
