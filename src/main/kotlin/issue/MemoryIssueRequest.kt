package issue

import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import memorybuffer.LoadMemoryBufferOperation
import memorybuffer.MemoryBufferEnqueueResult
import memorybuffer.MemoryBufferEnqueueUnavailable
import memorybuffer.StoreMemoryBufferOperation
import reorderbuffer.LoadRegisterWriteReorderBufferEntryCategory
import reorderbuffer.ReorderBufferAllocationResult
import reorderbuffer.ReorderBufferAllocationUnavailable
import types.Operand
import types.ProcessorResult
import types.ReadyOperand
import types.RegisterAddress
import types.RobId
import types.Word

internal data class LoadIssueRequest(
    val operation: decoder.LoadOperation,
    val baseOperand: Operand,
    val immediate: Word,
    val destinationRegisterAddress: RegisterAddress
) : IssueRequest {

    override fun applyTo(workingState: IssueWorkingState): ProcessorResult<IssueAttemptOutcome> =
        workingState.reorderBuffer.enqueueRegisterWrite(
            destinationRegisterAddress,
            LoadRegisterWriteReorderBufferEntryCategory
        ).flatMap { allocationOutcome ->
            when (allocationOutcome) {
                ReorderBufferAllocationUnavailable -> IssueBackpressured.asSuccess()
                is ReorderBufferAllocationResult ->
                    workingState.memoryBufferQueue.enqueue(
                        LoadMemoryBufferOperation(operation),
                        baseOperand,
                        ReadyOperand(zeroWord()),
                        immediate,
                        allocationOutcome.robId
                    ).map { enqueueOutcome ->
                        when (enqueueOutcome) {
                            MemoryBufferEnqueueUnavailable -> IssueBackpressured
                            is MemoryBufferEnqueueResult ->
                                IssueApplied(
                                    workingState.copy(
                                        registerFile = workingState.registerFile.reserveDestination(
                                            destinationRegisterAddress,
                                            allocationOutcome.robId
                                        ),
                                        reorderBuffer = allocationOutcome.reorderBuffer,
                                        memoryBufferQueue = enqueueOutcome.memoryBufferQueue,
                                        cycleChanges = workingState.cycleChanges.mergedWith(cycleDeltaFor(allocationOutcome.robId))
                                    )
                                )
                        }
                    }
            }
        }

    private fun cycleDeltaFor(robId: RobId) =
        IssueCycleDelta(
            1,
            listOf(IssueCycleDelta.IssueRegisterReservation(destinationRegisterAddress, robId)),
            listOf(
                IssueCycleDelta.RegisterWriteAllocation(
                    destinationRegisterAddress,
                    LoadRegisterWriteReorderBufferEntryCategory
                )
            ),
            emptyList(),
            emptyList(),
            listOf(
                IssueCycleDelta.MemoryBufferEnqueue(
                    LoadMemoryBufferOperation(operation),
                    baseOperand,
                    ReadyOperand(zeroWord()),
                    immediate,
                    robId
                )
            )
        )
}

internal data class StoreIssueRequest(
    val operation: decoder.StoreOperation,
    val baseOperand: Operand,
    val valueOperand: Operand,
    val immediate: Word
) : IssueRequest {

    override fun applyTo(workingState: IssueWorkingState): ProcessorResult<IssueAttemptOutcome> =
        workingState.reorderBuffer.enqueueStore(operation, valueOperand)
            .flatMap { allocationOutcome ->
                when (allocationOutcome) {
                    ReorderBufferAllocationUnavailable -> IssueBackpressured.asSuccess()
                    is ReorderBufferAllocationResult ->
                        workingState.memoryBufferQueue.enqueue(
                            StoreMemoryBufferOperation(operation),
                            baseOperand,
                            valueOperand,
                            immediate,
                            allocationOutcome.robId
                        ).map { enqueueOutcome ->
                            when (enqueueOutcome) {
                                MemoryBufferEnqueueUnavailable -> IssueBackpressured
                                is MemoryBufferEnqueueResult ->
                                    IssueApplied(
                                        workingState.copy(
                                            reorderBuffer = allocationOutcome.reorderBuffer,
                                            memoryBufferQueue = enqueueOutcome.memoryBufferQueue,
                                            cycleChanges = workingState.cycleChanges.mergedWith(cycleDeltaFor(allocationOutcome.robId))
                                        )
                                    )
                            }
                        }
                }
            }

    private fun cycleDeltaFor(robId: RobId) =
        IssueCycleDelta(
            1,
            emptyList(),
            listOf(IssueCycleDelta.StoreAllocation(operation, valueOperand)),
            emptyList(),
            emptyList(),
            listOf(
                IssueCycleDelta.MemoryBufferEnqueue(
                    StoreMemoryBufferOperation(operation),
                    baseOperand,
                    valueOperand,
                    immediate,
                    robId
                )
            )
        )
}

private fun zeroWord() = Word(0u)
