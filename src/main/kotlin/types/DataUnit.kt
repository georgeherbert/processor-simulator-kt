package types

@JvmInline
value class Byte(val value: UByte)

@JvmInline
value class HalfWord(val value: UShort)

@JvmInline
value class Word(val value: UInt)

fun Byte.toHalfWord() = HalfWord(value.toUShort())
fun HalfWord.toByte() = Byte(value.toUByte())
fun HalfWord.toWord() = Word(value.toUInt())
fun Word.toHalfWord() = HalfWord(value.toUShort())

infix fun HalfWord.shl(bits: Int) = (toWord() shl bits).toHalfWord()
infix fun HalfWord.shr(bits: Int) = (toWord() shr bits).toHalfWord()
infix fun Word.shl(bits: Int) = Word(value shl bits)
infix fun Word.shr(bits: Int) = Word(value shr bits)

infix fun HalfWord.or(other: HalfWord) = HalfWord(value or other.value)
infix fun Word.or(other: Word) = Word(value or other.value)
