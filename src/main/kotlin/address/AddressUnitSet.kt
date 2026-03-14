package address

import cpu.AddressResolution
import cpu.LoadAddressResolution
import cpu.StoreAddressResolution
import memorybuffer.AddressComputationWork
import memorybuffer.LoadAddressComputationWork
import memorybuffer.StoreAddressComputationWork
import types.CycleCount
import types.DataAddress
import types.Size

interface AddressUnitSet {
    fun availableLaneCount(): Int
    fun step(dispatchedWork: List<AddressComputationWork>): AddressUnitSetStepResult
    fun clear(): AddressUnitSet
}

data class AddressUnitSetStepResult(
    val addressUnitSet: AddressUnitSet,
    val addressResolutions: List<AddressResolution>
)

@ConsistentCopyVisibility
data class RealAddressUnitSet private constructor(
    private val laneCount: Int,
    private val latency: CycleCount,
    private val lanes: List<Lane>
) : AddressUnitSet {

    constructor(laneCount: Size, latency: CycleCount) : this(laneCount.value, latency, emptyList())

    override fun availableLaneCount() = laneCount - lanes.size

    override fun step(dispatchedWork: List<AddressComputationWork>): AddressUnitSetStepResult {
        val advancedLanes = advanceExistingLanes()
        val startedLanes = mutableListOf<Lane>()
        val immediateResolutions = mutableListOf<AddressResolution>()

        dispatchedWork
            .take(availableLaneCount())
            .forEach { workItem ->
                when (latency.value == 1) {
                    true -> immediateResolutions += workItem.resolution()
                    false -> startedLanes += Lane(latency.value - 1, workItem.resolution())
                }
            }

        return AddressUnitSetStepResult(
            copy(lanes = advancedLanes.remainingLanes + startedLanes),
            advancedLanes.addressResolutions + immediateResolutions
        )
    }

    override fun clear() = copy(lanes = emptyList())

    private fun advanceExistingLanes(): AdvancedLanes {
        val remainingLanes = mutableListOf<Lane>()
        val addressResolutions = mutableListOf<AddressResolution>()

        lanes.forEach { lane ->
            when (lane.remainingCycles) {
                1 -> addressResolutions += lane.addressResolution
                else -> remainingLanes += lane.copy(remainingCycles = lane.remainingCycles - 1)
            }
        }

        return AdvancedLanes(remainingLanes, addressResolutions)
    }

    private fun AddressComputationWork.resolution() =
        when (this) {
            is LoadAddressComputationWork ->
                LoadAddressResolution(
                    memoryBufferId,
                    DataAddress((baseValue.value + immediate.value).toInt())
                )

            is StoreAddressComputationWork ->
                StoreAddressResolution(
                    memoryBufferId,
                    robId,
                    DataAddress((baseValue.value + immediate.value).toInt())
                )
        }

    private data class Lane(
        val remainingCycles: Int,
        val addressResolution: AddressResolution
    )

    private data class AdvancedLanes(
        val remainingLanes: List<Lane>,
        val addressResolutions: List<AddressResolution>
    )
}
