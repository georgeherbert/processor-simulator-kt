package reservationstation

import commondatabus.StubCommonDataBus
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*

class ReservationStationBankTest {

    @Test
    fun `enqueues entries until the reservation station bank is full`() {
        val reservationStationBank = expectThat(
            RealReservationStationBank<String>(Size(1)).enqueue(
                "add",
                ReadyOperand(Word(1u)),
                ReadyOperand(Word(2u)),
                Word(0u),
                RobId(1),
                InstructionAddress(4)
            )
        )
            .isSuccess()
            .subject

        expectThat(
            reservationStationBank.enqueue(
                "sub",
                ReadyOperand(Word(3u)),
                ReadyOperand(Word(4u)),
                Word(0u),
                RobId(2),
                InstructionAddress(8)
            )
        )
            .isFailure()
            .isEqualTo(ReservationStationFull)
    }

    @Test
    fun `dispatches only ready entries up to the requested maximum`() {
        val firstBank = expectThat(
            RealReservationStationBank<String>(Size(3)).enqueue(
                "add",
                ReadyOperand(Word(1u)),
                ReadyOperand(Word(2u)),
                Word(3u),
                RobId(1),
                InstructionAddress(4)
            )
        )
            .isSuccess()
            .subject
        val secondBank = expectThat(
            firstBank.enqueue(
                "sub",
                PendingOperand(RobId(9)),
                ReadyOperand(Word(4u)),
                Word(5u),
                RobId(2),
                InstructionAddress(8)
            )
        )
            .isSuccess()
            .subject
        val thirdBank = expectThat(
            secondBank.enqueue(
                "xor",
                ReadyOperand(Word(6u)),
                ReadyOperand(Word(7u)),
                Word(8u),
                RobId(3),
                InstructionAddress(12)
            )
        )
            .isSuccess()
            .subject

        val dispatchResult = thirdBank.dispatchReady(1)

        expectThat(dispatchResult.entries)
            .isEqualTo(
                listOf(
                    ReadyReservationStationEntry(
                        types.ReservationStationId(1),
                        "add",
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
                        "sub",
                        Word(11u),
                        Word(4u),
                        Word(5u),
                        RobId(2),
                        InstructionAddress(8)
                    ),
                    ReadyReservationStationEntry(
                        types.ReservationStationId(3),
                        "xor",
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
            RealReservationStationBank<String>(Size(1)).enqueue(
                "and",
                PendingOperand(RobId(7)),
                ReadyOperand(Word(9u)),
                Word(10u),
                RobId(1),
                InstructionAddress(16)
            )
        )
            .isSuccess()
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
                        "and",
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
            RealReservationStationBank<String>(Size(1)).enqueue(
                "or",
                PendingOperand(RobId(7)),
                ReadyOperand(Word(9u)),
                Word(10u),
                RobId(1),
                InstructionAddress(16)
            )
        )
            .isSuccess()
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
            RealReservationStationBank<String>(Size(1)).enqueue(
                "add",
                ReadyOperand(Word(1u)),
                ReadyOperand(Word(2u)),
                Word(0u),
                RobId(1),
                InstructionAddress(4)
            )
        )
            .isSuccess()
            .subject
        val clearedReservationStationBank = reservationStationBank.clear()

        expectThat(clearedReservationStationBank.dispatchReady(1).entries)
            .isEqualTo(emptyList())

        expectThat(
            clearedReservationStationBank.enqueue(
                "sub",
                ReadyOperand(Word(3u)),
                ReadyOperand(Word(4u)),
                Word(0u),
                RobId(2),
                InstructionAddress(8)
            )
        )
            .isSuccess()
    }

}
