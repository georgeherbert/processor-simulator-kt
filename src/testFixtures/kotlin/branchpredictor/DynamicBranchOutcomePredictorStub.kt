package branchpredictor

import types.InstructionAddress

@ConsistentCopyVisibility
data class DynamicBranchOutcomePredictorStub private constructor(
    private val outcomes: Map<InstructionAddress, Boolean>,
) : DynamicBranchOutcomePredictor {
    companion object {
        fun create() = DynamicBranchOutcomePredictorStub(emptyMap())
    }

    override fun predict(instructionAddress: InstructionAddress) = outcomes
        .getOrElse(instructionAddress) { throw IllegalStateException("No outcome stubbed for $instructionAddress") }

    override fun outcome(instructionAddress: InstructionAddress, taken: Boolean) = this

    fun stubOutcome(instructionAddress: InstructionAddress, taken: Boolean) = this.copy(
        outcomes + (instructionAddress to taken)
    )
}
