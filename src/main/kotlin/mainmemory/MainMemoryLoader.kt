package mainmemory

import dataunit.HalfWord
import dataunit.Word

interface MainMemoryLoader {
    fun loadByte(address: Int): Byte
    fun loadHalfWord(address: Int): HalfWord
    fun loadWord(address: Int): Word
}