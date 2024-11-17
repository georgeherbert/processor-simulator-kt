package mainmemory

import dataunit.HalfWord
import dataunit.Word
import dataunit.shl
import dataunit.shr
import dataunit.toHalfWord
import dataunit.toWord
import java.lang.Exception
import kotlin.experimental.or

class InvalidAddressException(address: Int) : Exception("Address $address is out of bounds")

class MainMemory(private val size: Int) {
    private val data = ByteArray(size)

    private fun checkAddress(address: Int) {
        if (address < 0 || address >= size) {
            throw InvalidAddressException(address)
        }
    }

    // TODO [GH] Will need to % here when we introduce speculative execution
    fun loadByte(address: Int): Byte {
        checkAddress(address)
        return data[address]
    }

    fun loadHalfWord(address: Int): HalfWord =
        loadByte(address).toHalfWord() or (loadByte(address + 1).toHalfWord() shl 8)

    fun loadWord(address: Int): Word = loadHalfWord(address).toWord() or (loadHalfWord(address + 2).toWord() shl 16)

    fun storeByte(address: Int, value: Byte) {
        checkAddress(address)
        data[address] = value
    }

    fun storeHalfWord(address: Int, value: HalfWord) {
        storeByte(address, value.toByte())
        storeByte(address + 1, (value shr 8).toByte())
    }

    fun storeWord(address: Int, value: Word) {
        storeHalfWord(address, value.toHalfWord())
        storeHalfWord(address + 2, (value shr 16).toHalfWord())
    }
}
