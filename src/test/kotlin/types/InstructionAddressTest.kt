package types

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

class InstructionAddressTest {

    @Test
    fun `instruction addresses can be zero or positive`() {
        InstructionAddress(0)
        InstructionAddress(8)
    }

    @Test
    fun `instruction addresses cannot be negative`() {
        expectCatching { InstructionAddress(-4) }
            .isFailure()
            .isA<IllegalArgumentException>()
            .get { message }.isEqualTo("InstructionAddress must be non-negative")
    }
}
