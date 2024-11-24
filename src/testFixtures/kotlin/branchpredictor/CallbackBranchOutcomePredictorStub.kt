package branchpredictor

import types.InstructionAddress

class CallbackBranchOutcomePredictorStub(private val callback: (Boolean) -> Unit) : DynamicBranchOutcomePredictor {
    override fun outcome(instructionAddress: InstructionAddress, taken: Boolean) = this.also { callback(taken) }

    override fun predict(instructionAddress: InstructionAddress) =
        throw NotImplementedError("Not used in stub")
}