package branchpredictor

import types.InstructionAddress

class LastOutcomeBranchOutcomePredictorStub private constructor(
    private val outcome: Boolean?,
) : DynamicBranchOutcomePredictor {
    constructor() : this(null)

    override fun predict(instructionAddress: InstructionAddress) =
        outcome ?: false

    override fun outcome(instructionAddress: InstructionAddress, taken: Boolean) =
        LastOutcomeBranchOutcomePredictorStub(taken)
}
