package arithmeticlogic

sealed interface ArithmeticLogicOperation

data object Add : ArithmeticLogicOperation
data object AddImmediate : ArithmeticLogicOperation
data object LoadUpperImmediate : ArithmeticLogicOperation
data object AddUpperImmediateToProgramCounter : ArithmeticLogicOperation
data object Subtract : ArithmeticLogicOperation
data object ShiftLeftLogical : ArithmeticLogicOperation
data object ShiftLeftLogicalImmediate : ArithmeticLogicOperation
data object SetLessThanSigned : ArithmeticLogicOperation
data object SetLessThanImmediateSigned : ArithmeticLogicOperation
data object SetLessThanUnsigned : ArithmeticLogicOperation
data object SetLessThanImmediateUnsigned : ArithmeticLogicOperation
data object ExclusiveOr : ArithmeticLogicOperation
data object ExclusiveOrImmediate : ArithmeticLogicOperation
data object ShiftRightLogical : ArithmeticLogicOperation
data object ShiftRightLogicalImmediate : ArithmeticLogicOperation
data object ShiftRightArithmetic : ArithmeticLogicOperation
data object ShiftRightArithmeticImmediate : ArithmeticLogicOperation
data object Or : ArithmeticLogicOperation
data object OrImmediate : ArithmeticLogicOperation
data object And : ArithmeticLogicOperation
data object AndImmediate : ArithmeticLogicOperation
