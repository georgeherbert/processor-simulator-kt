package types

private const val INSTRUCTION_SIZE_BYTES = 4

@JvmInline
value class InstructionAddress(val value: Int) {
    init {
        require(value >= 0) { "InstructionAddress must be non-negative" }
    }
}

val InstructionAddress.next get() = InstructionAddress(value + INSTRUCTION_SIZE_BYTES)
