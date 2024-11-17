package branchpredictor

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import types.BitWidth
import types.ProgramCounter
import types.Size

class SaturatingCounterBranchPredictorTest {
    @Test
    fun `cannot create a predictor with zero entries`() {
        expectCatching { predictor(Size(0), BitWidth(1)) }
            .isFailure()
            .isA<IllegalArgumentException>()
            .get { message }.isNotNull().isEqualTo("Size must be greater than 0")
    }

    @Test
    fun `cannot create a predictor with a zero bit width`() {
        expectCatching { predictor(Size(1), BitWidth(0)) }
            .isFailure()
            .isA<IllegalArgumentException>()
            .get { message }.isNotNull().isEqualTo("Bit width must be greater than 0")
    }

    @Test
    fun `is immutable`() {
        val predictor = predictor(Size(1), BitWidth(1))
        expectThat(predictor.outcome(ProgramCounter(0), true).predict(ProgramCounter(0))).isTrue()
        expectThat(predictor.predict(ProgramCounter(0))).isFalse()
    }

    @Test
    fun `initially predicts weakest not taken`() {
        expectThat(
            predictor(Size(1), BitWidth(1))
                .outcome(ProgramCounter(0), true)
                .predict(ProgramCounter(0))
        ).isTrue()

        expectThat(
            predictor(Size(1), BitWidth(2))
                .outcome(ProgramCounter(0), true)
                .predict(ProgramCounter(0))
        ).isTrue()

        expectThat(
            predictor(Size(1), BitWidth(1))
                .outcome(ProgramCounter(0), true)
                .outcome(ProgramCounter(0), false)
                .predict(ProgramCounter(0))
        ).isFalse()

        expectThat(
            predictor(Size(1), BitWidth(2))
                .outcome(ProgramCounter(0), true)
                .outcome(ProgramCounter(0), false)
                .predict(ProgramCounter(0))
        ).isFalse()
    }

    @Test
    fun `predicts not taken if program counter not in table`() {
        expectThat(predictor(Size(1), BitWidth(1)).predict(ProgramCounter(0))).isFalse()
    }

    @Test
    fun `predicts not taken on a prediction collision regardless of counter in entry`() {
        val predictor = predictor(Size(1), BitWidth(1)).outcome(ProgramCounter(0), true)
        expectThat(predictor.predict(ProgramCounter(0))).isTrue()
        expectThat(predictor.predict(ProgramCounter(1))).isFalse()
    }

    @Test
    fun `overwrites entry on an outcome collision`() {
        val predictor = predictor(Size(1), BitWidth(1)).outcome(ProgramCounter(0), false)
        expectThat(predictor.outcome(ProgramCounter(0), false).predict(ProgramCounter(0))).isFalse()
    }

    @Test
    fun `counter cannot be decremented below zero`() {
        expectThat(
            predictor(Size(1), BitWidth(1))
                .outcome(ProgramCounter(0), false)
                .outcome(ProgramCounter(0), true)
                .predict(ProgramCounter(0))
        ).isTrue()
    }

    @Test
    fun `counter cannot be incremented above max`() {
        expectThat(
            predictor(Size(1), BitWidth(1))
                .outcome(ProgramCounter(0), true)
                .outcome(ProgramCounter(0), true)
                .outcome(ProgramCounter(0), false)
                .predict(ProgramCounter(0))
        ).isFalse()
    }

    private fun predictor(size: Size, bitWidth: BitWidth) = SaturatingCounterBranchPredictor(size, bitWidth)
}
