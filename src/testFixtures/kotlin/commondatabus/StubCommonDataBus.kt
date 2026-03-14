package commondatabus

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

data class StubCommonDataBus(
    private val valuesByRobId: Map<RobId, ProcessorResult<Word>>
) : CommonDataBus {

    constructor() : this(emptyMap())

    constructor(robId: RobId, value: Word) : this(mapOf(robId to value.asSuccess()))

    override fun write(robId: RobId, value: Word) =
        CommonDataBusFull.asFailure()

    override fun isValueReady(robId: RobId) =
        valuesByRobId.containsKey(robId)

    override fun valueFor(robId: RobId) =
        valuesByRobId[robId]
            ?: CommonDataBusValueNotPresent.asFailure()

    override fun clear() = StubCommonDataBus()
}
