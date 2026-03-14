package issue

import arithmeticlogic.*
import branchlogic.*
import branchlogic.BranchOperation
import decoder.*
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import instructionqueue.InstructionQueue
import instructionqueue.InstructionQueueEntry
import instructionqueue.StubInstructionQueue
import memorybuffer.LoadAddressComputationWork
import memorybuffer.RecordingMemoryBufferQueue
import memorybuffer.StoreAddressComputationWork
import org.junit.jupiter.api.Test
import registerfile.RegisterFile
import registerfile.StubRegisterFile
import reorderbuffer.ReorderBuffer
import reorderbuffer.StubReorderBuffer
import reservationstation.ReadyReservationStationEntry
import reservationstation.RecordingReservationStationBank
import reservationstation.ReservationStationBank
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*

class IssueUnitTest {

    @Test
    fun `returns the input state unchanged when the issue width is zero`() {
        val issueUnit = RealIssueUnit(cpu.IssueWidth(0))
        val instructionQueue = queueOf(listOf(Word(1u)))

        val stepResult = expectThat(
            applyIssueUnit(
                issueUnit,
                instructionQueue,
                decoderFor(Word(1u), arithmeticImmediate(AddImmediateOperation)),
                StubRegisterFile(),
                StubReorderBuffer(),
                RecordingReservationStationBank(),
                RecordingReservationStationBank(),
                RecordingMemoryBufferQueue()
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.instructionQueue.entryCount())
            .isEqualTo(1)
    }

    @Test
    fun `returns the input state unchanged when the instruction queue is empty`() {
        val stepResult = expectThat(
            applyIssueUnit(
                RealIssueUnit(cpu.IssueWidth(1)),
                StubInstructionQueue(emptyList()),
                decoderFor(Word(1u), arithmeticImmediate(AddImmediateOperation)),
                StubRegisterFile(),
                StubReorderBuffer(),
                RecordingReservationStationBank(),
                RecordingReservationStationBank(),
                RecordingMemoryBufferQueue()
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.reorderBuffer.entryCount())
            .isEqualTo(0)
    }

    @Test
    fun `issues multiple instructions up to the configured width`() {
        val stepResult = expectThat(
            applyIssueUnit(
                RealIssueUnit(cpu.IssueWidth(2)),
                queueOf(listOf(Word(1u), Word(2u))),
                decoderFor(
                    Word(1u) to arithmeticImmediate(AddImmediateOperation),
                    Word(2u) to arithmeticImmediate(OrImmediateOperation)
                ),
                StubRegisterFile(),
                StubReorderBuffer(),
                RecordingReservationStationBank(),
                RecordingReservationStationBank(),
                RecordingMemoryBufferQueue()
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.instructionQueue.entryCount())
            .isEqualTo(0)

        expectThat(stepResult.reorderBuffer.entryCount())
            .isEqualTo(2)

        expectThat(stepResult.arithmeticLogicReservationStations.dispatchReady(2).entries)
            .isEqualTo(
                listOf(
                    arithmeticReadyEntry(AddImmediate, RobId(1), Word(0u), Word(9u), InstructionAddress(0)),
                    ReadyReservationStationEntry(
                        types.ReservationStationId(2),
                        OrImmediate,
                        Word(0u),
                        Word(9u),
                        Word(0u),
                        RobId(2),
                        InstructionAddress(0)
                    )
                )
            )
    }

    @Test
    fun `issues each arithmetic immediate operation`() {
        assertArithmeticImmediateOperation(AddImmediateOperation, AddImmediate)
        assertArithmeticImmediateOperation(SetLessThanImmediateSignedOperation, SetLessThanImmediateSigned)
        assertArithmeticImmediateOperation(SetLessThanImmediateUnsignedOperation, SetLessThanImmediateUnsigned)
        assertArithmeticImmediateOperation(ExclusiveOrImmediateOperation, ExclusiveOrImmediate)
        assertArithmeticImmediateOperation(OrImmediateOperation, OrImmediate)
        assertArithmeticImmediateOperation(AndImmediateOperation, AndImmediate)
        assertArithmeticImmediateOperation(ShiftLeftLogicalImmediateOperation, ShiftLeftLogicalImmediate)
        assertArithmeticImmediateOperation(ShiftRightLogicalImmediateOperation, ShiftRightLogicalImmediate)
        assertArithmeticImmediateOperation(ShiftRightArithmeticImmediateOperation, ShiftRightArithmeticImmediate)
    }

    @Test
    fun `issues each arithmetic register operation and resolves ready rob operands`() {
        assertArithmeticRegisterOperation(AddOperation, Add)
        assertArithmeticRegisterOperation(SubtractOperation, Subtract)
        assertArithmeticRegisterOperation(ShiftLeftLogicalOperation, ShiftLeftLogical)
        assertArithmeticRegisterOperation(SetLessThanSignedOperation, SetLessThanSigned)
        assertArithmeticRegisterOperation(SetLessThanUnsignedOperation, SetLessThanUnsigned)
        assertArithmeticRegisterOperation(ExclusiveOrOperation, ExclusiveOr)
        assertArithmeticRegisterOperation(ShiftRightLogicalOperation, ShiftRightLogical)
        assertArithmeticRegisterOperation(ShiftRightArithmeticOperation, ShiftRightArithmetic)
        assertArithmeticRegisterOperation(OrOperation, Or)
        assertArithmeticRegisterOperation(AndOperation, And)
    }

    @Test
    fun `issues load upper immediate and add upper immediate to program counter`() {
        val loadUpperResult = expectThat(
            issueSingleInstruction(DecodedLoadUpperImmediateInstruction(RegisterAddress(5), Word(10u)))
        )
            .isSuccess()
            .subject
        val addUpperResult = expectThat(
            issueSingleInstruction(
                DecodedAddUpperImmediateToProgramCounterInstruction(
                    RegisterAddress(6),
                    Word(12u),
                    Word(16u)
                )
            )
        )
            .isSuccess()
            .subject

        expectThat(loadUpperResult.arithmeticLogicReservationStations.dispatchReady(1).entries)
            .isEqualTo(
                listOf(
                    arithmeticReadyEntry(LoadUpperImmediate, RobId(1), Word(10u), Word(0u), InstructionAddress(0))
                )
            )

        expectThat(addUpperResult.arithmeticLogicReservationStations.dispatchReady(1).entries)
            .isEqualTo(
                listOf(
                    arithmeticReadyEntry(
                        AddUpperImmediateToProgramCounter,
                        RobId(1),
                        Word(12u),
                        Word(0u),
                        InstructionAddress(16)
                    )
                )
            )
    }

    @Test
    fun `issues jump instructions`() {
        val jumpResult = expectThat(
            issueSingleInstruction(
                DecodedJumpAndLinkInstruction(
                    RegisterAddress(5),
                    Word(20u),
                    Word(24u),
                    Word(28u)
                )
            )
        )
            .isSuccess()
            .subject
        val jumpRegisterResult = expectThat(
            issueSingleInstruction(
                DecodedJumpAndLinkRegisterInstruction(
                    RegisterAddress(6),
                    RegisterAddress(1),
                    Word(20u),
                    Word(24u),
                    Word(28u)
                ),
                StubRegisterFile().seed(RegisterAddress(1), Word(30u))
            )
        )
            .isSuccess()
            .subject

        expectThat(jumpResult.branchReservationStations.dispatchReady(1).entries)
            .isEqualTo(
                listOf(
                    branchReadyEntry(JumpAndLink, RobId(1), Word(0u), Word(0u), Word(20u), InstructionAddress(24))
                )
            )

        expectThat(jumpRegisterResult.branchReservationStations.dispatchReady(1).entries)
            .isEqualTo(
                listOf(
                    branchReadyEntry(JumpAndLinkRegister, RobId(1), Word(30u), Word(0u), Word(20u), InstructionAddress(24))
                )
            )
    }

    @Test
    fun `issues each branch operation`() {
        assertBranchOperation(BranchEqualOperation, BranchEqual)
        assertBranchOperation(BranchNotEqualOperation, BranchNotEqual)
        assertBranchOperation(BranchLessThanSignedOperation, BranchLessThanSigned)
        assertBranchOperation(BranchLessThanUnsignedOperation, BranchLessThanUnsigned)
        assertBranchOperation(BranchGreaterThanOrEqualSignedOperation, BranchGreaterThanOrEqualSigned)
        assertBranchOperation(BranchGreaterThanOrEqualUnsignedOperation, BranchGreaterThanOrEqualUnsigned)
    }

    @Test
    fun `issues each load operation`() {
        assertLoadOperation(LoadWordOperation)
        assertLoadOperation(LoadHalfWordOperation)
        assertLoadOperation(LoadHalfWordUnsignedOperation)
        assertLoadOperation(LoadByteOperation)
        assertLoadOperation(LoadByteUnsignedOperation)
    }

    @Test
    fun `issues each store operation`() {
        assertStoreOperation(StoreWordOperation)
        assertStoreOperation(StoreHalfWordOperation)
        assertStoreOperation(StoreByteOperation)
    }

    @Test
    fun `propagates decoder failures`() {
        expectThat(
            applyIssueUnit(
                RealIssueUnit(cpu.IssueWidth(1)),
                queueOf(listOf(Word(1u))),
                decoderForFailure(Word(1u), DecoderUnknownOpcode(99)),
                StubRegisterFile(),
                StubReorderBuffer(),
                RecordingReservationStationBank(),
                RecordingReservationStationBank(),
                RecordingMemoryBufferQueue()
            )
        )
            .isFailure()
            .isEqualTo(DecoderUnknownOpcode(99))
    }

    @Test
    fun `returns the input state unchanged when the reorder buffer is full`() {
        val stepResult = expectThat(
            applyIssueUnit(
                RealIssueUnit(cpu.IssueWidth(1)),
                queueOf(listOf(Word(1u))),
                decoderFor(Word(1u), arithmeticImmediate(AddImmediateOperation)),
                StubRegisterFile(),
                StubReorderBuffer().withRegisterWriteFailure(ReorderBufferFull),
                RecordingReservationStationBank(),
                RecordingReservationStationBank(),
                RecordingMemoryBufferQueue()
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.instructionQueue.entryCount())
            .isEqualTo(1)
    }

    @Test
    fun `returns the input state unchanged when the arithmetic reservation station bank is full`() {
        val stepResult = expectThat(
            applyIssueUnit(
                RealIssueUnit(cpu.IssueWidth(1)),
                queueOf(listOf(Word(1u))),
                decoderFor(Word(1u), arithmeticImmediate(AddImmediateOperation)),
                StubRegisterFile(),
                StubReorderBuffer(),
                RecordingReservationStationBank(ReservationStationFull),
                RecordingReservationStationBank(),
                RecordingMemoryBufferQueue()
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.instructionQueue.entryCount())
            .isEqualTo(1)
    }

    @Test
    fun `returns the input state unchanged when the memory buffer is full`() {
        val stepResult = expectThat(
            applyIssueUnit(
                RealIssueUnit(cpu.IssueWidth(1)),
                queueOf(listOf(Word(1u))),
                decoderFor(
                    Word(1u),
                    DecodedLoadInstruction(
                        LoadWordOperation,
                        RegisterAddress(5),
                        RegisterAddress(1),
                        Word(12u)
                    )
                ),
                StubRegisterFile().seed(RegisterAddress(1), Word(8u)),
                StubReorderBuffer(),
                RecordingReservationStationBank(),
                RecordingReservationStationBank(),
                RecordingMemoryBufferQueue(MemoryBufferFull)
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.instructionQueue.entryCount())
            .isEqualTo(1)
    }

    @Test
    fun `propagates non capacity failures from issue components`() {
        expectThat(
            applyIssueUnit(
                RealIssueUnit(cpu.IssueWidth(1)),
                queueOf(listOf(Word(1u))),
                decoderFor(Word(1u), arithmeticImmediate(AddImmediateOperation)),
                StubRegisterFile(),
                StubReorderBuffer(),
                RecordingReservationStationBank(CommonDataBusFull),
                RecordingReservationStationBank(),
                RecordingMemoryBufferQueue()
            )
        )
            .isFailure()
            .isEqualTo(CommonDataBusFull)
    }

    private fun assertArithmeticImmediateOperation(
        decodedOperation: decoder.ArithmeticImmediateOperation,
        issuedOperation: ArithmeticLogicOperation
    ) {
        val stepResult = expectThat(issueSingleInstruction(arithmeticImmediate(decodedOperation)))
            .isSuccess()
            .subject

        expectThat(stepResult.arithmeticLogicReservationStations.dispatchReady(1).entries)
            .isEqualTo(
                listOf(
                    arithmeticReadyEntry(issuedOperation, RobId(1), Word(0u), Word(9u), InstructionAddress(0))
                )
            )
    }

    private fun assertArithmeticRegisterOperation(
        decodedOperation: decoder.ArithmeticRegisterOperation,
        issuedOperation: ArithmeticLogicOperation
    ) {
        val reorderBuffer = StubReorderBuffer()
            .withResolvedValue(RobId(9), Word(7u))
        val registerFile = StubRegisterFile()
            .withOperand(RegisterAddress(1), PendingOperand(RobId(9)))
            .seed(RegisterAddress(2), Word(8u))

        val stepResult = expectThat(
            issueSingleInstruction(
                DecodedArithmeticRegisterInstruction(
                    decodedOperation,
                    RegisterAddress(5),
                    RegisterAddress(1),
                    RegisterAddress(2)
                ),
                registerFile,
                reorderBuffer
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.arithmeticLogicReservationStations.dispatchReady(1).entries)
            .isEqualTo(
                listOf(
                    arithmeticReadyEntry(issuedOperation, RobId(1), Word(7u), Word(8u), InstructionAddress(0))
                )
            )
    }

    private fun assertBranchOperation(
        decodedOperation: decoder.BranchOperation,
        issuedOperation: BranchOperation
    ) {
        val stepResult = expectThat(
            issueSingleInstruction(
                DecodedBranchInstruction(
                    decodedOperation,
                    RegisterAddress(1),
                    RegisterAddress(2),
                    Word(12u),
                    Word(16u),
                    Word(20u)
                ),
                StubRegisterFile()
                    .seed(RegisterAddress(1), Word(5u))
                    .seed(RegisterAddress(2), Word(6u))
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.branchReservationStations.dispatchReady(1).entries)
            .isEqualTo(
                listOf(
                    branchReadyEntry(issuedOperation, RobId(1), Word(5u), Word(6u), Word(12u), InstructionAddress(16))
                )
            )
    }

    private fun assertLoadOperation(operation: LoadOperation) {
        val stepResult = expectThat(
            issueSingleInstruction(
                DecodedLoadInstruction(
                    operation,
                    RegisterAddress(5),
                    RegisterAddress(1),
                    Word(12u)
                ),
                StubRegisterFile().seed(RegisterAddress(1), Word(8u))
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.registerFile.operandFor(RegisterAddress(5)))
            .isA<PendingOperand>()
            .get { robId }
            .isEqualTo(RobId(1))

        expectThat(stepResult.memoryBufferQueue.dispatchAddressComputations(1).workItems)
            .isEqualTo(
                listOf(
                    LoadAddressComputationWork(
                        MemoryBufferId(1),
                        operation,
                        Word(8u),
                        Word(12u),
                        RobId(1)
                    )
                )
            )
    }

    private fun assertStoreOperation(operation: StoreOperation) {
        val stepResult = expectThat(
            issueSingleInstruction(
                DecodedStoreInstruction(
                    operation,
                    RegisterAddress(1),
                    RegisterAddress(2),
                    Word(12u)
                ),
                StubRegisterFile()
                    .seed(RegisterAddress(1), Word(8u))
                    .seed(RegisterAddress(2), Word(15u))
            )
        )
            .isSuccess()
            .subject

        expectThat(stepResult.memoryBufferQueue.dispatchAddressComputations(1).workItems)
            .isEqualTo(
                listOf(
                    StoreAddressComputationWork(
                        MemoryBufferId(1),
                        operation,
                        Word(8u),
                        Word(12u),
                        RobId(1)
                    )
                )
            )
    }

    private fun issueSingleInstruction(decodedInstruction: DecodedInstruction) =
        issueSingleInstruction(
            decodedInstruction,
            StubRegisterFile()
        )

    private fun issueSingleInstruction(
        decodedInstruction: DecodedInstruction,
        registerFile: RegisterFile
    ) =
        issueSingleInstruction(
            decodedInstruction,
            registerFile,
            StubReorderBuffer()
        )

    private fun issueSingleInstruction(
        decodedInstruction: DecodedInstruction,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer
    ) =
        applyIssueUnit(
            RealIssueUnit(cpu.IssueWidth(1)),
            queueOf(listOf(Word(1u))),
            decoderFor(Word(1u), decodedInstruction),
            registerFile,
            reorderBuffer,
            RecordingReservationStationBank(),
            RecordingReservationStationBank(),
            RecordingMemoryBufferQueue()
        )

    private fun arithmeticImmediate(operation: decoder.ArithmeticImmediateOperation) =
        DecodedArithmeticImmediateInstruction(
            operation,
            RegisterAddress(5),
            RegisterAddress(1),
            Word(9u)
        )

    private fun arithmeticReadyEntry(
        operation: ArithmeticLogicOperation,
        robId: RobId,
        leftValue: Word,
        rightValue: Word,
        instructionAddress: InstructionAddress
    ) =
        ReadyReservationStationEntry(
            types.ReservationStationId(1),
            operation,
            leftValue,
            rightValue,
            Word(0u),
            robId,
            instructionAddress
        )

    private fun branchReadyEntry(
        operation: BranchOperation,
        robId: RobId,
        leftValue: Word,
        rightValue: Word,
        immediate: Word,
        instructionAddress: InstructionAddress
    ) =
        ReadyReservationStationEntry(
            types.ReservationStationId(1),
            operation,
            leftValue,
            rightValue,
            immediate,
            robId,
            instructionAddress
        )

    private fun queueOf(instructions: List<Word>): InstructionQueue =
        StubInstructionQueue(
            instructions.mapIndexed { index, instruction ->
                InstructionQueueEntry(
                    instruction,
                    InstructionAddress(index * 4),
                    InstructionAddress(index * 4 + 4)
                )
            }
        )

    private fun decoderFor(instruction: Word, decodedInstruction: DecodedInstruction) =
        decoderFor(instruction to decodedInstruction)

    private fun decoderFor(vararg mappings: Pair<Word, DecodedInstruction>) =
        MapInstructionDecoderStub(mappings.toMap().mapValues { mapping -> mapping.value.asSuccess() })

    private fun decoderForFailure(instruction: Word, error: types.ProcessorError) =
        MapInstructionDecoderStub(mapOf(instruction to error.asFailure()))

    private fun applyIssueUnit(
        issueUnit: IssueUnit,
        instructionQueue: InstructionQueue,
        instructionDecoder: InstructionDecoder,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: RecordingReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: RecordingReservationStationBank<BranchOperation>,
        memoryBufferQueue: RecordingMemoryBufferQueue
    ) =
        issueUnit
            .nextCycleDelta(
                instructionQueue,
                instructionDecoder,
                registerFile,
                reorderBuffer,
                arithmeticLogicReservationStations,
                branchReservationStations,
                memoryBufferQueue
            )
            .flatMap { cycleDelta ->
                cycleDelta.applyToInstructionQueue(instructionQueue)
                    .flatMap { nextInstructionQueue ->
                        cycleDelta.applyToReorderBuffer(reorderBuffer)
                            .flatMap { nextReorderBuffer ->
                                cycleDelta.applyToArithmeticLogicReservationStations(arithmeticLogicReservationStations)
                                    .flatMap { nextArithmeticLogicReservationStations ->
                                        cycleDelta.applyToBranchReservationStations(branchReservationStations)
                                            .flatMap { nextBranchReservationStations ->
                                                cycleDelta.applyToMemoryBufferQueue(memoryBufferQueue)
                                                    .map { nextMemoryBufferQueue ->
                                                        AppliedIssueDelta(
                                                            nextInstructionQueue,
                                                            cycleDelta.applyToRegisterFile(registerFile),
                                                            nextReorderBuffer,
                                                            nextArithmeticLogicReservationStations,
                                                            nextBranchReservationStations,
                                                            nextMemoryBufferQueue
                                                        )
                                                    }
                                            }
                                    }
                            }
                    }
            }

    private data class AppliedIssueDelta(
        val instructionQueue: InstructionQueue,
        val registerFile: RegisterFile,
        val reorderBuffer: ReorderBuffer,
        val arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        val branchReservationStations: ReservationStationBank<BranchOperation>,
        val memoryBufferQueue: memorybuffer.MemoryBufferQueue
    )
}
