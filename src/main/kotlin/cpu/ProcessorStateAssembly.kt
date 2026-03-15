package cpu

import commit.CommitCycleDelta
import control.ControlState
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import instructionqueue.InstructionQueue
import instructionqueue.InstructionQueueEntry
import issue.IssueCycleDelta
import types.InstructionAddress
import types.ProcessorResult

internal fun buildTerminalState(
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

internal fun buildContinuingState(
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
                                        control.PredictedProgramCounterSelection
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

internal fun haltedControlState() =
    ControlState(
        InstructionAddress(0),
        control.RedirectedProgramCounterSelection(InstructionAddress(0))
    )

internal fun redirectedControlState(targetInstructionAddress: InstructionAddress) =
    ControlState(
        targetInstructionAddress,
        control.RedirectedProgramCounterSelection(targetInstructionAddress)
    )

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

private fun nextRegisterFile(
    registerFile: registerfile.RegisterFile,
    commitChanges: CommitCycleDelta,
    issueChanges: IssueCycleDelta
) =
    issueChanges.applyToRegisterFile(
        commitChanges.applyToRegisterFile(registerFile)
    )
