package arithmeticlogic

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.Word

class ArithmeticLogicUnitTest {

    private val arithmeticLogicUnit = RealArithmeticLogicUnit

    @Test
    fun `add operations produce summed value`() {
        expectThat(evaluate(Add, 5u, 7u, 100))
            .isEqualTo(Word(12u))

        expectThat(evaluate(AddImmediate, 5u, 7u, 100))
            .isEqualTo(Word(12u))
    }

    @Test
    fun `add operations wrap on overflow`() {
        expectThat(evaluate(Add, 0xffffffffu, 1u, 100))
            .isEqualTo(Word(0u))

        expectThat(evaluate(AddImmediate, 0xffffffffu, 1u, 100))
            .isEqualTo(Word(0u))
    }

    @Test
    fun `load upper immediate returns immediate value unchanged`() {
        expectThat(evaluate(LoadUpperImmediate, 0x12340000u, 77u, 100))
            .isEqualTo(Word(0x12340000u))

        expectThat(evaluate(LoadUpperImmediate, 0xffffffffu, 0u, 0))
            .isEqualTo(Word(0xffffffffu))
    }

    @Test
    fun `add upper immediate to program counter adds instruction address`() {
        expectThat(evaluate(AddUpperImmediateToProgramCounter, 20u, 0u, 40))
            .isEqualTo(Word(60u))
    }

    @Test
    fun `add upper immediate to program counter wraps on overflow`() {
        expectThat(evaluate(AddUpperImmediateToProgramCounter, 0xfffffff0u, 0u, 32))
            .isEqualTo(Word(0x10u))
    }

    @Test
    fun `subtract operation produces subtracted value`() {
        expectThat(evaluate(Subtract, 9u, 4u, 100))
            .isEqualTo(Word(5u))
    }

    @Test
    fun `subtract wraps on underflow`() {
        expectThat(evaluate(Subtract, 0u, 1u, 100))
            .isEqualTo(Word(0xffffffffu))
    }

    @Test
    fun `shift left logical operations shift left by right operand`() {
        expectThat(evaluate(ShiftLeftLogical, 1u, 4u, 100))
            .isEqualTo(Word(16u))

        expectThat(evaluate(ShiftLeftLogicalImmediate, 1u, 4u, 100))
            .isEqualTo(Word(16u))
    }

    @Test
    fun `shift left logical operations handle edge shift amounts`() {
        expectThat(evaluate(ShiftLeftLogical, 1u, 31u, 100))
            .isEqualTo(Word(0x80000000u))

        expectThat(evaluate(ShiftLeftLogicalImmediate, 1u, 33u, 100))
            .isEqualTo(Word(2u))
    }

    @Test
    fun `set less than signed operations use signed comparison`() {
        expectThat(evaluate(SetLessThanSigned, 0xfffffff0u, 1u, 100))
            .isEqualTo(Word(1u))

        expectThat(evaluate(SetLessThanImmediateSigned, 0xfffffff0u, 1u, 100))
            .isEqualTo(Word(1u))
    }

    @Test
    fun `set less than signed operations return zero for equal operands`() {
        expectThat(evaluate(SetLessThanSigned, 9u, 9u, 100))
            .isEqualTo(Word(0u))

        expectThat(evaluate(SetLessThanImmediateSigned, 9u, 9u, 100))
            .isEqualTo(Word(0u))
    }

    @Test
    fun `set less than unsigned operations use unsigned comparison`() {
        expectThat(evaluate(SetLessThanUnsigned, 0xfffffff0u, 1u, 100))
            .isEqualTo(Word(0u))

        expectThat(evaluate(SetLessThanImmediateUnsigned, 0xfffffff0u, 1u, 100))
            .isEqualTo(Word(0u))
    }

    @Test
    fun `set less than unsigned operations return one for low to high comparison`() {
        expectThat(evaluate(SetLessThanUnsigned, 0u, 0xffffffffu, 100))
            .isEqualTo(Word(1u))

        expectThat(evaluate(SetLessThanImmediateUnsigned, 0u, 0xffffffffu, 100))
            .isEqualTo(Word(1u))
    }

    @Test
    fun `exclusive or operations apply bitwise xor`() {
        expectThat(evaluate(ExclusiveOr, 0b1010u, 0b1100u, 100))
            .isEqualTo(Word(0b0110u))

        expectThat(evaluate(ExclusiveOrImmediate, 0b1010u, 0b1100u, 100))
            .isEqualTo(Word(0b0110u))
    }

    @Test
    fun `exclusive or operations handle edge identities`() {
        expectThat(evaluate(ExclusiveOr, 0xabcdef12u, 0xabcdef12u, 100))
            .isEqualTo(Word(0u))

        expectThat(evaluate(ExclusiveOrImmediate, 0x0u, 0xffffffffu, 100))
            .isEqualTo(Word(0xffffffffu))
    }

    @Test
    fun `shift right logical operations shift in zeros`() {
        expectThat(evaluate(ShiftRightLogical, 0x80000000u, 1u, 100))
            .isEqualTo(Word(0x40000000u))

        expectThat(evaluate(ShiftRightLogicalImmediate, 0x80000000u, 1u, 100))
            .isEqualTo(Word(0x40000000u))
    }

    @Test
    fun `shift right logical operations handle edge shift amounts`() {
        expectThat(evaluate(ShiftRightLogical, 0x80000000u, 31u, 100))
            .isEqualTo(Word(1u))

        expectThat(evaluate(ShiftRightLogicalImmediate, 0x80000000u, 33u, 100))
            .isEqualTo(Word(0x40000000u))
    }

    @Test
    fun `shift right arithmetic operations preserve sign bit`() {
        expectThat(evaluate(ShiftRightArithmetic, 0x80000000u, 1u, 100))
            .isEqualTo(Word(0xc0000000u))

        expectThat(evaluate(ShiftRightArithmeticImmediate, 0x80000000u, 1u, 100))
            .isEqualTo(Word(0xc0000000u))
    }

    @Test
    fun `shift right arithmetic operations handle positive and negative extremes`() {
        expectThat(evaluate(ShiftRightArithmetic, 0x80000000u, 31u, 100))
            .isEqualTo(Word(0xffffffffu))

        expectThat(evaluate(ShiftRightArithmeticImmediate, 0x7fffffffu, 31u, 100))
            .isEqualTo(Word(0u))
    }

    @Test
    fun `or operations apply bitwise or`() {
        expectThat(evaluate(Or, 0b1010u, 0b1100u, 100))
            .isEqualTo(Word(0b1110u))

        expectThat(evaluate(OrImmediate, 0b1010u, 0b1100u, 100))
            .isEqualTo(Word(0b1110u))
    }

    @Test
    fun `or operations handle edge identities`() {
        expectThat(evaluate(Or, 0u, 0u, 100))
            .isEqualTo(Word(0u))

        expectThat(evaluate(OrImmediate, 0x12345678u, 0xffffffffu, 100))
            .isEqualTo(Word(0xffffffffu))
    }

    @Test
    fun `and operations apply bitwise and`() {
        expectThat(evaluate(And, 0b1010u, 0b1100u, 100))
            .isEqualTo(Word(0b1000u))

        expectThat(evaluate(AndImmediate, 0b1010u, 0b1100u, 100))
            .isEqualTo(Word(0b1000u))
    }

    @Test
    fun `and operations handle edge identities`() {
        expectThat(evaluate(And, 0xffffffffu, 0xffffffffu, 100))
            .isEqualTo(Word(0xffffffffu))

        expectThat(evaluate(AndImmediate, 0x12345678u, 0u, 100))
            .isEqualTo(Word(0u))
    }

    @Test
    fun `shift operations use platform masked shift amount semantics`() {
        expectThat(evaluate(ShiftLeftLogical, 1u, 32u, 100))
            .isEqualTo(Word(1u))

        expectThat(evaluate(ShiftRightLogical, 1u, 32u, 100))
            .isEqualTo(Word(1u))

        expectThat(evaluate(ShiftRightArithmetic, 0xffffffffu, 32u, 100))
            .isEqualTo(Word(0xffffffffu))
    }

    private fun evaluate(
        operation: ArithmeticLogicOperation,
        leftOperand: UInt,
        rightOperand: UInt,
        instructionAddress: Int
    ) =
        arithmeticLogicUnit.evaluate(
            operation = operation,
            leftOperand = Word(leftOperand),
            rightOperand = Word(rightOperand),
            instructionAddress = instructionAddress
        )
}
