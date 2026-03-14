package cpu

import mainmemory.StubMainMemory
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*

class ProcessorFactoryTest {

    @Test
    fun `seeds stack and frame pointer registers with stack headroom`() {
        val mainMemorySize = Size(64)
        val createdState = expectThat(
            RealProcessorFactory.create(
                testProcessorConfiguration(),
                StubMainMemory(),
                mainMemorySize,
                InstructionAddress(0)
            )
        )
            .isSuccess()
            .subject

        expectThat(createdState.registerFile.readCommitted(RegisterAddress(2)))
            .isEqualTo(Word(32u))

        expectThat(createdState.registerFile.readCommitted(RegisterAddress(8)))
            .isEqualTo(Word(32u))

        expectThat(createdState.branchTargetPredictor.predict(InstructionAddress(0)))
            .isEqualTo(InstructionAddress(0).next)
    }

    @Test
    fun `caps stack headroom at the default guard size for larger memories`() {
        val mainMemorySize = Size(4096)
        val createdState = expectThat(
            RealProcessorFactory.create(
                testProcessorConfiguration(),
                StubMainMemory(),
                mainMemorySize,
                InstructionAddress(0)
            )
        )
            .isSuccess()
            .subject

        expectThat(createdState.registerFile.readCommitted(RegisterAddress(2)))
            .isEqualTo(Word(3072u))

        expectThat(createdState.registerFile.readCommitted(RegisterAddress(8)))
            .isEqualTo(Word(3072u))
    }

    @Test
    fun `fails when branch predictor buffer size is invalid`() {
        expectThat(
            RealProcessorFactory.create(
                testProcessorConfiguration().copy(branchTargetBufferSize = Size(0)),
                StubMainMemory(),
                Size(64),
                InstructionAddress(0)
            )
        )
            .isFailure()
            .isEqualTo(BranchOutcomeBufferSizeInvalid(0))
    }

    @Test
    fun `fails when branch outcome counter bit width is invalid`() {
        expectThat(
            RealProcessorFactory.create(
                testProcessorConfiguration().copy(branchOutcomeCounterBitWidth = BitWidth(0)),
                StubMainMemory(),
                Size(64),
                InstructionAddress(0)
            )
        )
            .isFailure()
            .isEqualTo(BranchOutcomePredictorBitWidthInvalid(0))
    }
}
