package branchpredictor

import types.InstructionAddress

interface BranchOutcomePredictor {
    fun predict(instructionAddress: InstructionAddress): Boolean
}

interface DynamicBranchOutcomePredictor : BranchOutcomePredictor {
    fun outcome(instructionAddress: InstructionAddress, taken: Boolean): DynamicBranchOutcomePredictor
}
