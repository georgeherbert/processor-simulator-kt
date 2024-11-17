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
            val byte: Byte = 0x12
            expectThat(byte.toHalfWord()).isEqualTo(0x12)
        }

        @Test
        fun `can convert a byte with the most significant bit set to a half word`() {
            val byte: Byte = 0xFF.toByte()
            expectThat(byte.toHalfWord()).isEqualTo(0xFF)
        }
    }

    @Nested
    inner class HalfWordTest {
        @Test
        fun `can convert a half word to a word`() {
            val halfWord: HalfWord = 0x1234
            expectThat(halfWord.toWord()).isEqualTo(0x1234)
        }

        @Test
        fun `can convert a half word with the most significant bit set to a word`() {
            val halfWord: HalfWord = 0xFFFF.toShort()
            expectThat(halfWord.toWord()).isEqualTo(0xFFFF)
        }

        @Test
        fun `can shift a half word left`() {
            val halfWord: HalfWord = 0x1234
            expectThat(halfWord shl 8).isEqualTo(0x3400)
        }

        @Test
        fun `can shift a half word right`() {
            val halfWord: HalfWord = 0x1234
            expectThat(halfWord shr 8).isEqualTo(0x0012)
        }
    }

    @Nested
    inner class WordTest {
        @Test
        fun `can convert a word to a half word`() {
            val word: Word = 0x12345678
            expectThat(word.toHalfWord()).isEqualTo(0x5678)
        }
    }
}
