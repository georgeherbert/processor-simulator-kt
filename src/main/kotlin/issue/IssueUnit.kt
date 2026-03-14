package issue

import arithmeticlogic.*
import branchlogic.*
import branchlogic.BranchOperation
import cpu.IssueWidth
import decoder.*
import dev.forkhandles.result4k.*
import instructionqueue.InstructionQueue
import instructionqueue.InstructionQueueDequeueResult
import instructionqueue.InstructionQueueDequeueUnavailable
import memorybuffer.LoadMemoryBufferOperation
import memorybuffer.MemoryBufferOperation
import memorybuffer.MemoryBufferQueue
import memorybuffer.StoreMemoryBufferOperation
import registerfile.RegisterFile
import reorderbuffer.ArithmeticLogicRegisterWriteReorderBufferEntryCategory
import reorderbuffer.LoadRegisterWriteReorderBufferEntryCategory
import reorderbuffer.RegisterWriteReorderBufferEntryCategory
import reorderbuffer.ReorderBuffer
import reservationstation.ReservationStationBank
import types.*

interface IssueUnit {
    fun nextCycleDelta(
        instructionQueue: InstructionQueue,
        instructionDecoder: InstructionDecoder,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ): ProcessorResult<IssueCycleDelta>
}

data class IssueCycleDelta(
    private val dequeuedInstructionCount: Int,
    private val registerReservations: List<IssueRegisterReservation>,
    private val reorderBufferAllocations: List<ReorderBufferAllocation>,
    private val arithmeticLogicReservationStationEnqueues: List<ReservationStationEnqueue<ArithmeticLogicOperation>>,
    private val branchReservationStationEnqueues: List<ReservationStationEnqueue<BranchOperation>>,
    private val memoryBufferEnqueues: List<MemoryBufferEnqueue>
) {
    fun applyToInstructionQueue(instructionQueue: InstructionQueue): ProcessorResult<InstructionQueue> {
        val initialInstructionQueueResult: ProcessorResult<InstructionQueue> = instructionQueue.asSuccess()

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
                )
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
                )
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
                )
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
                .map { allocationResult -> allocationResult.reorderBuffer }
    }

    data class JumpAllocation(
        val destinationRegisterAddress: RegisterAddress,
        val instructionAddress: InstructionAddress,
        val predictedNextInstructionAddress: InstructionAddress
    ) : ReorderBufferAllocation {
        override fun applyTo(reorderBuffer: ReorderBuffer) =
            reorderBuffer
                .enqueueJump(destinationRegisterAddress, instructionAddress, predictedNextInstructionAddress)
                .map { allocationResult -> allocationResult.reorderBuffer }
    }

    data class BranchAllocation(
        val instructionAddress: InstructionAddress,
        val predictedNextInstructionAddress: InstructionAddress
    ) : ReorderBufferAllocation {
        override fun applyTo(reorderBuffer: ReorderBuffer) =
            reorderBuffer
                .enqueueBranch(instructionAddress, predictedNextInstructionAddress)
                .map { allocationResult -> allocationResult.reorderBuffer }
    }

    data class StoreAllocation(
        val operation: StoreOperation,
        val valueOperand: Operand
    ) : ReorderBufferAllocation {
        override fun applyTo(reorderBuffer: ReorderBuffer) =
            reorderBuffer
                .enqueueStore(operation, valueOperand)
                .map { allocationResult -> allocationResult.reorderBuffer }
    }

    data class ReservationStationEnqueue<Operation>(
        val operation: Operation,
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

data class RealIssueUnit(private val issueWidth: IssueWidth) : IssueUnit {

    override fun nextCycleDelta(
        instructionQueue: InstructionQueue,
        instructionDecoder: InstructionDecoder,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ) =
        issueInstructions(
            issueWidth.value,
            instructionDecoder,
            IssueAccumulation(
                instructionQueue,
                instructionDecoder,
                registerFile,
                reorderBuffer,
                arithmeticLogicReservationStations,
                branchReservationStations,
                memoryBufferQueue,
                IssueCycleDelta.none()
            )
        )

    private fun issueInstructions(
        remainingIssueSlots: Int,
        instructionDecoder: InstructionDecoder,
        accumulation: IssueAccumulation
    ): ProcessorResult<IssueCycleDelta> {
        if (remainingIssueSlots == 0) {
            return accumulation.cycleChanges.asSuccess()
        }

        return when (val dequeueOutcome = accumulation.instructionQueue.dequeueIfPresent()) {
            InstructionQueueDequeueUnavailable -> accumulation.cycleChanges.asSuccess()
            is InstructionQueueDequeueResult -> issueDequeuedInstruction(
                remainingIssueSlots,
                instructionDecoder,
                accumulation,
                dequeueOutcome
            )
        }
    }

    private fun issueDequeuedInstruction(
        remainingIssueSlots: Int,
        instructionDecoder: InstructionDecoder,
        accumulation: IssueAccumulation,
        dequeueResult: InstructionQueueDequeueResult
    ): ProcessorResult<IssueCycleDelta> {
        val queueEntry = dequeueResult.entry
        val decodedInstructionResult = instructionDecoder.decode(
            queueEntry.instruction,
            queueEntry.instructionAddress.asWord(),
            queueEntry.predictedNextInstructionAddress.asWord()
        )

        return when (decodedInstructionResult) {
            is Failure -> decodedInstructionResult
            is Success ->
                continueIssueInstructions(
                    remainingIssueSlots,
                    accumulation,
                    dequeueResult,
                    issueDecodedInstruction(
                        decodedInstructionResult.value,
                        accumulation.registerFile,
                        accumulation.reorderBuffer,
                        accumulation.arithmeticLogicReservationStations,
                        accumulation.branchReservationStations,
                        accumulation.memoryBufferQueue
                    )
                )
        }
    }

    private fun continueIssueInstructions(
        remainingIssueSlots: Int,
        accumulation: IssueAccumulation,
        dequeueResult: InstructionQueueDequeueResult,
        issuedInstructionResult: ProcessorResult<IssueWorkingState>
    ): ProcessorResult<IssueCycleDelta> =
        when (issuedInstructionResult) {
            is Failure ->
                when (issuedInstructionResult.reason) {
                    ReorderBufferFull,
                    ReservationStationFull,
                    MemoryBufferFull -> accumulation.cycleChanges.asSuccess()

                    else -> issuedInstructionResult
                }

            is Success ->
                issueInstructions(
                    remainingIssueSlots - 1,
                    accumulation.instructionDecoder,
                    IssueAccumulation(
                        dequeueResult.instructionQueue,
                        accumulation.instructionDecoder,
                        issuedInstructionResult.value.registerFile,
                        issuedInstructionResult.value.reorderBuffer,
                        issuedInstructionResult.value.arithmeticLogicReservationStations,
                        issuedInstructionResult.value.branchReservationStations,
                        issuedInstructionResult.value.memoryBufferQueue,
                        accumulation.cycleChanges.mergedWith(issuedInstructionResult.value.cycleChanges)
                    )
                )
        }

    private fun issueDecodedInstruction(
        decodedInstruction: DecodedInstruction,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ): ProcessorResult<IssueWorkingState> =
        when (decodedInstruction) {
            is DecodedArithmeticImmediateInstruction ->
                issueArithmeticInstruction(
                    arithmeticOperationFor(decodedInstruction.operation),
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.sourceRegisterAddress),
                    ReadyOperand(decodedInstruction.immediate),
                    decodedInstruction.instructionAddress(),
                    decodedInstruction.destinationRegisterAddress,
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )

            is DecodedArithmeticRegisterInstruction ->
                issueArithmeticInstruction(
                    arithmeticOperationFor(decodedInstruction.operation),
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.leftSourceRegisterAddress),
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.rightSourceRegisterAddress),
                    InstructionAddress(0),
                    decodedInstruction.destinationRegisterAddress,
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )

            is DecodedLoadUpperImmediateInstruction ->
                issueArithmeticInstruction(
                    LoadUpperImmediate,
                    ReadyOperand(decodedInstruction.immediate),
                    ReadyOperand(Word(0u)),
                    InstructionAddress(0),
                    decodedInstruction.destinationRegisterAddress,
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )

            is DecodedAddUpperImmediateToProgramCounterInstruction ->
                issueArithmeticInstruction(
                    AddUpperImmediateToProgramCounter,
                    ReadyOperand(decodedInstruction.immediate),
                    ReadyOperand(Word(0u)),
                    InstructionAddress(decodedInstruction.instructionAddress.value.toInt()),
                    decodedInstruction.destinationRegisterAddress,
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )

            is DecodedJumpAndLinkInstruction ->
                issueJumpInstruction(
                    JumpAndLink,
                    ReadyOperand(Word(0u)),
                    decodedInstruction.immediate,
                    InstructionAddress(decodedInstruction.instructionAddress.value.toInt()),
                    InstructionAddress(decodedInstruction.predictedNextInstructionAddress.value.toInt()),
                    decodedInstruction.destinationRegisterAddress,
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )

            is DecodedJumpAndLinkRegisterInstruction ->
                issueJumpInstruction(
                    JumpAndLinkRegister,
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.sourceRegisterAddress),
                    decodedInstruction.immediate,
                    InstructionAddress(decodedInstruction.instructionAddress.value.toInt()),
                    InstructionAddress(decodedInstruction.predictedNextInstructionAddress.value.toInt()),
                    decodedInstruction.destinationRegisterAddress,
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )

            is DecodedBranchInstruction ->
                issueBranchInstruction(
                    branchOperationFor(decodedInstruction.operation),
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.leftSourceRegisterAddress),
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.rightSourceRegisterAddress),
                    decodedInstruction.immediate,
                    InstructionAddress(decodedInstruction.instructionAddress.value.toInt()),
                    InstructionAddress(decodedInstruction.predictedNextInstructionAddress.value.toInt()),
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )

            is DecodedLoadInstruction ->
                issueLoadInstruction(
                    decodedInstruction.operation,
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.baseRegisterAddress),
                    decodedInstruction.immediate,
                    decodedInstruction.destinationRegisterAddress,
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )

            is DecodedStoreInstruction ->
                issueStoreInstruction(
                    decodedInstruction.operation,
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.baseRegisterAddress),
                    resolveRegisterOperand(registerFile, reorderBuffer, decodedInstruction.valueRegisterAddress),
                    decodedInstruction.immediate,
                    registerFile,
                    reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferQueue
                )
        }

    private fun issueArithmeticInstruction(
        operation: ArithmeticLogicOperation,
        leftOperand: Operand,
        rightOperand: Operand,
        instructionAddress: InstructionAddress,
        destinationRegisterAddress: RegisterAddress,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ): ProcessorResult<IssueWorkingState> =
        reorderBuffer.enqueueRegisterWrite(
            destinationRegisterAddress,
            ArithmeticLogicRegisterWriteReorderBufferEntryCategory
        ).flatMap { allocationResult ->
            arithmeticLogicReservationStations.enqueue(
                operation,
                leftOperand,
                rightOperand,
                Word(0u),
                allocationResult.robId,
                instructionAddress
            ).map { reservationStationResult ->
                IssueWorkingState(
                    registerFile.reserveDestination(destinationRegisterAddress, allocationResult.robId),
                    allocationResult.reorderBuffer,
                    reservationStationResult,
                    branchReservationStations,
                    memoryBufferQueue,
                    IssueCycleDelta(
                        1,
                        listOf(IssueCycleDelta.IssueRegisterReservation(destinationRegisterAddress, allocationResult.robId)),
                        listOf(
                            IssueCycleDelta.RegisterWriteAllocation(
                                destinationRegisterAddress,
                                ArithmeticLogicRegisterWriteReorderBufferEntryCategory
                            )
                        ),
                        listOf(
                            IssueCycleDelta.ReservationStationEnqueue(
                                operation,
                                leftOperand,
                                rightOperand,
                                Word(0u),
                                allocationResult.robId,
                                instructionAddress
                            )
                        ),
                        emptyList(),
                        emptyList()
                    )
                )
            }
        }

    private fun issueJumpInstruction(
        operation: BranchOperation,
        leftOperand: Operand,
        immediate: Word,
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress,
        destinationRegisterAddress: RegisterAddress,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ): ProcessorResult<IssueWorkingState> =
        reorderBuffer.enqueueJump(
            destinationRegisterAddress,
            instructionAddress,
            predictedNextInstructionAddress
        ).flatMap { allocationResult ->
            branchReservationStations.enqueue(
                operation,
                leftOperand,
                ReadyOperand(Word(0u)),
                immediate,
                allocationResult.robId,
                instructionAddress
            ).map { reservationStationResult ->
                IssueWorkingState(
                    registerFile.reserveDestination(destinationRegisterAddress, allocationResult.robId),
                    allocationResult.reorderBuffer,
                    arithmeticLogicReservationStations,
                    reservationStationResult,
                    memoryBufferQueue,
                    IssueCycleDelta(
                        1,
                        listOf(IssueCycleDelta.IssueRegisterReservation(destinationRegisterAddress, allocationResult.robId)),
                        listOf(
                            IssueCycleDelta.JumpAllocation(
                                destinationRegisterAddress,
                                instructionAddress,
                                predictedNextInstructionAddress
                            )
                        ),
                        emptyList(),
                        listOf(
                            IssueCycleDelta.ReservationStationEnqueue(
                                operation,
                                leftOperand,
                                ReadyOperand(Word(0u)),
                                immediate,
                                allocationResult.robId,
                                instructionAddress
                            )
                        ),
                        emptyList()
                    )
                )
            }
        }

    private fun issueBranchInstruction(
        operation: BranchOperation,
        leftOperand: Operand,
        rightOperand: Operand,
        immediate: Word,
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ): ProcessorResult<IssueWorkingState> =
        reorderBuffer.enqueueBranch(
            instructionAddress,
            predictedNextInstructionAddress
        ).flatMap { allocationResult ->
            branchReservationStations.enqueue(
                operation,
                leftOperand,
                rightOperand,
                immediate,
                allocationResult.robId,
                instructionAddress
            ).map { reservationStationResult ->
                IssueWorkingState(
                    registerFile,
                    allocationResult.reorderBuffer,
                    arithmeticLogicReservationStations,
                    reservationStationResult,
                    memoryBufferQueue,
                    IssueCycleDelta(
                        1,
                        emptyList(),
                        listOf(
                            IssueCycleDelta.BranchAllocation(
                                instructionAddress,
                                predictedNextInstructionAddress
                            )
                        ),
                        emptyList(),
                        listOf(
                            IssueCycleDelta.ReservationStationEnqueue(
                                operation,
                                leftOperand,
                                rightOperand,
                                immediate,
                                allocationResult.robId,
                                instructionAddress
                            )
                        ),
                        emptyList()
                    )
                )
            }
        }

    private fun issueLoadInstruction(
        operation: LoadOperation,
        baseOperand: Operand,
        immediate: Word,
        destinationRegisterAddress: RegisterAddress,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ): ProcessorResult<IssueWorkingState> =
        reorderBuffer.enqueueRegisterWrite(
            destinationRegisterAddress,
            LoadRegisterWriteReorderBufferEntryCategory
        ).flatMap { allocationResult ->
            memoryBufferQueue.enqueue(
                LoadMemoryBufferOperation(operation),
                baseOperand,
                ReadyOperand(Word(0u)),
                immediate,
                allocationResult.robId
            ).map { memoryBufferResult ->
                IssueWorkingState(
                    registerFile.reserveDestination(destinationRegisterAddress, allocationResult.robId),
                    allocationResult.reorderBuffer,
                    arithmeticLogicReservationStations,
                    branchReservationStations,
                    memoryBufferResult,
                    IssueCycleDelta(
                        1,
                        listOf(IssueCycleDelta.IssueRegisterReservation(destinationRegisterAddress, allocationResult.robId)),
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
                                ReadyOperand(Word(0u)),
                                immediate,
                                allocationResult.robId
                            )
                        )
                    )
                )
            }
        }

    private fun issueStoreInstruction(
        operation: StoreOperation,
        baseOperand: Operand,
        valueOperand: Operand,
        immediate: Word,
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        branchReservationStations: ReservationStationBank<BranchOperation>,
        memoryBufferQueue: MemoryBufferQueue
    ): ProcessorResult<IssueWorkingState> =
        reorderBuffer.enqueueStore(operation, valueOperand)
            .flatMap { allocationResult ->
                memoryBufferQueue.enqueue(
                    StoreMemoryBufferOperation(operation),
                    baseOperand,
                    valueOperand,
                    immediate,
                    allocationResult.robId
                ).map { memoryBufferResult ->
                    IssueWorkingState(
                        registerFile,
                        allocationResult.reorderBuffer,
                        arithmeticLogicReservationStations,
                        branchReservationStations,
                        memoryBufferResult,
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
                                    allocationResult.robId
                                )
                            )
                        )
                    )
                }
            }

    private fun resolveRegisterOperand(
        registerFile: RegisterFile,
        reorderBuffer: ReorderBuffer,
        registerAddress: RegisterAddress
    ) =
        when (val operand = registerFile.operandFor(registerAddress)) {
            is ReadyOperand -> operand
            is PendingOperand -> reorderBuffer.resolveOperand(operand)
        }

    private fun arithmeticOperationFor(operation: ArithmeticImmediateOperation) =
        when (operation) {
            AddImmediateOperation -> AddImmediate
            SetLessThanImmediateSignedOperation -> SetLessThanImmediateSigned
            SetLessThanImmediateUnsignedOperation -> SetLessThanImmediateUnsigned
            ExclusiveOrImmediateOperation -> ExclusiveOrImmediate
            OrImmediateOperation -> OrImmediate
            AndImmediateOperation -> AndImmediate
            ShiftLeftLogicalImmediateOperation -> ShiftLeftLogicalImmediate
            ShiftRightLogicalImmediateOperation -> ShiftRightLogicalImmediate
            ShiftRightArithmeticImmediateOperation -> ShiftRightArithmeticImmediate
        }

    private fun arithmeticOperationFor(operation: ArithmeticRegisterOperation) =
        when (operation) {
            AddOperation -> Add
            SubtractOperation -> Subtract
            ShiftLeftLogicalOperation -> ShiftLeftLogical
            SetLessThanSignedOperation -> SetLessThanSigned
            SetLessThanUnsignedOperation -> SetLessThanUnsigned
            ExclusiveOrOperation -> ExclusiveOr
            ShiftRightLogicalOperation -> ShiftRightLogical
            ShiftRightArithmeticOperation -> ShiftRightArithmetic
            OrOperation -> Or
            AndOperation -> And
        }

    private fun branchOperationFor(operation: decoder.BranchOperation) =
        when (operation) {
            BranchEqualOperation -> BranchEqual
            BranchNotEqualOperation -> BranchNotEqual
            BranchLessThanSignedOperation -> BranchLessThanSigned
            BranchLessThanUnsignedOperation -> BranchLessThanUnsigned
            BranchGreaterThanOrEqualSignedOperation -> BranchGreaterThanOrEqualSigned
            BranchGreaterThanOrEqualUnsignedOperation -> BranchGreaterThanOrEqualUnsigned
        }

    private fun InstructionAddress.asWord() = Word(value.toUInt())

    private fun DecodedArithmeticImmediateInstruction.instructionAddress() = InstructionAddress(0)

    private data class IssueAccumulation(
        val instructionQueue: InstructionQueue,
        val instructionDecoder: InstructionDecoder,
        val registerFile: RegisterFile,
        val reorderBuffer: ReorderBuffer,
        val arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        val branchReservationStations: ReservationStationBank<BranchOperation>,
        val memoryBufferQueue: MemoryBufferQueue,
        val cycleChanges: IssueCycleDelta
    )

    private data class IssueWorkingState(
        val registerFile: RegisterFile,
        val reorderBuffer: ReorderBuffer,
        val arithmeticLogicReservationStations: ReservationStationBank<ArithmeticLogicOperation>,
        val branchReservationStations: ReservationStationBank<BranchOperation>,
        val memoryBufferQueue: MemoryBufferQueue,
        val cycleChanges: IssueCycleDelta
    )
}
