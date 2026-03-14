package commondatabus

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
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

    override fun resolveOperand(operand: PendingOperand) =
        when (val valueResult = valuesByRobId[operand.robId]) {
            null -> operand
            is Failure -> operand
            is Success -> ReadyOperand(valueResult.value)
        }

    override fun clear() = StubCommonDataBus()
}
