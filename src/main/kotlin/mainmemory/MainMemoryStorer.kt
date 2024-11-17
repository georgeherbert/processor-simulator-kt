package mainmemory

import dataunit.HalfWord
import dataunit.Word

interface MainMemoryStorer {
    fun storeByte(address: Int, value: Byte): MainMemoryStorer
    fun storeHalfWord(address: Int, value: HalfWord): MainMemoryStorer
    fun storeWord(address: Int, value: Word): MainMemoryStorer
}