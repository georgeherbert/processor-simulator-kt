package branchlogic

import cpu.BranchLatencies
import cpu.BranchResolution
import cpu.CommonDataBusWrite
import org.junit.jupiter.api.Test
import reservationstation.ReadyReservationStationEntry
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.*

class BranchUnitSetTest {

    @Test
    fun `emits branch resolutions after the configured latency for each operation`() {
        val operationsWithExpectedLatency = listOf(
            JumpAndLink to 1,
            JumpAndLinkRegister to 2,
            BranchEqual to 3,
            BranchNotEqual to 4,
            BranchLessThanSigned to 5,
            BranchLessThanUnsigned to 6,
            BranchGreaterThanOrEqualSigned to 7,
            BranchGreaterThanOrEqualUnsigned to 8
        )

        operationsWithExpectedLatency.forEach { operationWithLatency ->
            val dispatchedResult = unitSetWithDistinctLatencies().step(
                listOf(entry(operationWithLatency.first, RobId(operationWithLatency.second)))
            )
            val followUpResults = followUpResults(
                dispatchedResult.branchUnitSet,
                operationWithLatency.second
            )

            expectThat(dispatchedResult.commonDataBusWrites)
                .isEqualTo(emptyList())

            expectThat(dispatchedResult.branchResolutions)
                .isEqualTo(emptyList())

            expectThat(followUpResults.dropLast(1).flatMap { stepResult -> stepResult.branchResolutions })
                .isEqualTo(emptyList())

            expectThat(followUpResults.last().branchResolutions)
                .isEqualTo(
                    listOf(
                        BranchResolution(RobId(operationWithLatency.second), InstructionAddress(12))
                    )
                )

            expectThat(followUpResults.last().commonDataBusWrites)
                .isEqualTo(expectedWritesFor(operationWithLatency.first, operationWithLatency.second))
        }
    }

    @Test
    fun `dispatch is limited by the available lane count`() {
        val firstStepResult = RealBranchUnitSet(
            Size(1),
            StubBranchEvaluator,
            singleCycleLatencies()
        ).step(
            listOf(
                entry(JumpAndLink, RobId(1)),
                entry(BranchEqual, RobId(2))
            )
        )
        val secondStepResult = firstStepResult.branchUnitSet.step(emptyList())

        expectThat(secondStepResult.branchResolutions)
            .isEqualTo(
                listOf(
                    BranchResolution(RobId(1), InstructionAddress(12))
                )
            )

        expectThat(secondStepResult.commonDataBusWrites)
            .isEqualTo(
                listOf(
                    CommonDataBusWrite(RobId(1), Word(99u))
                )
            )
    }

    @Test
    fun `clear drops in flight branch work`() {
        val firstStepResult = RealBranchUnitSet(
            Size(1),
            StubBranchEvaluator,
            BranchLatencies(
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2)
            )
        ).step(listOf(entry(JumpAndLink, RobId(1))))

        val clearedUnitSet = firstStepResult.branchUnitSet.clear()
        val secondStepResult = clearedUnitSet.step(emptyList())

        expectThat(secondStepResult.commonDataBusWrites)
            .isEqualTo(emptyList())

        expectThat(secondStepResult.branchResolutions)
            .isEqualTo(emptyList())
    }

    private fun unitSetWithDistinctLatencies() =
        RealBranchUnitSet(Size(1), StubBranchEvaluator, distinctLatencies())

    private fun distinctLatencies() =
        BranchLatencies(
            CycleCount(1),
            CycleCount(2),
            CycleCount(3),
            CycleCount(4),
            CycleCount(5),
            CycleCount(6),
            CycleCount(7),
            CycleCount(8)
        )

    private fun singleCycleLatencies() =
        BranchLatencies(
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
        branchUnitSet: BranchUnitSet,
        remainingSteps: Int
    ): List<BranchUnitSetStepResult> =
        when (remainingSteps == 0) {
            true -> emptyList()
            false -> {
                val stepResult = branchUnitSet.step(emptyList())
                listOf(stepResult) + followUpResults(stepResult.branchUnitSet, remainingSteps - 1)
            }
        }

    private fun expectedWritesFor(operation: BranchOperation, robIdValue: Int) =
        when (operation) {
            JumpAndLink,
            JumpAndLinkRegister ->
                listOf(CommonDataBusWrite(RobId(robIdValue), Word(99u)))

            BranchEqual,
            BranchNotEqual,
            BranchLessThanSigned,
            BranchLessThanUnsigned,
            BranchGreaterThanOrEqualSigned,
            BranchGreaterThanOrEqualUnsigned -> emptyList()
        }

    private fun entry(operation: BranchOperation, robId: RobId) =
        ReadyReservationStationEntry(
            ReservationStationId(1),
            operation,
            Word(2u),
            Word(3u),
            Word(8u),
            robId,
            InstructionAddress(4)
        )
}
