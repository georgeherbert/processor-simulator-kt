package cpu

import mainmemory.RealMainMemory
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.ProcessorAlreadyHalted
import types.RegisterAddress
import types.Size
import types.Word

class ProcessorTest {

    @Test
    fun `processor executes arithmetic store and terminal jump program`() {
        val configuration = testConfiguration()
        val mainMemorySize = Size(64)
        val processor = RealProcessor(configuration)
        val initialState = expectThat(
            RealProcessorFactory.create(
                configuration,
                RealMainMemory.fromProgramBytes(mainMemorySize, programBytes()),
                mainMemorySize,
                types.InstructionAddress(0)
            )
        )
            .isSuccess()
            .subject

        val finalState = runUntilHalt(processor, initialState, 32)

        expectThat(finalState.halted)
            .isTrue()

        expectThat(finalState.registerFile.readCommitted(RegisterAddress(2)))
            .isEqualTo(Word(12u))

        expectThat(finalState.mainMemory.loadWord(32))
            .isSuccess()
            .isEqualTo(Word(12u))

        expectThat(finalState.statistics.committedInstructionCount)
            .isEqualTo(4)

        expectThat(finalState.statistics.mispredictionCount)
            .isEqualTo(1)

        expectThat(processor.step(finalState))
            .isFailure()
            .isEqualTo(ProcessorAlreadyHalted)
    }

    private fun runUntilHalt(
        processor: Processor,
        state: ProcessorState,
        remainingCycles: Int
    ): ProcessorState =
        when {
            state.halted -> state
            remainingCycles == 0 -> state
            else ->
                runUntilHalt(
                    processor,
                    expectThat(processor.step(state))
                        .isSuccess()
                        .subject,
                    remainingCycles - 1
                )
        }

    private fun testConfiguration() =
        ProcessorConfiguration(
            fetch.FetchWidth(2),
            IssueWidth(2),
            CommitWidth(2),
            Size(8),
            Size(8),
            Size(8),
            types.BitWidth(2),
            Size(8),
            Size(8),
            Size(8),
            Size(1),
            Size(1),
            Size(1),
            Size(1),
            ArithmeticLogicLatencies(
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1)
            ),
            BranchLatencies(
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1)
            ),
            types.CycleCount(1),
            MemoryLatencies(
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1),
                types.CycleCount(1)
            )
        )

    private fun programBytes(): ByteArray =
        instructionWords()
            .flatMap { word ->
                listOf(
                    (word.value and 0xFFu).toByte(),
                    ((word.value shr 8) and 0xFFu).toByte(),
                    ((word.value shr 16) and 0xFFu).toByte(),
                    ((word.value shr 24) and 0xFFu).toByte()
                )
            }
            .toByteArray()

    private fun instructionWords() =
        listOf(
            encodeI(0x13, 1, 0x0, 0, 5),
            encodeI(0x13, 2, 0x0, 1, 7),
            encodeS(0x23, 0x2, 0, 2, 32),
            encodeJ(0x6F, 0, -12)
        ) + List(12) { unusedIndex ->
            encodeI(0x13, 0, 0x0, 0, unusedIndex - unusedIndex)
        }

    private fun encodeI(opcode: Int, rd: Int, funct3: Int, rs1: Int, immediate: Int) =
        Word(
            (
                (((immediate and 0xFFF) shl 20)) or
                    ((rs1 and 0x1F) shl 15) or
                    ((funct3 and 0x7) shl 12) or
                    ((rd and 0x1F) shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )

    private fun encodeS(opcode: Int, funct3: Int, rs1: Int, rs2: Int, immediate: Int): Word {
        val immediateLow = immediate and 0x1F
        val immediateHigh = (immediate shr 5) and 0x7F
        return Word(
            (
                (immediateHigh shl 25) or
                    ((rs2 and 0x1F) shl 20) or
                    ((rs1 and 0x1F) shl 15) or
                    ((funct3 and 0x7) shl 12) or
                    (immediateLow shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )
    }

    private fun encodeJ(opcode: Int, rd: Int, immediate: Int): Word {
        val bit20 = (immediate shr 20) and 0x1
        val bits10To1 = (immediate shr 1) and 0x3FF
        val bit11 = (immediate shr 11) and 0x1
        val bits19To12 = (immediate shr 12) and 0xFF
        return Word(
            (
                (bit20 shl 31) or
                    (bits19To12 shl 12) or
                    (bit11 shl 20) or
                    (bits10To1 shl 21) or
                    ((rd and 0x1F) shl 7) or
                    (opcode and 0x7F)
                ).toUInt()
        )
    }
}
