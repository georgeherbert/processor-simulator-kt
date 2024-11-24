package branchpredictor

import types.BitWidth
import types.InstructionAddress
import types.Size
import types.max

@ConsistentCopyVisibility
private data class SaturatingCounter private constructor(private val value: Int, private val bitWidth: BitWidth) {
    constructor(bitWidth: BitWidth) : this(bitWidth.max / 2, bitWidth)

    fun increment() = copy(value = (value + 1).coerceAtMost(bitWidth.max))
    fun decrement() = copy(value = (value - 1).coerceAtLeast(0))

    fun takeBranch() = value > bitWidth.max / 2
}

@ConsistentCopyVisibility
data class SaturatingCounterBranchOutcomeBuffer private constructor(
    private val entries: List<Entry?>,
    private val bitWidth: BitWidth,
) : DynamicBranchOutcomePredictor {
    constructor(size: Size, bitWidth: BitWidth) : this(List(size.value) { null }, bitWidth)

    init {
        require(entries.isNotEmpty()) { "Size must be greater than 0" }
        require(bitWidth.value > 0) { "Bit width must be greater than 0" }
    }

    override fun predict(instructionAddress: InstructionAddress) = entries[instructionAddress.index].let {
        it?.instructionAddress == instructionAddress && it.saturatingCounter.takeBranch()
    }

    override fun outcome(instructionAddress: InstructionAddress, taken: Boolean): SaturatingCounterBranchOutcomeBuffer {
        val currentEntry = entries[instructionAddress.index]

        val saturatingCounter = when (currentEntry?.instructionAddress) {
            instructionAddress -> currentEntry.saturatingCounter
            else -> SaturatingCounter(bitWidth)
        }

        val newEntry = Entry(
            instructionAddress = instructionAddress,
            saturatingCounter = if (taken) saturatingCounter.increment() else saturatingCounter.decrement()
        )

        return copy(entries = entries.withEntry(newEntry))
    }

    private fun List<Entry?>.withEntry(entry: Entry) =
        toMutableList().apply { this[entry.instructionAddress.index] = entry }

    private val InstructionAddress.index get() = value % entries.size

    private data class Entry(
        val instructionAddress: InstructionAddress,
        val saturatingCounter: SaturatingCounter,
    )
}
