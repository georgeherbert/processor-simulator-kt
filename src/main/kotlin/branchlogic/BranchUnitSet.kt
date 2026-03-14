package branchlogic

import cpu.BranchLatencies
import cpu.BranchResolution
import cpu.CommonDataBusWrite
import reservationstation.ReadyReservationStationEntry
import types.InstructionAddress
import types.Size

interface BranchUnitSet {
    fun availableLaneCount(): Int
    fun step(dispatchedEntries: List<ReadyReservationStationEntry<BranchOperation>>): BranchUnitSetStepResult
    fun clear(): BranchUnitSet
}

data class BranchUnitSetStepResult(
    val branchUnitSet: BranchUnitSet,
    val commonDataBusWrites: List<CommonDataBusWrite>,
    val branchResolutions: List<BranchResolution>
)

@ConsistentCopyVisibility
data class RealBranchUnitSet private constructor(
    private val laneCount: Int,
    private val branchEvaluator: BranchEvaluator,
    private val latencies: BranchLatencies,
    private val lanes: List<Lane>
) : BranchUnitSet {

    constructor(
        laneCount: Size,
        branchEvaluator: BranchEvaluator,
        latencies: BranchLatencies
    ) : this(laneCount.value, branchEvaluator, latencies, emptyList())

    override fun availableLaneCount() = laneCount - lanes.size

    override fun step(dispatchedEntries: List<ReadyReservationStationEntry<BranchOperation>>): BranchUnitSetStepResult {
        val advancedLanes = advanceExistingLanes()
        val startedLanes = dispatchedEntries
            .take(availableLaneCount())
            .map { entry ->
                val evaluation = branchEvaluator.evaluate(
                    entry.operation,
                    entry.leftValue,
                    entry.rightValue,
                    entry.immediate,
                    entry.instructionAddress.asWord()
                )

                Lane(
                    latencyFor(entry.operation),
                    writeBackFor(entry.robId, evaluation.writeBack),
                    BranchResolution(
                        entry.robId,
                        InstructionAddress(evaluation.actualNextInstructionAddress.value.toInt())
                    )
                )
            }

        return BranchUnitSetStepResult(
            copy(lanes = advancedLanes.remainingLanes + startedLanes),
            advancedLanes.commonDataBusWrites,
            advancedLanes.branchResolutions
        )
    }

    override fun clear() = copy(lanes = emptyList())

    private fun advanceExistingLanes(): AdvancedLanes {
        val remainingLanes = mutableListOf<Lane>()
        val commonDataBusWrites = mutableListOf<CommonDataBusWrite>()
        val branchResolutions = mutableListOf<BranchResolution>()

        lanes.forEach { lane ->
            when (lane.remainingCycles) {
                1 -> {
                    lane.commonDataBusWrite?.let { write -> commonDataBusWrites += write }
                    branchResolutions += lane.branchResolution
                }

                else -> remainingLanes += lane.copy(remainingCycles = lane.remainingCycles - 1)
            }
        }

        return AdvancedLanes(remainingLanes, commonDataBusWrites, branchResolutions)
    }

    private fun latencyFor(operation: BranchOperation) =
        when (operation) {
            JumpAndLink -> latencies.jumpAndLink.value
            JumpAndLinkRegister -> latencies.jumpAndLinkRegister.value
            BranchEqual -> latencies.branchEqual.value
            BranchNotEqual -> latencies.branchNotEqual.value
            BranchLessThanSigned -> latencies.branchLessThanSigned.value
            BranchLessThanUnsigned -> latencies.branchLessThanUnsigned.value
            BranchGreaterThanOrEqualSigned -> latencies.branchGreaterThanOrEqualSigned.value
            BranchGreaterThanOrEqualUnsigned -> latencies.branchGreaterThanOrEqualUnsigned.value
        }

    private fun writeBackFor(robId: types.RobId, writeBack: BranchWriteBack) =
        when (writeBack) {
            is BranchLinkWriteBack -> CommonDataBusWrite(robId, writeBack.value)
            NoBranchWriteBack -> null
        }

    private fun InstructionAddress.asWord() = types.Word(value.toUInt())

    private data class Lane(
        val remainingCycles: Int,
        val commonDataBusWrite: CommonDataBusWrite?,
        val branchResolution: BranchResolution
    )

    private data class AdvancedLanes(
        val remainingLanes: List<Lane>,
        val commonDataBusWrites: List<CommonDataBusWrite>,
        val branchResolutions: List<BranchResolution>
    )
}
