package types

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DataAddressTest {

    @Test
    fun `data addresses retain the provided value`() {
        expectThat(DataAddress(0))
            .isEqualTo(DataAddress(0))

        expectThat(DataAddress(16))
            .isEqualTo(DataAddress(16))

        expectThat(DataAddress(-1))
            .isEqualTo(DataAddress(-1))
    }
}
