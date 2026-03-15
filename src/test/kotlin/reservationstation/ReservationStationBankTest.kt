package reservationstation

import arithmeticlogic.Add
import arithmeticlogic.ArithmeticLogicOperation
import arithmeticlogic.And
import arithmeticlogic.ExclusiveOr
import arithmeticlogic.Or
import arithmeticlogic.Subtract
import commondatabus.StubCommonDataBus
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isReservationStationEnqueueResult
import testfixtures.isSuccess
import types.*

class ReservationStationBankTest {

    @Test
    fun `enqueues entries until the reservation station bank is full`() {
        val reservationStationBank = expectThat(
            RealReservationStationBank<ArithmeticLogicOperation>(Size(1)).enqueue(
                Add,
                ReadyOperand(Word(1u)),
                ReadyOperand(Word(2u)),
                Word(0u),
                RobId(1),
                InstructionAddress(4)
            )
        )
            .isSuccess()
            .isReservationStationEnqueueResult()
            .subject

        expectThat(reservationStationBank.dispatchReady(1).entries)
            .isEqualTo(
                listOf(
                    ReadyReservationStationEntry(
                        ReservationStationId(1),
                        Add,
                        Word(1u),
                        Word(2u),
                        Word(0u),
                        RobId(1),
                        InstructionAddress(4)
                    )
                )
            )

        expectThat(
            reservationStationBank.enqueue(
                Subtract,
                ReadyOperand(Word(3u)),
                ReadyOperand(Word(4u)),
                Word(0u),
                RobId(2),
                InstructionAddress(8)
            )
        )
            .isSuccess()
            .isEqualTo(ReservationStationEnqueueUnavailable)
    }

    @Test
    fun `dispatches only ready entries up to the requested maximum`() {
        val firstBank = expectThat(
            RealReservationStationBank<ArithmeticLogicOperation>(Size(3)).enqueue(
                Add,
                ReadyOperand(Word(1u)),
                ReadyOperand(Word(2u)),
                Word(3u),
                RobId(1),
                InstructionAddress(4)
            )
        )
            .isSuccess()
            .isReservationStationEnqueueResult()
            .subject
        val secondBank = expectThat(
            firstBank.enqueue(
                Subtract,
                PendingOperand(RobId(9)),
                ReadyOperand(Word(4u)),
                Word(5u),
                RobId(2),
                InstructionAddress(8)
            )
        )
            .isSuccess()
            .isReservationStationEnqueueResult()
            .subject
        val thirdBank = expectThat(
            secondBank.enqueue(
                ExclusiveOr,
                ReadyOperand(Word(6u)),
                ReadyOperand(Word(7u)),
                Word(8u),
                RobId(3),
                InstructionAddress(12)
            )
        )
            .isSuccess()
            .isReservationStationEnqueueResult()
            .subject

        val dispatchResult = thirdBank.dispatchReady(1)

        expectThat(dispatchResult.entries)
            .isEqualTo(
                listOf(
                    ReadyReservationStationEntry(
                        types.ReservationStationId(1),
                        Add,
                        Word(1u),
                        Word(2u),
                        Word(3u),
                        RobId(1),
                        InstructionAddress(4)
                    )
                )
            )

        expectThat(
            dispatchResult.reservationStationBank
                .acceptCommonDataBus(StubCommonDataBus(RobId(9), Word(11u)))
                .dispatchReady(2)
                .entries
        )
            .isEqualTo(
                listOf(
                    ReadyReservationStationEntry(
                        types.ReservationStationId(2),
                        Subtract,
                        Word(11u),
                        Word(4u),
                        Word(5u),
                        RobId(2),
                        InstructionAddress(8)
                    ),
                    ReadyReservationStationEntry(
                        types.ReservationStationId(3),
                        ExclusiveOr,
                        Word(6u),
                        Word(7u),
                        Word(8u),
                        RobId(3),
                        InstructionAddress(12)
                    )
                )
            )
    }

    @Test
    fun `accept common data bus resolves pending operands and makes entries dispatchable`() {
        val reservationStationBank = expectThat(
            RealReservationStationBank<ArithmeticLogicOperation>(Size(1)).enqueue(
                And,
                PendingOperand(RobId(7)),
                ReadyOperand(Word(9u)),
                Word(10u),
                RobId(1),
                InstructionAddress(16)
            )
        )
            .isSuccess()
            .isReservationStationEnqueueResult()
            .subject
        val commonDataBus = StubCommonDataBus(RobId(7), Word(11u))

        val dispatchResult = reservationStationBank
            .acceptCommonDataBus(commonDataBus)
            .dispatchReady(1)

        expectThat(dispatchResult.entries)
            .isEqualTo(
                listOf(
                    ReadyReservationStationEntry(
                        types.ReservationStationId(1),
                        And,
                        Word(11u),
                        Word(9u),
                        Word(10u),
                        RobId(1),
                        InstructionAddress(16)
                    )
                )
            )
    }

    @Test
    fun `accept common data bus leaves pending operands unresolved when the value is unavailable`() {
        val reservationStationBank = expectThat(
            RealReservationStationBank<ArithmeticLogicOperation>(Size(1)).enqueue(
                Or,
                PendingOperand(RobId(7)),
                ReadyOperand(Word(9u)),
                Word(10u),
                RobId(1),
                InstructionAddress(16)
            )
        )
            .isSuccess()
            .isReservationStationEnqueueResult()
            .subject
        val commonDataBus = StubCommonDataBus()

        expectThat(
            reservationStationBank
                .acceptCommonDataBus(commonDataBus)
                .dispatchReady(1)
                .entries
        )
            .isEqualTo(emptyList())
    }

    @Test
    fun `clear removes all entries`() {
        val reservationStationBank = expectThat(
            RealReservationStationBank<ArithmeticLogicOperation>(Size(1)).enqueue(
                Add,
                ReadyOperand(Word(1u)),
                ReadyOperand(Word(2u)),
                Word(0u),
                RobId(1),
                InstructionAddress(4)
            )
        )
            .isSuccess()
            .isReservationStationEnqueueResult()
            .subject
        val clearedReservationStationBank = reservationStationBank.clear()

        expectThat(clearedReservationStationBank.dispatchReady(1).entries)
            .isEqualTo(emptyList())

        expectThat(
            clearedReservationStationBank.enqueue(
                Subtract,
                ReadyOperand(Word(3u)),
                ReadyOperand(Word(4u)),
                Word(0u),
                RobId(2),
                InstructionAddress(8)
            )
        )
            .isSuccess()
            .isReservationStationEnqueueResult()
    }

}
