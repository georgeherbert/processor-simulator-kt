package branchpredictor

import types.InstructionAddress

class StaticBranchOutcomePredictorStub(private val outcome: Boolean) : DynamicBranchOutcomePredictor {
    override fun predict(instructionAddress: InstructionAddress) = outcome
    override fun outcome(instructionAddress: InstructionAddress, taken: Boolean) = this
}

val alwaysTakenPredictor = StaticBranchOutcomePredictorStub(true)
val alwaysNotTakenPredictor = StaticBranchOutcomePredictorStub(false)
