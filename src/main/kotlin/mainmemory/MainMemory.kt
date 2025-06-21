package mainmemory

import types.Byte
import types.HalfWord
import types.Size
import types.Word
import types.or
import types.shl
import types.shr
import types.toByte
import types.toHalfWord
import types.toWord

class InvalidAddressException(address: Int) : Exception("Address $address is out of bounds")

@ConsistentCopyVisibility
data class MainMemory private constructor(private val bytes: List<Byte>) : MainMemoryLoader, MainMemoryStorer {
    constructor(size: Size) : this(List(size.value) { Byte(0u) })

    // TODO [GH] Will need to % here when we introduce speculative execution
    override fun loadByte(address: Int): Byte {
        checkAddress(address)
        return bytes[address]
    }

    override fun loadHalfWord(address: Int) =
        (loadByte(address + 1).toHalfWord() shl 8) or loadByte(address).toHalfWord()

    override fun loadWord(address: Int) =
        (loadHalfWord(address + 2).toWord() shl 16) or loadHalfWord(address).toWord()

    override fun storeByte(address: Int, value: Byte): MainMemory {
        checkAddress(address)
        return copy(
            bytes = bytes
                .toMutableList()
                .apply {
                    this[address] = value
                }
        )
    }

    override fun storeHalfWord(address: Int, value: HalfWord) =
        this
            .storeByte(address, value.toByte())
            .storeByte(address + 1, (value shr 8).toByte())

    override fun storeWord(address: Int, value: Word) =
        this
            .storeHalfWord(address, value.toHalfWord())
            .storeHalfWord(address + 2, (value shr 16).toHalfWord())

    private fun checkAddress(address: Int) {
        if (address < 0 || address >= bytes.size) {
            throw InvalidAddressException(address)
        }
    }
}
