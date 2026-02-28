package register

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.Word

class RegisterTest {

    @Test
    fun `defaults to zero`() {
        expectThat(RealRegister().read())
            .isEqualTo(Word(0u))
    }

    @Test
    fun `write returns a new register with the new value`() {
        val register = RealRegister()
            .write(Word(42u))

        expectThat(register.read())
            .isEqualTo(Word(42u))
    }

    @Test
    fun `multiple writes use the last written value`() {
        val register = RealRegister()
            .write(Word(1u))
            .write(Word(2u))

        expectThat(register.read())
            .isEqualTo(Word(2u))
    }

    @Test
    fun `is immutable`() {
        val register = RealRegister()
        val updatedRegister = register
            .write(Word(7u))

        expectThat(register.read())
            .isEqualTo(Word(0u))

        expectThat(updatedRegister.read())
            .isEqualTo(Word(7u))
    }
}
