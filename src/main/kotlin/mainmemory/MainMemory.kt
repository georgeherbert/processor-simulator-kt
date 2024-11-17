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

class MainMemory private constructor(private val bytes: List<Byte>) : MainMemoryLoader, MainMemoryStorer {
    constructor(size: Int) : this(List(size) { 0 })

    // TODO [GH] Will need to % here when we introduce speculative execution
    override fun loadByte(address: Int): Byte {
        checkAddress(address)
        return bytes[address]
    }

    override fun loadHalfWord(address: Int) =
        loadByte(address).toHalfWord() or (loadByte(address + 1).toHalfWord() shl 8)

    override fun loadWord(address: Int) =
        loadHalfWord(address).toWord() or (loadHalfWord(address + 2).toWord() shl 16)

    override fun storeByte(address: Int, value: Byte): MainMemory {
        checkAddress(address)
        return MainMemory(bytes.toMutableList().apply { this[address] = value })
    }

    override fun storeHalfWord(address: Int, value: HalfWord) =
        storeByte(address, value.toByte()).storeByte(address + 1, (value shr 8).toByte())

    override fun storeWord(address: Int, value: Word) =
        storeHalfWord(address, value.toHalfWord()).storeHalfWord(address + 2, (value shr 16).toHalfWord())

    private fun checkAddress(address: Int) {
        if (address < 0 || address >= bytes.size) {
            throw InvalidAddressException(address)
        }
    }
}
