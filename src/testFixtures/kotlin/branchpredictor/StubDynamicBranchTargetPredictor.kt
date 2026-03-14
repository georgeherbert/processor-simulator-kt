package branchpredictor

import types.InstructionAddress
import types.next

data class StubDynamicBranchTargetPredictor(
    private val outcomes: Map<InstructionAddress, InstructionAddress>
) : DynamicBranchTargetPredictor {
    override fun predict(instructionAddress: InstructionAddress) =
        outcomes[instructionAddress]
            ?: instructionAddress.next

    override fun outcome(
        instructionAddress: InstructionAddress,
        targetInstructionAddress: InstructionAddress
    ) =
        copy(outcomes = outcomes + (instructionAddress to targetInstructionAddress))
}
