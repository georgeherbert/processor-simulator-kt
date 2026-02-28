package instructionqueue

data class InstructionQueueDequeueResult(
    val instructionQueue: InstructionQueue,
    val entry: InstructionQueueEntry,
)
