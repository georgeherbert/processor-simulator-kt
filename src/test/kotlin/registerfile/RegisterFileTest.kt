package registerfile

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.*

class RegisterFileTest {

    @Test
    fun `zero register remains zero regardless of seed reserve and commit operations`() {
        val registerFile = RealRegisterFile()
            .seed(RegisterAddress(0), Word(99u))
            .reserveDestination(RegisterAddress(0), RobId(1))
            .commit(RegisterAddress(0), Word(77u), RobId(1))

        expectThat(registerFile.readCommitted(RegisterAddress(0)))
            .isEqualTo(Word(0u))

        expectThat(registerFile.operandFor(RegisterAddress(0)))
            .isEqualTo(ReadyOperand(Word(0u)))
    }

    @Test
    fun `commit clears only the matching pending tag`() {
        val reservedRegisterFile = RealRegisterFile()
            .reserveDestination(RegisterAddress(1), RobId(1))

        expectThat(reservedRegisterFile.operandFor(RegisterAddress(1)))
            .isEqualTo(PendingOperand(RobId(1)))

        val committedRegisterFile = reservedRegisterFile.commit(RegisterAddress(1), Word(5u), RobId(1))

        expectThat(committedRegisterFile.readCommitted(RegisterAddress(1)))
            .isEqualTo(Word(5u))

        expectThat(committedRegisterFile.operandFor(RegisterAddress(1)))
            .isEqualTo(ReadyOperand(Word(5u)))

        val newerReservationRegisterFile = reservedRegisterFile
            .reserveDestination(RegisterAddress(1), RobId(2))
            .commit(RegisterAddress(1), Word(6u), RobId(1))

        expectThat(newerReservationRegisterFile.readCommitted(RegisterAddress(1)))
            .isEqualTo(Word(6u))

        expectThat(newerReservationRegisterFile.operandFor(RegisterAddress(1)))
            .isEqualTo(PendingOperand(RobId(2)))
    }

    @Test
    fun `flush clears pending destinations and preserves committed values`() {
        val registerFile = RealRegisterFile()
            .seed(RegisterAddress(3), Word(11u))
            .reserveDestination(RegisterAddress(3), RobId(9))
            .flushPendingDestinations()

        expectThat(registerFile.readCommitted(RegisterAddress(3)))
            .isEqualTo(Word(11u))

        expectThat(registerFile.operandFor(RegisterAddress(3)))
            .isEqualTo(ReadyOperand(Word(11u)))
    }
}
