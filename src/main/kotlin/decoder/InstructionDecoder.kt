package decoder

import types.ProcessorResult
import types.Word

interface InstructionDecoder {
    fun decode(
        instruction: Word,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ): ProcessorResult<DecodedInstruction>
}
