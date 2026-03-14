package types

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class CycleCountTest {

    @Test
    fun `cycle counts retain the provided value`() {
        expectThat(CycleCount(1))
            .isEqualTo(CycleCount(1))

        expectThat(CycleCount(3))
            .isEqualTo(CycleCount(3))

        expectThat(CycleCount(0))
            .isEqualTo(CycleCount(0))

        expectThat(CycleCount(-1))
            .isEqualTo(CycleCount(-1))
    }
}
