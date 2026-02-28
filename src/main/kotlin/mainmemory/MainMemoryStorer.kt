package mainmemory

import types.Byte
import types.HalfWord
import types.ProcessorResult
import types.Word

interface MainMemoryStorer {
    fun storeByte(address: Int, value: Byte): ProcessorResult<MainMemoryStorer>
    fun storeHalfWord(address: Int, value: HalfWord): ProcessorResult<MainMemoryStorer>
    fun storeWord(address: Int, value: Word): ProcessorResult<MainMemoryStorer>
}
