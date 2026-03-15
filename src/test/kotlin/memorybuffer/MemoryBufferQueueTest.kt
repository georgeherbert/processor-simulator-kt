package memorybuffer

import decoder.LoadWordOperation
import decoder.StoreWordOperation
import org.junit.jupiter.api.Test
import reorderbuffer.StubReorderBuffer
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isMemoryBufferEnqueueResult
import testfixtures.isSuccess
import types.*

class MemoryBufferQueueTest {

    @Test
    fun `unresolved earlier store blocks later load address dispatch`() {
        val firstQueue = expectThat(
            RealMemoryBufferQueue(Size(4)).enqueue(
                StoreMemoryBufferOperation(StoreWordOperation),
                PendingOperand(RobId(9)),
                ReadyOperand(Word(1u)),
                Word(0u),
                RobId(1)
            )
        )
            .isSuccess()
            .isMemoryBufferEnqueueResult()
            .subject

        val secondQueue = expectThat(
            firstQueue.enqueue(
                LoadMemoryBufferOperation(LoadWordOperation),
                ReadyOperand(Word(0u)),
                ReadyOperand(Word(0u)),
                Word(32u),
                RobId(2)
            )
        )
            .isSuccess()
            .isMemoryBufferEnqueueResult()
            .subject

        expectThat(secondQueue.dispatchAddressComputations(2).workItems)
            .isEqualTo(emptyList())
    }

    @Test
    fun `load moves from address dispatch to memory dispatch once its address is ready`() {
        val queue = expectThat(
            RealMemoryBufferQueue(Size(2)).enqueue(
                LoadMemoryBufferOperation(LoadWordOperation),
                ReadyOperand(Word(4u)),
                ReadyOperand(Word(0u)),
                Word(8u),
                RobId(1)
            )
        )
            .isSuccess()
            .isMemoryBufferEnqueueResult()
            .subject

        val addressDispatchResult = queue.dispatchAddressComputations(1)

        expectThat(addressDispatchResult.workItems)
            .isEqualTo(
                listOf(
                    LoadAddressComputationWork(
                        memoryBufferId = types.MemoryBufferId(1),
                        operation = LoadWordOperation,
                        baseValue = Word(4u),
                        immediate = Word(8u),
                        robId = RobId(1)
                    )
                )
            )

        val addressedQueue = addressDispatchResult.memoryBufferQueue
            .recordComputedAddress(types.MemoryBufferId(1), DataAddress(12))

        expectThat(addressedQueue.dispatchMemoryLoads(1, StubReorderBuffer()).workItems)
            .isEqualTo(
                listOf(
                    MemoryBufferLoadWork(
                        memoryBufferId = types.MemoryBufferId(1),
                        operation = LoadWordOperation,
                        address = DataAddress(12),
                        robId = RobId(1)
                    )
                )
            )
    }

    @Test
    fun `load dispatch stays blocked while an earlier store address computation is in flight`() {
        val queueWithStore = expectThat(
            RealMemoryBufferQueue(Size(3)).enqueue(
                StoreMemoryBufferOperation(StoreWordOperation),
                ReadyOperand(Word(4u)),
                ReadyOperand(Word(9u)),
                Word(8u),
                RobId(1)
            )
        )
            .isSuccess()
            .isMemoryBufferEnqueueResult()
            .subject
        val queueWithLoad = expectThat(
            queueWithStore.enqueue(
                LoadMemoryBufferOperation(LoadWordOperation),
                ReadyOperand(Word(4u)),
                ReadyOperand(Word(0u)),
                Word(8u),
                RobId(2)
            )
        )
            .isSuccess()
            .isMemoryBufferEnqueueResult()
            .subject

        val addressDispatchResult = queueWithLoad.dispatchAddressComputations(1)

        expectThat(addressDispatchResult.workItems)
            .isEqualTo(
                listOf(
                    StoreAddressComputationWork(
                        MemoryBufferId(1),
                        StoreWordOperation,
                        Word(4u),
                        Word(8u),
                        RobId(1)
                    )
                )
            )

        expectThat(addressDispatchResult.memoryBufferQueue.dispatchMemoryLoads(1, StubReorderBuffer()).workItems)
            .isEqualTo(emptyList())
    }

    @Test
    fun `remove entry drops a resolved store once its address has been recorded in the reorder buffer`() {
        val queue = expectThat(
            RealMemoryBufferQueue(Size(2)).enqueue(
                StoreMemoryBufferOperation(StoreWordOperation),
                ReadyOperand(Word(4u)),
                ReadyOperand(Word(9u)),
                Word(8u),
                RobId(1)
            )
        )
            .isSuccess()
            .isMemoryBufferEnqueueResult()
            .subject

        expectThat(queue.removeEntry(MemoryBufferId(1)).dispatchAddressComputations(1).workItems)
            .isEqualTo(emptyList())
    }

    @Test
    fun `enqueue returns unavailable when the memory buffer is full`() {
        val fullOutcome = expectThat(
            RealMemoryBufferQueue(Size(1))
                .enqueue(
                    LoadMemoryBufferOperation(LoadWordOperation),
                    ReadyOperand(Word(4u)),
                    ReadyOperand(Word(0u)),
                    Word(8u),
                    RobId(1)
                )
        )
            .isSuccess()
            .isMemoryBufferEnqueueResult()
            .get {
                enqueue(
                    LoadMemoryBufferOperation(LoadWordOperation),
                    ReadyOperand(Word(4u)),
                    ReadyOperand(Word(0u)),
                    Word(8u),
                    RobId(2)
                )
            }
            .isSuccess()
            .subject

        expectThat(fullOutcome)
            .isEqualTo(MemoryBufferEnqueueUnavailable)
    }
}
