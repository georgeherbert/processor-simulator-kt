package types

import kotlin.experimental.and

typealias HalfWord = Short
typealias Word = Int

fun Byte.toHalfWord(): HalfWord = toShort() and 0xFF
fun HalfWord.toWord(): Word = toInt() and 0xFFFF

fun Word.toHalfWord(): HalfWord = toShort()

infix fun HalfWord.shl(bits: Int): HalfWord = (toInt() shl bits).toShort()
infix fun HalfWord.shr(bits: Int): HalfWord = (toInt() shr bits).toShort()
