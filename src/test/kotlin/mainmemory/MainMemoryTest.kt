package mainmemory

import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

class MainMemoryTest {
    private val memory = MainMemory(8)

    @Test
    fun `memory is initially zeroed`() {
        expectThat(memory.loadByte(0)).isEqualTo(0)
    }

    @Test
    fun `can store and load bytes`() {
        memory.storeByte(0, 0x12)
        memory.storeByte(1, 0x34)
        expectThat(memory.loadByte(0)).isEqualTo(0x12)
        expectThat(memory.loadByte(1)).isEqualTo(0x34)
    }

    @Test
    fun `can store and load half words`() {
        memory.storeHalfWord(0, 0x1234)
        memory.storeHalfWord(2, 0x5678)
        expectThat(memory.loadHalfWord(0)).isEqualTo(0x1234)
        expectThat(memory.loadHalfWord(2)).isEqualTo(0x5678)
    }

    @Test
    fun `can store and load words`() {
        memory.storeWord(0, 0x12345678)
        memory.storeWord(4, 0x09abcdef)
        expectThat(memory.loadWord(0)).isEqualTo(0x12345678)
        expectThat(memory.loadWord(4)).isEqualTo(0x09abcdef)
    }

    @Test
    fun `memory is stored in little-endian format`() {
        memory.storeWord(0, 0x12345678)
        expectThat(memory.loadByte(0)).isEqualTo(0x78)
        expectThat(memory.loadByte(1)).isEqualTo(0x56)
        expectThat(memory.loadByte(2)).isEqualTo(0x34)
        expectThat(memory.loadByte(3)).isEqualTo(0x12)
    }

    @Test
    fun `throws exception when storing a byte out of bounds`() {
        expectCatching { memory.storeByte(8, 0x12) }
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
        expectCatching { memory.storeHalfWord(7, 0x1234) }
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
        expectCatching { memory.storeWord(5, 0x12345678) }
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
