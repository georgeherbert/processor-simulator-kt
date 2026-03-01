package decoder

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.map
import types.*

interface InstructionDecoder {
    fun decode(
        instruction: Word,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ): ProcessorResult<DecodedInstruction>
}

data object RealInstructionDecoder : InstructionDecoder {

    override fun decode(
        instruction: Word,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        when (val opcode = opcodeOf(instruction)) {
            OpcodeOperationImmediate -> decodeOperationImmediate(iTypeFieldsOf(instruction))
            OpcodeLoadUpperImmediate -> decodeLoadUpperImmediate(uTypeFieldsOf(instruction))
            OpcodeAddUpperImmediateToProgramCounter ->
                decodeAddUpperImmediateToProgramCounter(uTypeFieldsOf(instruction), instructionAddress)

            OpcodeOperation -> decodeOperationRegister(rTypeFieldsOf(instruction))
            OpcodeJumpAndLink ->
                decodeJumpAndLink(jTypeFieldsOf(instruction), instructionAddress, predictedNextInstructionAddress)

            OpcodeJumpAndLinkRegister ->
                decodeJumpAndLinkRegister(iTypeFieldsOf(instruction), instructionAddress, predictedNextInstructionAddress)

            OpcodeBranch -> decodeBranch(bTypeFieldsOf(instruction), instructionAddress, predictedNextInstructionAddress)
            OpcodeLoad -> decodeLoad(iTypeFieldsOf(instruction))
            OpcodeStore -> decodeStore(sTypeFieldsOf(instruction))
            OpcodeMiscMemory -> DecoderUnknownOpcode(opcode).asFailure()
            OpcodeSystem -> DecoderUnknownOpcode(opcode).asFailure()
            else -> DecoderUnknownOpcode(opcode).asFailure()
        }

    private fun decodeOperationImmediate(iTypeFields: ITypeFields) =
        decodeArithmeticImmediateOperation(iTypeFields)
            .map { operationAndImmediate ->
                DecodedArithmeticImmediateInstruction(
                    operationAndImmediate.operation,
                    iTypeFields.destinationRegisterAddress,
                    iTypeFields.sourceRegisterAddress,
                    operationAndImmediate.immediate
                )
            }

    private fun decodeArithmeticImmediateOperation(iTypeFields: ITypeFields) =
        when (iTypeFields.funct3) {
            Funct3AddImmediate ->
                ArithmeticImmediateOperationAndImmediate(AddImmediateOperation, iTypeFields.immediate).asSuccess()

            Funct3SetLessThanImmediateSigned ->
                ArithmeticImmediateOperationAndImmediate(
                    SetLessThanImmediateSignedOperation,
                    iTypeFields.immediate
                ).asSuccess()

            Funct3SetLessThanImmediateUnsigned ->
                ArithmeticImmediateOperationAndImmediate(
                    SetLessThanImmediateUnsignedOperation,
                    iTypeFields.immediate
                ).asSuccess()

            Funct3ExclusiveOrImmediate ->
                ArithmeticImmediateOperationAndImmediate(
                    ExclusiveOrImmediateOperation,
                    iTypeFields.immediate
                ).asSuccess()

            Funct3OrImmediate ->
                ArithmeticImmediateOperationAndImmediate(OrImmediateOperation, iTypeFields.immediate).asSuccess()

            Funct3AndImmediate ->
                ArithmeticImmediateOperationAndImmediate(AndImmediateOperation, iTypeFields.immediate).asSuccess()

            Funct3ShiftLeftLogicalImmediate ->
                decodeShiftLeftImmediateOperation(shiftImmediateFieldsOf(iTypeFields))

            Funct3ShiftRightLogicalOrArithmeticImmediate ->
                decodeShiftRightImmediateOperation(shiftImmediateFieldsOf(iTypeFields))

            else -> DecoderUnknownFunct3(OpcodeOperationImmediate, iTypeFields.funct3).asFailure()
        }

    private fun decodeShiftLeftImmediateOperation(shiftImmediateFields: ShiftImmediateFields) =
        when (shiftImmediateFields.functionCode7) {
            Funct7ShiftLeftLogical ->
                ArithmeticImmediateOperationAndImmediate(
                    ShiftLeftLogicalImmediateOperation,
                    shiftImmediateFields.shiftAmount
                ).asSuccess()

            else ->
                DecoderUnknownFunct7(
                    OpcodeOperationImmediate,
                    Funct3ShiftLeftLogicalImmediate,
                    shiftImmediateFields.functionCode7
                ).asFailure()
        }

    private fun decodeShiftRightImmediateOperation(shiftImmediateFields: ShiftImmediateFields) =
        when (shiftImmediateFields.functionCode7) {
            Funct7ShiftRightLogical ->
                ArithmeticImmediateOperationAndImmediate(
                    ShiftRightLogicalImmediateOperation,
                    shiftImmediateFields.shiftAmount
                ).asSuccess()

            Funct7ShiftRightArithmetic ->
                ArithmeticImmediateOperationAndImmediate(
                    ShiftRightArithmeticImmediateOperation,
                    shiftImmediateFields.shiftAmount
                ).asSuccess()

            else ->
                DecoderUnknownFunct7(
                    OpcodeOperationImmediate,
                    Funct3ShiftRightLogicalOrArithmeticImmediate,
                    shiftImmediateFields.functionCode7
                ).asFailure()
        }

    private fun shiftImmediateFieldsOf(iTypeFields: ITypeFields) =
        ShiftImmediateFields(
            Word((iTypeFields.immediateField and ShiftAmountMask).toUInt()),
            (iTypeFields.immediateField shr ShiftImmediateFunctionCodeOffset) and Funct7Mask
        )

    private fun decodeLoadUpperImmediate(uTypeFields: UTypeFields) =
        DecodedLoadUpperImmediateInstruction(
            uTypeFields.destinationRegisterAddress,
            uTypeFields.immediate
        ).asSuccess()

    private fun decodeAddUpperImmediateToProgramCounter(
        uTypeFields: UTypeFields,
        instructionAddress: Word
    ) =
        DecodedAddUpperImmediateToProgramCounterInstruction(
            uTypeFields.destinationRegisterAddress,
            uTypeFields.immediate,
            instructionAddress
        ).asSuccess()

    private fun decodeOperationRegister(rTypeFields: RTypeFields) =
        decodeArithmeticRegisterOperation(rTypeFields)
            .map { operation ->
                DecodedArithmeticRegisterInstruction(
                    operation,
                    rTypeFields.destinationRegisterAddress,
                    rTypeFields.leftSourceRegisterAddress,
                    rTypeFields.rightSourceRegisterAddress
                )
            }

    private fun decodeArithmeticRegisterOperation(rTypeFields: RTypeFields) =
        when (rTypeFields.funct3) {
            Funct3AddOrSubtract -> decodeAddOrSubtractOperation(rTypeFields)
            Funct3ShiftLeftLogical -> decodeFixedFunct7RegisterOperation(rTypeFields, ShiftLeftLogicalOperation)
            Funct3SetLessThanSigned -> decodeFixedFunct7RegisterOperation(rTypeFields, SetLessThanSignedOperation)
            Funct3SetLessThanUnsigned -> decodeFixedFunct7RegisterOperation(rTypeFields, SetLessThanUnsignedOperation)
            Funct3ExclusiveOr -> decodeFixedFunct7RegisterOperation(rTypeFields, ExclusiveOrOperation)
            Funct3ShiftRightLogicalOrArithmetic -> decodeShiftRightRegisterOperation(rTypeFields)
            Funct3Or -> decodeFixedFunct7RegisterOperation(rTypeFields, OrOperation)
            Funct3And -> decodeFixedFunct7RegisterOperation(rTypeFields, AndOperation)
            else -> DecoderUnknownFunct3(OpcodeOperation, rTypeFields.funct3).asFailure()
        }

    private fun decodeFixedFunct7RegisterOperation(
        rTypeFields: RTypeFields,
        operation: ArithmeticRegisterOperation
    ) =
        when (rTypeFields.funct7) {
            Funct7Add -> operation.asSuccess()
            else -> DecoderUnknownFunct7(OpcodeOperation, rTypeFields.funct3, rTypeFields.funct7).asFailure()
        }

    private fun decodeAddOrSubtractOperation(rTypeFields: RTypeFields) =
        when (rTypeFields.funct7) {
            Funct7Add -> AddOperation.asSuccess()
            Funct7Subtract -> SubtractOperation.asSuccess()
            else -> DecoderUnknownFunct7(OpcodeOperation, Funct3AddOrSubtract, rTypeFields.funct7).asFailure()
        }

    private fun decodeShiftRightRegisterOperation(rTypeFields: RTypeFields) =
        when (rTypeFields.funct7) {
            Funct7ShiftRightLogical -> ShiftRightLogicalOperation.asSuccess()
            Funct7ShiftRightArithmetic -> ShiftRightArithmeticOperation.asSuccess()
            else ->
                DecoderUnknownFunct7(
                    OpcodeOperation,
                    Funct3ShiftRightLogicalOrArithmetic,
                    rTypeFields.funct7
                ).asFailure()
        }

    private fun decodeJumpAndLink(
        jTypeFields: JTypeFields,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        DecodedJumpAndLinkInstruction(
            jTypeFields.destinationRegisterAddress,
            jTypeFields.immediate,
            instructionAddress,
            predictedNextInstructionAddress
        ).asSuccess()

    private fun decodeJumpAndLinkRegister(
        iTypeFields: ITypeFields,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        when (iTypeFields.funct3) {
            Funct3JumpAndLinkRegister ->
                DecodedJumpAndLinkRegisterInstruction(
                    iTypeFields.destinationRegisterAddress,
                    iTypeFields.sourceRegisterAddress,
                    iTypeFields.immediate,
                    instructionAddress,
                    predictedNextInstructionAddress
                ).asSuccess()

            else -> DecoderUnknownFunct3(OpcodeJumpAndLinkRegister, iTypeFields.funct3).asFailure()
        }

    private fun decodeBranch(
        bTypeFields: BTypeFields,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        decodeBranchOperation(bTypeFields)
            .map { operation ->
                DecodedBranchInstruction(
                    operation,
                    bTypeFields.leftSourceRegisterAddress,
                    bTypeFields.rightSourceRegisterAddress,
                    bTypeFields.immediate,
                    instructionAddress,
                    predictedNextInstructionAddress
                )
            }

    private fun decodeBranchOperation(bTypeFields: BTypeFields) =
        when (bTypeFields.funct3) {
            Funct3BranchEqual -> BranchEqualOperation.asSuccess()
            Funct3BranchNotEqual -> BranchNotEqualOperation.asSuccess()
            Funct3BranchLessThanSigned -> BranchLessThanSignedOperation.asSuccess()
            Funct3BranchGreaterThanOrEqualSigned -> BranchGreaterThanOrEqualSignedOperation.asSuccess()
            Funct3BranchLessThanUnsigned -> BranchLessThanUnsignedOperation.asSuccess()
            Funct3BranchGreaterThanOrEqualUnsigned -> BranchGreaterThanOrEqualUnsignedOperation.asSuccess()
            else -> DecoderUnknownFunct3(OpcodeBranch, bTypeFields.funct3).asFailure()
        }

    private fun decodeLoad(iTypeFields: ITypeFields) =
        decodeLoadOperation(iTypeFields)
            .map { operation ->
                DecodedLoadInstruction(
                    operation,
                    iTypeFields.destinationRegisterAddress,
                    iTypeFields.sourceRegisterAddress,
                    iTypeFields.immediate
                )
            }

    private fun decodeLoadOperation(iTypeFields: ITypeFields) =
        when (iTypeFields.funct3) {
            Funct3LoadByte -> LoadByteOperation.asSuccess()
            Funct3LoadHalfWord -> LoadHalfWordOperation.asSuccess()
            Funct3LoadWord -> LoadWordOperation.asSuccess()
            Funct3LoadByteUnsigned -> LoadByteUnsignedOperation.asSuccess()
            Funct3LoadHalfWordUnsigned -> LoadHalfWordUnsignedOperation.asSuccess()
            else -> DecoderUnknownFunct3(OpcodeLoad, iTypeFields.funct3).asFailure()
        }

    private fun decodeStore(sTypeFields: STypeFields) =
        decodeStoreOperation(sTypeFields)
            .map { operation ->
                DecodedStoreInstruction(
                    operation,
                    sTypeFields.baseRegisterAddress,
                    sTypeFields.valueRegisterAddress,
                    sTypeFields.immediate
                )
            }

    private fun decodeStoreOperation(sTypeFields: STypeFields) =
        when (sTypeFields.funct3) {
            Funct3StoreByte -> StoreByteOperation.asSuccess()
            Funct3StoreHalfWord -> StoreHalfWordOperation.asSuccess()
            Funct3StoreWord -> StoreWordOperation.asSuccess()
            else -> DecoderUnknownFunct3(OpcodeStore, sTypeFields.funct3).asFailure()
        }

    private fun opcodeOf(instruction: Word) =
        instruction.bits(6, 0)

    private fun rTypeFieldsOf(instruction: Word) =
        RTypeFields(
            destinationRegisterAddressOf(instruction),
            firstSourceRegisterAddressOf(instruction),
            secondSourceRegisterAddressOf(instruction),
            funct3Of(instruction),
            funct7Of(instruction)
        )

    private fun iTypeFieldsOf(instruction: Word) =
        ITypeFields(
            destinationRegisterAddressOf(instruction),
            firstSourceRegisterAddressOf(instruction),
            funct3Of(instruction),
            immediateFieldOf(instruction),
            immediateIOf(instruction)
        )

    private fun sTypeFieldsOf(instruction: Word) =
        STypeFields(
            firstSourceRegisterAddressOf(instruction),
            secondSourceRegisterAddressOf(instruction),
            funct3Of(instruction),
            immediateSOf(instruction)
        )

    private fun bTypeFieldsOf(instruction: Word) =
        BTypeFields(
            firstSourceRegisterAddressOf(instruction),
            secondSourceRegisterAddressOf(instruction),
            funct3Of(instruction),
            immediateBOf(instruction)
        )

    private fun uTypeFieldsOf(instruction: Word) =
        UTypeFields(
            destinationRegisterAddressOf(instruction),
            immediateUOf(instruction)
        )

    private fun jTypeFieldsOf(instruction: Word) =
        JTypeFields(
            destinationRegisterAddressOf(instruction),
            immediateJOf(instruction)
        )

    private fun funct3Of(instruction: Word) =
        instruction.bits(14, 12)

    private fun funct7Of(instruction: Word) =
        instruction.bits(31, 25)

    private fun immediateFieldOf(instruction: Word) =
        instruction.bits(31, 20)

    private fun destinationRegisterAddressOf(instruction: Word) =
        RegisterAddress(instruction.bits(11, 7))

    private fun firstSourceRegisterAddressOf(instruction: Word) =
        RegisterAddress(instruction.bits(19, 15))

    private fun secondSourceRegisterAddressOf(instruction: Word) =
        RegisterAddress(instruction.bits(24, 20))

    private fun immediateIOf(instruction: Word) =
        signedWord(instruction.bits(31, 20), 12)

    private fun immediateSOf(instruction: Word) =
        signedWord(
            bitwiseOr(
                instruction.bits(11, 7),
                instruction.bits(31, 25) shl 5
            ),
            12
        )

    private fun immediateBOf(instruction: Word) =
        signedWord(
            bitwiseOr(
                instruction.bits(11, 8) shl 1,
                instruction.bits(30, 25) shl 5,
                instruction.bit(7) shl 11,
                instruction.bit(31) shl 12
            ),
            13
        )

    private fun immediateUOf(instruction: Word) =
        Word(instruction.value and UpperImmediateMask)

    private fun immediateJOf(instruction: Word) =
        signedWord(
            bitwiseOr(
                instruction.bits(30, 21) shl 1,
                instruction.bit(20) shl 11,
                instruction.bits(19, 12) shl 12,
                instruction.bit(31) shl 20
            ),
            21
        )

    private fun signedWord(rawValue: Int, bitWidth: Int) =
        Word(signExtend(rawValue, bitWidth).toUInt())

    private fun signExtend(value: Int, bitWidth: Int) =
        (value shl (32 - bitWidth)) shr (32 - bitWidth)

    private fun bitwiseOr(vararg values: Int) =
        values.fold(0) { accumulatedValue, value -> accumulatedValue or value }

    private fun Word.bit(bitIndex: Int) =
        ((value shr bitIndex) and OneBitMask).toInt()

    private fun Word.bits(mostSignificantBitIndex: Int, leastSignificantBitIndex: Int) =
        ((value shr leastSignificantBitIndex) and maskForBitRange(mostSignificantBitIndex, leastSignificantBitIndex)).toInt()

    private fun maskForBitRange(mostSignificantBitIndex: Int, leastSignificantBitIndex: Int): UInt {
        val width = mostSignificantBitIndex - leastSignificantBitIndex + 1
        return UInt.MAX_VALUE shr (UInt.SIZE_BITS - width)
    }

    private data class ArithmeticImmediateOperationAndImmediate(
        val operation: ArithmeticImmediateOperation,
        val immediate: Word
    )

    private data class ShiftImmediateFields(
        val shiftAmount: Word,
        val functionCode7: Int
    )

    private data class RTypeFields(
        val destinationRegisterAddress: RegisterAddress,
        val leftSourceRegisterAddress: RegisterAddress,
        val rightSourceRegisterAddress: RegisterAddress,
        val funct3: Int,
        val funct7: Int
    )

    private data class ITypeFields(
        val destinationRegisterAddress: RegisterAddress,
        val sourceRegisterAddress: RegisterAddress,
        val funct3: Int,
        val immediateField: Int,
        val immediate: Word
    )

    private data class STypeFields(
        val baseRegisterAddress: RegisterAddress,
        val valueRegisterAddress: RegisterAddress,
        val funct3: Int,
        val immediate: Word
    )

    private data class BTypeFields(
        val leftSourceRegisterAddress: RegisterAddress,
        val rightSourceRegisterAddress: RegisterAddress,
        val funct3: Int,
        val immediate: Word
    )

    private data class UTypeFields(
        val destinationRegisterAddress: RegisterAddress,
        val immediate: Word
    )

    private data class JTypeFields(
        val destinationRegisterAddress: RegisterAddress,
        val immediate: Word
    )
}

private const val OpcodeOperationImmediate = 0x13
private const val OpcodeLoadUpperImmediate = 0x37
private const val OpcodeAddUpperImmediateToProgramCounter = 0x17
private const val OpcodeOperation = 0x33
private const val OpcodeJumpAndLink = 0x6F
private const val OpcodeJumpAndLinkRegister = 0x67
private const val OpcodeBranch = 0x63
private const val OpcodeLoad = 0x03
private const val OpcodeStore = 0x23
private const val OpcodeMiscMemory = 0x0F
private const val OpcodeSystem = 0x73

private const val Funct3AddImmediate = 0x0
private const val Funct3SetLessThanImmediateSigned = 0x2
private const val Funct3SetLessThanImmediateUnsigned = 0x3
private const val Funct3ExclusiveOrImmediate = 0x4
private const val Funct3OrImmediate = 0x6
private const val Funct3AndImmediate = 0x7
private const val Funct3ShiftLeftLogicalImmediate = 0x1
private const val Funct3ShiftRightLogicalOrArithmeticImmediate = 0x5

private const val Funct3AddOrSubtract = 0x0
private const val Funct3ShiftLeftLogical = 0x1
private const val Funct3SetLessThanSigned = 0x2
private const val Funct3SetLessThanUnsigned = 0x3
private const val Funct3ExclusiveOr = 0x4
private const val Funct3ShiftRightLogicalOrArithmetic = 0x5
private const val Funct3Or = 0x6
private const val Funct3And = 0x7

private const val Funct3BranchEqual = 0x0
private const val Funct3BranchNotEqual = 0x1
private const val Funct3BranchLessThanSigned = 0x4
private const val Funct3BranchGreaterThanOrEqualSigned = 0x5
private const val Funct3BranchLessThanUnsigned = 0x6
private const val Funct3BranchGreaterThanOrEqualUnsigned = 0x7
private const val Funct3JumpAndLinkRegister = 0x0

private const val Funct3LoadByte = 0x0
private const val Funct3LoadHalfWord = 0x1
private const val Funct3LoadWord = 0x2
private const val Funct3LoadByteUnsigned = 0x4
private const val Funct3LoadHalfWordUnsigned = 0x5

private const val Funct3StoreByte = 0x0
private const val Funct3StoreHalfWord = 0x1
private const val Funct3StoreWord = 0x2

private const val Funct7Mask = 0x7F
private const val Funct7Add = 0x00
private const val Funct7Subtract = 0x20
private const val Funct7ShiftLeftLogical = 0x00
private const val Funct7ShiftRightLogical = 0x00
private const val Funct7ShiftRightArithmetic = 0x20

private const val ShiftImmediateFunctionCodeOffset = 5
private const val ShiftAmountMask = 0x1F
private const val OneBitMask = 0x1u
private const val UpperImmediateMask = 0xFFFFF000u
