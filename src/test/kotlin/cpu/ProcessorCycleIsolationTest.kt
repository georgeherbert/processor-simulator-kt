package cpu

import arithmeticlogic.AddImmediate
import arithmeticlogic.ArithmeticLogicOperation
import arithmeticlogic.RealArithmeticLogicUnit
import arithmeticlogic.RealArithmeticLogicUnitSet
import commit.CallbackCommitUnitStub
import commit.CommitCycleDelta
import commit.NoCommitControlEvent
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import decoder.RealInstructionDecoder
import dev.forkhandles.result4k.asSuccess
import fetch.FetchWidth
import instructionqueue.InstructionQueueEntry
import instructionqueue.RealInstructionQueue
import issue.CallbackIssueUnitStub
import issue.IssueCycleDelta
import mainmemory.StubMainMemory
import org.junit.jupiter.api.Test
import reorderbuffer.ArithmeticLogicRegisterWriteReorderBufferEntryCategory
import reorderbuffer.RealReorderBuffer
import commondatabus.StubCommonDataBus
import reservationstation.ReadyReservationStationEntry
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*

class ProcessorCycleIsolationTest {

    @Test
    fun `fetch does not use instruction queue capacity freed by issue in the same cycle`() {
        val configuration = testProcessorConfiguration().copy(
            fetchWidth = FetchWidth(1),
            instructionQueueSize = Size(1)
        )
        val mainMemory = expectThat(
            StubMainMemory().storeWord(0, Word(0u))
        )
            .isSuccess()
            .subject
        val initialState = expectThat(
            RealProcessorFactory.create(
                configuration,
                mainMemory,
                Size(64),
                InstructionAddress(0)
            )
        )
            .isSuccess()
            .subject
            .copy(
                instructionQueue = expectThat(
                    RealInstructionQueue(Size(1)).enqueue(
                        InstructionQueueEntry(
                            Word(0u),
                            InstructionAddress(12),
                            InstructionAddress(16)
                        )
                    )
                )
                    .isSuccess()
                    .subject
            )
        val processor = RealProcessor(
            configuration,
            RealInstructionDecoder,
            CallbackIssueUnitStub { _, _, _, _, _, _, _ ->
                IssueCycleDelta(
                    1,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    emptyList()
                ).asSuccess()
            },
            CallbackCommitUnitStub { _, _, _, _ ->
                CommitCycleDelta.none().asSuccess()
            }
        )

        val nextState = expectThat(processor.step(initialState))
            .isSuccess()
            .subject

        expectThat(nextState.instructionQueue.entryCount())
            .isEqualTo(0)

        expectThat(nextState.controlState.fetchInstructionAddress)
            .isEqualTo(InstructionAddress(0))
    }

    @Test
    fun `issue does not use reorder buffer capacity freed by commit in the same cycle`() {
        val configuration = testProcessorConfiguration().copy(
            fetchWidth = FetchWidth(0),
            reorderBufferSize = Size(1)
        )
        val initialState = expectThat(
            RealProcessorFactory.create(
                configuration,
                StubMainMemory(),
                Size(64),
                InstructionAddress(0)
            )
        )
            .isSuccess()
            .subject
            .copy(reorderBuffer = readySingleEntryReorderBuffer())
        val processor = RealProcessor(
            configuration,
            RealInstructionDecoder,
            CallbackIssueUnitStub { _, _, _, reorderBuffer, _, _, _ ->
                when (reorderBuffer.enqueueRegisterWrite(RegisterAddress(5), ArithmeticLogicRegisterWriteReorderBufferEntryCategory)) {
                    is Failure -> IssueCycleDelta.none().asSuccess()
                    is Success ->
                        IssueCycleDelta(
                            0,
                            emptyList(),
                            listOf(
                                IssueCycleDelta.RegisterWriteAllocation(
                                    RegisterAddress(5),
                                    ArithmeticLogicRegisterWriteReorderBufferEntryCategory
                                )
                            ),
                            emptyList(),
                            emptyList(),
                            emptyList()
                        ).asSuccess()
                }
            },
            CallbackCommitUnitStub { _, _, _, _ ->
                CommitCycleDelta(
                    1,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    NoCommitControlEvent,
                    commit.CommitStatisticsDelta(1, 0, 0),
                    false
                ).asSuccess()
            }
        )

        val nextState = expectThat(processor.step(initialState))
            .isSuccess()
            .subject

        expectThat(nextState.reorderBuffer.commitReadyHead())
            .isFailure()
            .isEqualTo(ReorderBufferEmpty)
    }

    @Test
    fun `newly issued entries observe same cycle common data bus values in next state`() {
        val configuration = testProcessorConfiguration().copy(
            fetchWidth = FetchWidth(0),
            issueWidth = IssueWidth(1),
            arithmeticLogicUnitCount = Size(1),
            arithmeticLogicReservationStationCount = Size(2),
            reorderBufferSize = Size(2)
        )
        val initialState = expectThat(
            RealProcessorFactory.create(
                configuration,
                StubMainMemory(),
                Size(64),
                InstructionAddress(0)
            )
        )
            .isSuccess()
            .subject
        val producerAllocation = expectThat(
            RealReorderBuffer(Size(2)).enqueueRegisterWrite(
                RegisterAddress(1),
                ArithmeticLogicRegisterWriteReorderBufferEntryCategory
            )
        )
            .isSuccess()
            .subject
        val instructionQueue = expectThat(
            RealInstructionQueue(Size(2)).enqueue(
                InstructionQueueEntry(
                    Word(0x00108113u),
                    InstructionAddress(0),
                    InstructionAddress(4)
                )
            )
        )
            .isSuccess()
            .subject
        val arithmeticLogicUnitSet = RealArithmeticLogicUnitSet(
            Size(1),
            RealArithmeticLogicUnit,
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
        ).step(
            listOf(
                ReadyReservationStationEntry(
                    ReservationStationId(1),
                    AddImmediate,
                    Word(0u),
                    Word(1u),
                    Word(0u),
                    producerAllocation.robId,
                    InstructionAddress(0)
                )
            )
        ).arithmeticLogicUnitSet
        val processor = RealProcessor(
            configuration,
            RealInstructionDecoder,
            issue.RealIssueUnit(IssueWidth(1)),
            CallbackCommitUnitStub { _, _, _, _ ->
                CommitCycleDelta.none().asSuccess()
            }
        )
        val stateWithProducerInFlight = initialState.copy(
            instructionQueue = instructionQueue,
            registerFile = initialState.registerFile.reserveDestination(RegisterAddress(1), producerAllocation.robId),
            reorderBuffer = producerAllocation.reorderBuffer,
            arithmeticLogicUnitSet = arithmeticLogicUnitSet
        )

        val nextState = expectThat(processor.step(stateWithProducerInFlight))
            .isSuccess()
            .subject
        val stateAfterDispatchOpportunity = expectThat(processor.step(nextState))
            .isSuccess()
            .subject

        expectThat(
            nextState.arithmeticLogicReservationStations
                .acceptCommonDataBus(StubCommonDataBus(producerAllocation.robId, Word(1u)))
                .dispatchReady(1)
                .entries
        )
            .isEqualTo(
                listOf(
                    arithmeticReadyEntry(
                        RobId(2)
                    )
                )
            )

        expectThat(stateAfterDispatchOpportunity.arithmeticLogicReservationStations.dispatchReady(1).entries)
            .isEqualTo(emptyList())
    }

    private fun readySingleEntryReorderBuffer() =
        expectThat(
            RealReorderBuffer(Size(1)).enqueueBranch(
                InstructionAddress(0),
                InstructionAddress(4)
            )
        )
            .isSuccess()
            .subject
            .let { allocationResult ->
                allocationResult.reorderBuffer.recordBranchActualNextInstructionAddress(
                    allocationResult.robId,
                    InstructionAddress(4)
                )
            }

    private fun arithmeticReadyEntry(robId: RobId): ReadyReservationStationEntry<ArithmeticLogicOperation> =
        ReadyReservationStationEntry(
            ReservationStationId(1),
            AddImmediate,
            Word(1u),
            Word(1u),
            Word(0u),
            robId,
            InstructionAddress(0)
        )
}
