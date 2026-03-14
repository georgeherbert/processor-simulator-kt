package instructionqueue

interface InstructionQueueSlotsSource {
    fun get(): InstructionQueueSlots
}

@JvmInline
value class InstructionQueueSlots(val value: Int)
