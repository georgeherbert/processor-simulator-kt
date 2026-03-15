package cpu

import commit.CommitCycleDelta
import commit.CommitUnit
import commit.RealCommitUnit
import commit.RedirectCommitControlEvent
import control.ControlState
import control.PredictedProgramCounterSelection
import control.RedirectedProgramCounterSelection
import decoder.InstructionDecoder
import decoder.RealInstructionDecoder
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import fetch.FetchStepResult
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
                        terminalState(
                            state,
                            commitChanges,
                            updatedStatistics,
                            haltedControlState(),
                            true
                        )

                    commitChanges.controlEvent is RedirectCommitControlEvent ->
                        terminalState(
                            state,
                            commitChanges,
                            updatedStatistics,
                            redirectedControlState(commitChanges.controlEvent.targetInstructionAddress),
                            false
                        )

                    else ->
                        continueActiveCycle(
                            state,
                            commitChanges,
                            updatedStatistics
                        )
                }
            }

    private fun terminalState(
        state: ProcessorState,
        commitChanges: CommitCycleDelta,
        updatedStatistics: ProcessorStatistics,
        controlState: ControlState,
        halted: Boolean
    ) =
        commitChanges.applyToMainMemory(state.mainMemory)
            .map { committedMainMemory ->
                ProcessorState(
                    controlState = controlState,
                    statistics = updatedStatistics,
                    halted = halted,
                    branchTargetPredictor = commitChanges.applyToBranchTargetPredictor(state.branchTargetPredictor),
                    commonDataBus = state.commonDataBus.clear(),
                    mainMemory = committedMainMemory,
                    instructionQueue = state.instructionQueue.clear(),
                    registerFile = commitChanges.applyToRegisterFile(state.registerFile).flushPendingDestinations(),
                    reorderBuffer = state.reorderBuffer.clear(),
                    arithmeticLogicReservationStations = state.arithmeticLogicReservationStations.clear(),
                    branchReservationStations = state.branchReservationStations.clear(),
                    memoryBufferQueue = state.memoryBufferQueue.clear(),
                    arithmeticLogicUnitSet = state.arithmeticLogicUnitSet.clear(),
                    branchUnitSet = state.branchUnitSet.clear(),
                    addressUnitSet = state.addressUnitSet.clear(),
                    memoryUnitSet = state.memoryUnitSet.clear()
                )
            }

    private fun continueActiveCycle(
        state: ProcessorState,
        commitChanges: CommitCycleDelta,
        updatedStatistics: ProcessorStatistics
    ) =
        collectCurrentCycleActivity(state)
            .flatMap { currentCycleActivity ->
                collectNextCycleInputs(state)
                    .flatMap { nextCycleInputs ->
                        assembleContinuingState(
                            state,
                            commitChanges,
                            updatedStatistics,
                            currentCycleActivity,
                            nextCycleInputs
                        )
                    }
            }

    private fun collectCurrentCycleActivity(state: ProcessorState): ProcessorResult<CurrentCycleActivity> {
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
                ).map { commonDataBus ->
                    CurrentCycleActivity(
                        arithmeticDispatchResult,
                        branchDispatchResult,
                        memoryDispatchResult,
                        arithmeticLogicUnitStepResult,
                        branchUnitStepResult,
                        addressUnitStepResult,
                        memoryUnitStepResult,
                        commonDataBus
                    )
                }
            }
    }

    private fun collectNextCycleInputs(state: ProcessorState) =
        issueUnit.nextCycleDelta(
            state.instructionQueue,
            instructionDecoder,
            state.registerFile,
            state.reorderBuffer,
            state.arithmeticLogicReservationStations,
            state.branchReservationStations,
            state.memoryBufferQueue
        ).flatMap { issueChanges ->
            fetchNextInstructionBatch(
                state.instructionQueue,
                state.mainMemory,
                state.branchTargetPredictor,
                state.controlState.fetchInstructionAddress
            ).map { fetchStepResult ->
                NextCycleInputs(
                    issueChanges,
                    fetchStepResult
                )
            }
        }

    private fun assembleContinuingState(
        state: ProcessorState,
        commitChanges: CommitCycleDelta,
        updatedStatistics: ProcessorStatistics,
        currentCycleActivity: CurrentCycleActivity,
        nextCycleInputs: NextCycleInputs
    ): ProcessorResult<ProcessorState> =
        nextInstructionQueue(
            state.instructionQueue,
            nextCycleInputs.issueChanges,
            nextCycleInputs.fetchStepResult.fetchedInstructions
        ).flatMap { instructionQueue ->
            nextReorderBuffer(
                state.reorderBuffer,
                commitChanges,
                currentCycleActivity.commonDataBus,
                currentCycleActivity.branchUnitStepResult.branchResolutions,
                currentCycleActivity.addressUnitStepResult.addressResolutions,
                nextCycleInputs.issueChanges
            ).flatMap { reorderBuffer ->
                nextArithmeticLogicReservationStations(
                    currentCycleActivity.arithmeticDispatchResult.reservationStationBank,
                    currentCycleActivity.commonDataBus,
                    nextCycleInputs.issueChanges
                ).flatMap { arithmeticLogicReservationStations ->
                    nextBranchReservationStations(
                        currentCycleActivity.branchDispatchResult.reservationStationBank,
                        currentCycleActivity.commonDataBus,
                        nextCycleInputs.issueChanges
                    ).flatMap { branchReservationStations ->
                        nextMemoryBufferQueue(
                            currentCycleActivity.memoryDispatchResult.memoryBufferQueue,
                            currentCycleActivity.addressUnitStepResult.addressResolutions,
                            currentCycleActivity.commonDataBus,
                            nextCycleInputs.issueChanges
                        ).flatMap { memoryBufferQueue ->
                            commitChanges.applyToMainMemory(state.mainMemory)
                                .map { mainMemory ->
                                    ProcessorState(
                                        controlState = ControlState(
                                            nextCycleInputs.fetchStepResult.nextInstructionAddress,
                                            PredictedProgramCounterSelection
                                        ),
                                        statistics = updatedStatistics,
                                        halted = false,
                                        branchTargetPredictor = commitChanges.applyToBranchTargetPredictor(state.branchTargetPredictor),
                                        commonDataBus = currentCycleActivity.commonDataBus,
                                        mainMemory = mainMemory,
                                        instructionQueue = instructionQueue,
                                        registerFile = nextRegisterFile(
                                            state.registerFile,
                                            commitChanges,
                                            nextCycleInputs.issueChanges
                                        ),
                                        reorderBuffer = reorderBuffer,
                                        arithmeticLogicReservationStations = arithmeticLogicReservationStations,
                                        branchReservationStations = branchReservationStations,
                                        memoryBufferQueue = memoryBufferQueue,
                                        arithmeticLogicUnitSet = currentCycleActivity.arithmeticLogicUnitStepResult.arithmeticLogicUnitSet,
                                        branchUnitSet = currentCycleActivity.branchUnitStepResult.branchUnitSet,
                                        addressUnitSet = currentCycleActivity.addressUnitStepResult.addressUnitSet,
                                        memoryUnitSet = currentCycleActivity.memoryUnitStepResult.memoryUnitSet
                                    )
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

    private fun nextRegisterFile(
        registerFile: registerfile.RegisterFile,
        commitChanges: CommitCycleDelta,
        issueChanges: IssueCycleDelta
    ) =
        issueChanges.applyToRegisterFile(
            commitChanges.applyToRegisterFile(registerFile)
        )

    private fun haltedControlState() =
        ControlState(
            InstructionAddress(0),
            RedirectedProgramCounterSelection(InstructionAddress(0))
        )

    private fun redirectedControlState(targetInstructionAddress: InstructionAddress) =
        ControlState(
            targetInstructionAddress,
            RedirectedProgramCounterSelection(targetInstructionAddress)
        )
}

private data class CurrentCycleActivity(
    val arithmeticDispatchResult: reservationstation.ReservationStationDispatchResult<arithmeticlogic.ArithmeticLogicOperation>,
    val branchDispatchResult: reservationstation.ReservationStationDispatchResult<branchlogic.BranchOperation>,
    val memoryDispatchResult: memorybuffer.MemoryBufferLoadDispatchResult,
    val arithmeticLogicUnitStepResult: arithmeticlogic.ArithmeticLogicUnitSetStepResult,
    val branchUnitStepResult: branchlogic.BranchUnitSetStepResult,
    val addressUnitStepResult: address.AddressUnitSetStepResult,
    val memoryUnitStepResult: memoryaccess.MemoryUnitSetStepResult,
    val commonDataBus: commondatabus.CommonDataBus
)

private data class NextCycleInputs(
    val issueChanges: IssueCycleDelta,
    val fetchStepResult: FetchStepResult
)
