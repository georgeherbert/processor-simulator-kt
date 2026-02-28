package commondatabus

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

interface CommonDataBus {
    fun write(robId: RobId, value: Word): ProcessorResult<CommonDataBus>
    fun isValueReady(robId: RobId): Boolean
    fun valueFor(robId: RobId): ProcessorResult<Word>
    fun clear(): CommonDataBus
}

@ConsistentCopyVisibility
data class RealCommonDataBus private constructor(
    private val entries: List<Entry?>,
) : CommonDataBus {

    constructor(size: Size) : this(List(size.value) { null })

    override fun write(robId: RobId, value: Word): ProcessorResult<CommonDataBus> {
        val freeIndex = entries.indexOfFirst { entry -> entry == null }

        return when (freeIndex) {
            -1 -> CommonDataBusFull.asFailure()
            else -> copy(entries = entries.withEntryAt(freeIndex, Entry(robId, value))).asSuccess()
        }
    }

    override fun isValueReady(robId: RobId) = entries.any { entry -> entry?.robId == robId }

    override fun valueFor(robId: RobId): ProcessorResult<Word> =
        entries
            .firstNotNullOfOrNull { entry ->
                when (entry?.robId) {
                    robId -> entry.value.asSuccess()
                    else -> null
                }
            }
            ?: CommonDataBusValueNotPresent.asFailure()

    override fun clear() = copy(entries = entries.map { null })

    private fun List<Entry?>.withEntryAt(index: Int, entry: Entry) =
        toMutableList().apply { this[index] = entry }

    private data class Entry(
        val robId: RobId,
        val value: Word,
    )
}
