package mainmemory

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.Byte
import types.HalfWord
import types.MainMemoryAddressOutOfBounds
import types.Word

@ConsistentCopyVisibility
data class StubMainMemory private constructor(
    private val bytesByAddress: Map<Int, Byte>,
    private val halfWordsByAddress: Map<Int, HalfWord>,
    private val wordsByAddress: Map<Int, Word>
) : MainMemory {

    constructor() : this(emptyMap(), emptyMap(), emptyMap())

    override fun loadByte(address: Int) =
        bytesByAddress[address]
            ?.asSuccess()
            ?: MainMemoryAddressOutOfBounds(address).asFailure()

    override fun loadHalfWord(address: Int) =
        halfWordsByAddress[address]
            ?.asSuccess()
            ?: MainMemoryAddressOutOfBounds(address).asFailure()

    override fun loadWord(address: Int) =
        wordsByAddress[address]
            ?.asSuccess()
            ?: MainMemoryAddressOutOfBounds(address).asFailure()

    override fun storeByte(address: Int, value: Byte) =
        copy(bytesByAddress = bytesByAddress + (address to value)).asSuccess()

    override fun storeHalfWord(address: Int, value: HalfWord) =
        copy(halfWordsByAddress = halfWordsByAddress + (address to value)).asSuccess()

    override fun storeWord(address: Int, value: Word) =
        copy(wordsByAddress = wordsByAddress + (address to value)).asSuccess()
}
