package branchlogic

import types.Word

data object StubBranchEvaluator : BranchEvaluator {
    override fun evaluate(
        operation: BranchOperation,
        leftOperand: Word,
        rightOperand: Word,
        branchOffset: Word,
        instructionAddress: Word
    ) =
        when (operation) {
            JumpAndLink,
            JumpAndLinkRegister ->
                BranchEvaluation(
                    Word(instructionAddress.value + branchOffset.value),
                    BranchLinkWriteBack(Word(99u))
                )

            BranchEqual,
            BranchNotEqual,
            BranchLessThanSigned,
            BranchLessThanUnsigned,
            BranchGreaterThanOrEqualSigned,
            BranchGreaterThanOrEqualUnsigned ->
                BranchEvaluation(
                    Word(instructionAddress.value + branchOffset.value),
                    NoBranchWriteBack
                )
        }
}
