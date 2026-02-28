package commondatabus

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.CommonDataBusFull
import types.CommonDataBusValueNotPresent
import types.RobId
import types.Size
import types.Word

class CommonDataBusTest {

    @Test
    fun `value is not ready when bus is empty`() {
        val commonDataBus = RealCommonDataBus(Size(2))

        expectThat(commonDataBus.isValueReady(RobId(1)))
            .isFalse()

        expectThat(commonDataBus.valueFor(RobId(1)))
            .isFailure()
            .isEqualTo(CommonDataBusValueNotPresent)
    }

    @Test
    fun `write makes value ready and retrievable`() {
        val writeResult = RealCommonDataBus(Size(2))
            .write(RobId(1), Word(42u))

        expectThat(writeResult)
            .isSuccess()
            .get { isValueReady(RobId(1)) }
            .isTrue()

        expectThat(writeResult)
            .isSuccess()
            .get { valueFor(RobId(1)) }
            .isSuccess()
            .isEqualTo(Word(42u))
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
            .get { isValueReady(RobId(1)) }
            .isTrue()

        expectThat(firstWriteResult)
            .isSuccess()
            .get { isValueReady(RobId(2)) }
            .isFalse()

        expectThat(firstWriteResult)
            .isSuccess()
            .get { valueFor(RobId(1)) }
            .isSuccess()
            .isEqualTo(Word(10u))

        expectThat(firstWriteResult)
            .isSuccess()
            .get { valueFor(RobId(2)) }
            .isFailure()
            .isEqualTo(CommonDataBusValueNotPresent)
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

        expectThat(commonDataBus.isValueReady(RobId(1)))
            .isFalse()

        expectThat(commonDataBus.isValueReady(RobId(2)))
            .isFalse()

        expectThat(commonDataBus.valueFor(RobId(1)))
            .isFailure()
            .isEqualTo(CommonDataBusValueNotPresent)

        expectThat(commonDataBus.valueFor(RobId(2)))
            .isFailure()
            .isEqualTo(CommonDataBusValueNotPresent)
    }

    @Test
    fun `size zero bus is always full and never ready`() {
        val commonDataBus = RealCommonDataBus(Size(0))
        val writeResult = commonDataBus.write(RobId(1), Word(1u))

        expectThat(writeResult)
            .isFailure()
            .isEqualTo(CommonDataBusFull)

        expectThat(commonDataBus.isValueReady(RobId(1)))
            .isFalse()

        expectThat(commonDataBus.valueFor(RobId(1)))
            .isFailure()
            .isEqualTo(CommonDataBusValueNotPresent)
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
            .get { isValueReady(RobId(1)) }
            .isFalse()

        expectThat(secondWriteResult)
            .isSuccess()
            .get { isValueReady(RobId(2)) }
            .isTrue()

        expectThat(secondWriteResult)
            .isSuccess()
            .get { valueFor(RobId(2)) }
            .isSuccess()
            .isEqualTo(Word(20u))
    }

    @Test
    fun `write is immutable`() {
        val original = RealCommonDataBus(Size(1))
        val writeResult = original
            .write(RobId(1), Word(10u))

        expectThat(original.isValueReady(RobId(1)))
            .isFalse()

        expectThat(original.valueFor(RobId(1)))
            .isFailure()
            .isEqualTo(CommonDataBusValueNotPresent)

        expectThat(writeResult)
            .isSuccess()
            .get { isValueReady(RobId(1)) }
            .isTrue()

        expectThat(writeResult)
            .isSuccess()
            .get { valueFor(RobId(1)) }
            .isSuccess()
            .isEqualTo(Word(10u))
    }

    @Test
    fun `clear is immutable`() {
        val writeResult = RealCommonDataBus(Size(1))
            .write(RobId(1), Word(10u))

        val original = expectThat(writeResult)
            .isSuccess()
            .subject

        val cleared = original.clear()

        expectThat(original.isValueReady(RobId(1)))
            .isTrue()

        expectThat(original.valueFor(RobId(1)))
            .isSuccess()
            .isEqualTo(Word(10u))

        expectThat(cleared.isValueReady(RobId(1)))
            .isFalse()

        expectThat(cleared.valueFor(RobId(1)))
            .isFailure()
            .isEqualTo(CommonDataBusValueNotPresent)
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
            .get { valueFor(RobId(1)) }
            .isSuccess()
            .isEqualTo(Word(10u))
    }
}
