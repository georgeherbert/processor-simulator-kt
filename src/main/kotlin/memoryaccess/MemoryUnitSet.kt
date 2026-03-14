package memoryaccess

import cpu.CommonDataBusWrite
import cpu.MemoryLatencies
import decoder.*
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import mainmemory.MainMemoryLoader
import memorybuffer.MemoryBufferLoadWork
import types.ProcessorResult
import types.Size
import types.Word

interface MemoryUnitSet {
    fun availableLaneCount(): Int
    fun step(
        mainMemoryLoader: MainMemoryLoader,
        dispatchedWork: List<MemoryBufferLoadWork>
    ): ProcessorResult<MemoryUnitSetStepResult>

    fun clear(): MemoryUnitSet
}

data class MemoryUnitSetStepResult(
    val memoryUnitSet: MemoryUnitSet,
    val commonDataBusWrites: List<CommonDataBusWrite>
)

@ConsistentCopyVisibility
data class RealMemoryUnitSet private constructor(
    private val laneCount: Int,
    private val latencies: MemoryLatencies,
    private val lanes: List<Lane>
) : MemoryUnitSet {

    constructor(laneCount: Size, latencies: MemoryLatencies) : this(laneCount.value, latencies, emptyList())

    override fun availableLaneCount() = laneCount - lanes.size

    override fun step(
        mainMemoryLoader: MainMemoryLoader,
        dispatchedWork: List<MemoryBufferLoadWork>
    ): ProcessorResult<MemoryUnitSetStepResult> {
        val initialStartedLanesResult: ProcessorResult<List<Lane>> = emptyList<Lane>().asSuccess()

        return dispatchedWork
            .take(availableLaneCount())
            .fold(initialStartedLanesResult) { startedLanesResult, workItem ->
                startedLanesResult.flatMap { startedLanes ->
                    laneFor(mainMemoryLoader, workItem)
                        .map { lane -> startedLanes + lane }
                }
            }
            .map { startedLanes ->
                val advancedLanes = advanceExistingLanes()
                MemoryUnitSetStepResult(
                    copy(lanes = advancedLanes.remainingLanes + startedLanes),
                    advancedLanes.commonDataBusWrites
                )
            }
    }

    override fun clear() = copy(lanes = emptyList())

    private fun advanceExistingLanes(): AdvancedLanes {
        val remainingLanes = mutableListOf<Lane>()
        val commonDataBusWrites = mutableListOf<CommonDataBusWrite>()

        lanes.forEach { lane ->
            when (lane.remainingCycles) {
                1 -> commonDataBusWrites += lane.write
                else -> remainingLanes += lane.copy(remainingCycles = lane.remainingCycles - 1)
            }
        }

        return AdvancedLanes(remainingLanes, commonDataBusWrites)
    }

    private fun laneFor(
        mainMemoryLoader: MainMemoryLoader,
        workItem: MemoryBufferLoadWork
    ) =
        loadValue(mainMemoryLoader, workItem)
            .map { value ->
                Lane(
                    latencyFor(workItem.operation),
                    CommonDataBusWrite(workItem.robId, value)
                )
            }

    private fun loadValue(
        mainMemoryLoader: MainMemoryLoader,
        workItem: MemoryBufferLoadWork
    ) =
        when (workItem.operation) {
            LoadWordOperation ->
                mainMemoryLoader.loadWord(workItem.address.value)

            LoadHalfWordOperation ->
                mainMemoryLoader
                    .loadHalfWord(workItem.address.value)
                    .map { value -> Word(signExtend(value.value.toInt(), 16).toUInt()) }

            LoadHalfWordUnsignedOperation ->
                mainMemoryLoader
                    .loadHalfWord(workItem.address.value)
                    .map { value -> Word(value.value.toUInt()) }

            LoadByteOperation ->
                mainMemoryLoader
                    .loadByte(workItem.address.value)
                    .map { value -> Word(signExtend(value.value.toInt(), 8).toUInt()) }

            LoadByteUnsignedOperation ->
                mainMemoryLoader
                    .loadByte(workItem.address.value)
                    .map { value -> Word(value.value.toUInt()) }
        }

    private fun latencyFor(operation: LoadOperation) =
        when (operation) {
            LoadWordOperation -> latencies.loadWord.value
            LoadHalfWordOperation -> latencies.loadHalfWord.value
            LoadHalfWordUnsignedOperation -> latencies.loadHalfWordUnsigned.value
            LoadByteOperation -> latencies.loadByte.value
            LoadByteUnsignedOperation -> latencies.loadByteUnsigned.value
        }

    private fun signExtend(value: Int, bitWidth: Int) =
        (value shl (32 - bitWidth)) shr (32 - bitWidth)

    private data class Lane(
        val remainingCycles: Int,
        val write: CommonDataBusWrite
    )

    private data class AdvancedLanes(
        val remainingLanes: List<Lane>,
        val commonDataBusWrites: List<CommonDataBusWrite>
    )
}
