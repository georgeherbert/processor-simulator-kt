package decoder

import types.RegisterAddress
import types.Word

sealed interface DecodedInstruction

data class DecodedArithmeticImmediateInstruction(
    val operation: ArithmeticImmediateOperation,
    val destinationRegisterAddress: RegisterAddress,
    val sourceRegisterAddress: RegisterAddress,
    val immediate: Word
) : DecodedInstruction

data class DecodedArithmeticRegisterInstruction(
    val operation: ArithmeticRegisterOperation,
    val destinationRegisterAddress: RegisterAddress,
    val leftSourceRegisterAddress: RegisterAddress,
    val rightSourceRegisterAddress: RegisterAddress
) : DecodedInstruction

data class DecodedLoadUpperImmediateInstruction(
    val destinationRegisterAddress: RegisterAddress,
    val immediate: Word
) : DecodedInstruction

data class DecodedAddUpperImmediateToProgramCounterInstruction(
    val destinationRegisterAddress: RegisterAddress,
    val immediate: Word,
    val instructionAddress: Word
) : DecodedInstruction

data class DecodedJumpAndLinkInstruction(
    val destinationRegisterAddress: RegisterAddress,
    val immediate: Word,
    val instructionAddress: Word,
    val predictedNextInstructionAddress: Word
) : DecodedInstruction

data class DecodedJumpAndLinkRegisterInstruction(
    val destinationRegisterAddress: RegisterAddress,
    val sourceRegisterAddress: RegisterAddress,
    val immediate: Word,
    val instructionAddress: Word,
    val predictedNextInstructionAddress: Word
) : DecodedInstruction

data class DecodedBranchInstruction(
    val operation: BranchOperation,
    val leftSourceRegisterAddress: RegisterAddress,
    val rightSourceRegisterAddress: RegisterAddress,
    val immediate: Word,
    val instructionAddress: Word,
    val predictedNextInstructionAddress: Word
) : DecodedInstruction

data class DecodedLoadInstruction(
    val operation: LoadOperation,
    val destinationRegisterAddress: RegisterAddress,
    val baseRegisterAddress: RegisterAddress,
    val immediate: Word
) : DecodedInstruction

data class DecodedStoreInstruction(
    val operation: StoreOperation,
    val baseRegisterAddress: RegisterAddress,
    val valueRegisterAddress: RegisterAddress,
    val immediate: Word
) : DecodedInstruction

sealed interface ArithmeticImmediateOperation
data object AddImmediateOperation : ArithmeticImmediateOperation
data object SetLessThanImmediateSignedOperation : ArithmeticImmediateOperation
data object SetLessThanImmediateUnsignedOperation : ArithmeticImmediateOperation
data object ExclusiveOrImmediateOperation : ArithmeticImmediateOperation
data object OrImmediateOperation : ArithmeticImmediateOperation
data object AndImmediateOperation : ArithmeticImmediateOperation
data object ShiftLeftLogicalImmediateOperation : ArithmeticImmediateOperation
data object ShiftRightLogicalImmediateOperation : ArithmeticImmediateOperation
data object ShiftRightArithmeticImmediateOperation : ArithmeticImmediateOperation

sealed interface ArithmeticRegisterOperation
data object AddOperation : ArithmeticRegisterOperation
data object SubtractOperation : ArithmeticRegisterOperation
data object ShiftLeftLogicalOperation : ArithmeticRegisterOperation
data object SetLessThanSignedOperation : ArithmeticRegisterOperation
data object SetLessThanUnsignedOperation : ArithmeticRegisterOperation
data object ExclusiveOrOperation : ArithmeticRegisterOperation
data object ShiftRightLogicalOperation : ArithmeticRegisterOperation
data object ShiftRightArithmeticOperation : ArithmeticRegisterOperation
data object OrOperation : ArithmeticRegisterOperation
data object AndOperation : ArithmeticRegisterOperation

sealed interface BranchOperation
data object BranchEqualOperation : BranchOperation
data object BranchNotEqualOperation : BranchOperation
data object BranchLessThanSignedOperation : BranchOperation
data object BranchLessThanUnsignedOperation : BranchOperation
data object BranchGreaterThanOrEqualSignedOperation : BranchOperation
data object BranchGreaterThanOrEqualUnsignedOperation : BranchOperation

sealed interface LoadOperation
data object LoadByteOperation : LoadOperation
data object LoadByteUnsignedOperation : LoadOperation
data object LoadHalfWordOperation : LoadOperation
data object LoadHalfWordUnsignedOperation : LoadOperation
data object LoadWordOperation : LoadOperation

sealed interface StoreOperation
data object StoreByteOperation : StoreOperation
data object StoreHalfWordOperation : StoreOperation
data object StoreWordOperation : StoreOperation
