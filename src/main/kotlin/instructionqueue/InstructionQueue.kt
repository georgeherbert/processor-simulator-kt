package instructionqueue

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.InstructionQueueEmpty
import types.InstructionQueueFull
import types.ProcessorResult
import types.Size

interface InstructionQueue {
    fun enqueue(entry: InstructionQueueEntry): ProcessorResult<InstructionQueue>
    fun dequeueIfPresent(): InstructionQueueDequeueOutcome
    fun dequeue(): ProcessorResult<InstructionQueueDequeueResult>
    fun clear(): InstructionQueue
    fun entryCount(): Int
}

@ConsistentCopyVisibility
data class RealInstructionQueue private constructor(
    private val capacity: Int,
    private val entries: List<InstructionQueueEntry>,
) : InstructionQueue {

    constructor(size: Size) : this(size.value, emptyList())

    override fun enqueue(entry: InstructionQueueEntry) =
        when (entries.size >= capacity) {
            true -> InstructionQueueFull.asFailure()
            false -> copy(entries = entries + entry).asSuccess()
        }

    override fun dequeueIfPresent() =
        when (entries.isEmpty()) {
            true -> InstructionQueueDequeueUnavailable
            false -> InstructionQueueDequeueResult(copy(entries = entries.drop(1)), entries.first())
        }

    override fun dequeue() =
        when (val dequeueOutcome = dequeueIfPresent()) {
            is InstructionQueueDequeueResult -> dequeueOutcome.asSuccess()
            InstructionQueueDequeueUnavailable -> InstructionQueueEmpty.asFailure()
        }

    override fun clear() = copy(entries = emptyList())

    override fun entryCount() = entries.size
}
