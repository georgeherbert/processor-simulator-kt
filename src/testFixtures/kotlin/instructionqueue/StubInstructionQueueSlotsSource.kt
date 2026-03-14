package instructionqueue

class StubInstructionQueueSlotsSource(
    private val slots: InstructionQueueSlots
) : InstructionQueueSlotsSource {
    override fun get() =
        slots
}
