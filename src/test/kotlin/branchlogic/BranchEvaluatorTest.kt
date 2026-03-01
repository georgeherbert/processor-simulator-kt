package branchlogic

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.Word

class BranchEvaluatorTest {

    private val evaluator = RealBranchEvaluator

    @Test
    fun `jal returns branch target and link writeback`() {
        expectThat(evaluate(JumpAndLink, 0u, 0u, 16u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(116u),
                    writeBack = BranchLinkWriteBack(Word(104u))
                )
            )
    }

    @Test
    fun `jal wraps on overflow for target and link`() {
        expectThat(evaluate(JumpAndLink, 0u, 0u, 32u, 0xfffffff0u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(0x10u),
                    writeBack = BranchLinkWriteBack(Word(0xfffffff4u))
                )
            )
    }

    @Test
    fun `jalr uses rs1 plus immediate clears least significant bit and writes link`() {
        expectThat(evaluate(JumpAndLinkRegister, 9u, 0u, 4u, 200u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(12u),
                    writeBack = BranchLinkWriteBack(Word(204u))
                )
            )
    }

    @Test
    fun `jalr clears least significant bit when computed target is odd`() {
        expectThat(evaluate(JumpAndLinkRegister, 5u, 0u, 2u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(6u),
                    writeBack = BranchLinkWriteBack(Word(104u))
                )
            )
    }

    @Test
    fun `jalr wraps and clears least significant bit`() {
        expectThat(evaluate(JumpAndLinkRegister, 0xffffffffu, 0u, 2u, 0xfffffffcu))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(0u),
                    writeBack = BranchLinkWriteBack(Word(0u))
                )
            )
    }

    @Test
    fun `beq takes branch when operands equal and otherwise falls through`() {
        expectThat(evaluate(BranchEqual, 8u, 8u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchEqual, 8u, 9u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(104u),
                    writeBack = NoBranchWriteBack
                )
            )
    }

    @Test
    fun `bne takes branch when operands differ and otherwise falls through`() {
        expectThat(evaluate(BranchNotEqual, 8u, 9u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchNotEqual, 8u, 8u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(104u),
                    writeBack = NoBranchWriteBack
                )
            )
    }

    @Test
    fun `blt uses signed comparison`() {
        expectThat(evaluate(BranchLessThanSigned, 0xfffffff0u, 1u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchLessThanSigned, 5u, 5u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(104u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchLessThanSigned, 0x80000000u, 0x7fffffffu, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )
    }

    @Test
    fun `bltu uses unsigned comparison`() {
        expectThat(evaluate(BranchLessThanUnsigned, 0xfffffff0u, 1u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(104u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchLessThanUnsigned, 1u, 0xfffffff0u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchLessThanUnsigned, 9u, 9u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(104u),
                    writeBack = NoBranchWriteBack
                )
            )
    }

    @Test
    fun `bge uses signed comparison`() {
        expectThat(evaluate(BranchGreaterThanOrEqualSigned, 0xfffffff0u, 1u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(104u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchGreaterThanOrEqualSigned, 5u, 5u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchGreaterThanOrEqualSigned, 0x7fffffffu, 0x80000000u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )
    }

    @Test
    fun `bgeu uses unsigned comparison`() {
        expectThat(evaluate(BranchGreaterThanOrEqualUnsigned, 0xfffffff0u, 1u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchGreaterThanOrEqualUnsigned, 1u, 0xfffffff0u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(104u),
                    writeBack = NoBranchWriteBack
                )
            )

        expectThat(evaluate(BranchGreaterThanOrEqualUnsigned, 9u, 9u, 12u, 100u))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(112u),
                    writeBack = NoBranchWriteBack
                )
            )
    }

    @Test
    fun `not taken branches fall through with wraparound`() {
        expectThat(evaluate(BranchEqual, 1u, 0u, 4u, 0xfffffffcu))
            .isEqualTo(
                BranchEvaluation(
                    actualNextInstructionAddress = Word(0u),
                    writeBack = NoBranchWriteBack
                )
            )
    }

    private fun evaluate(
        operation: BranchOperation,
        leftOperand: UInt,
        rightOperand: UInt,
        branchOffset: UInt,
        instructionAddress: UInt
    ) = evaluator.evaluate(
        operation = operation,
        leftOperand = Word(leftOperand),
        rightOperand = Word(rightOperand),
        branchOffset = Word(branchOffset),
        instructionAddress = Word(instructionAddress)
    )
}
