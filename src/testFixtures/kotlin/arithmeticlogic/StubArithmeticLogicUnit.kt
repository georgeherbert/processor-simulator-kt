package arithmeticlogic

import types.Word

data object StubArithmeticLogicUnit : ArithmeticLogicUnit {
    override fun evaluate(
        operation: ArithmeticLogicOperation,
        leftOperand: Word,
        rightOperand: Word,
        instructionAddress: Int
    ) =
        Word(leftOperand.value + rightOperand.value + instructionAddress.toUInt())
}
