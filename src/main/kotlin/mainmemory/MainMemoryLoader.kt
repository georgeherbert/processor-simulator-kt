package mainmemory

import types.Byte
import types.HalfWord
import types.ProcessorResult
import types.Word

interface MainMemoryLoader {
    fun loadByte(address: Int): ProcessorResult<Byte>
    fun loadHalfWord(address: Int): ProcessorResult<HalfWord>
    fun loadWord(address: Int): ProcessorResult<Word>
}
