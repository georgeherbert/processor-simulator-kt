package types

sealed interface Operand

data class ReadyOperand(val value: Word) : Operand

data class PendingOperand(val robId: RobId) : Operand
