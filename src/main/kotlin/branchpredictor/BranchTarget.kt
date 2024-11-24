package branchpredictor

import types.InstructionAddress

interface BranchTargetPredictor {
    fun predict(instructionAddress: InstructionAddress): InstructionAddress
}

interface DynamicBranchTargetPredictor : BranchTargetPredictor {
    fun outcome(
        instructionAddress: InstructionAddress,
        targetInstructionAddress: InstructionAddress,
    ): DynamicBranchTargetPredictor
}
