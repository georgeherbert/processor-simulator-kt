package mainmemory

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.MainMemoryAddressOutOfBounds
import types.Word

class StubMainMemoryLoader(
    private val wordsByAddress: Map<Int, UInt>,
    private val failingAddresses: Set<Int>
) : MainMemoryLoader {
    override fun loadByte(address: Int) =
        MainMemoryAddressOutOfBounds(address).asFailure()

    override fun loadHalfWord(address: Int) =
        MainMemoryAddressOutOfBounds(address).asFailure()

    override fun loadWord(address: Int) =
        when (failingAddresses.contains(address) || !wordsByAddress.containsKey(address)) {
            true ->
                MainMemoryAddressOutOfBounds(address)
                    .asFailure()

            false ->
                Word(wordsByAddress[address]!!)
                    .asSuccess()
        }
}
