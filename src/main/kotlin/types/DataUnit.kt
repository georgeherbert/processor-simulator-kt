package types

@JvmInline
value class Byte(val value: Int)

@JvmInline
value class HalfWord(val value: Int)

@JvmInline
value class Word(val value: Int)

fun Byte.toHalfWord(): HalfWord = HalfWord(value and 0xFF)
fun HalfWord.toWord(): Word = Word(value and 0xFFFF)
fun Word.toHalfWord(): HalfWord = HalfWord(value and 0xFFFF)

infix fun HalfWord.shl(bits: Int): HalfWord = HalfWord((value shl bits) and 0xFFFF)
infix fun HalfWord.shr(bits: Int): HalfWord = HalfWord((value ushr bits) and 0xFFFF)
