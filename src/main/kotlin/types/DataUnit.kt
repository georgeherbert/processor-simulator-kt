package types

@JvmInline
value class Byte(val value: Int) {
    init {
        require(value in 0..0xFF) { "Byte out of range: $value" }
    }
}

@JvmInline
value class HalfWord(val value: Int) {
    init {
        require(value in 0..0xFFFF) { "HalfWord out of range: $value" }
    }
}

@JvmInline
value class Word(val value: Int)

fun Byte.toHalfWord(): HalfWord = HalfWord(value)
fun HalfWord.toWord(): Word = Word(value)
fun Word.toHalfWord(): HalfWord = HalfWord(value and 0xFFFF)

infix fun HalfWord.shl(bits: Int): HalfWord = HalfWord((value shl bits) and 0xFFFF)
infix fun HalfWord.shr(bits: Int): HalfWord = HalfWord((value ushr bits) and 0xFFFF)
