package branchpredictor

import types.BitWidth
import types.ProgramCounter
import types.Size
import types.max

private class SaturatingCounter private constructor(private val value: Int, private val bitWidth: BitWidth) {
    constructor(bitWidth: BitWidth) : this(bitWidth.max / 2, bitWidth)

    init {
        require(bitWidth.value > 0) { "Bit width must be greater than 0" }
    }

    fun increment() = SaturatingCounter((value + 1).coerceAtMost(bitWidth.max), bitWidth)
    fun decrement() = SaturatingCounter((value - 1).coerceAtLeast(0), bitWidth)

    fun takeBranch() = value > bitWidth.max / 2
}

class SaturatingCounterBranchPredictor private constructor(
    private val entries: List<Entry>,
    private val bitWidth: BitWidth,
) : BranchPredictor, BranchListener {
    constructor(
        size: Size,
        bitWidth: BitWidth,
    ) : this(List(size.value) { Entry(ProgramCounter(-1), SaturatingCounter(bitWidth)) }, bitWidth)

    init {
        require(entries.isNotEmpty()) { "Size must be greater than 0" }
    }

    override fun predict(programCounter: ProgramCounter) = entries[programCounter.index].let {
        it.programCounter == programCounter && it.saturatingCounter.takeBranch()
    }

    override fun outcome(programCounter: ProgramCounter, taken: Boolean): SaturatingCounterBranchPredictor {
        val currentEntry = entries[programCounter.index]

        val saturatingCounter = when (currentEntry.programCounter) {
            programCounter -> currentEntry.saturatingCounter
            else -> SaturatingCounter(bitWidth)
        }

        val newEntry = Entry(
            programCounter = programCounter,
            saturatingCounter = if (taken) saturatingCounter.increment() else saturatingCounter.decrement()
        )

        return SaturatingCounterBranchPredictor(
            entries = this.entries.toMutableList().apply { this[programCounter.index] = newEntry },
            bitWidth = bitWidth
        )
    }

    private val ProgramCounter.index get() = this.value % entries.size

    private data class Entry(
        val programCounter: ProgramCounter,
        val saturatingCounter: SaturatingCounter,
    )
}