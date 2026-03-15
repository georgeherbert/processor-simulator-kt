package cpu

import commondatabus.CommonDataBus
import decoder.InstructionDecoder
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
import mainmemory.MainMemory
import types.InstructionAddress
import types.ProcessorResult

internal fun collectCurrentCycleActivity(state: ProcessorState): ProcessorResult<CurrentCycleActivity> {
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

internal fun collectNextCycleInputs(
    state: ProcessorState,
    configuration: ProcessorConfiguration,
    instructionDecoder: InstructionDecoder,
    issueUnit: IssueUnit
) =
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
            configuration,
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

private fun fetchNextInstructionBatch(
    configuration: ProcessorConfiguration,
    instructionQueue: InstructionQueue,
    mainMemory: MainMemory,
    branchTargetPredictor: branchpredictor.DynamicBranchTargetPredictor,
    startInstructionAddress: InstructionAddress
) =
    fetcherFor(configuration, instructionQueue, mainMemory, branchTargetPredictor)
        .step(startInstructionAddress)

private fun fetcherFor(
    configuration: ProcessorConfiguration,
    instructionQueue: InstructionQueue,
    mainMemory: MainMemory,
    branchTargetPredictor: branchpredictor.DynamicBranchTargetPredictor
): InstructionFetcher =
    RealInstructionFetcher(
        configuration.fetchWidth,
        mainMemory,
        branchTargetPredictor,
        RealInstructionQueueAvailableSlotsSource(configuration.instructionQueueSize, instructionQueue)
    )

private fun writeToCommonDataBus(
    commonDataBus: CommonDataBus,
    writes: List<CommonDataBusWrite>
): ProcessorResult<CommonDataBus> {
    val initialCommonDataBusResult: ProcessorResult<CommonDataBus> = commonDataBus.asSuccess()

    return writes.fold(initialCommonDataBusResult) { commonDataBusResult, write ->
        commonDataBusResult.flatMap { currentCommonDataBus ->
            currentCommonDataBus.write(write.robId, write.value)
        }
    }
}

internal data class CurrentCycleActivity(
    val arithmeticDispatchResult: reservationstation.ReservationStationDispatchResult<arithmeticlogic.ArithmeticLogicOperation>,
    val branchDispatchResult: reservationstation.ReservationStationDispatchResult<branchlogic.BranchOperation>,
    val memoryDispatchResult: memorybuffer.MemoryBufferLoadDispatchResult,
    val arithmeticLogicUnitStepResult: arithmeticlogic.ArithmeticLogicUnitSetStepResult,
    val branchUnitStepResult: branchlogic.BranchUnitSetStepResult,
    val addressUnitStepResult: address.AddressUnitSetStepResult,
    val memoryUnitStepResult: memoryaccess.MemoryUnitSetStepResult,
    val commonDataBus: CommonDataBus
)

internal data class NextCycleInputs(
    val issueChanges: IssueCycleDelta,
    val fetchStepResult: FetchStepResult
)
