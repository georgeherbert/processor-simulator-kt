package mainmemory

import types.Byte
import types.HalfWord
import types.Size
import types.Word

class InvalidAddressException(address: Int) : Exception("Address $address is out of bounds")

@ConsistentCopyVisibility
data class MainMemory private constructor(private val bytes: List<kotlin.Byte>) : MainMemoryLoader, MainMemoryStorer {
    constructor(size: Size) : this(List(size.value) { 0.toByte() })

    // TODO [GH] Will need to % here when we introduce speculative execution
    override fun loadByte(address: Int): Byte {
        checkAddress(address)
        val raw = bytes[address]
        return Byte(raw.toInt() and 0xFF)
    }

    override fun loadHalfWord(address: Int): HalfWord =
        HalfWord((loadByte(address).value) or (loadByte(address + 1).value shl 8))

    override fun loadWord(address: Int): Word =
        Word((loadHalfWord(address).value) or (loadHalfWord(address + 2).value shl 16))

    override fun storeByte(address: Int, value: Byte): MainMemory {
        checkAddress(address)
        return copy(bytes = bytes.toMutableList().apply { this[address] = value.value.toByte() })
    }

    override fun storeHalfWord(address: Int, value: HalfWord): MainMemory =
        storeByte(address, Byte(value.value and 0xFF))
            .storeByte(address + 1, Byte((value.value ushr 8) and 0xFF))

    override fun storeWord(address: Int, value: Word): MainMemory =
        storeHalfWord(address, HalfWord(value.value and 0xFFFF))
            .storeHalfWord(address + 2, HalfWord((value.value ushr 16) and 0xFFFF))

    private fun checkAddress(address: Int) {
        if (address < 0 || address >= bytes.size) {
            throw InvalidAddressException(address)
        }
    }
}
