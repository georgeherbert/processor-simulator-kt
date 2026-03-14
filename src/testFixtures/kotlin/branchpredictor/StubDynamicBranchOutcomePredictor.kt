package branchpredictor

import types.InstructionAddress

class StubDynamicBranchOutcomePredictor(
    private val predictions: Map<InstructionAddress, Boolean>,
    private val onOutcome: (InstructionAddress, Boolean) -> Unit
) : DynamicBranchOutcomePredictor {
    constructor(predictions: Map<InstructionAddress, Boolean>) : this(
        predictions,
        { _: InstructionAddress, _: Boolean -> }
    )

    override fun predict(instructionAddress: InstructionAddress) =
        predictions[instructionAddress]
            ?: false

    override fun outcome(instructionAddress: InstructionAddress, taken: Boolean) =
        this.also {
            onOutcome(instructionAddress, taken)
        }
}
