package branchpredictor

import types.InstructionAddress
import types.Size
import types.next

@ConsistentCopyVisibility
data class BranchTargetBuffer private constructor(
    private val entries: List<Entry?>,
    private val branchOutcomePredictor: DynamicBranchOutcomePredictor,
) : DynamicBranchTargetPredictor {
    constructor(
        size: Size,
        branchOutcomePredictor: DynamicBranchOutcomePredictor,
    ) : this(List(size.value) { null }, branchOutcomePredictor)

    init {
        require(entries.isNotEmpty()) { "Size must be greater than 0" }
    }

    override fun predict(instructionAddress: InstructionAddress) =
        entries[instructionAddress.index]
            ?.let { entry ->
                when (isPredictedTaken(entry, instructionAddress)) {
                    true -> entry.targetInstructionAddress
                    false -> instructionAddress.next
                }
            }
            ?: instructionAddress.next

    override fun outcome(
        instructionAddress: InstructionAddress,
        targetInstructionAddress: InstructionAddress,
    ) = copy(
        entries = entries.withEntry(Entry(instructionAddress, targetInstructionAddress)),
        branchOutcomePredictor = branchOutcomePredictor.outcome(
            instructionAddress,
            instructionAddress.next != targetInstructionAddress
        )
    )

    private fun isPredictedTaken(
        entry: Entry,
        instructionAddress: InstructionAddress,
    ) =
        entry.instructionAddress == instructionAddress && branchOutcomePredictor.predict(instructionAddress)

    private fun List<Entry?>.withEntry(entry: Entry) =
        toMutableList().apply { this[entry.instructionAddress.index] = entry }

    private val InstructionAddress.index get() = (value / 4) % entries.size

    private data class Entry(
        val instructionAddress: InstructionAddress,
        val targetInstructionAddress: InstructionAddress,
    )
}
