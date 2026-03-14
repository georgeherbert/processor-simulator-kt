package commondatabus

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.CommonDataBusFull
import types.PendingOperand
import types.ReadyOperand
import types.RobId
import types.Size
import types.Word

class CommonDataBusTest {

    @Test
    fun `resolveOperand leaves pending operand unchanged when bus has no value`() {
        val operand = PendingOperand(RobId(1))

        expectThat(RealCommonDataBus(Size(2)).resolveOperand(operand))
            .isEqualTo(operand)
    }

    @Test
    fun `write makes value ready and retrievable`() {
        val writeResult = RealCommonDataBus(Size(2))
            .write(RobId(1), Word(42u))

        expectThat(writeResult)
            .isSuccess()
            .get { resolveOperand(PendingOperand(RobId(1))) }
            .isEqualTo(ReadyOperand(Word(42u)))
    }

    @Test
    fun `resolveOperand returns ready operand when bus contains the value`() {
        val commonDataBus = expectThat(
            RealCommonDataBus(Size(2)).write(RobId(1), Word(42u))
        )
            .isSuccess()
            .subject

        expectThat(commonDataBus.resolveOperand(PendingOperand(RobId(1))))
            .isEqualTo(ReadyOperand(Word(42u)))
    }

    @Test
    fun `write fails when bus has no free entries`() {
        val firstWriteResult = RealCommonDataBus(Size(1))
            .write(RobId(1), Word(10u))

        val firstWrittenBus = expectThat(firstWriteResult)
            .isSuccess()
            .subject

        val secondWriteResult = firstWrittenBus
            .write(RobId(2), Word(20u))

        expectThat(secondWriteResult)
            .isFailure()
            .isEqualTo(CommonDataBusFull)

        expectThat(firstWriteResult)
            .isSuccess()
            .get { resolveOperand(PendingOperand(RobId(1))) }
            .isEqualTo(ReadyOperand(Word(10u)))

        expectThat(firstWriteResult)
            .isSuccess()
            .get { resolveOperand(PendingOperand(RobId(2))) }
            .isEqualTo(PendingOperand(RobId(2)))
    }

    @Test
    fun `clear removes all entries`() {
        val firstWriteResult = RealCommonDataBus(Size(2))
            .write(RobId(1), Word(10u))

        val firstWrittenBus = expectThat(firstWriteResult)
            .isSuccess()
            .subject

        val secondWriteResult = firstWrittenBus
            .write(RobId(2), Word(20u))

        val secondWrittenBus = expectThat(secondWriteResult)
            .isSuccess()
            .subject

        val commonDataBus = secondWrittenBus.clear()

        expectThat(commonDataBus.resolveOperand(PendingOperand(RobId(1))))
            .isEqualTo(PendingOperand(RobId(1)))

        expectThat(commonDataBus.resolveOperand(PendingOperand(RobId(2))))
            .isEqualTo(PendingOperand(RobId(2)))
    }

    @Test
    fun `size zero bus is always full and never ready`() {
        val commonDataBus = RealCommonDataBus(Size(0))
        val writeResult = commonDataBus.write(RobId(1), Word(1u))

        expectThat(writeResult)
            .isFailure()
            .isEqualTo(CommonDataBusFull)

        expectThat(commonDataBus.resolveOperand(PendingOperand(RobId(1))))
            .isEqualTo(PendingOperand(RobId(1)))
    }

    @Test
    fun `clear allows writes again`() {
        val firstWriteResult = RealCommonDataBus(Size(1))
            .write(RobId(1), Word(10u))

        val firstWrittenBus = expectThat(firstWriteResult)
            .isSuccess()
            .subject

        val secondWriteResult = firstWrittenBus
            .clear()
            .write(RobId(2), Word(20u))

        expectThat(secondWriteResult)
            .isSuccess()
            .get { resolveOperand(PendingOperand(RobId(1))) }
            .isEqualTo(PendingOperand(RobId(1)))

        expectThat(secondWriteResult)
            .isSuccess()
            .get { resolveOperand(PendingOperand(RobId(2))) }
            .isEqualTo(ReadyOperand(Word(20u)))
    }

    @Test
    fun `write is immutable`() {
        val original = RealCommonDataBus(Size(1))
        val writeResult = original
            .write(RobId(1), Word(10u))

        expectThat(original.resolveOperand(PendingOperand(RobId(1))))
            .isEqualTo(PendingOperand(RobId(1)))

        expectThat(writeResult)
            .isSuccess()
            .get { resolveOperand(PendingOperand(RobId(1))) }
            .isEqualTo(ReadyOperand(Word(10u)))
    }

    @Test
    fun `clear is immutable`() {
        val writeResult = RealCommonDataBus(Size(1))
            .write(RobId(1), Word(10u))

        val original = expectThat(writeResult)
            .isSuccess()
            .subject

        val cleared = original.clear()

        expectThat(original.resolveOperand(PendingOperand(RobId(1))))
            .isEqualTo(ReadyOperand(Word(10u)))

        expectThat(cleared.resolveOperand(PendingOperand(RobId(1))))
            .isEqualTo(PendingOperand(RobId(1)))
    }

    @Test
    fun `value lookup returns first matching rob id`() {
        val firstWriteResult = RealCommonDataBus(Size(2))
            .write(RobId(1), Word(10u))

        val firstWrittenBus = expectThat(firstWriteResult)
            .isSuccess()
            .subject

        val secondWriteResult = firstWrittenBus
            .write(RobId(1), Word(20u))

        expectThat(secondWriteResult)
            .isSuccess()
            .get { resolveOperand(PendingOperand(RobId(1))) }
            .isEqualTo(ReadyOperand(Word(10u)))
    }
}
