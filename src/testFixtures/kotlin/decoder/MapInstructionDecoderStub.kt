package decoder

import dev.forkhandles.result4k.asFailure
import types.DecoderUnknownOpcode
import types.ProcessorResult
import types.Word

data class MapInstructionDecoderStub(
    private val decodedInstructions: Map<Word, ProcessorResult<DecodedInstruction>>
) : InstructionDecoder {

    override fun decode(
        instruction: Word,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        decodedInstructions[instruction]
            ?: DecoderUnknownOpcode(-1).asFailure()
}
