package arithmeticlogic

import cpu.ArithmeticLogicLatencies
import cpu.CommonDataBusWrite
import reservationstation.ReadyReservationStationEntry
import types.Size

interface ArithmeticLogicUnitSet {
    fun availableLaneCount(): Int
    fun step(dispatchedEntries: List<ReadyReservationStationEntry<ArithmeticLogicOperation>>): ArithmeticLogicUnitSetStepResult
    fun clear(): ArithmeticLogicUnitSet
}

data class ArithmeticLogicUnitSetStepResult(
    val arithmeticLogicUnitSet: ArithmeticLogicUnitSet,
    val commonDataBusWrites: List<CommonDataBusWrite>
)

@ConsistentCopyVisibility
data class RealArithmeticLogicUnitSet private constructor(
    private val laneCount: Int,
    private val arithmeticLogicUnit: ArithmeticLogicUnit,
    private val latencies: ArithmeticLogicLatencies,
    private val lanes: List<Lane>
) : ArithmeticLogicUnitSet {

    constructor(
        laneCount: Size,
        arithmeticLogicUnit: ArithmeticLogicUnit,
        latencies: ArithmeticLogicLatencies
    ) : this(laneCount.value, arithmeticLogicUnit, latencies, emptyList())

    override fun availableLaneCount() = laneCount - lanes.size

    override fun step(dispatchedEntries: List<ReadyReservationStationEntry<ArithmeticLogicOperation>>): ArithmeticLogicUnitSetStepResult {
        val advancedLanes = advanceExistingLanes()
        val startedLanes = dispatchedEntries
            .take(availableLaneCount())
            .map { entry ->
                Lane(
                    latencyFor(entry.operation),
                    CommonDataBusWrite(
                        entry.robId,
                        arithmeticLogicUnit.evaluate(
                            entry.operation,
                            entry.leftValue,
                            entry.rightValue,
                            entry.instructionAddress.value
                        )
                    )
                )
            }

        return ArithmeticLogicUnitSetStepResult(
            copy(lanes = advancedLanes.remainingLanes + startedLanes),
            advancedLanes.commonDataBusWrites
        )
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

    private fun latencyFor(operation: ArithmeticLogicOperation) =
        when (operation) {
            Add,
            AddImmediate -> latencies.add.value

            LoadUpperImmediate -> latencies.loadUpperImmediate.value
            AddUpperImmediateToProgramCounter -> latencies.addUpperImmediateToProgramCounter.value
            Subtract -> latencies.subtract.value

            ShiftLeftLogical,
            ShiftLeftLogicalImmediate -> latencies.shiftLeftLogical.value

            SetLessThanSigned,
            SetLessThanImmediateSigned -> latencies.setLessThanSigned.value

            SetLessThanUnsigned,
            SetLessThanImmediateUnsigned -> latencies.setLessThanUnsigned.value

            ExclusiveOr,
            ExclusiveOrImmediate -> latencies.exclusiveOr.value

            ShiftRightLogical,
            ShiftRightLogicalImmediate -> latencies.shiftRightLogical.value

            ShiftRightArithmetic,
            ShiftRightArithmeticImmediate -> latencies.shiftRightArithmetic.value

            Or,
            OrImmediate -> latencies.or.value

            And,
            AndImmediate -> latencies.and.value
        }

    private data class Lane(
        val remainingCycles: Int,
        val write: CommonDataBusWrite
    )

    private data class AdvancedLanes(
        val remainingLanes: List<Lane>,
        val commonDataBusWrites: List<CommonDataBusWrite>
    )
}
