package types

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class DataUnitTest {

    @Nested
    inner class ByteTest {

        @Test
        fun `can convert a byte to a half word`() {
            val byte = Byte(0x12u)
            expectThat(byte.toHalfWord()).isEqualTo(HalfWord(0x12u))
        }

        @Test
        fun `can convert a byte with the most significant bit set to a half word`() {
            val byte = Byte(0xFFu)
            expectThat(byte.toHalfWord()).isEqualTo(HalfWord(0xFFu))
        }
    }

    @Nested
    inner class HalfWordTest {

        @Test
        fun `can truncate a half word to a byte`() {
            val halfWord = HalfWord(0x1234u)
            expectThat(halfWord.toByte()).isEqualTo(Byte(0x34u))
        }

        @Test
        fun `can convert a half word to a word`() {
            val halfWord = HalfWord(0x1234u)
            expectThat(halfWord.toWord()).isEqualTo(Word(0x1234u))
        }

        @Test
        fun `can convert a half word with the most significant bit set to a word`() {
            val halfWord = HalfWord(0xFFFFu)
            expectThat(halfWord.toWord()).isEqualTo(Word(0xFFFFu))
        }

        @Test
        fun `can shift a half word left`() {
            val halfWord = HalfWord(0x1234u)
            expectThat(halfWord shl 8).isEqualTo(HalfWord(0x3400u))
        }

        @Test
        fun `can shift a half word right`() {
            val halfWord = HalfWord(0x1234u)
            expectThat(halfWord shr 8).isEqualTo(HalfWord(0x0012u))
        }

        @Test
        fun `shifting a half word right performs a logical shift rather than an arithmetic shift`() {
            val halfWord = HalfWord(0x8000u)
            expectThat(halfWord shr 1).isEqualTo(HalfWord(0x4000u))
        }
    }

    @Nested
    inner class WordTest {

        @Test
        fun `can truncate a word to a half word`() {
            val word = Word(0x12345678u)
            expectThat(word.toHalfWord()).isEqualTo(HalfWord(0x5678u))
        }

        @Test
        fun `can shift a word left`() {
            val word = Word(0x12345678u)
            expectThat(word shl 16).isEqualTo(Word(0x56780000u))
        }

        @Test
        fun `can shift a word right`() {
            val word = Word(0x12345678u)
            expectThat(word shr 16).isEqualTo(Word(0x00001234u))
        }

        @Test
        fun `shifting a word right performs a logical shift rather than an arithmetic shift`() {
            val word = Word(0x80000000u)
            expectThat(word shr 1).isEqualTo(Word(0x40000000u))
        }
    }
}
