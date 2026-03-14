package branchpredictor

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNotEqualTo
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.BranchTargetBufferSizeInvalid
import types.InstructionAddress
import types.Size
import types.next

class BranchTargetBufferTest {

    @Test
    fun `cannot create a buffer with zero entries`() {
        expectThat(bufferResult(Size(0), alwaysTakenPredictor))
            .isFailure()
            .isEqualTo(BranchTargetBufferSizeInvalid(0))
    }

    @Test
    fun `is immutable`() {
        val buffer = buffer(Size(1), alwaysTakenPredictor)

        expectThat(
            buffer
                .outcome(InstructionAddress(0), InstructionAddress(42))
                .predict(InstructionAddress(0))
        ).isEqualTo(InstructionAddress(42))

        expectThat(
            buffer
                .predict(InstructionAddress(0))
        ).isNotEqualTo(InstructionAddress(42))
    }

    @Test
    fun `predicts next instruction address if entry not populated in buffer`() {
        val instructionAddress = InstructionAddress(0)
        expectThat(buffer(Size(1), alwaysTakenPredictor).predict(instructionAddress)).isEqualTo(instructionAddress.next)
    }

    @Test
    fun `predicts next instruction address if entry populated and predictor predicts not taken`() {
        val instructionAddress = InstructionAddress(0)

        val buffer = buffer(Size(1), alwaysNotTakenPredictor)
            .outcome(instructionAddress, InstructionAddress(1))

        expectThat(buffer.predict(instructionAddress)).isEqualTo(instructionAddress.next)
    }

    @Test
    fun `predicts target instruction address if entry populated and predictor predicts taken`() {
        val instructionAddress = InstructionAddress(0)
        val targetInstructionAddress = InstructionAddress(42)

        val buffer = buffer(Size(1), alwaysTakenPredictor)
            .outcome(instructionAddress, targetInstructionAddress)

        expectThat(buffer.predict(instructionAddress)).isEqualTo(targetInstructionAddress)
    }

    @Test
    fun `overwrites entry on an outcome collision`() {
        expectThat(
            buffer(Size(1), alwaysTakenPredictor)
                .outcome(InstructionAddress(0), InstructionAddress(21))
                .outcome(InstructionAddress(1), InstructionAddress(42))
                .predict(InstructionAddress(1))
        ).isEqualTo(InstructionAddress(42))
    }

    @Test
    fun `non-colliding outcomes do not interfere with each other`() {
        val buffer = buffer(Size(2), alwaysTakenPredictor)
            .outcome(InstructionAddress(0), InstructionAddress(21))
            .outcome(InstructionAddress(4), InstructionAddress(42))

        expectThat(buffer.predict(InstructionAddress(0))).isEqualTo(InstructionAddress(21))
        expectThat(buffer.predict(InstructionAddress(4))).isEqualTo(InstructionAddress(42))
    }

    @Test
    fun `updates the outcome predictor on each outcome`() {
        expectThat(
            buffer(Size(1), LastOutcomeBranchOutcomePredictorStub())
                .outcome(InstructionAddress(0), InstructionAddress(42))
                .predict(InstructionAddress(0))
        ).isEqualTo(InstructionAddress(42))
    }

    @Test
    fun `reports not taken to the outcome predictor if the target instruction address is the next instruction`() {
        val outcomes = mutableListOf<Boolean>()

        val instructionAddress = InstructionAddress(0)
        buffer(Size(1), CallbackBranchOutcomePredictorStub { callbackOutcome -> outcomes += callbackOutcome })
            .outcome(instructionAddress, instructionAddress.next)

        expectThat(outcomes.single()).isFalse()
    }

    @Test
    fun `reports taken to the outcome predictor if the target instruction address is not the next instruction`() {
        val outcomes = mutableListOf<Boolean>()

        buffer(Size(1), CallbackBranchOutcomePredictorStub { callbackOutcome -> outcomes += callbackOutcome })
            .outcome(InstructionAddress(0), InstructionAddress(42))

        expectThat(outcomes.single()).isTrue()
    }

    private fun buffer(size: Size, predictor: DynamicBranchOutcomePredictor) =
        expectThat(bufferResult(size, predictor))
            .isSuccess()
            .subject

    private fun bufferResult(size: Size, predictor: DynamicBranchOutcomePredictor) =
        BranchTargetBuffer.create(size, predictor)
}
