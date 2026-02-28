package mainmemory

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.Byte
import types.HalfWord
import types.MainMemoryAddressOutOfBounds
import types.Size
import types.Word

class MainMemoryTest {

    private val size = Size(8)
    private val memory = MainMemory(size)

    @Test
    fun `is initially zeroed`() {
        expectThat(memory.loadByte(0))
            .isSuccess()
            .isEqualTo(Byte(0u))
    }

    @Test
    fun `is immutable`() {
        expectThat(memory.storeByte(0, Byte(0x12u)))
            .isSuccess()

        expectThat(memory.loadByte(0))
            .isSuccess()
            .isEqualTo(Byte(0u))
    }

    @Test
    fun `can store and load bytes`() {
        val firstStoreResult = memory
            .storeByte(0, Byte(0x12u))

        val firstStoredMemory = expectThat(firstStoreResult)
            .isSuccess()
            .subject

        val secondStoreResult = firstStoredMemory
            .storeByte(1, Byte(0x34u))

        val secondStoredMemory = expectThat(secondStoreResult)
            .isSuccess()
            .subject

        expectThat(secondStoredMemory.loadByte(0))
            .isSuccess()
            .isEqualTo(Byte(0x12u))

        expectThat(secondStoredMemory.loadByte(1))
            .isSuccess()
            .isEqualTo(Byte(0x34u))
    }

    @Test
    fun `can store and load half words`() {
        val firstStoreResult = memory
            .storeHalfWord(0, HalfWord(0x1234u))

        val firstStoredMemory = expectThat(firstStoreResult)
            .isSuccess()
            .subject

        val secondStoreResult = firstStoredMemory
            .storeHalfWord(2, HalfWord(0x5678u))

        val secondStoredMemory = expectThat(secondStoreResult)
            .isSuccess()
            .subject

        expectThat(secondStoredMemory.loadHalfWord(0))
            .isSuccess()
            .isEqualTo(HalfWord(0x1234u))

        expectThat(secondStoredMemory.loadHalfWord(2))
            .isSuccess()
            .isEqualTo(HalfWord(0x5678u))
    }

    @Test
    fun `can store and load words`() {
        val firstStoreResult = memory
            .storeWord(0, Word(0x12345678u))

        val firstStoredMemory = expectThat(firstStoreResult)
            .isSuccess()
            .subject

        val secondStoreResult = firstStoredMemory
            .storeWord(4, Word(0x09abcdefu))

        val secondStoredMemory = expectThat(secondStoreResult)
            .isSuccess()
            .subject

        expectThat(secondStoredMemory.loadWord(0))
            .isSuccess()
            .isEqualTo(Word(0x12345678u))

        expectThat(secondStoredMemory.loadWord(4))
            .isSuccess()
            .isEqualTo(Word(0x09abcdefu))
    }

    @Test
    fun `memory is stored in little-endian format`() {
        val storeResult = memory.storeWord(0, Word(0x12345678u))
        val storedMemory = expectThat(storeResult)
            .isSuccess()
            .subject

        expectThat(storedMemory.loadByte(0))
            .isSuccess()
            .isEqualTo(Byte(0x78u))

        expectThat(storedMemory.loadByte(1))
            .isSuccess()
            .isEqualTo(Byte(0x56u))

        expectThat(storedMemory.loadByte(2))
            .isSuccess()
            .isEqualTo(Byte(0x34u))

        expectThat(storedMemory.loadByte(3))
            .isSuccess()
            .isEqualTo(Byte(0x12u))
    }

    @Test
    fun `returns out of bounds failure when storing a byte`() {
        expectThat(memory.storeByte(8, Byte(0x12u)))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(8))
    }

    @Test
    fun `returns out of bounds failure when loading a byte`() {
        expectThat(memory.loadByte(8))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(8))
    }

    @Test
    fun `returns out of bounds failure when storing a half word`() {
        expectThat(memory.storeHalfWord(7, HalfWord(0x1234u)))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(8))
    }

    @Test
    fun `returns out of bounds failure when loading a half word`() {
        expectThat(memory.loadHalfWord(7))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(8))
    }

    @Test
    fun `returns out of bounds failure when storing a word`() {
        expectThat(memory.storeWord(5, Word(0x12345678u)))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(8))
    }

    @Test
    fun `returns out of bounds failure when loading a word`() {
        expectThat(memory.loadWord(5))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(8))
    }

    @Test
    fun `returns out of bounds failure for negative byte address`() {
        expectThat(memory.loadByte(-1))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(-1))

        expectThat(memory.storeByte(-1, Byte(0x12u)))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(-1))
    }

    @Test
    fun `returns out of bounds failure for negative half word and word addresses`() {
        expectThat(memory.loadHalfWord(-1))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(-1))

        expectThat(memory.storeHalfWord(-1, HalfWord(0x1234u)))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(-1))

        expectThat(memory.loadWord(-1))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(-1))

        expectThat(memory.storeWord(-1, Word(0x12345678u)))
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(-1))
    }

    @Test
    fun `byte half word and word operations succeed at highest valid start addresses`() {
        val byteStoreResult = memory.storeByte(7, Byte(0xABu))
        val byteStoredMemory = expectThat(byteStoreResult)
            .isSuccess()
            .subject

        expectThat(byteStoredMemory.loadByte(7))
            .isSuccess()
            .isEqualTo(Byte(0xABu))

        val halfWordStoreResult = memory.storeHalfWord(6, HalfWord(0x1234u))
        val halfWordStoredMemory = expectThat(halfWordStoreResult)
            .isSuccess()
            .subject

        expectThat(halfWordStoredMemory.loadHalfWord(6))
            .isSuccess()
            .isEqualTo(HalfWord(0x1234u))

        val wordStoreResult = memory.storeWord(4, Word(0x12345678u))
        val wordStoredMemory = expectThat(wordStoreResult)
            .isSuccess()
            .subject

        expectThat(wordStoredMemory.loadWord(4))
            .isSuccess()
            .isEqualTo(Word(0x12345678u))
    }

    @Test
    fun `failed store does not mutate memory`() {
        val failedStoreResult = memory.storeByte(8, Byte(0xFFu))

        expectThat(failedStoreResult)
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(8))

        expectThat(memory.loadByte(0))
            .isSuccess()
            .isEqualTo(Byte(0u))
    }
}
