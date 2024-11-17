package types

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class BitWidthTest {
    @Test
    fun `max is the maximum unsigned value that can be represented by the bit width`() {
        expectThat(BitWidth(0).max).isEqualTo(0)
        expectThat(BitWidth(1).max).isEqualTo(1)
        expectThat(BitWidth(2).max).isEqualTo(3)
        expectThat(BitWidth(3).max).isEqualTo(7)
    }
}
