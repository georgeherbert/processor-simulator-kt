package instructionqueue

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.InstructionQueueEmpty

data class StubInstructionQueue(
    private val entries: List<InstructionQueueEntry>
) : InstructionQueue {

    override fun enqueue(entry: InstructionQueueEntry) =
        copy(entries = entries + entry).asSuccess()

    override fun dequeue() =
        when (entries.isEmpty()) {
            true -> InstructionQueueEmpty.asFailure()
            false ->
                InstructionQueueDequeueResult(
                    StubInstructionQueue(entries.drop(1)),
                    entries.first()
                ).asSuccess()
        }

    override fun clear() = StubInstructionQueue(emptyList())

    override fun entryCount() = entries.size
}
