package commit

import branchpredictor.DynamicBranchTargetPredictor
import cpu.CommitWidth
import decoder.StoreByteOperation
import decoder.StoreHalfWordOperation
import decoder.StoreOperation
import decoder.StoreWordOperation
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import mainmemory.MainMemory
import mainmemory.MainMemoryStorer
import registerfile.RegisterFile
import reorderbuffer.*
import types.*
import types.Byte

interface CommitUnit {
    fun nextCycleDelta(
        reorderBuffer: ReorderBuffer,
        registerFile: RegisterFile,
        mainMemory: MainMemory,
        branchTargetPredictor: DynamicBranchTargetPredictor
    ): ProcessorResult<CommitCycleDelta>
}

data class CommitCycleDelta(
    private val committedHeadCount: Int,
    private val registerCommits: List<RegisterCommit>,
    private val stores: List<CommittedStore>,
    private val branchOutcomes: List<BranchOutcome>,
    val controlEvent: CommitControlEvent,
    val statisticsDelta: CommitStatisticsDelta,
    val halted: Boolean
) {
    fun applyToReorderBuffer(reorderBuffer: ReorderBuffer): ProcessorResult<ReorderBuffer> {
        val initialReorderBufferResult: ProcessorResult<ReorderBuffer> = reorderBuffer.asSuccess()

        return (0 until committedHeadCount).fold(initialReorderBufferResult) { reorderBufferResult, _ ->
            reorderBufferResult.flatMap { currentReorderBuffer ->
                currentReorderBuffer.commitReadyHead()
                    .map { commitHeadResult -> commitHeadResult.reorderBuffer }
            }
        }
    }

    fun applyToRegisterFile(registerFile: RegisterFile) =
        registerCommits.fold(registerFile) { currentRegisterFile, registerCommit ->
            currentRegisterFile.commit(
                registerCommit.registerAddress,
                registerCommit.value,
                registerCommit.robId
            )
        }

    fun <T : MainMemoryStorer<T>> applyToMainMemory(mainMemory: T): ProcessorResult<T> {
        val initialMainMemoryResult: ProcessorResult<T> = mainMemory.asSuccess()

        return stores.fold(initialMainMemoryResult) { mainMemoryResult, store ->
            mainMemoryResult.flatMap { currentMainMemory ->
                store.applyTo(currentMainMemory)
            }
        }
    }

    fun applyToBranchTargetPredictor(branchTargetPredictor: DynamicBranchTargetPredictor) =
        branchOutcomes.fold(branchTargetPredictor) { currentBranchTargetPredictor, branchOutcome ->
            currentBranchTargetPredictor.outcome(
                branchOutcome.instructionAddress,
                branchOutcome.targetInstructionAddress
            )
        }

    fun mergedWith(other: CommitCycleDelta) =
        CommitCycleDelta(
            committedHeadCount + other.committedHeadCount,
            registerCommits + other.registerCommits,
            stores + other.stores,
            branchOutcomes + other.branchOutcomes,
            other.controlEvent,
            CommitStatisticsDelta(
                statisticsDelta.committedInstructionCount + other.statisticsDelta.committedInstructionCount,
                statisticsDelta.branchInstructionCount + other.statisticsDelta.branchInstructionCount,
                statisticsDelta.mispredictionCount + other.statisticsDelta.mispredictionCount
            ),
            halted || other.halted
        )

    companion object {
        fun none() =
            CommitCycleDelta(
                0,
                emptyList(),
                emptyList(),
                emptyList(),
                NoCommitControlEvent,
                CommitStatisticsDelta(0, 0, 0),
                false
            )
    }

    data class RegisterCommit(
        val registerAddress: RegisterAddress,
        val value: Word,
        val robId: RobId
    )

    data class BranchOutcome(
        val instructionAddress: InstructionAddress,
        val targetInstructionAddress: InstructionAddress
    )

    data class CommittedStore(
        val operation: StoreOperation,
        val address: DataAddress,
        val value: Word
    ) {
        fun <T : MainMemoryStorer<T>> applyTo(mainMemory: T) =
            when (operation) {
                StoreWordOperation ->
                    mainMemory.storeWord(address.value, value)

                StoreHalfWordOperation ->
                    mainMemory.storeHalfWord(address.value, HalfWord(value.value.toUShort()))

                StoreByteOperation ->
                    mainMemory.storeByte(address.value, Byte(value.value.toUByte()))
            }
    }
}

data class CommitStatisticsDelta(
    val committedInstructionCount: Int,
    val branchInstructionCount: Int,
    val mispredictionCount: Int
)

sealed interface CommitControlEvent
data object NoCommitControlEvent : CommitControlEvent
data class RedirectCommitControlEvent(val targetInstructionAddress: InstructionAddress) : CommitControlEvent

data class RealCommitUnit(private val commitWidth: CommitWidth) : CommitUnit {

    override fun nextCycleDelta(
        reorderBuffer: ReorderBuffer,
        registerFile: RegisterFile,
        mainMemory: MainMemory,
        branchTargetPredictor: DynamicBranchTargetPredictor
    ) =
        commitReadyEntries(
            commitWidth.value,
            reorderBuffer,
            CommitCycleDelta.none()
        )

    private fun commitReadyEntries(
        remainingCommitSlots: Int,
        reorderBuffer: ReorderBuffer,
        cycleChanges: CommitCycleDelta
    ): ProcessorResult<CommitCycleDelta> {
        if (remainingCommitSlots == 0 || cycleChanges.controlEvent is RedirectCommitControlEvent || cycleChanges.halted) {
            return cycleChanges.asSuccess()
        }

        return when (val headCommitOutcome = reorderBuffer.commitReadyHeadIfPossible()) {
            ReorderBufferCommitReadyHeadUnavailable -> cycleChanges.asSuccess()
            is ReorderBufferCommitHeadResult ->
                commitHeadEntry(
                    remainingCommitSlots,
                    headCommitOutcome,
                    cycleChanges
                )
        }
    }

    private fun commitHeadEntry(
        remainingCommitSlots: Int,
        headCommitResult: ReorderBufferCommitHeadResult,
        cycleChanges: CommitCycleDelta
    ): ProcessorResult<CommitCycleDelta> {
        val committedEntry = headCommitResult.entry

        return when (committedEntry) {
            is RegisterWriteReorderBufferEntry ->
                committedEntry.value()
                    .flatMap { value ->
                        commitReadyEntries(
                            remainingCommitSlots - 1,
                            headCommitResult.reorderBuffer,
                            cycleChanges.mergedWith(
                                CommitCycleDelta(
                                    1,
                                    listOf(CommitCycleDelta.RegisterCommit(committedEntry.destinationRegisterAddress, value, committedEntry.robId)),
                                    emptyList(),
                                    emptyList(),
                                    NoCommitControlEvent,
                                    CommitStatisticsDelta(1, 0, 0),
                                    false
                                )
                            )
                        )
                    }

            is JumpReorderBufferEntry ->
                committedEntry.actualNextInstructionAddress()
                    .flatMap { actualNextInstructionAddress ->
                        committedEntry.value()
                            .flatMap { value ->
                                val nextControlEvent = controlEventFor(
                                    committedEntry.predictedNextInstructionAddress,
                                    actualNextInstructionAddress
                                )

                                commitReadyEntries(
                                    remainingCommitSlots - 1,
                                    headCommitResult.reorderBuffer,
                                    cycleChanges.mergedWith(
                                        CommitCycleDelta(
                                            1,
                                            listOf(CommitCycleDelta.RegisterCommit(committedEntry.destinationRegisterAddress, value, committedEntry.robId)),
                                            emptyList(),
                                            listOf(
                                                CommitCycleDelta.BranchOutcome(
                                                    committedEntry.instructionAddress,
                                                    actualNextInstructionAddress
                                                )
                                            ),
                                            nextControlEvent,
                                            CommitStatisticsDelta(1, 1, mispredictionDelta(nextControlEvent)),
                                            actualNextInstructionAddress.value == 0
                                        )
                                    )
                                )
                            }
                    }

            is BranchReorderBufferEntry ->
                committedEntry.actualNextInstructionAddress()
                    .flatMap { actualNextInstructionAddress ->
                        val nextControlEvent = controlEventFor(
                            committedEntry.predictedNextInstructionAddress,
                            actualNextInstructionAddress
                        )

                        commitReadyEntries(
                            remainingCommitSlots - 1,
                            headCommitResult.reorderBuffer,
                            cycleChanges.mergedWith(
                                CommitCycleDelta(
                                    1,
                                    emptyList(),
                                    emptyList(),
                                    listOf(
                                        CommitCycleDelta.BranchOutcome(
                                            committedEntry.instructionAddress,
                                            actualNextInstructionAddress
                                        )
                                    ),
                                    nextControlEvent,
                                    CommitStatisticsDelta(1, 1, mispredictionDelta(nextControlEvent)),
                                    false
                                )
                            )
                        )
                    }

            is StoreReorderBufferEntry ->
                storeFor(committedEntry)
                    .flatMap { store ->
                        commitReadyEntries(
                            remainingCommitSlots - 1,
                            headCommitResult.reorderBuffer,
                            cycleChanges.mergedWith(
                                CommitCycleDelta(
                                    1,
                                    emptyList(),
                                    listOf(store),
                                    emptyList(),
                                    NoCommitControlEvent,
                                    CommitStatisticsDelta(1, 0, 0),
                                    false
                                )
                            )
                        )
                    }
        }
    }

    private fun controlEventFor(
        predictedNextInstructionAddress: InstructionAddress,
        actualNextInstructionAddress: InstructionAddress
    ) =
        when (predictedNextInstructionAddress == actualNextInstructionAddress) {
            true -> NoCommitControlEvent
            false -> RedirectCommitControlEvent(actualNextInstructionAddress)
        }

    private fun mispredictionDelta(controlEvent: CommitControlEvent) =
        when (controlEvent) {
            NoCommitControlEvent -> 0
            is RedirectCommitControlEvent -> 1
        }

    private fun storeFor(committedEntry: StoreReorderBufferEntry) =
        committedEntry.addressValue()
            .flatMap { address ->
                committedEntry.value()
                    .map { value ->
                        CommitCycleDelta.CommittedStore(
                            committedEntry.operation,
                            address,
                            value
                        )
                    }
            }

    private fun RegisterWriteReorderBufferEntry.value() =
        valueOperand.readyValue(robId)

    private fun JumpReorderBufferEntry.value() =
        valueOperand.readyValue(robId)

    private fun JumpReorderBufferEntry.actualNextInstructionAddress() =
        actualNextInstructionAddress.readyInstructionAddress(robId)

    private fun BranchReorderBufferEntry.actualNextInstructionAddress() =
        actualNextInstructionAddress.readyInstructionAddress(robId)

    private fun StoreReorderBufferEntry.value() =
        valueOperand.readyValue(robId)

    private fun StoreReorderBufferEntry.addressValue() =
        address.readyDataAddress(robId)

    private fun Operand.readyValue(robId: RobId) =
        when (this) {
            is ReadyOperand -> value.asSuccess()
            is PendingOperand -> CommitEntryValueUnavailable(robId).asFailure()
        }

    private fun InstructionAddress?.readyInstructionAddress(robId: RobId) =
        this?.asSuccess()
            ?: CommitEntryActualNextInstructionAddressUnavailable(robId).asFailure()

    private fun DataAddress?.readyDataAddress(robId: RobId) =
        this?.asSuccess()
            ?: CommitEntryAddressUnavailable(robId).asFailure()
}
