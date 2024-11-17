package types

@JvmInline
value class BitWidth(val value: Int)

val BitWidth.max get() = (1 shl value) - 1
