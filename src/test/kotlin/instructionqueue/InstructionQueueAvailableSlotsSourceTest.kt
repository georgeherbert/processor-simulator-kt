package instructionqueue

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.InstructionAddress
import types.Size
import types.Word

class InstructionQueueAvailableSlotsSourceTest {

    @Test
    fun `returns the full capacity when the instruction queue is empty`() {
        expectThat(
            RealInstructionQueueAvailableSlotsSource(
                Size(4),
                StubInstructionQueue(emptyList())
            ).get()
        )
            .isEqualTo(InstructionQueueSlots(4))
    }

    @Test
    fun `subtracts the queued entry count from the configured capacity`() {
        val instructionQueue =
            StubInstructionQueue(
                listOf(
                    InstructionQueueEntry(
                        Word(1u),
                        InstructionAddress(0),
                        InstructionAddress(4)
                    )
                )
            )

        expectThat(
            RealInstructionQueueAvailableSlotsSource(
                Size(4),
                instructionQueue
            ).get()
        )
            .isEqualTo(InstructionQueueSlots(3))
    }
}
