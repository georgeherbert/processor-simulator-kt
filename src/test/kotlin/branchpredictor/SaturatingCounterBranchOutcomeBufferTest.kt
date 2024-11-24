package branchpredictor

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.*
import types.BitWidth
import types.InstructionAddress
import types.Size

class SaturatingCounterBranchOutcomeBufferTest {
    @Test
    fun `cannot create a buffer with zero entries`() {
        expectCatching { buffer(Size(0), BitWidth(1)) }
            .isFailure()
            .isA<IllegalArgumentException>()
            .get { message }.isNotNull().isEqualTo("Size must be greater than 0")
    }

    @Test
    fun `cannot create a buffer with a zero bit width`() {
        expectCatching { buffer(Size(1), BitWidth(0)) }
            .isFailure()
            .isA<IllegalArgumentException>()
            .get { message }.isNotNull().isEqualTo("Bit width must be greater than 0")
    }

    @Test
    fun `is immutable`() {
        val buffer = buffer(Size(1), BitWidth(1))
        expectThat(buffer.outcome(InstructionAddress(0), true).predict(InstructionAddress(0))).isTrue()
        expectThat(buffer.predict(InstructionAddress(0))).isFalse()
    }

    @Test
    fun `initially predicts weakest not taken`() {
        expectThat(
            buffer(Size(1), BitWidth(1))
                .outcome(InstructionAddress(0), true)
                .predict(InstructionAddress(0))
        ).isTrue()

        expectThat(
            buffer(Size(1), BitWidth(2))
                .outcome(InstructionAddress(0), true)
                .predict(InstructionAddress(0))
        ).isTrue()

        expectThat(
            buffer(Size(1), BitWidth(1))
                .outcome(InstructionAddress(0), true)
                .outcome(InstructionAddress(0), false)
                .predict(InstructionAddress(0))
        ).isFalse()

        expectThat(
            buffer(Size(1), BitWidth(2))
                .outcome(InstructionAddress(0), true)
                .outcome(InstructionAddress(0), false)
                .predict(InstructionAddress(0))
        ).isFalse()
    }

    @Test
    fun `predicts not taken if instruction address not in buffer`() {
        expectThat(buffer(Size(1), BitWidth(1)).predict(InstructionAddress(0))).isFalse()
    }

    @Test
    fun `predicts not taken on a prediction collision regardless of counter in entry`() {
        val buffer = buffer(Size(1), BitWidth(1)).outcome(InstructionAddress(0), true)
        expectThat(buffer.predict(InstructionAddress(0))).isTrue()
        expectThat(buffer.predict(InstructionAddress(1))).isFalse()
    }

    @Test
    fun `overwrites entry on an outcome collision`() {
        expectThat(
            buffer(Size(1), BitWidth(1))
                .outcome(InstructionAddress(0), true)
                .outcome(InstructionAddress(1), false)
                .predict(InstructionAddress(0))
        ).isFalse()
    }

    @Test
    fun `non-colliding outcomes do not interfere with each other`() {
        val buffer = buffer(Size(2), BitWidth(1))
            .outcome(InstructionAddress(0), false)
            .outcome(InstructionAddress(1), true)

        expectThat(buffer.predict(InstructionAddress(0))).isFalse()
        expectThat(buffer.predict(InstructionAddress(1))).isTrue()

        val newBuffer = buffer
            .outcome(InstructionAddress(0), true)
            .outcome(InstructionAddress(1), false)

        expectThat(newBuffer.predict(InstructionAddress(0))).isTrue()
        expectThat(newBuffer.predict(InstructionAddress(1))).isFalse()
    }

    @Test
    fun `counter cannot be decremented below zero`() {
        expectThat(
            buffer(Size(1), BitWidth(1))
                .outcome(InstructionAddress(0), false)
                .outcome(InstructionAddress(0), true)
                .predict(InstructionAddress(0))
        ).isTrue()
    }

    @Test
    fun `counter cannot be incremented above max`() {
        expectThat(
            buffer(Size(1), BitWidth(1))
                .outcome(InstructionAddress(0), true)
                .outcome(InstructionAddress(0), true)
                .outcome(InstructionAddress(0), false)
                .predict(InstructionAddress(0))
        ).isFalse()
    }

    private fun buffer(size: Size, bitWidth: BitWidth) = SaturatingCounterBranchOutcomeBuffer(size, bitWidth)
}
