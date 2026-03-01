package arithmeticlogic

import types.Word

interface ArithmeticLogicUnit {
    fun evaluate(
        operation: ArithmeticLogicOperation,
        leftOperand: Word,
        rightOperand: Word,
        instructionAddress: Int
    ): Word
}

data object RealArithmeticLogicUnit : ArithmeticLogicUnit {

    override fun evaluate(
        operation: ArithmeticLogicOperation,
        leftOperand: Word,
        rightOperand: Word,
        instructionAddress: Int
    ) =
        when (operation) {
            Add,
            AddImmediate -> Word(leftOperand.value + rightOperand.value)

            LoadUpperImmediate ->
                leftOperand

            AddUpperImmediateToProgramCounter ->
                Word(leftOperand.value + instructionAddress.toUInt())

            Subtract ->
                Word(leftOperand.value - rightOperand.value)

            ShiftLeftLogical,
            ShiftLeftLogicalImmediate ->
                Word(leftOperand.value shl rightOperand.value.toInt())

            SetLessThanSigned,
            SetLessThanImmediateSigned ->
                (leftOperand.value.toInt() < rightOperand.value.toInt()).asWord()

            SetLessThanUnsigned,
            SetLessThanImmediateUnsigned ->
                (leftOperand.value < rightOperand.value).asWord()

            ExclusiveOr,
            ExclusiveOrImmediate ->
                Word(leftOperand.value xor rightOperand.value)

            ShiftRightLogical,
            ShiftRightLogicalImmediate ->
                Word(leftOperand.value shr rightOperand.value.toInt())

            ShiftRightArithmetic,
            ShiftRightArithmeticImmediate ->
                Word((leftOperand.value.toInt() shr rightOperand.value.toInt()).toUInt())

            Or,
            OrImmediate ->
                Word(leftOperand.value or rightOperand.value)

            And,
            AndImmediate -> Word(leftOperand.value and rightOperand.value)
        }

    private fun Boolean.asWord() =
        when (this) {
            true -> Word(1u)
            false -> Word(0u)
        }
}
