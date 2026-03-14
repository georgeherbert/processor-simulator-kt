package branchpredictor

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

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
    companion object {
        fun create(size: Size, bitWidth: BitWidth): ProcessorResult<SaturatingCounterBranchOutcomeBuffer> =
            when {
                size.value <= 0 -> BranchOutcomeBufferSizeInvalid(size.value).asFailure()
                bitWidth.value <= 0 -> BranchOutcomePredictorBitWidthInvalid(bitWidth.value).asFailure()
                else -> SaturatingCounterBranchOutcomeBuffer(List(size.value) { null }, bitWidth).asSuccess()
            }
    }

    override fun predict(instructionAddress: InstructionAddress) =
        entries[instructionAddress.index]
            ?.let { entry ->
                entry.instructionAddress == instructionAddress && entry.saturatingCounter.takeBranch()
            }
            ?: false

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

    private val InstructionAddress.index get() = ((value / 4) % entries.size + entries.size) % entries.size

    private data class Entry(
        val instructionAddress: InstructionAddress,
        val saturatingCounter: SaturatingCounter,
    )
}
