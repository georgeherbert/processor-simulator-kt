package instructionqueue

sealed interface InstructionQueueDequeueOutcome

data object InstructionQueueDequeueUnavailable : InstructionQueueDequeueOutcome

data class InstructionQueueDequeueResult(
    val instructionQueue: InstructionQueue,
    val entry: InstructionQueueEntry,
) : InstructionQueueDequeueOutcome
