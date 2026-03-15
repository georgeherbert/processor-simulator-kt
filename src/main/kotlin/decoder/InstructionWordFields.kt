package decoder

import types.RegisterAddress
import types.Word

internal fun opcodeOf(instruction: Word) =
    instruction.bits(6, 0)

internal fun rTypeFieldsOf(instruction: Word) =
    RTypeFields(
        destinationRegisterAddressOf(instruction),
        firstSourceRegisterAddressOf(instruction),
        secondSourceRegisterAddressOf(instruction),
        funct3Of(instruction),
        funct7Of(instruction)
    )

internal fun iTypeFieldsOf(instruction: Word) =
    ITypeFields(
        destinationRegisterAddressOf(instruction),
        firstSourceRegisterAddressOf(instruction),
        funct3Of(instruction),
        immediateFieldOf(instruction),
        immediateIOf(instruction)
    )

internal fun sTypeFieldsOf(instruction: Word) =
    STypeFields(
        firstSourceRegisterAddressOf(instruction),
        secondSourceRegisterAddressOf(instruction),
        funct3Of(instruction),
        immediateSOf(instruction)
    )

internal fun bTypeFieldsOf(instruction: Word) =
    BTypeFields(
        firstSourceRegisterAddressOf(instruction),
        secondSourceRegisterAddressOf(instruction),
        funct3Of(instruction),
        immediateBOf(instruction)
    )

internal fun uTypeFieldsOf(instruction: Word) =
    UTypeFields(
        destinationRegisterAddressOf(instruction),
        immediateUOf(instruction)
    )

internal fun jTypeFieldsOf(instruction: Word) =
    JTypeFields(
        destinationRegisterAddressOf(instruction),
        immediateJOf(instruction)
    )

internal fun shiftImmediateFieldsOf(iTypeFields: ITypeFields) =
    ShiftImmediateFields(
        Word((iTypeFields.immediateField and ShiftAmountMask).toUInt()),
        (iTypeFields.immediateField shr ShiftImmediateFunctionCodeOffset) and Funct7Mask
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

internal data class ArithmeticImmediateOperationAndImmediate(
    val operation: ArithmeticImmediateOperation,
    val immediate: Word
)

internal data class ShiftImmediateFields(
    val shiftAmount: Word,
    val functionCode7: Int
)

internal data class RTypeFields(
    val destinationRegisterAddress: RegisterAddress,
    val leftSourceRegisterAddress: RegisterAddress,
    val rightSourceRegisterAddress: RegisterAddress,
    val funct3: Int,
    val funct7: Int
)

internal data class ITypeFields(
    val destinationRegisterAddress: RegisterAddress,
    val sourceRegisterAddress: RegisterAddress,
    val funct3: Int,
    val immediateField: Int,
    val immediate: Word
)

internal data class STypeFields(
    val baseRegisterAddress: RegisterAddress,
    val valueRegisterAddress: RegisterAddress,
    val funct3: Int,
    val immediate: Word
)

internal data class BTypeFields(
    val leftSourceRegisterAddress: RegisterAddress,
    val rightSourceRegisterAddress: RegisterAddress,
    val funct3: Int,
    val immediate: Word
)

internal data class UTypeFields(
    val destinationRegisterAddress: RegisterAddress,
    val immediate: Word
)

internal data class JTypeFields(
    val destinationRegisterAddress: RegisterAddress,
    val immediate: Word
)

internal const val OpcodeOperationImmediate = 0x13
internal const val OpcodeLoadUpperImmediate = 0x37
internal const val OpcodeAddUpperImmediateToProgramCounter = 0x17
internal const val OpcodeOperation = 0x33
internal const val OpcodeJumpAndLink = 0x6F
internal const val OpcodeJumpAndLinkRegister = 0x67
internal const val OpcodeBranch = 0x63
internal const val OpcodeLoad = 0x03
internal const val OpcodeStore = 0x23
internal const val OpcodeMiscMemory = 0x0F
internal const val OpcodeSystem = 0x73

internal const val Funct3AddImmediate = 0x0
internal const val Funct3SetLessThanImmediateSigned = 0x2
internal const val Funct3SetLessThanImmediateUnsigned = 0x3
internal const val Funct3ExclusiveOrImmediate = 0x4
internal const val Funct3OrImmediate = 0x6
internal const val Funct3AndImmediate = 0x7
internal const val Funct3ShiftLeftLogicalImmediate = 0x1
internal const val Funct3ShiftRightLogicalOrArithmeticImmediate = 0x5

internal const val Funct3AddOrSubtract = 0x0
internal const val Funct3ShiftLeftLogical = 0x1
internal const val Funct3SetLessThanSigned = 0x2
internal const val Funct3SetLessThanUnsigned = 0x3
internal const val Funct3ExclusiveOr = 0x4
internal const val Funct3ShiftRightLogicalOrArithmetic = 0x5
internal const val Funct3Or = 0x6
internal const val Funct3And = 0x7

internal const val Funct3BranchEqual = 0x0
internal const val Funct3BranchNotEqual = 0x1
internal const val Funct3BranchLessThanSigned = 0x4
internal const val Funct3BranchGreaterThanOrEqualSigned = 0x5
internal const val Funct3BranchLessThanUnsigned = 0x6
internal const val Funct3BranchGreaterThanOrEqualUnsigned = 0x7
internal const val Funct3JumpAndLinkRegister = 0x0

internal const val Funct3LoadByte = 0x0
internal const val Funct3LoadHalfWord = 0x1
internal const val Funct3LoadWord = 0x2
internal const val Funct3LoadByteUnsigned = 0x4
internal const val Funct3LoadHalfWordUnsigned = 0x5

internal const val Funct3StoreByte = 0x0
internal const val Funct3StoreHalfWord = 0x1
internal const val Funct3StoreWord = 0x2

internal const val Funct7Add = 0x00
internal const val Funct7Subtract = 0x20
internal const val Funct7ShiftLeftLogical = 0x00
internal const val Funct7ShiftRightLogical = 0x00
internal const val Funct7ShiftRightArithmetic = 0x20

internal const val ShiftAmountMask = 0x1f
internal const val ShiftImmediateFunctionCodeOffset = 5
internal const val Funct7Mask = 0x7f
internal const val OneBitMask = 0x1u
internal const val UpperImmediateMask = 0xfffff000u
