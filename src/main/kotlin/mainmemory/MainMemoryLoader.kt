package mainmemory

import types.HalfWord
import types.Word

interface MainMemoryLoader {
    fun loadByte(address: Int): Byte
    fun loadHalfWord(address: Int): HalfWord
    fun loadWord(address: Int): Word
}
