package instructionqueue

import types.InstructionAddress
import types.Word

data class InstructionQueueEntry(
    val instruction: Word,
    val instructionAddress: InstructionAddress,
    val predictedNextInstructionAddress: InstructionAddress,
)
