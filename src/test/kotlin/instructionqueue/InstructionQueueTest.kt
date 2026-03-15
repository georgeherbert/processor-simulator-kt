package instructionqueue

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.InstructionAddress
import types.InstructionQueueEmpty
import types.InstructionQueueFull
import types.Size
import types.Word

class InstructionQueueTest {

    @Test
    fun `starts empty`() {
        val instructionQueue = RealInstructionQueue(Size(2))

        expectThat(instructionQueue.entryCount())
            .isEqualTo(0)
    }

    @Test
    fun `enqueue adds an entry`() {
        val instructionQueue = RealInstructionQueue(Size(2))
        val entry = entry(0)
        val enqueueResult = instructionQueue.enqueue(entry)

        expectThat(enqueueResult)
            .isSuccess()
            .get { entryCount() }
            .isEqualTo(1)

        expectThat(enqueueResult)
            .isSuccess()
            .get { dequeueIfPresent() }
            .isEqualTo(
                InstructionQueueDequeueResult(
                    RealInstructionQueue(Size(2)),
                    entry
                )
            )
    }

    @Test
    fun `enqueue fails when queue is full`() {
        val queueSize = Size(1)
        val firstEnqueueResult = RealInstructionQueue(queueSize).enqueue(entry(0))
        val fullInstructionQueue = expectThat(firstEnqueueResult)
            .isSuccess()
            .subject
        val secondEnqueueResult = fullInstructionQueue.enqueue(entry(4))

        expectThat(secondEnqueueResult)
            .isFailure()
            .isEqualTo(InstructionQueueFull)

        expectThat(fullInstructionQueue.entryCount())
            .isEqualTo(1)
    }

    @Test
    fun `dequeue removes the first entry`() {
        val firstEnqueueResult = RealInstructionQueue(Size(2))
            .enqueue(entry(0))
        val firstInstructionQueue = expectThat(firstEnqueueResult)
            .isSuccess()
            .subject
        val secondEnqueueResult = firstInstructionQueue
            .enqueue(entry(4))
        val secondInstructionQueue = expectThat(secondEnqueueResult)
            .isSuccess()
            .subject

        val dequeueResult = secondInstructionQueue.dequeue()

        expectThat(dequeueResult)
            .isSuccess()
            .get { entry.instructionAddress }
            .isEqualTo(InstructionAddress(0))

        expectThat(dequeueResult)
            .isSuccess()
            .get { entry.instruction }
            .isEqualTo(Word(0u))

        expectThat(dequeueResult)
            .isSuccess()
            .get { entry.predictedNextInstructionAddress }
            .isEqualTo(InstructionAddress(4))

        expectThat(dequeueResult)
            .isSuccess()
            .get { instructionQueue.entryCount() }
            .isEqualTo(1)
    }

    @Test
    fun `dequeue removes entries in order across calls`() {
        val firstEnqueueResult = RealInstructionQueue(Size(3))
            .enqueue(entry(0))
        val firstInstructionQueue = expectThat(firstEnqueueResult)
            .isSuccess()
            .subject
        val secondEnqueueResult = firstInstructionQueue
            .enqueue(entry(4))
        val secondInstructionQueue = expectThat(secondEnqueueResult)
            .isSuccess()
            .subject
        val thirdEnqueueResult = secondInstructionQueue
            .enqueue(entry(8))
        val thirdInstructionQueue = expectThat(thirdEnqueueResult)
            .isSuccess()
            .subject

        val firstDequeueResult = thirdInstructionQueue.dequeue()
        val queueAfterFirstDequeue = expectThat(firstDequeueResult)
            .isSuccess()
            .get { instructionQueue }
            .subject
        val secondDequeueResult = queueAfterFirstDequeue.dequeue()

        expectThat(secondDequeueResult)
            .isSuccess()
            .get { entry.instructionAddress }
            .isEqualTo(InstructionAddress(4))

        expectThat(secondDequeueResult)
            .isSuccess()
            .get { instructionQueue.entryCount() }
            .isEqualTo(1)
    }

    @Test
    fun `dequeue fails when queue is empty`() {
        val instructionQueue = RealInstructionQueue(Size(2))
        val dequeueResult = instructionQueue.dequeue()

        expectThat(dequeueResult)
            .isFailure()
            .isEqualTo(InstructionQueueEmpty)
    }

    @Test
    fun `dequeueIfPresent returns unavailable when queue is empty`() {
        val instructionQueue = RealInstructionQueue(Size(2))

        expectThat(instructionQueue.dequeueIfPresent())
            .isEqualTo(InstructionQueueDequeueUnavailable)
    }

    @Test
    fun `dequeueIfPresent returns the first entry without using failure semantics`() {
        val instructionQueue = expectThat(
            RealInstructionQueue(Size(2)).enqueue(entry(0))
        )
            .isSuccess()
            .subject

        expectThat(instructionQueue.dequeueIfPresent())
            .isEqualTo(
                InstructionQueueDequeueResult(
                    RealInstructionQueue(Size(2)),
                    entry(0)
                )
            )
    }

    @Test
    fun `clear empties queue`() {
        val enqueueResult = RealInstructionQueue(Size(2))
            .enqueue(entry(0))
        val instructionQueue = expectThat(enqueueResult)
            .isSuccess()
            .subject
        val clearedInstructionQueue = instructionQueue.clear()

        expectThat(clearedInstructionQueue.entryCount())
            .isEqualTo(0)
    }

    @Test
    fun `clear is immutable`() {
        val enqueueResult = RealInstructionQueue(Size(2))
            .enqueue(entry(0))
        val originalInstructionQueue = expectThat(enqueueResult)
            .isSuccess()
            .subject
        val clearedInstructionQueue = originalInstructionQueue.clear()

        expectThat(originalInstructionQueue.entryCount())
            .isEqualTo(1)

        expectThat(clearedInstructionQueue.entryCount())
            .isEqualTo(0)
    }

    @Test
    fun `clear is idempotent`() {
        val enqueueResult = RealInstructionQueue(Size(2))
            .enqueue(entry(0))
        val instructionQueue = expectThat(enqueueResult)
            .isSuccess()
            .subject
        val firstClear = instructionQueue.clear()
        val secondClear = firstClear.clear()

        expectThat(firstClear.entryCount())
            .isEqualTo(0)

        expectThat(secondClear.entryCount())
            .isEqualTo(0)
    }

    @Test
    fun `enqueue is immutable`() {
        val instructionQueue = RealInstructionQueue(Size(2))
        val enqueueResult = instructionQueue.enqueue(entry(0))

        expectThat(instructionQueue.entryCount())
            .isEqualTo(0)

        expectThat(enqueueResult)
            .isSuccess()
            .get { entryCount() }
            .isEqualTo(1)
    }

    @Test
    fun `dequeue is immutable`() {
        val enqueueResult = RealInstructionQueue(Size(2))
            .enqueue(entry(0))
        val originalQueue = expectThat(enqueueResult)
            .isSuccess()
            .subject
        val dequeueResult = originalQueue.dequeue()

        expectThat(originalQueue.entryCount())
            .isEqualTo(1)

        expectThat(dequeueResult)
            .isSuccess()
            .get { this.instructionQueue.entryCount() }
            .isEqualTo(0)
    }

    private fun entry(address: Int) = InstructionQueueEntry(
        instruction = Word(address.toUInt()),
        instructionAddress = InstructionAddress(address),
        predictedNextInstructionAddress = InstructionAddress(address + 4),
    )
}
