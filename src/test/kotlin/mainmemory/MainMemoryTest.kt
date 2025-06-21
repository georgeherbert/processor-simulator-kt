package mainmemory

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import types.Byte
import types.HalfWord
import types.Size
import types.Word

class MainMemoryTest {

    private val size = Size(8)
    private val memory = MainMemory(size)

    @Test
    fun `is initially zeroed`() {
        expectThat(memory.loadByte(0)).isEqualTo(Byte(0u))
    }

    @Test
    fun `is immutable`() {
        memory.storeByte(0, Byte(0x12u))
        expectThat(memory.loadByte(0)).isEqualTo(Byte(0u))
    }

    @Test
    fun `can store and load bytes`() {
        val newMemory = memory
            .storeByte(0, Byte(0x12u))
            .storeByte(1, Byte(0x34u))

        expectThat(newMemory.loadByte(0)).isEqualTo(Byte(0x12u))
        expectThat(newMemory.loadByte(1)).isEqualTo(Byte(0x34u))
    }

    @Test
    fun `can store and load half words`() {
        val newMemory = memory
            .storeHalfWord(0, HalfWord(0x1234u))
            .storeHalfWord(2, HalfWord(0x5678u))

        expectThat(newMemory.loadHalfWord(0)).isEqualTo(HalfWord(0x1234u))
        expectThat(newMemory.loadHalfWord(2)).isEqualTo(HalfWord(0x5678u))
    }

    @Test
    fun `can store and load words`() {
        val newMemory = memory
            .storeWord(0, Word(0x12345678u))
            .storeWord(4, Word(0x09abcdefu))
        expectThat(newMemory.loadWord(0)).isEqualTo(Word(0x12345678u))
        expectThat(newMemory.loadWord(4)).isEqualTo(Word(0x09abcdefu))
    }

    @Test
    fun `memory is stored in little-endian format`() {
        val newMemory = memory.storeWord(0, Word(0x12345678u))
        expectThat(newMemory.loadByte(0)).isEqualTo(Byte(0x78u))
        expectThat(newMemory.loadByte(1)).isEqualTo(Byte(0x56u))
        expectThat(newMemory.loadByte(2)).isEqualTo(Byte(0x34u))
        expectThat(newMemory.loadByte(3)).isEqualTo(Byte(0x12u))
    }

    @Test
    fun `throws exception when storing a byte out of bounds`() {
        expectCatching { memory.storeByte(8, Byte(0x12u)) }
            .isFailure()
            .isA<InvalidAddressException>()
    }

    @Test
    fun `throws exception when loading a byte out of bounds`() {
        expectCatching { memory.loadByte(8) }
            .isFailure()
            .isA<InvalidAddressException>()
    }

    @Test
    fun `throws exception when storing a half word out of bounds`() {
        expectCatching { memory.storeHalfWord(7, HalfWord(0x1234u)) }
            .isFailure()
            .isA<InvalidAddressException>()
    }

    @Test
    fun `throws exception when loading a half word out of bounds`() {
        expectCatching { memory.loadHalfWord(7) }
            .isFailure()
            .isA<InvalidAddressException>()
    }

    @Test
    fun `throws exception when storing a word out of bounds`() {
        expectCatching { memory.storeWord(5, Word(0x12345678u)) }
            .isFailure()
            .isA<InvalidAddressException>()
    }

    @Test
    fun `throws exception when loading a word out of bounds`() {
        expectCatching { memory.loadWord(5) }
            .isFailure()
            .isA<InvalidAddressException>()
    }
}
