package types

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

class DataUnitTest {

    @Nested
    inner class ByteTest {

        @Test
        fun `can convert a byte to a half word`() {
            val byte = Byte(0x12)
            expectThat(byte.toHalfWord()).isEqualTo(HalfWord(0x12))
        }

        @Test
        fun `can convert a byte with the most significant bit set to a half word`() {
            val byte = Byte(0xFF)
            expectThat(byte.toHalfWord()).isEqualTo(HalfWord(0xFF))
        }

        @Test
        fun `bytes can be zero or positive`() {
            Byte(0)
            Byte(0xFF)
        }

        @Test
        fun `bytes cannot be negative`() {
            expectCatching { Byte(-1) }
                .isFailure()
                .isA<IllegalArgumentException>()
                .get { message }.isEqualTo("Byte out of range: -1")

            expectCatching { Byte(0x100) }
                .isFailure()
                .isA<IllegalArgumentException>()
                .get { message }.isEqualTo("Byte out of range: 256")
        }
    }

    @Nested
    inner class HalfWordTest {

        @Test
        fun `can convert a half word to a word`() {
            val halfWord = HalfWord(0x1234)
            expectThat(halfWord.toWord()).isEqualTo(Word(0x1234))
        }

        @Test
        fun `can convert a half word with the most significant bit set to a word`() {
            val halfWord = HalfWord(0xFFFF)
            expectThat(halfWord.toWord()).isEqualTo(Word(0xFFFF))
        }

        @Test
        fun `can shift a half word left`() {
            val halfWord = HalfWord(0x1234)
            expectThat(halfWord shl 8).isEqualTo(HalfWord(0x3400))
        }

        @Test
        fun `can shift a half word right`() {
            val halfWord = HalfWord(0x1234)
            expectThat(halfWord shr 8).isEqualTo(HalfWord(0x0012))
        }

        @Test
        fun `can shift a half word right without preserving the sign bit`() {
            val halfWord = HalfWord(0x8000)
            expectThat(halfWord shr 1).isEqualTo(HalfWord(0x4000))
        }

        @Test
        fun `half words can be zero or positive`() {
            HalfWord(0)
            HalfWord(0xFFFF)
        }

        @Test
        fun `half words cannot be negative`() {
            expectCatching { HalfWord(-1) }
                .isFailure()
                .isA<IllegalArgumentException>()
                .get { message }.isEqualTo("HalfWord out of range: -1")

            expectCatching { HalfWord(0x10000) }
                .isFailure()
                .isA<IllegalArgumentException>()
                .get { message }.isEqualTo("HalfWord out of range: 65536")
        }
    }

    @Nested
    inner class WordTest {

        @Test
        fun `can truncate a word to a half word`() {
            val word = Word(0x12345678)
            expectThat(word.toHalfWord()).isEqualTo(HalfWord(0x5678))
        }
    }

}