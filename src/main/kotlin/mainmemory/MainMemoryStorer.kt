package mainmemory

import types.Byte
import types.HalfWord
import types.ProcessorResult
import types.Word

interface MainMemoryStorer<T : MainMemoryStorer<T>> {
    fun storeByte(address: Int, value: Byte): ProcessorResult<T>
    fun storeHalfWord(address: Int, value: HalfWord): ProcessorResult<T>
    fun storeWord(address: Int, value: Word): ProcessorResult<T>
}
