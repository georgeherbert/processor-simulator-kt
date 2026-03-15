package issue

import arithmeticlogic.ArithmeticLogicOperation
import branchlogic.BranchOperation
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import memorybuffer.MemoryBufferEnqueueResult
import memorybuffer.MemoryBufferEnqueueUnavailable
import memorybuffer.MemoryBufferOperation
import memorybuffer.MemoryBufferQueue
import registerfile.RegisterFile
import reorderbuffer.RegisterWriteReorderBufferEntryCategory
import reorderbuffer.ReorderBufferAllocationResult
import reorderbuffer.ReorderBufferAllocationUnavailable
import reorderbuffer.ReorderBuffer
import reservationstation.ReservationStationBank
import reservationstation.ReservationStationEnqueueResult
import reservationstation.ReservationStationEnqueueUnavailable
import types.InstructionAddress
import types.IssueCycleDeltaMemoryBufferEnqueueUnavailable
import types.IssueCycleDeltaReorderBufferAllocationUnavailable
import types.IssueCycleDeltaReservationStationEnqueueUnavailable
import types.Operand
import types.ProcessorResult
import types.RegisterAddress
import types.RobId
import types.Word

data class IssueCycleDelta(
    private val dequeuedInstructionCount: Int,
    private val registerReservations: List<IssueRegisterReservation>,
    private val reorderBufferAllocations: List<ReorderBufferAllocation>,
    private val arithmeticLogicReservationStationEnqueues: List<ReservationStationEnqueue<ArithmeticLogicOperation>>,
    private val branchReservationStationEnqueues: List<ReservationStationEnqueue<BranchOperation>>,
    private val memoryBufferEnqueues: List<MemoryBufferEnqueue>
) {
    fun applyToInstructionQueue(instructionQueue: instructionqueue.InstructionQueue): ProcessorResult<instructionqueue.InstructionQueue> {
        val initialInstructionQueueResult: ProcessorResult<instructionqueue.InstructionQueue> = instructionQueue.asSuccess()

        return (0 until dequeuedInstructionCount).fold(initialInstructionQueueResult) { instructionQueueResult, _ ->
            instructionQueueResult.flatMap { currentInstructionQueue ->
                currentInstructionQueue.dequeue()
                    .map { dequeueResult -> dequeueResult.instructionQueue }
            }
        }
    }

    fun applyToRegisterFile(registerFile: RegisterFile) =
        registerReservations.fold(registerFile) { currentRegisterFile, reservation ->
            currentRegisterFile.reserveDestination(reservation.registerAddress, reservation.robId)
        }

    fun applyToReorderBuffer(reorderBuffer: ReorderBuffer): ProcessorResult<ReorderBuffer> {
        val initialReorderBufferResult: ProcessorResult<ReorderBuffer> = reorderBuffer.asSuccess()

        return reorderBufferAllocations.fold(initialReorderBufferResult) { reorderBufferResult, allocation ->
            reorderBufferResult.flatMap { currentReorderBuffer ->
                allocation.applyTo(currentReorderBuffer)
            }
        }
    }

    fun applyToArithmeticLogicReservationStations(
        reservationStationBank: ReservationStationBank<ArithmeticLogicOperation>
    ): ProcessorResult<ReservationStationBank<ArithmeticLogicOperation>> {
        val initialReservationStationResult: ProcessorResult<ReservationStationBank<ArithmeticLogicOperation>> =
            reservationStationBank.asSuccess()

        return arithmeticLogicReservationStationEnqueues.fold(initialReservationStationResult) {
            reservationStationResult,
            enqueue
        ->
            reservationStationResult.flatMap { currentReservationStationBank ->
                currentReservationStationBank.enqueue(
                    enqueue.operation,
                    enqueue.leftOperand,
                    enqueue.rightOperand,
                    enqueue.immediate,
                    enqueue.robId,
                    enqueue.instructionAddress
                ).flatMap { enqueueOutcome ->
                    when (enqueueOutcome) {
                        is ReservationStationEnqueueResult ->
                            enqueueOutcome.reservationStationBank.asSuccess()

                        ReservationStationEnqueueUnavailable ->
                            IssueCycleDeltaReservationStationEnqueueUnavailable.asFailure()
                    }
                }
            }
        }
    }

    fun applyToBranchReservationStations(
        reservationStationBank: ReservationStationBank<BranchOperation>
    ): ProcessorResult<ReservationStationBank<BranchOperation>> {
        val initialReservationStationResult: ProcessorResult<ReservationStationBank<BranchOperation>> =
            reservationStationBank.asSuccess()

        return branchReservationStationEnqueues.fold(initialReservationStationResult) { reservationStationResult, enqueue ->
            reservationStationResult.flatMap { currentReservationStationBank ->
                currentReservationStationBank.enqueue(
                    enqueue.operation,
                    enqueue.leftOperand,
                    enqueue.rightOperand,
                    enqueue.immediate,
                    enqueue.robId,
                    enqueue.instructionAddress
                ).flatMap { enqueueOutcome ->
                    when (enqueueOutcome) {
                        is ReservationStationEnqueueResult ->
                            enqueueOutcome.reservationStationBank.asSuccess()

                        ReservationStationEnqueueUnavailable ->
                            IssueCycleDeltaReservationStationEnqueueUnavailable.asFailure()
                    }
                }
            }
        }
    }

    fun applyToMemoryBufferQueue(memoryBufferQueue: MemoryBufferQueue): ProcessorResult<MemoryBufferQueue> {
        val initialMemoryBufferQueueResult: ProcessorResult<MemoryBufferQueue> = memoryBufferQueue.asSuccess()

        return memoryBufferEnqueues.fold(initialMemoryBufferQueueResult) { memoryBufferQueueResult, enqueue ->
            memoryBufferQueueResult.flatMap { currentMemoryBufferQueue ->
                currentMemoryBufferQueue.enqueue(
                    enqueue.operation,
                    enqueue.baseOperand,
                    enqueue.valueOperand,
                    enqueue.immediate,
                    enqueue.robId
                ).flatMap { enqueueOutcome ->
                    when (enqueueOutcome) {
                        is MemoryBufferEnqueueResult ->
                            enqueueOutcome.memoryBufferQueue.asSuccess()

                        MemoryBufferEnqueueUnavailable ->
                            IssueCycleDeltaMemoryBufferEnqueueUnavailable.asFailure()
                    }
                }
            }
        }
    }

    fun mergedWith(other: IssueCycleDelta) =
        IssueCycleDelta(
            dequeuedInstructionCount + other.dequeuedInstructionCount,
            registerReservations + other.registerReservations,
            reorderBufferAllocations + other.reorderBufferAllocations,
            arithmeticLogicReservationStationEnqueues + other.arithmeticLogicReservationStationEnqueues,
            branchReservationStationEnqueues + other.branchReservationStationEnqueues,
            memoryBufferEnqueues + other.memoryBufferEnqueues
        )

    companion object {
        fun none() =
            IssueCycleDelta(
                0,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList()
            )
    }

    data class IssueRegisterReservation(
        val registerAddress: RegisterAddress,
        val robId: RobId
    )

    sealed interface ReorderBufferAllocation {
        fun applyTo(reorderBuffer: ReorderBuffer): ProcessorResult<ReorderBuffer>
    }

    data class RegisterWriteAllocation(
        val destinationRegisterAddress: RegisterAddress,
        val category: RegisterWriteReorderBufferEntryCategory
    ) : ReorderBufferAllocation {
        override fun applyTo(reorderBuffer: ReorderBuffer) =
            reorderBuffer
                .enqueueRegisterWrite(destinationRegisterAddress, category)
                .flatMap { allocationOutcome ->
                    when (allocationOutcome) {
                        is ReorderBufferAllocationResult ->
                            allocationOutcome.reorderBuffer.asSuccess()

                        ReorderBufferAllocationUnavailable ->
                            IssueCycleDeltaReorderBufferAllocationUnavailable.asFailure()
                    }
                }
    }

    data class JumpAllocation(
        val destinationRegisterAddress: RegisterAddress,
        val instructionAddress: InstructionAddress,
        val predictedNextInstructionAddress: InstructionAddress
    ) : ReorderBufferAllocation {
        override fun applyTo(reorderBuffer: ReorderBuffer) =
            reorderBuffer
                .enqueueJump(destinationRegisterAddress, instructionAddress, predictedNextInstructionAddress)
                .flatMap { allocationOutcome ->
                    when (allocationOutcome) {
                        is ReorderBufferAllocationResult ->
                            allocationOutcome.reorderBuffer.asSuccess()

                        ReorderBufferAllocationUnavailable ->
                            IssueCycleDeltaReorderBufferAllocationUnavailable.asFailure()
                    }
                }
    }

    data class BranchAllocation(
        val instructionAddress: InstructionAddress,
        val predictedNextInstructionAddress: InstructionAddress
    ) : ReorderBufferAllocation {
        override fun applyTo(reorderBuffer: ReorderBuffer) =
            reorderBuffer
                .enqueueBranch(instructionAddress, predictedNextInstructionAddress)
                .flatMap { allocationOutcome ->
                    when (allocationOutcome) {
                        is ReorderBufferAllocationResult ->
                            allocationOutcome.reorderBuffer.asSuccess()

                        ReorderBufferAllocationUnavailable ->
                            IssueCycleDeltaReorderBufferAllocationUnavailable.asFailure()
                    }
                }
    }

    data class StoreAllocation(
        val operation: decoder.StoreOperation,
        val valueOperand: Operand
    ) : ReorderBufferAllocation {
        override fun applyTo(reorderBuffer: ReorderBuffer) =
            reorderBuffer
                .enqueueStore(operation, valueOperand)
                .flatMap { allocationOutcome ->
                    when (allocationOutcome) {
                        is ReorderBufferAllocationResult ->
                            allocationOutcome.reorderBuffer.asSuccess()

                        ReorderBufferAllocationUnavailable ->
                            IssueCycleDeltaReorderBufferAllocationUnavailable.asFailure()
                    }
                }
    }

    data class ReservationStationEnqueue<T>(
        val operation: T,
        val leftOperand: Operand,
        val rightOperand: Operand,
        val immediate: Word,
        val robId: RobId,
        val instructionAddress: InstructionAddress
    )

    data class MemoryBufferEnqueue(
        val operation: MemoryBufferOperation,
        val baseOperand: Operand,
        val valueOperand: Operand,
        val immediate: Word,
        val robId: RobId
    )
}
