package instructionqueue

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.InstructionQueueEmpty

data class StubInstructionQueue(
    private val entries: List<InstructionQueueEntry>
) : InstructionQueue {

    override fun enqueue(entry: InstructionQueueEntry) =
        copy(entries = entries + entry).asSuccess()

    override fun dequeueIfPresent() =
        when (entries.isEmpty()) {
            true -> InstructionQueueDequeueUnavailable
            false ->
                InstructionQueueDequeueResult(
                    StubInstructionQueue(entries.drop(1)),
                    entries.first()
                )
        }

    override fun dequeue() =
        when (val dequeueOutcome = dequeueIfPresent()) {
            is InstructionQueueDequeueResult -> dequeueOutcome.asSuccess()
            InstructionQueueDequeueUnavailable -> InstructionQueueEmpty.asFailure()
        }

    override fun clear() = StubInstructionQueue(emptyList())

    override fun entryCount() = entries.size
}
