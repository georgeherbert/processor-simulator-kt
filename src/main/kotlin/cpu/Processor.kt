package cpu

import commit.CommitCycleDelta
import commit.CommitUnit
import commit.RealCommitUnit
import commit.RedirectCommitControlEvent
import decoder.InstructionDecoder
import decoder.RealInstructionDecoder
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import fetch.InstructionFetcher
import fetch.RealInstructionFetcher
import instructionqueue.InstructionQueue
import instructionqueue.InstructionQueueEntry
import instructionqueue.RealInstructionQueueAvailableSlotsSource
import issue.IssueCycleDelta
import issue.IssueUnit
import issue.RealIssueUnit
import types.InstructionAddress
import types.ProcessorAlreadyHalted
import types.ProcessorResult

interface Processor {
    fun step(state: ProcessorState): ProcessorResult<ProcessorState>
}

data class RealProcessor(
    private val configuration: ProcessorConfiguration,
    private val instructionDecoder: InstructionDecoder,
    private val issueUnit: IssueUnit,
    private val commitUnit: CommitUnit
) : Processor {

    constructor(configuration: ProcessorConfiguration) : this(
        configuration,
        RealInstructionDecoder,
        RealIssueUnit(configuration.issueWidth),
        RealCommitUnit(configuration.commitWidth)
    )

    override fun step(state: ProcessorState) =
        when (state.halted) {
            true -> ProcessorAlreadyHalted.asFailure()
            false -> stepActiveProcessor(state)
        }

    private fun stepActiveProcessor(state: ProcessorState) =
        commitUnit
            .nextCycleDelta(
                state.reorderBuffer,
                state.registerFile,
                state.mainMemory,
                state.branchTargetPredictor
            )
            .flatMap { commitChanges ->
                val updatedStatistics = state.statistics.updatedWith(commitChanges.statisticsDelta)
                when {
                    commitChanges.halted ->
                        haltedState(
                            state,
                            commitChanges,
                            updatedStatistics
                        )

                    commitChanges.controlEvent is RedirectCommitControlEvent ->
                        flushedState(
                            state,
                            commitChanges,
                            updatedStatistics,
                            commitChanges.controlEvent
                        )

                    else ->
                        continueActiveCycle(
                            state,
                            commitChanges,
                            updatedStatistics
                        )
                }
            }

    private fun haltedState(
        state: ProcessorState,
        commitChanges: CommitCycleDelta,
        updatedStatistics: ProcessorStatistics
    ) =
        commitChanges.applyToMainMemory(state.mainMemory)
            .map { committedMainMemory ->
                ProcessorState(
                    control.ControlState(
                        InstructionAddress(0),
                        control.RedirectedProgramCounterSelection(InstructionAddress(0))
                    ),
                    updatedStatistics,
                    true,
                    commitChanges.applyToBranchTargetPredictor(state.branchTargetPredictor),
                    state.commonDataBus.clear(),
                    committedMainMemory,
                    state.instructionQueue.clear(),
                    commitChanges.applyToRegisterFile(state.registerFile).flushPendingDestinations(),
                    state.reorderBuffer.clear(),
                    state.arithmeticLogicReservationStations.clear(),
                    state.branchReservationStations.clear(),
                    state.memoryBufferQueue.clear(),
                    state.arithmeticLogicUnitSet.clear(),
                    state.branchUnitSet.clear(),
                    state.addressUnitSet.clear(),
                    state.memoryUnitSet.clear()
                )
            }

    private fun flushedState(
        state: ProcessorState,
        commitChanges: CommitCycleDelta,
        updatedStatistics: ProcessorStatistics,
        controlEvent: RedirectCommitControlEvent
    ) =
        commitChanges.applyToMainMemory(state.mainMemory)
            .map { committedMainMemory ->
                ProcessorState(
                    control.ControlState(
                        controlEvent.targetInstructionAddress,
                        control.RedirectedProgramCounterSelection(controlEvent.targetInstructionAddress)
                    ),
                    updatedStatistics,
                    false,
                    commitChanges.applyToBranchTargetPredictor(state.branchTargetPredictor),
                    state.commonDataBus.clear(),
                    committedMainMemory,
                    state.instructionQueue.clear(),
                    commitChanges.applyToRegisterFile(state.registerFile).flushPendingDestinations(),
                    state.reorderBuffer.clear(),
                    state.arithmeticLogicReservationStations.clear(),
                    state.branchReservationStations.clear(),
                    state.memoryBufferQueue.clear(),
                    state.arithmeticLogicUnitSet.clear(),
                    state.branchUnitSet.clear(),
                    state.addressUnitSet.clear(),
                    state.memoryUnitSet.clear()
                )
            }

    private fun continueActiveCycle(
        state: ProcessorState,
        commitChanges: CommitCycleDelta,
        updatedStatistics: ProcessorStatistics
    ): ProcessorResult<ProcessorState> {
        val arithmeticDispatchResult = state.arithmeticLogicReservationStations.dispatchReady(
            state.arithmeticLogicUnitSet.availableLaneCount()
        )
        val branchDispatchResult = state.branchReservationStations.dispatchReady(
            state.branchUnitSet.availableLaneCount()
        )
        val addressDispatchResult = state.memoryBufferQueue.dispatchAddressComputations(
            state.addressUnitSet.availableLaneCount()
        )
        val memoryDispatchResult = addressDispatchResult.memoryBufferQueue.dispatchMemoryLoads(
            state.memoryUnitSet.availableLaneCount(),
            state.reorderBuffer
        )
        val arithmeticLogicUnitStepResult = state.arithmeticLogicUnitSet.step(arithmeticDispatchResult.entries)
        val branchUnitStepResult = state.branchUnitSet.step(branchDispatchResult.entries)
        val addressUnitStepResult = state.addressUnitSet.step(addressDispatchResult.workItems)

        return state.memoryUnitSet.step(state.mainMemory, memoryDispatchResult.workItems)
            .flatMap { memoryUnitStepResult ->
                writeToCommonDataBus(
                    state.commonDataBus.clear(),
                    arithmeticLogicUnitStepResult.commonDataBusWrites +
                            branchUnitStepResult.commonDataBusWrites +
                            memoryUnitStepResult.commonDataBusWrites
                )
                    .flatMap { commonDataBus ->
                        issueUnit.nextCycleDelta(
                            state.instructionQueue,
                            instructionDecoder,
                            state.registerFile,
                            state.reorderBuffer,
                            state.arithmeticLogicReservationStations,
                            state.branchReservationStations,
                            state.memoryBufferQueue
                        )
                            .flatMap { issueChanges ->
                                fetchNextInstructionBatch(
                                    state.instructionQueue,
                                    state.mainMemory,
                                    state.branchTargetPredictor,
                                    state.controlState.fetchInstructionAddress
                                )
                                    .flatMap { fetchStepResult ->
                                        nextInstructionQueue(state.instructionQueue, issueChanges, fetchStepResult.fetchedInstructions)
                                            .flatMap { instructionQueue ->
                                                nextReorderBuffer(
                                                    state.reorderBuffer,
                                                    commitChanges,
                                                    commonDataBus,
                                                    branchUnitStepResult.branchResolutions,
                                                    addressUnitStepResult.addressResolutions,
                                                    issueChanges
                                                )
                                                    .flatMap { reorderBuffer ->
                                                        nextArithmeticLogicReservationStations(
                                                            arithmeticDispatchResult.reservationStationBank,
                                                            commonDataBus,
                                                            issueChanges
                                                        )
                                                            .flatMap { arithmeticLogicReservationStations ->
                                                                nextBranchReservationStations(
                                                                    branchDispatchResult.reservationStationBank,
                                                                    commonDataBus,
                                                                    issueChanges
                                                                )
                                                                    .flatMap { branchReservationStations ->
                                                                        nextMemoryBufferQueue(
                                                                            memoryDispatchResult.memoryBufferQueue,
                                                                            addressUnitStepResult.addressResolutions,
                                                                            commonDataBus,
                                                                            issueChanges
                                                                        )
                                                                            .flatMap { memoryBufferQueue ->
                                                                                commitChanges.applyToMainMemory(state.mainMemory)
                                                                                    .map { mainMemory ->
                                                                                        ProcessorState(
                                                                                            control.ControlState(
                                                                                                fetchStepResult.nextInstructionAddress,
                                                                                                control.PredictedProgramCounterSelection
                                                                                            ),
                                                                                            updatedStatistics,
                                                                                            false,
                                                                                            commitChanges.applyToBranchTargetPredictor(state.branchTargetPredictor),
                                                                                            commonDataBus,
                                                                                            mainMemory,
                                                                                            instructionQueue,
                                                                                            issueChanges.applyToRegisterFile(
                                                                                                commitChanges.applyToRegisterFile(state.registerFile)
                                                                                            ),
                                                                                            reorderBuffer,
                                                                                            arithmeticLogicReservationStations,
                                                                                            branchReservationStations,
                                                                                            memoryBufferQueue,
                                                                                            arithmeticLogicUnitStepResult.arithmeticLogicUnitSet,
                                                                                            branchUnitStepResult.branchUnitSet,
                                                                                            addressUnitStepResult.addressUnitSet,
                                                                                            memoryUnitStepResult.memoryUnitSet
                                                                                        )
                                                                                    }
                                                                            }
                                                                    }
                                                            }
                                                    }
                                            }
                                    }
                            }
                    }
            }
    }

    private fun nextInstructionQueue(
        instructionQueue: InstructionQueue,
        issueChanges: IssueCycleDelta,
        fetchedInstructions: List<InstructionQueueEntry>
    ) =
        issueChanges.applyToInstructionQueue(instructionQueue)
            .flatMap { dequeuedInstructionQueue ->
                enqueueFetchedInstructions(dequeuedInstructionQueue, fetchedInstructions)
            }

    private fun nextReorderBuffer(
        reorderBuffer: reorderbuffer.ReorderBuffer,
        commitChanges: CommitCycleDelta,
        commonDataBus: commondatabus.CommonDataBus,
        branchResolutions: List<BranchResolution>,
        addressResolutions: List<AddressResolution>,
        issueChanges: IssueCycleDelta
    ): ProcessorResult<reorderbuffer.ReorderBuffer> =
        commitChanges.applyToReorderBuffer(reorderBuffer)
            .map { reorderBufferAfterCommit ->
                branchResolutions.fold(reorderBufferAfterCommit) { currentReorderBuffer, branchResolution ->
                    currentReorderBuffer.recordBranchActualNextInstructionAddress(
                        branchResolution.robId,
                        branchResolution.actualNextInstructionAddress
                    )
                }
            }
            .map { reorderBufferAfterBranchResolutions ->
                addressResolutions.fold(reorderBufferAfterBranchResolutions) { currentReorderBuffer, addressResolution ->
                    when (addressResolution) {
                        is StoreAddressResolution ->
                            currentReorderBuffer.recordStoreAddress(
                                addressResolution.robId,
                                addressResolution.address
                            )

                        is LoadAddressResolution -> currentReorderBuffer
                    }
                }
            }
            .map { reorderBufferAfterAddressResolutions ->
                reorderBufferAfterAddressResolutions
            }
            .flatMap { reorderBufferAfterAddressResolutions ->
                issueChanges.applyToReorderBuffer(reorderBufferAfterAddressResolutions)
            }
            .map { reorderBufferAfterIssue ->
                reorderBufferAfterIssue.acceptCommonDataBus(commonDataBus)
            }

    private fun nextArithmeticLogicReservationStations(
        reservationStationBank: reservationstation.ReservationStationBank<arithmeticlogic.ArithmeticLogicOperation>,
        commonDataBus: commondatabus.CommonDataBus,
        issueChanges: IssueCycleDelta
    ): ProcessorResult<reservationstation.ReservationStationBank<arithmeticlogic.ArithmeticLogicOperation>> =
        issueChanges.applyToArithmeticLogicReservationStations(reservationStationBank)
            .map { reservationStationBankAfterIssue ->
                reservationStationBankAfterIssue.acceptCommonDataBus(commonDataBus)
            }

    private fun nextBranchReservationStations(
        reservationStationBank: reservationstation.ReservationStationBank<branchlogic.BranchOperation>,
        commonDataBus: commondatabus.CommonDataBus,
        issueChanges: IssueCycleDelta
    ): ProcessorResult<reservationstation.ReservationStationBank<branchlogic.BranchOperation>> =
        issueChanges.applyToBranchReservationStations(reservationStationBank)
            .map { reservationStationBankAfterIssue ->
                reservationStationBankAfterIssue.acceptCommonDataBus(commonDataBus)
            }

    private fun nextMemoryBufferQueue(
        memoryBufferQueue: memorybuffer.MemoryBufferQueue,
        addressResolutions: List<AddressResolution>,
        commonDataBus: commondatabus.CommonDataBus,
        issueChanges: IssueCycleDelta
    ): ProcessorResult<memorybuffer.MemoryBufferQueue> =
        issueChanges.applyToMemoryBufferQueue(
            addressResolutions.fold(memoryBufferQueue) { currentMemoryBufferQueue, addressResolution ->
                when (addressResolution) {
                    is StoreAddressResolution -> currentMemoryBufferQueue
                        .removeEntry(addressResolution.memoryBufferId)

                    is LoadAddressResolution ->
                        currentMemoryBufferQueue.recordComputedAddress(
                            addressResolution.memoryBufferId,
                            addressResolution.address
                        )
                }
            }
        ).map { memoryBufferQueueAfterIssue ->
            memoryBufferQueueAfterIssue.acceptCommonDataBus(commonDataBus)
        }

    private fun fetchNextInstructionBatch(
        instructionQueue: InstructionQueue,
        mainMemory: mainmemory.MainMemory,
        branchTargetPredictor: branchpredictor.DynamicBranchTargetPredictor,
        startInstructionAddress: types.InstructionAddress
    ) =
        fetcherFor(instructionQueue, mainMemory, branchTargetPredictor)
            .step(startInstructionAddress)

    private fun fetcherFor(
        instructionQueue: InstructionQueue,
        mainMemory: mainmemory.MainMemory,
        branchTargetPredictor: branchpredictor.DynamicBranchTargetPredictor
    ): InstructionFetcher =
        RealInstructionFetcher(
            configuration.fetchWidth,
            mainMemory,
            branchTargetPredictor,
            RealInstructionQueueAvailableSlotsSource(configuration.instructionQueueSize, instructionQueue)
        )

    private fun enqueueFetchedInstructions(
        instructionQueue: InstructionQueue,
        fetchedInstructions: List<InstructionQueueEntry>
    ): ProcessorResult<InstructionQueue> {
        val initialInstructionQueueResult: ProcessorResult<InstructionQueue> = instructionQueue.asSuccess()

        return fetchedInstructions.fold(initialInstructionQueueResult) { instructionQueueResult, fetchedInstruction ->
            instructionQueueResult.flatMap { currentInstructionQueue ->
                currentInstructionQueue.enqueue(fetchedInstruction)
            }
        }
    }

    private fun writeToCommonDataBus(
        commonDataBus: commondatabus.CommonDataBus,
        writes: List<CommonDataBusWrite>
    ): ProcessorResult<commondatabus.CommonDataBus> {
        val initialCommonDataBusResult: ProcessorResult<commondatabus.CommonDataBus> = commonDataBus.asSuccess()

        return writes.fold(initialCommonDataBusResult) { commonDataBusResult, write ->
            commonDataBusResult.flatMap { currentCommonDataBus ->
                currentCommonDataBus.write(write.robId, write.value)
            }
        }
    }
}
