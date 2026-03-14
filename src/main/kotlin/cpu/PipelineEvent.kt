package cpu

import types.*

data class CommonDataBusWrite(
    val robId: RobId,
    val value: Word
)

data class BranchResolution(
    val robId: RobId,
    val actualNextInstructionAddress: InstructionAddress
)

sealed interface AddressResolution

data class LoadAddressResolution(
    val memoryBufferId: MemoryBufferId,
    val address: DataAddress
) : AddressResolution

data class StoreAddressResolution(
    val memoryBufferId: MemoryBufferId,
    val robId: RobId,
    val address: DataAddress
) : AddressResolution
