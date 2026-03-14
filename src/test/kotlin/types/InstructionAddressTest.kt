package types

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class InstructionAddressTest {

    @Test
    fun `instruction addresses retain the provided value`() {
        expectThat(InstructionAddress(0))
            .isEqualTo(InstructionAddress(0))

        expectThat(InstructionAddress(8))
            .isEqualTo(InstructionAddress(8))

        expectThat(InstructionAddress(-4))
            .isEqualTo(InstructionAddress(-4))
    }

    @Test
    fun `next advances the instruction address by one instruction`() {
        expectThat(InstructionAddress(12).next)
            .isEqualTo(InstructionAddress(16))
    }
}
