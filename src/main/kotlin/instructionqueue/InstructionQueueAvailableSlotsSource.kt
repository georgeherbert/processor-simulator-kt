package instructionqueue

import types.Size

data class RealInstructionQueueAvailableSlotsSource(
    private val capacity: Size,
    private val instructionQueue: InstructionQueue
) : InstructionQueueSlotsSource {

    override fun get() = InstructionQueueSlots(capacity.value - instructionQueue.entryCount())
}
