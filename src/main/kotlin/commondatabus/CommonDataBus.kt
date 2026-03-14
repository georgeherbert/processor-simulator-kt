package commondatabus

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

interface CommonDataBus {
    fun write(robId: RobId, value: Word): ProcessorResult<CommonDataBus>
    fun resolveOperand(operand: PendingOperand): Operand
    fun isValueReady(robId: RobId): Boolean
    fun valueFor(robId: RobId): ProcessorResult<Word>
    fun clear(): CommonDataBus
}

@ConsistentCopyVisibility
data class RealCommonDataBus private constructor(
    private val entries: List<Entry?>,
) : CommonDataBus {

    constructor(size: Size) : this(List(size.value) { null })

    override fun write(robId: RobId, value: Word) =
        entries
            .indexOfFirst { entry -> entry == null }
            .let { freeIndex ->
                when (freeIndex) {
                    -1 -> CommonDataBusFull.asFailure()
                    else -> copy(entries = entries.withEntryAt(freeIndex, Entry(robId, value))).asSuccess()
                }
            }
            .let { writeResult: ProcessorResult<CommonDataBus> -> writeResult }

    override fun resolveOperand(operand: PendingOperand) =
        entries
            .firstNotNullOfOrNull { entry ->
                when (entry?.robId) {
                    operand.robId -> ReadyOperand(entry.value)
                    else -> null
                }
            }
            ?: operand

    override fun isValueReady(robId: RobId) =
        resolveOperand(PendingOperand(robId)) is ReadyOperand

    override fun valueFor(robId: RobId) =
        when (val resolvedOperand = resolveOperand(PendingOperand(robId))) {
            is ReadyOperand -> resolvedOperand.value.asSuccess()
            is PendingOperand -> CommonDataBusValueNotPresent.asFailure()
        }

    override fun clear() = copy(entries = entries.map { null })

    private fun List<Entry?>.withEntryAt(index: Int, entry: Entry) =
        toMutableList().apply { this[index] = entry }

    private data class Entry(
        val robId: RobId,
        val value: Word,
    )
}
