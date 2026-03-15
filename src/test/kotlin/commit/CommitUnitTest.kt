package commit

import branchpredictor.DynamicBranchTargetPredictor
import branchpredictor.StubDynamicBranchTargetPredictor
import decoder.StoreByteOperation
import decoder.StoreHalfWordOperation
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import mainmemory.StubMainMemory
import org.junit.jupiter.api.Test
import registerfile.StubRegisterFile
import reorderbuffer.*
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*
import types.Byte

class CommitUnitTest {

    private val commitUnit = RealCommitUnit(cpu.CommitWidth(2))

    @Test
    fun `returns success with no changes when the reorder buffer has no ready head`() {
        val registerFile = StubRegisterFile()
        val mainMemory = StubMainMemory()
        val predictor = StubDynamicBranchTargetPredictor(emptyMap())

        val stepResult = expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(emptyList()),
                registerFile,
                mainMemory,
                predictor
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.statisticsDelta)
            .isEqualTo(CommitStatisticsDelta(0))

        expectThat(stepResult.controlEvent)
            .isEqualTo(NoCommitControlEvent)

        expectThat(stepResult.halted)
            .isFalse()
    }

    @Test
    fun `commits resolved register writes`() {
        val robId = RobId(1)
        val registerFile = StubRegisterFile().reserveDestination(RegisterAddress(5), robId)

        val stepResult = expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(
                    RegisterWriteReorderBufferEntry(
                        robId,
                        RegisterAddress(5),
                        ArithmeticLogicRegisterWriteReorderBufferEntryCategory,
                        ReadyOperand(Word(33u))
                    )
                ),
                registerFile,
                StubMainMemory(),
                StubDynamicBranchTargetPredictor(emptyMap())
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.registerFile.readCommitted(RegisterAddress(5)))
            .isEqualTo(Word(33u))

        expectThat(stepResult.statisticsDelta)
            .isEqualTo(CommitStatisticsDelta(1))
    }

    @Test
    fun `commits a jump and halts when the resolved target is zero`() {
        val robId = RobId(1)
        val registerFile = StubRegisterFile().reserveDestination(RegisterAddress(1), robId)

        val stepResult = expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(
                    JumpReorderBufferEntry(
                        robId,
                        RegisterAddress(1),
                        InstructionAddress(12),
                        InstructionAddress(16),
                        InstructionAddress(0),
                        ReadyOperand(Word(16u))
                    )
                ),
                registerFile,
                StubMainMemory(),
                StubDynamicBranchTargetPredictor(emptyMap())
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.halted)
            .isTrue()

        expectThat(stepResult.controlEvent)
            .isEqualTo(RedirectCommitControlEvent(InstructionAddress(0)))

        expectThat(stepResult.statisticsDelta)
            .isEqualTo(CommitStatisticsDelta(1))

        expectThat(stepResult.branchTargetPredictor.predict(InstructionAddress(12)))
            .isEqualTo(InstructionAddress(0))
    }

    @Test
    fun `commits a correctly predicted branch without redirecting`() {
        val stepResult = expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(
                    BranchReorderBufferEntry(
                        RobId(1),
                        InstructionAddress(4),
                        InstructionAddress(8),
                        InstructionAddress(8)
                    )
                ),
                StubRegisterFile(),
                StubMainMemory(),
                StubDynamicBranchTargetPredictor(emptyMap())
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.controlEvent)
            .isEqualTo(NoCommitControlEvent)

        expectThat(stepResult.statisticsDelta)
            .isEqualTo(CommitStatisticsDelta(1))
    }

    @Test
    fun `commits store half word operations`() {
        val stepResult = expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(
                    StoreReorderBufferEntry(
                        RobId(1),
                        StoreHalfWordOperation,
                        DataAddress(4),
                        ReadyOperand(Word(0xabceu))
                    )
                ),
                StubRegisterFile(),
                StubMainMemory(),
                StubDynamicBranchTargetPredictor(emptyMap())
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.mainMemory.loadHalfWord(4))
            .isSuccess()
            .isEqualTo(HalfWord(0xabceu))
    }

    @Test
    fun `commits store byte operations`() {
        val stepResult = expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(
                    StoreReorderBufferEntry(
                        RobId(1),
                        StoreByteOperation,
                        DataAddress(6),
                        ReadyOperand(Word(0x7fu))
                    )
                ),
                StubRegisterFile(),
                StubMainMemory(),
                StubDynamicBranchTargetPredictor(emptyMap())
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.mainMemory.loadByte(6))
            .isSuccess()
            .isEqualTo(Byte(0x7fu))
    }

    @Test
    fun `fails when a committed register write entry has no resolved value`() {
        expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(
                    RegisterWriteReorderBufferEntry(
                        RobId(1),
                        RegisterAddress(5),
                        ArithmeticLogicRegisterWriteReorderBufferEntryCategory,
                        PendingOperand(RobId(9))
                    )
                ),
                StubRegisterFile(),
                StubMainMemory(),
                StubDynamicBranchTargetPredictor(emptyMap())
            )
        )
            .isFailure()
            .isEqualTo(CommitEntryValueUnavailable(RobId(1)))
    }

    @Test
    fun `fails when a committed store entry has no resolved address`() {
        expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(
                    StoreReorderBufferEntry(
                        RobId(2),
                        StoreHalfWordOperation,
                        null,
                        ReadyOperand(Word(1u))
                    )
                ),
                StubRegisterFile(),
                StubMainMemory(),
                StubDynamicBranchTargetPredictor(emptyMap())
            )
        )
            .isFailure()
            .isEqualTo(CommitEntryAddressUnavailable(RobId(2)))
    }

    @Test
    fun `fails when a committed branch entry has no resolved next instruction address`() {
        expectThat(
            applyCommitUnit(
                commitUnit,
                StubCommitHeadReorderBuffer(
                    BranchReorderBufferEntry(
                        RobId(3),
                        InstructionAddress(4),
                        InstructionAddress(8),
                        null
                    )
                ),
                StubRegisterFile(),
                StubMainMemory(),
                StubDynamicBranchTargetPredictor(emptyMap())
            )
        )
            .isFailure()
            .isEqualTo(CommitEntryActualNextInstructionAddressUnavailable(RobId(3)))
    }
    private fun applyCommitUnit(
        commitUnit: CommitUnit,
        reorderBuffer: ReorderBuffer,
        registerFile: StubRegisterFile,
        mainMemory: StubMainMemory,
        branchTargetPredictor: DynamicBranchTargetPredictor
    ) =
        commitUnit
            .nextCycleDelta(
                reorderBuffer,
                registerFile,
                mainMemory,
                branchTargetPredictor
            )
            .flatMap { cycleDelta ->
                cycleDelta.applyToReorderBuffer(reorderBuffer)
                    .flatMap { nextReorderBuffer ->
                        cycleDelta.applyToMainMemory(mainMemory)
                            .map { nextMainMemory ->
                                AppliedCommitDelta(
                                    nextReorderBuffer,
                                    cycleDelta.applyToRegisterFile(registerFile),
                                    nextMainMemory,
                                    cycleDelta.applyToBranchTargetPredictor(branchTargetPredictor),
                                    cycleDelta.controlEvent,
                                    cycleDelta.statisticsDelta,
                                    cycleDelta.halted
                                )
                            }
                    }
            }

    private data class AppliedCommitDelta(
        val reorderBuffer: ReorderBuffer,
        val registerFile: registerfile.RegisterFile,
        val mainMemory: mainmemory.MainMemory,
        val branchTargetPredictor: DynamicBranchTargetPredictor,
        val controlEvent: CommitControlEvent,
        val statisticsDelta: CommitStatisticsDelta,
        val halted: Boolean
    )
}
