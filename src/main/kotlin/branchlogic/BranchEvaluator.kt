package branchlogic

import types.Word

sealed interface BranchWriteBack
data object NoBranchWriteBack : BranchWriteBack
data class BranchLinkWriteBack(val value: Word) : BranchWriteBack

data class BranchEvaluation(
    val actualNextInstructionAddress: Word,
    val writeBack: BranchWriteBack,
)

interface BranchEvaluator {
    fun evaluate(
        operation: BranchOperation,
        leftOperand: Word,
        rightOperand: Word,
        branchOffset: Word,
        instructionAddress: Word,
    ): BranchEvaluation
}

data object RealBranchEvaluator : BranchEvaluator {

    override fun evaluate(
        operation: BranchOperation,
        leftOperand: Word,
        rightOperand: Word,
        branchOffset: Word,
        instructionAddress: Word,
    ) =
        when (operation) {
            JumpAndLink ->
                BranchEvaluation(
                    actualNextInstructionAddress = branchOffset + instructionAddress,
                    writeBack = BranchLinkWriteBack(instructionAddress.nextSequential()),
                )

            JumpAndLinkRegister ->
                BranchEvaluation(
                    actualNextInstructionAddress = (leftOperand + branchOffset).withLeastSignificantBitCleared(),
                    writeBack = BranchLinkWriteBack(instructionAddress.nextSequential()),
                )

            BranchEqual ->
                BranchEvaluation(
                    actualNextInstructionAddress = when (leftOperand == rightOperand) {
                        true -> branchOffset + instructionAddress
                        false -> instructionAddress.nextSequential()
                    },
                    writeBack = NoBranchWriteBack,
                )

            BranchNotEqual ->
                BranchEvaluation(
                    actualNextInstructionAddress = when (leftOperand != rightOperand) {
                        true -> branchOffset + instructionAddress
                        false -> instructionAddress.nextSequential()
                    },
                    writeBack = NoBranchWriteBack,
                )

            BranchLessThanSigned ->
                BranchEvaluation(
                    actualNextInstructionAddress = when (leftOperand.value.toInt() < rightOperand.value.toInt()) {
                        true -> branchOffset + instructionAddress
                        false -> instructionAddress.nextSequential()
                    },
                    writeBack = NoBranchWriteBack,
                )

            BranchLessThanUnsigned ->
                BranchEvaluation(
                    actualNextInstructionAddress = when (leftOperand.value < rightOperand.value) {
                        true -> branchOffset + instructionAddress
                        false -> instructionAddress.nextSequential()
                    },
                    writeBack = NoBranchWriteBack,
                )

            BranchGreaterThanOrEqualSigned ->
                BranchEvaluation(
                    actualNextInstructionAddress = when (leftOperand.value.toInt() >= rightOperand.value.toInt()) {
                        true -> branchOffset + instructionAddress
                        false -> instructionAddress.nextSequential()
                    },
                    writeBack = NoBranchWriteBack,
                )

            BranchGreaterThanOrEqualUnsigned ->
                BranchEvaluation(
                    actualNextInstructionAddress = when (leftOperand.value >= rightOperand.value) {
                        true -> branchOffset + instructionAddress
                        false -> instructionAddress.nextSequential()
                    },
                    writeBack = NoBranchWriteBack,
                )
        }

    private operator fun Word.plus(other: Word) = Word(value + other.value)

    private fun Word.nextSequential() = Word(value + 4u)

    private fun Word.withLeastSignificantBitCleared() = Word(value and 0xfffffffeu)
}
