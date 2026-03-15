package commit

import branchpredictor.DynamicBranchTargetPredictor
import decoder.StoreByteOperation
import decoder.StoreHalfWordOperation
import decoder.StoreOperation
import decoder.StoreWordOperation
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import mainmemory.MainMemoryStorer
import registerfile.RegisterFile
import reorderbuffer.ReorderBuffer
import types.Byte
import types.DataAddress
import types.HalfWord
import types.InstructionAddress
import types.ProcessorResult
import types.RegisterAddress
import types.RobId
import types.Word

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
                statisticsDelta.committedInstructionCount + other.statisticsDelta.committedInstructionCount
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
                CommitStatisticsDelta(0),
                false
            )

        fun registerWrite(
            registerAddress: RegisterAddress,
            value: Word,
            robId: RobId
        ) =
            committedInstruction(
                listOf(RegisterCommit(registerAddress, value, robId)),
                emptyList(),
                emptyList(),
                NoCommitControlEvent,
                false
            )

        fun jump(
            registerAddress: RegisterAddress,
            value: Word,
            robId: RobId,
            instructionAddress: InstructionAddress,
            actualNextInstructionAddress: InstructionAddress,
            controlEvent: CommitControlEvent,
            halted: Boolean
        ) =
            committedInstruction(
                listOf(RegisterCommit(registerAddress, value, robId)),
                emptyList(),
                listOf(BranchOutcome(instructionAddress, actualNextInstructionAddress)),
                controlEvent,
                halted
            )

        fun branch(
            instructionAddress: InstructionAddress,
            actualNextInstructionAddress: InstructionAddress,
            controlEvent: CommitControlEvent
        ) =
            committedInstruction(
                emptyList(),
                emptyList(),
                listOf(BranchOutcome(instructionAddress, actualNextInstructionAddress)),
                controlEvent,
                false
            )

        fun store(
            operation: StoreOperation,
            address: DataAddress,
            value: Word
        ) =
            committedInstruction(
                emptyList(),
                listOf(CommittedStore(operation, address, value)),
                emptyList(),
                NoCommitControlEvent,
                false
            )

        private fun committedInstruction(
            registerCommits: List<RegisterCommit>,
            stores: List<CommittedStore>,
            branchOutcomes: List<BranchOutcome>,
            controlEvent: CommitControlEvent,
            halted: Boolean
        ) =
            CommitCycleDelta(
                1,
                registerCommits,
                stores,
                branchOutcomes,
                controlEvent,
                CommitStatisticsDelta(1),
                halted
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
    val committedInstructionCount: Int
)

sealed interface CommitControlEvent
data object NoCommitControlEvent : CommitControlEvent
data class RedirectCommitControlEvent(val targetInstructionAddress: InstructionAddress) : CommitControlEvent
