package decoder

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.DecoderUnknownFunct3
import types.DecoderUnknownFunct7
import types.DecoderUnknownOpcode
import types.RegisterAddress
import types.Word

class InstructionDecoderTest {

    private val decoder = RealInstructionDecoder
    private val instructionAddress = Word(100u)
    private val predictedNextInstructionAddress = Word(104u)

    @Test
    fun `decodes all OP-IMM operations`() {
        expectThat(decode(encodeI(opcode = 0x13, rd = 1, funct3 = 0x0, rs1 = 2, immediate = 7)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = AddImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(7u)
                )
            )

        expectThat(decode(encodeI(opcode = 0x13, rd = 1, funct3 = 0x2, rs1 = 2, immediate = -1)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = SetLessThanImmediateSignedOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xffffffffu)
                )
            )

        expectThat(decode(encodeI(opcode = 0x13, rd = 1, funct3 = 0x3, rs1 = 2, immediate = 9)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = SetLessThanImmediateUnsignedOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(9u)
                )
            )

        expectThat(decode(encodeI(opcode = 0x13, rd = 1, funct3 = 0x4, rs1 = 2, immediate = 3)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = ExclusiveOrImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(3u)
                )
            )

        expectThat(decode(encodeI(opcode = 0x13, rd = 1, funct3 = 0x6, rs1 = 2, immediate = 5)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = OrImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(5u)
                )
            )

        expectThat(decode(encodeI(opcode = 0x13, rd = 1, funct3 = 0x7, rs1 = 2, immediate = 6)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = AndImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(6u)
                )
            )
    }

    @Test
    fun `decodes shift immediate operations with masked shift amount`() {
        expectThat(decode(encodeShiftImmediate(rd = 1, funct3 = 0x1, rs1 = 2, shamt = 0, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = ShiftLeftLogicalImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0u)
                )
            )

        expectThat(decode(encodeShiftImmediate(rd = 1, funct3 = 0x5, rs1 = 2, shamt = 31, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = ShiftRightLogicalImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(31u)
                )
            )

        expectThat(decode(encodeShiftImmediate(rd = 1, funct3 = 0x1, rs1 = 2, shamt = 63, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = ShiftLeftLogicalImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(31u)
                )
            )

        expectThat(decode(encodeShiftImmediate(rd = 1, funct3 = 0x5, rs1 = 2, shamt = 63, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = ShiftRightLogicalImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(31u)
                )
            )

        expectThat(decode(encodeShiftImmediate(rd = 1, funct3 = 0x5, rs1 = 2, shamt = 63, funct7 = 0x20)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = ShiftRightArithmeticImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(31u)
                )
            )
    }

    @Test
    fun `decodes lui and auipc`() {
        expectThat(decode(encodeU(opcode = 0x37, rd = 5, immediateUpper = 0xABCDE)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadUpperImmediateInstruction(
                    destinationRegisterAddress = RegisterAddress(5),
                    immediate = Word(0xABCDE000u)
                )
            )

        expectThat(decode(encodeU(opcode = 0x17, rd = 6, immediateUpper = 0x12345)))
            .isSuccess()
            .isEqualTo(
                DecodedAddUpperImmediateToProgramCounterInstruction(
                    destinationRegisterAddress = RegisterAddress(6),
                    immediate = Word(0x12345000u),
                    instructionAddress = instructionAddress
                )
            )
    }

    @Test
    fun `decodes all OP operations`() {
        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x0, rs1 = 2, rs2 = 3, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = AddOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x0, rs1 = 2, rs2 = 3, funct7 = 0x20)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = SubtractOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x1, rs1 = 2, rs2 = 3, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = ShiftLeftLogicalOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x2, rs1 = 2, rs2 = 3, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = SetLessThanSignedOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x3, rs1 = 2, rs2 = 3, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = SetLessThanUnsignedOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x4, rs1 = 2, rs2 = 3, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = ExclusiveOrOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x5, rs1 = 2, rs2 = 3, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = ShiftRightLogicalOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x5, rs1 = 2, rs2 = 3, funct7 = 0x20)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = ShiftRightArithmeticOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x6, rs1 = 2, rs2 = 3, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = OrOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x7, rs1 = 2, rs2 = 3, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = AndOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    leftSourceRegisterAddress = RegisterAddress(2),
                    rightSourceRegisterAddress = RegisterAddress(3)
                )
            )
    }

    @Test
    fun `decodes jumps and branches`() {
        expectThat(decode(encodeJ(opcode = 0x6F, rd = 1, immediate = -4)))
            .isSuccess()
            .isEqualTo(
                DecodedJumpAndLinkInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    immediate = Word(0xFFFFFFFCu),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeI(opcode = 0x67, rd = 1, funct3 = 0x0, rs1 = 2, immediate = -8)))
            .isSuccess()
            .isEqualTo(
                DecodedJumpAndLinkRegisterInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xFFFFFFF8u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x0, rs1 = 1, rs2 = 2, immediate = -16)))
            .isSuccess()
            .isEqualTo(
                DecodedBranchInstruction(
                    operation = BranchEqualOperation,
                    leftSourceRegisterAddress = RegisterAddress(1),
                    rightSourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xFFFFFFF0u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x1, rs1 = 1, rs2 = 2, immediate = 8)))
            .isSuccess()
            .isEqualTo(
                DecodedBranchInstruction(
                    operation = BranchNotEqualOperation,
                    leftSourceRegisterAddress = RegisterAddress(1),
                    rightSourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(8u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x4, rs1 = 1, rs2 = 2, immediate = 8)))
            .isSuccess()
            .isEqualTo(
                DecodedBranchInstruction(
                    operation = BranchLessThanSignedOperation,
                    leftSourceRegisterAddress = RegisterAddress(1),
                    rightSourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(8u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x6, rs1 = 1, rs2 = 2, immediate = 8)))
            .isSuccess()
            .isEqualTo(
                DecodedBranchInstruction(
                    operation = BranchLessThanUnsignedOperation,
                    leftSourceRegisterAddress = RegisterAddress(1),
                    rightSourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(8u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x5, rs1 = 1, rs2 = 2, immediate = 8)))
            .isSuccess()
            .isEqualTo(
                DecodedBranchInstruction(
                    operation = BranchGreaterThanOrEqualSignedOperation,
                    leftSourceRegisterAddress = RegisterAddress(1),
                    rightSourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(8u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x7, rs1 = 1, rs2 = 2, immediate = 8)))
            .isSuccess()
            .isEqualTo(
                DecodedBranchInstruction(
                    operation = BranchGreaterThanOrEqualUnsignedOperation,
                    leftSourceRegisterAddress = RegisterAddress(1),
                    rightSourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(8u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )
    }

    @Test
    fun `decodes loads and stores`() {
        expectThat(decode(encodeI(opcode = 0x03, rd = 1, funct3 = 0x0, rs1 = 2, immediate = -4)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadInstruction(
                    operation = LoadByteOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    baseRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xFFFFFFFCu)
                )
            )

        expectThat(decode(encodeI(opcode = 0x03, rd = 1, funct3 = 0x4, rs1 = 2, immediate = 4)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadInstruction(
                    operation = LoadByteUnsignedOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    baseRegisterAddress = RegisterAddress(2),
                    immediate = Word(4u)
                )
            )

        expectThat(decode(encodeI(opcode = 0x03, rd = 1, funct3 = 0x1, rs1 = 2, immediate = 4)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadInstruction(
                    operation = LoadHalfWordOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    baseRegisterAddress = RegisterAddress(2),
                    immediate = Word(4u)
                )
            )

        expectThat(decode(encodeI(opcode = 0x03, rd = 1, funct3 = 0x5, rs1 = 2, immediate = 4)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadInstruction(
                    operation = LoadHalfWordUnsignedOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    baseRegisterAddress = RegisterAddress(2),
                    immediate = Word(4u)
                )
            )

        expectThat(decode(encodeI(opcode = 0x03, rd = 1, funct3 = 0x2, rs1 = 2, immediate = 4)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadInstruction(
                    operation = LoadWordOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    baseRegisterAddress = RegisterAddress(2),
                    immediate = Word(4u)
                )
            )

        expectThat(decode(encodeS(opcode = 0x23, funct3 = 0x0, rs1 = 2, rs2 = 3, immediate = -8)))
            .isSuccess()
            .isEqualTo(
                DecodedStoreInstruction(
                    operation = StoreByteOperation,
                    baseRegisterAddress = RegisterAddress(2),
                    valueRegisterAddress = RegisterAddress(3),
                    immediate = Word(0xFFFFFFF8u)
                )
            )

        expectThat(decode(encodeS(opcode = 0x23, funct3 = 0x1, rs1 = 2, rs2 = 3, immediate = 8)))
            .isSuccess()
            .isEqualTo(
                DecodedStoreInstruction(
                    operation = StoreHalfWordOperation,
                    baseRegisterAddress = RegisterAddress(2),
                    valueRegisterAddress = RegisterAddress(3),
                    immediate = Word(8u)
                )
            )

        expectThat(decode(encodeS(opcode = 0x23, funct3 = 0x2, rs1 = 2, rs2 = 3, immediate = 8)))
            .isSuccess()
            .isEqualTo(
                DecodedStoreInstruction(
                    operation = StoreWordOperation,
                    baseRegisterAddress = RegisterAddress(2),
                    valueRegisterAddress = RegisterAddress(3),
                    immediate = Word(8u)
                )
            )
    }

    @Test
    fun `fails for unknown opcode and funct values`() {
        expectThat(decode(Word(0u)))
            .isFailure()
            .isEqualTo(DecoderUnknownOpcode(0))

        expectThat(decode(encodeShiftImmediate(rd = 1, funct3 = 0x5, rs1 = 2, shamt = 3, funct7 = 0x02)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x13, funct3 = 0x5, funct7 = 0x02))

        expectThat(decode(encodeI(opcode = 0x03, rd = 1, funct3 = 0x3, rs1 = 2, immediate = 0)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct3(opcode = 0x03, funct3 = 0x3))
    }

    @Test
    fun `fails for unsupported rv32i opcode families not implemented in this decoder`() {
        expectThat(decode(encodeI(opcode = 0x0F, rd = 0, funct3 = 0x0, rs1 = 0, immediate = 0)))
            .isFailure()
            .isEqualTo(DecoderUnknownOpcode(0x0F))

        expectThat(decode(encodeI(opcode = 0x73, rd = 0, funct3 = 0x0, rs1 = 0, immediate = 0)))
            .isFailure()
            .isEqualTo(DecoderUnknownOpcode(0x73))
    }

    @Test
    fun `fails for jalr when funct3 is not zero`() {
        expectThat(decode(encodeI(opcode = 0x67, rd = 1, funct3 = 0x1, rs1 = 2, immediate = 0)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct3(opcode = 0x67, funct3 = 0x1))
    }

    @Test
    fun `fails for slli when funct7 is not zero`() {
        expectThat(decode(encodeShiftImmediate(rd = 1, funct3 = 0x1, rs1 = 2, shamt = 3, funct7 = 0x20)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x13, funct3 = 0x1, funct7 = 0x20))
    }

    @Test
    fun `fails for unknown funct3 across opcode families`() {
        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x2, rs1 = 1, rs2 = 2, immediate = 8)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct3(opcode = 0x63, funct3 = 0x2))

        expectThat(decode(encodeS(opcode = 0x23, funct3 = 0x3, rs1 = 2, rs2 = 3, immediate = 8)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct3(opcode = 0x23, funct3 = 0x3))
    }

    @Test
    fun `fails for unknown funct7 in OP split cases`() {
        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x0, rs1 = 2, rs2 = 3, funct7 = 0x01)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x33, funct3 = 0x0, funct7 = 0x01))

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x5, rs1 = 2, rs2 = 3, funct7 = 0x01)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x33, funct3 = 0x5, funct7 = 0x01))
    }

    @Test
    fun `fails for op operations requiring funct7 zero`() {
        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x1, rs1 = 2, rs2 = 3, funct7 = 0x20)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x33, funct3 = 0x1, funct7 = 0x20))

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x2, rs1 = 2, rs2 = 3, funct7 = 0x20)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x33, funct3 = 0x2, funct7 = 0x20))

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x3, rs1 = 2, rs2 = 3, funct7 = 0x20)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x33, funct3 = 0x3, funct7 = 0x20))

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x4, rs1 = 2, rs2 = 3, funct7 = 0x20)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x33, funct3 = 0x4, funct7 = 0x20))

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x6, rs1 = 2, rs2 = 3, funct7 = 0x20)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x33, funct3 = 0x6, funct7 = 0x20))

        expectThat(decode(encodeR(opcode = 0x33, rd = 1, funct3 = 0x7, rs1 = 2, rs2 = 3, funct7 = 0x20)))
            .isFailure()
            .isEqualTo(DecoderUnknownFunct7(opcode = 0x33, funct3 = 0x7, funct7 = 0x20))
    }

    @Test
    fun `decodes immediate boundary values for all signed immediate formats`() {
        expectThat(decode(encodeI(opcode = 0x13, rd = 1, funct3 = 0x0, rs1 = 2, immediate = 0x7FF)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = AddImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0x7FFu)
                )
            )

        expectThat(decode(encodeI(opcode = 0x13, rd = 1, funct3 = 0x0, rs1 = 2, immediate = 0x800)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticImmediateInstruction(
                    operation = AddImmediateOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xFFFFF800u)
                )
            )

        expectThat(decode(encodeS(opcode = 0x23, funct3 = 0x0, rs1 = 2, rs2 = 3, immediate = 0x7FF)))
            .isSuccess()
            .isEqualTo(
                DecodedStoreInstruction(
                    operation = StoreByteOperation,
                    baseRegisterAddress = RegisterAddress(2),
                    valueRegisterAddress = RegisterAddress(3),
                    immediate = Word(0x7FFu)
                )
            )

        expectThat(decode(encodeS(opcode = 0x23, funct3 = 0x0, rs1 = 2, rs2 = 3, immediate = 0x800)))
            .isSuccess()
            .isEqualTo(
                DecodedStoreInstruction(
                    operation = StoreByteOperation,
                    baseRegisterAddress = RegisterAddress(2),
                    valueRegisterAddress = RegisterAddress(3),
                    immediate = Word(0xFFFFF800u)
                )
            )

        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x0, rs1 = 1, rs2 = 2, immediate = 0xFFE)))
            .isSuccess()
            .isEqualTo(
                DecodedBranchInstruction(
                    operation = BranchEqualOperation,
                    leftSourceRegisterAddress = RegisterAddress(1),
                    rightSourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xFFEu),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeB(opcode = 0x63, funct3 = 0x0, rs1 = 1, rs2 = 2, immediate = 0x1000)))
            .isSuccess()
            .isEqualTo(
                DecodedBranchInstruction(
                    operation = BranchEqualOperation,
                    leftSourceRegisterAddress = RegisterAddress(1),
                    rightSourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xFFFFF000u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeJ(opcode = 0x6F, rd = 1, immediate = 0x7FE)))
            .isSuccess()
            .isEqualTo(
                DecodedJumpAndLinkInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    immediate = Word(0x7FEu),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeJ(opcode = 0x6F, rd = 1, immediate = 0xFFFFE)))
            .isSuccess()
            .isEqualTo(
                DecodedJumpAndLinkInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    immediate = Word(0x000FFFFEu),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeJ(opcode = 0x6F, rd = 1, immediate = 0x100000)))
            .isSuccess()
            .isEqualTo(
                DecodedJumpAndLinkInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    immediate = Word(0xFFF00000u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )
    }

    @Test
    fun `decodes i type immediate boundaries consistently across jalr and load`() {
        expectThat(decode(encodeI(opcode = 0x67, rd = 1, funct3 = 0x0, rs1 = 2, immediate = 0x7FF)))
            .isSuccess()
            .isEqualTo(
                DecodedJumpAndLinkRegisterInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0x7FFu),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeI(opcode = 0x67, rd = 1, funct3 = 0x0, rs1 = 2, immediate = 0x800)))
            .isSuccess()
            .isEqualTo(
                DecodedJumpAndLinkRegisterInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    sourceRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xFFFFF800u),
                    instructionAddress = instructionAddress,
                    predictedNextInstructionAddress = predictedNextInstructionAddress
                )
            )

        expectThat(decode(encodeI(opcode = 0x03, rd = 1, funct3 = 0x2, rs1 = 2, immediate = 0x7FF)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadInstruction(
                    operation = LoadWordOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    baseRegisterAddress = RegisterAddress(2),
                    immediate = Word(0x7FFu)
                )
            )

        expectThat(decode(encodeI(opcode = 0x03, rd = 1, funct3 = 0x2, rs1 = 2, immediate = 0x800)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadInstruction(
                    operation = LoadWordOperation,
                    destinationRegisterAddress = RegisterAddress(1),
                    baseRegisterAddress = RegisterAddress(2),
                    immediate = Word(0xFFFFF800u)
                )
            )
    }

    @Test
    fun `decodes u type immediate boundaries`() {
        expectThat(decode(encodeU(opcode = 0x37, rd = 1, immediateUpper = 0x00000)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadUpperImmediateInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    immediate = Word(0x00000000u)
                )
            )

        expectThat(decode(encodeU(opcode = 0x17, rd = 1, immediateUpper = 0xFFFFF)))
            .isSuccess()
            .isEqualTo(
                DecodedAddUpperImmediateToProgramCounterInstruction(
                    destinationRegisterAddress = RegisterAddress(1),
                    immediate = Word(0xFFFFF000u),
                    instructionAddress = instructionAddress
                )
            )
    }

    @Test
    fun `decodes register fields at zero and max indices`() {
        expectThat(decode(encodeR(opcode = 0x33, rd = 0, funct3 = 0x0, rs1 = 0, rs2 = 31, funct7 = 0x00)))
            .isSuccess()
            .isEqualTo(
                DecodedArithmeticRegisterInstruction(
                    operation = AddOperation,
                    destinationRegisterAddress = RegisterAddress(0),
                    leftSourceRegisterAddress = RegisterAddress(0),
                    rightSourceRegisterAddress = RegisterAddress(31)
                )
            )

        expectThat(decode(encodeI(opcode = 0x03, rd = 31, funct3 = 0x2, rs1 = 31, immediate = 0)))
            .isSuccess()
            .isEqualTo(
                DecodedLoadInstruction(
                    operation = LoadWordOperation,
                    destinationRegisterAddress = RegisterAddress(31),
                    baseRegisterAddress = RegisterAddress(31),
                    immediate = Word(0u)
                )
            )
    }

    private fun decode(instruction: Word) =
        decoder.decode(
            instruction,
            instructionAddress,
            predictedNextInstructionAddress
        )

    private fun encodeR(opcode: Int, rd: Int, funct3: Int, rs1: Int, rs2: Int, funct7: Int) =
        Word(
            (
                ((funct7 and 0x7F) shl 25) or
                    ((rs2 and 0x1F) shl 20) or
                    ((rs1 and 0x1F) shl 15) or
                    ((funct3 and 0x7) shl 12) or
                    ((rd and 0x1F) shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )

    private fun encodeI(opcode: Int, rd: Int, funct3: Int, rs1: Int, immediate: Int) =
        Word(
            (
                (((immediate and 0xFFF) shl 20)) or
                    ((rs1 and 0x1F) shl 15) or
                    ((funct3 and 0x7) shl 12) or
                    ((rd and 0x1F) shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )

    private fun encodeShiftImmediate(rd: Int, funct3: Int, rs1: Int, shamt: Int, funct7: Int) =
        Word(
            (
                (((funct7 and 0x7F) shl 25)) or
                    ((shamt and 0x1F) shl 20) or
                    ((rs1 and 0x1F) shl 15) or
                    ((funct3 and 0x7) shl 12) or
                    ((rd and 0x1F) shl 7) or
                    0x13
                ).toUInt()
        )

    private fun encodeS(opcode: Int, funct3: Int, rs1: Int, rs2: Int, immediate: Int): Word {
        val immediateLow = immediate and 0x1F
        val immediateHigh = (immediate shr 5) and 0x7F
        return Word(
            (
                (immediateHigh shl 25) or
                    ((rs2 and 0x1F) shl 20) or
                    ((rs1 and 0x1F) shl 15) or
                    ((funct3 and 0x7) shl 12) or
                    (immediateLow shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )
    }

    private fun encodeB(opcode: Int, funct3: Int, rs1: Int, rs2: Int, immediate: Int): Word {
        val bit12 = (immediate shr 12) and 0x1
        val bit11 = (immediate shr 11) and 0x1
        val bits10To5 = (immediate shr 5) and 0x3F
        val bits4To1 = (immediate shr 1) and 0xF
        return Word(
            (
                (bit12 shl 31) or
                    (bits10To5 shl 25) or
                    ((rs2 and 0x1F) shl 20) or
                    ((rs1 and 0x1F) shl 15) or
                    ((funct3 and 0x7) shl 12) or
                    (bits4To1 shl 8) or
                    (bit11 shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )
    }

    private fun encodeU(opcode: Int, rd: Int, immediateUpper: Int) =
        Word(
            (
                ((immediateUpper and 0xFFFFF) shl 12) or
                    ((rd and 0x1F) shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )

    private fun encodeJ(opcode: Int, rd: Int, immediate: Int): Word {
        val bit20 = (immediate shr 20) and 0x1
        val bits10To1 = (immediate shr 1) and 0x3FF
        val bit11 = (immediate shr 11) and 0x1
        val bits19To12 = (immediate shr 12) and 0xFF
        return Word(
            (
                (bit20 shl 31) or
                    (bits19To12 shl 12) or
                    (bit11 shl 20) or
                    (bits10To1 shl 21) or
                    ((rd and 0x1F) shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )
    }

}
