package fetch

import instructionqueue.InstructionQueueEntry
import types.InstructionAddress

data class FetchStepResult(
    val fetchedInstructions: List<InstructionQueueEntry>,
    val nextInstructionAddress: InstructionAddress
)
