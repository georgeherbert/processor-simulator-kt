package arithmeticlogic

import cpu.ArithmeticLogicLatencies
import cpu.CommonDataBusWrite
import org.junit.jupiter.api.Test
import reservationstation.ReadyReservationStationEntry
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.*

class ArithmeticLogicUnitSetTest {

    @Test
    fun `emits arithmetic results after the configured latency for each operation group`() {
        val operationsWithExpectedLatency = listOf(
            Add to 1,
            LoadUpperImmediate to 2,
            AddUpperImmediateToProgramCounter to 3,
            Subtract to 4,
            ShiftLeftLogical to 5,
            SetLessThanSigned to 6,
            SetLessThanUnsigned to 7,
            ExclusiveOr to 8,
            ShiftRightLogical to 9,
            ShiftRightArithmetic to 10,
            Or to 11,
            And to 12
        )

        operationsWithExpectedLatency.forEach { operationWithLatency ->
            val dispatchedResult = unitSetWithDistinctLatencies().step(
                listOf(entry(operationWithLatency.first, RobId(operationWithLatency.second)))
            )
            val followUpResults = followUpResults(
                dispatchedResult.arithmeticLogicUnitSet,
                operationWithLatency.second
            )

            expectThat(dispatchedResult.commonDataBusWrites)
                .isEqualTo(emptyList())

            expectThat(followUpResults.dropLast(1).flatMap { stepResult -> stepResult.commonDataBusWrites })
                .isEqualTo(emptyList())

            expectThat(followUpResults.last().commonDataBusWrites)
                .isEqualTo(
                    listOf(
                        CommonDataBusWrite(RobId(operationWithLatency.second), Word(9u))
                    )
                )
        }
    }

    @Test
    fun `dispatch is limited by the available lane count`() {
        val firstStepResult = RealArithmeticLogicUnitSet(
            Size(1),
            StubArithmeticLogicUnit,
            singleCycleLatencies()
        ).step(
            listOf(
                entry(Add, RobId(1)),
                entry(Subtract, RobId(2))
            )
        )
        val secondStepResult = firstStepResult.arithmeticLogicUnitSet.step(emptyList())

        expectThat(secondStepResult.commonDataBusWrites)
            .isEqualTo(
                listOf(
                    CommonDataBusWrite(RobId(1), Word(9u))
                )
            )
    }

    @Test
    fun `clear drops in flight arithmetic work`() {
        val firstStepResult = RealArithmeticLogicUnitSet(
            Size(1),
            StubArithmeticLogicUnit,
            ArithmeticLogicLatencies(
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2)
            )
        ).step(listOf(entry(Add, RobId(1))))

        val clearedUnitSet = firstStepResult.arithmeticLogicUnitSet.clear()
        val secondStepResult = clearedUnitSet.step(emptyList())

        expectThat(secondStepResult.commonDataBusWrites)
            .isEqualTo(emptyList())
    }

    private fun unitSetWithDistinctLatencies() =
        RealArithmeticLogicUnitSet(Size(1), StubArithmeticLogicUnit, distinctLatencies())

    private fun distinctLatencies() =
        ArithmeticLogicLatencies(
            CycleCount(1),
            CycleCount(2),
            CycleCount(3),
            CycleCount(4),
            CycleCount(5),
            CycleCount(6),
            CycleCount(7),
            CycleCount(8),
            CycleCount(9),
            CycleCount(10),
            CycleCount(11),
            CycleCount(12)
        )

    private fun singleCycleLatencies() =
        ArithmeticLogicLatencies(
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1)
        )

    private fun followUpResults(
        arithmeticLogicUnitSet: ArithmeticLogicUnitSet,
        remainingSteps: Int
    ): List<ArithmeticLogicUnitSetStepResult> =
        when (remainingSteps == 0) {
            true -> emptyList()
            false -> {
                val stepResult = arithmeticLogicUnitSet.step(emptyList())
                listOf(stepResult) + followUpResults(stepResult.arithmeticLogicUnitSet, remainingSteps - 1)
            }
        }

    private fun entry(operation: ArithmeticLogicOperation, robId: RobId) =
        ReadyReservationStationEntry(
            ReservationStationId(1),
            operation,
            Word(2u),
            Word(3u),
            Word(0u),
            robId,
            InstructionAddress(4)
        )
}
