package branchpredictor

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*

class SaturatingCounterBranchOutcomeBufferTest {

    @Test
    fun `cannot create a buffer with zero entries`() {
        expectThat(bufferResult(Size(0), BitWidth(1)))
            .isFailure()
            .isEqualTo(BranchOutcomeBufferSizeInvalid(0))
    }

    @Test
    fun `cannot create a buffer with a zero bit width`() {
        expectThat(bufferResult(Size(1), BitWidth(0)))
            .isFailure()
            .isEqualTo(BranchOutcomePredictorBitWidthInvalid(0))
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
            .outcome(InstructionAddress(4), true)

        expectThat(buffer.predict(InstructionAddress(0))).isFalse()
        expectThat(buffer.predict(InstructionAddress(4))).isTrue()

        val newBuffer = buffer
            .outcome(InstructionAddress(0), true)
            .outcome(InstructionAddress(4), false)

        expectThat(newBuffer.predict(InstructionAddress(0))).isTrue()
        expectThat(newBuffer.predict(InstructionAddress(4))).isFalse()
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

    private fun buffer(size: Size, bitWidth: BitWidth) =
        expectThat(bufferResult(size, bitWidth))
            .isSuccess()
            .subject

    private fun bufferResult(size: Size, bitWidth: BitWidth) =
        SaturatingCounterBranchOutcomeBuffer.create(size, bitWidth)
}
