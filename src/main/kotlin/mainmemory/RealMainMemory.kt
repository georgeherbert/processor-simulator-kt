package mainmemory

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import types.Byte
import types.HalfWord
import types.MainMemoryAddressOutOfBounds
import types.ProcessorResult
import types.Size
import types.Word
import types.or
import types.shl
import types.shr
import types.toByte
import types.toHalfWord
import types.toWord

@ConsistentCopyVisibility
data class RealMainMemory private constructor(private val bytes: List<Byte>) : MainMemory {
    constructor(size: Size) : this(zeroBytes(size))

    // TODO [GH] Will need to % here when we introduce speculative execution
    override fun loadByte(address: Int) =
        address
            .check()
            .map { checkedAddress -> bytes[checkedAddress] }

    override fun loadHalfWord(address: Int) =
        loadByte(address)
            .flatMap { lowByte ->
                loadByte(address + 1)
                    .map { highByte -> (highByte.toHalfWord() shl 8) or lowByte.toHalfWord() }
            }

    override fun loadWord(address: Int) =
        loadHalfWord(address)
            .flatMap { lowHalfWord ->
                loadHalfWord(address + 2)
                    .map { highHalfWord -> (highHalfWord.toWord() shl 16) or lowHalfWord.toWord() }
            }

    override fun storeByte(address: Int, value: Byte) =
        address
            .check()
            .map { checkedAddress ->
                copy(
                    bytes = bytes
                        .toMutableList()
                        .apply {
                            this[checkedAddress] = value
                        }
                )
            }

    override fun storeHalfWord(address: Int, value: HalfWord) =
        storeByte(address, value.toByte())
            .flatMap { storedMemory -> storedMemory.storeByte(address + 1, (value shr 8).toByte()) }

    override fun storeWord(address: Int, value: Word) =
        storeHalfWord(address, value.toHalfWord())
            .flatMap { storedMemory -> storedMemory.storeHalfWord(address + 2, (value shr 16).toHalfWord()) }

    private fun Int.check(): ProcessorResult<Int> =
        when {
            this < 0 || this >= bytes.size -> MainMemoryAddressOutOfBounds(this).asFailure()
            else -> this.asSuccess()
        }

    companion object {
        fun fromProgramBytes(size: Size, programBytes: ByteArray) =
            RealMainMemory(
                List(size.value) { byteIndex ->
                    when (byteIndex < programBytes.size) {
                        true -> Byte(programBytes[byteIndex].toUByte())
                        false -> Byte(0u)
                    }
                }
            )

        private fun zeroBytes(size: Size) =
            List(size.value) { Byte(0u) }
    }
}
