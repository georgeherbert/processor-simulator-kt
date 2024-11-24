package types

private const val INSTRUCTION_SIZE_BYTES = 4

@JvmInline
value class InstructionAddress(val value: Int)

val InstructionAddress.next get() = InstructionAddress(value + INSTRUCTION_SIZE_BYTES)
