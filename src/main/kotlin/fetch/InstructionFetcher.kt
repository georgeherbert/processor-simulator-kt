package fetch

import branchpredictor.BranchTargetPredictor
import dev.forkhandles.result4k.*
import instructionqueue.InstructionQueueEntry
import instructionqueue.InstructionQueueSlots
import instructionqueue.InstructionQueueSlotsSource
import mainmemory.MainMemoryLoader
import types.InstructionAddress
import types.InstructionQueueSlotCountInvalid
import types.ProcessorResult
import types.next

interface InstructionFetcher {
    fun step(startInstructionAddress: InstructionAddress): ProcessorResult<FetchStepResult>
}

data class RealInstructionFetcher(
    private val fetchWidth: FetchWidth,
    private val mainMemoryLoader: MainMemoryLoader,
    private val branchTargetPredictor: BranchTargetPredictor,
    private val instructionQueueSlotsSource: InstructionQueueSlotsSource,
) : InstructionFetcher {

    override fun step(startInstructionAddress: InstructionAddress) =
        instructionCountToFetch()
            .map { instructionCount -> instructionsToFetch(startInstructionAddress, instructionCount) }
            .flatMap { instructionsToFetch ->
                fetchInstructions(instructionsToFetch)
                    .map { fetchedInstructions ->
                        FetchStepResult(
                            fetchedInstructions,
                            nextInstructionAddress(startInstructionAddress, instructionsToFetch)
                        )
                    }
            }

    private fun instructionCountToFetch() =
        instructionQueueSlotsSource
            .get()
            .validated()
            .map { instructionQueueSlots ->
                minOf(fetchWidth.value, instructionQueueSlots.value)
            }

    private fun InstructionQueueSlots.validated() =
        when (value < 0) {
            true -> InstructionQueueSlotCountInvalid(value).asFailure()
            false -> this.asSuccess()
        }

    private fun instructionsToFetch(
        startInstructionAddress: InstructionAddress,
        instructionCount: Int,
    ) =
        generateSequence(startInstructionAddress) { instructionAddress -> instructionAddress.next }
            .take(instructionCount)
            .toList()
            .map { instructionAddress ->
                InstructionToFetch(
                    instructionAddress,
                    branchTargetPredictor.predict(instructionAddress)
                )
            }
            .onPredictedPath()

    private fun List<InstructionToFetch>.onPredictedPath(): List<InstructionToFetch> {
        val firstRedirectIndex = firstRedirectIndex()
        return when (firstRedirectIndex < 0) {
            true -> this
            false -> take(firstRedirectIndex + 1)
        }
    }

    private fun List<InstructionToFetch>.firstRedirectIndex() =
        indexOfFirst { instructionToFetch ->
            instructionToFetch.predictedNextInstructionAddress !=
                    instructionToFetch.instructionAddress.next
        }

    private fun nextInstructionAddress(
        startInstructionAddress: InstructionAddress,
        instructionsToFetch: List<InstructionToFetch>,
    ) =
        when (instructionsToFetch.isEmpty()) {
            true -> startInstructionAddress
            false -> instructionsToFetch.last().predictedNextInstructionAddress
        }

    private fun fetchInstructions(instructionsToFetch: List<InstructionToFetch>) =
        instructionsToFetch
            .foldResult(emptyList<InstructionQueueEntry>().asSuccess()) { fetchedInstructions, instructionToFetch ->
                loadInstructionEntry(
                    instructionToFetch.instructionAddress,
                    instructionToFetch.predictedNextInstructionAddress
                )
                    .map { fetchedInstruction ->
                        fetchedInstructions + fetchedInstruction
                    }
            }

    private fun loadInstructionEntry(
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress,
    ) =
        mainMemoryLoader
            .loadWord(instructionAddress.value)
            .map { instructionWord ->
                InstructionQueueEntry(
                    instructionWord,
                    instructionAddress,
                    predictedNextInstructionAddress
                )
            }

    private data class InstructionToFetch(
        val instructionAddress: InstructionAddress,
        val predictedNextInstructionAddress: InstructionAddress,
    )
}
