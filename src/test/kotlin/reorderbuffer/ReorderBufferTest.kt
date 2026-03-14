package reorderbuffer

import commondatabus.StubCommonDataBus
import decoder.StoreWordOperation
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*

class ReorderBufferTest {

    @Test
    fun `register write entry becomes ready from the common data bus and can commit`() {
        val allocationResult = expectThat(
            RealReorderBuffer(Size(2)).enqueueRegisterWrite(
                RegisterAddress(1),
                ArithmeticLogicRegisterWriteReorderBufferEntryCategory
            )
        )
            .isSuccess()
            .subject

        expectThat(allocationResult.reorderBuffer.commitReadyHead())
            .isFailure()
            .isEqualTo(ReorderBufferHeadNotReady)

        val commonDataBus = StubCommonDataBus(allocationResult.robId, Word(42u))

        val readyReorderBuffer = allocationResult.reorderBuffer.acceptCommonDataBus(commonDataBus)

        expectThat(readyReorderBuffer.hasResolvedValue(allocationResult.robId))
            .isTrue()

        expectThat(readyReorderBuffer.valueFor(allocationResult.robId))
            .isSuccess()
            .isEqualTo(Word(42u))

        expectThat(readyReorderBuffer.commitReadyHead())
            .isSuccess()
            .get { entry }
            .isEqualTo(
                RegisterWriteReorderBufferEntry(
                    allocationResult.robId,
                    RegisterAddress(1),
                    ArithmeticLogicRegisterWriteReorderBufferEntryCategory,
                    ReadyOperand(Word(42u))
                )
            )
    }

    @Test
    fun `resolveOperand returns pending operand when reorder buffer value is unresolved`() {
        val allocationResult = expectThat(
            RealReorderBuffer(Size(1)).enqueueRegisterWrite(
                RegisterAddress(1),
                ArithmeticLogicRegisterWriteReorderBufferEntryCategory
            )
        )
            .isSuccess()
            .subject
        val operand = PendingOperand(allocationResult.robId)

        expectThat(allocationResult.reorderBuffer.resolveOperand(operand))
            .isEqualTo(operand)
    }

    @Test
    fun `resolveOperand returns ready operand when reorder buffer value is resolved`() {
        val allocationResult = expectThat(
            RealReorderBuffer(Size(1)).enqueueRegisterWrite(
                RegisterAddress(1),
                ArithmeticLogicRegisterWriteReorderBufferEntryCategory
            )
        )
            .isSuccess()
            .subject
        val readyReorderBuffer = allocationResult.reorderBuffer.acceptCommonDataBus(
            StubCommonDataBus(allocationResult.robId, Word(42u))
        )

        expectThat(readyReorderBuffer.resolveOperand(PendingOperand(allocationResult.robId)))
            .isEqualTo(ReadyOperand(Word(42u)))
    }

    @Test
    fun `commitReadyHeadIfPossible returns unavailable when no ready head exists`() {
        val allocationResult = expectThat(
            RealReorderBuffer(Size(1)).enqueueRegisterWrite(
                RegisterAddress(1),
                ArithmeticLogicRegisterWriteReorderBufferEntryCategory
            )
        )
            .isSuccess()
            .subject

        expectThat(allocationResult.reorderBuffer.commitReadyHeadIfPossible())
            .isEqualTo(ReorderBufferCommitReadyHeadUnavailable)
    }

    @Test
    fun `commitReadyHeadIfPossible returns the ready head when commit can proceed`() {
        val allocationResult = expectThat(
            RealReorderBuffer(Size(1)).enqueueRegisterWrite(
                RegisterAddress(1),
                ArithmeticLogicRegisterWriteReorderBufferEntryCategory
            )
        )
            .isSuccess()
            .subject
        val readyReorderBuffer = allocationResult.reorderBuffer.acceptCommonDataBus(
            StubCommonDataBus(allocationResult.robId, Word(42u))
        )
        val commitReadyHeadOutcome = readyReorderBuffer.commitReadyHeadIfPossible()

        expectThat(commitReadyHeadOutcome)
            .isA<ReorderBufferCommitHeadResult>()
            .get { entry }
            .isEqualTo(
                RegisterWriteReorderBufferEntry(
                    allocationResult.robId,
                    RegisterAddress(1),
                    ArithmeticLogicRegisterWriteReorderBufferEntryCategory,
                    ReadyOperand(Word(42u))
                )
            )

        expectThat(commitReadyHeadOutcome)
            .isA<ReorderBufferCommitHeadResult>()
            .get { reorderBuffer.entryCount() }
            .isEqualTo(0)
    }

    @Test
    fun `store earlier hazard detection uses older ready stores with the same address`() {
        val firstStoreAllocation = expectThat(
            RealReorderBuffer(Size(3)).enqueueStore(StoreWordOperation, ReadyOperand(Word(10u)))
        )
            .isSuccess()
            .subject

        val secondStoreAllocation = expectThat(
            firstStoreAllocation.reorderBuffer.enqueueStore(StoreWordOperation, ReadyOperand(Word(20u)))
        )
            .isSuccess()
            .subject

        val reorderBuffer = secondStoreAllocation.reorderBuffer
            .recordStoreAddress(firstStoreAllocation.robId, DataAddress(32))
            .recordStoreAddress(secondStoreAllocation.robId, DataAddress(32))

        expectThat(reorderBuffer.hasEarlierStore(secondStoreAllocation.robId, DataAddress(32)))
            .isTrue()

        expectThat(reorderBuffer.hasEarlierStore(firstStoreAllocation.robId, DataAddress(32)))
            .isFalse()
    }

    @Test
    fun `jump entry waits for both link value and actual next instruction address`() {
        val allocationResult = expectThat(
            RealReorderBuffer(Size(1)).enqueueJump(
                RegisterAddress(1),
                InstructionAddress(8),
                InstructionAddress(12)
            )
        )
            .isSuccess()
            .subject

        expectThat(allocationResult.reorderBuffer.commitReadyHead())
            .isFailure()
            .isEqualTo(ReorderBufferHeadNotReady)

        val commonDataBus = StubCommonDataBus(allocationResult.robId, Word(12u))

        val readyReorderBuffer = allocationResult.reorderBuffer
            .acceptCommonDataBus(commonDataBus)
            .recordBranchActualNextInstructionAddress(allocationResult.robId, InstructionAddress(0))

        expectThat(readyReorderBuffer.commitReadyHead())
            .isSuccess()
            .get { entry }
            .isEqualTo(
                JumpReorderBufferEntry(
                    allocationResult.robId,
                    RegisterAddress(1),
                    InstructionAddress(8),
                    InstructionAddress(12),
                    InstructionAddress(0),
                    ReadyOperand(Word(12u))
                )
            )
    }
}
