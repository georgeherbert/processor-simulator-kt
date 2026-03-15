package issue

import arithmeticlogic.Add
import arithmeticlogic.AddImmediate
import arithmeticlogic.AddUpperImmediateToProgramCounter
import arithmeticlogic.And
import arithmeticlogic.AndImmediate
import arithmeticlogic.ArithmeticLogicOperation
import arithmeticlogic.ExclusiveOr
import arithmeticlogic.ExclusiveOrImmediate
import arithmeticlogic.LoadUpperImmediate
import arithmeticlogic.Or
import arithmeticlogic.OrImmediate
import arithmeticlogic.SetLessThanImmediateSigned
import arithmeticlogic.SetLessThanImmediateUnsigned
import arithmeticlogic.SetLessThanSigned
import arithmeticlogic.SetLessThanUnsigned
import arithmeticlogic.ShiftLeftLogical
import arithmeticlogic.ShiftLeftLogicalImmediate
import arithmeticlogic.ShiftRightArithmetic
import arithmeticlogic.ShiftRightArithmeticImmediate
import arithmeticlogic.ShiftRightLogical
import arithmeticlogic.ShiftRightLogicalImmediate
import arithmeticlogic.Subtract
import decoder.*
import types.InstructionAddress
import types.Operand
import types.PendingOperand
import types.ReadyOperand
import types.RegisterAddress
import types.Word

internal fun DecodedInstruction.toIssueRequest(workingState: IssueWorkingState): IssueRequest =
    when (this) {
        is DecodedArithmeticImmediateInstruction ->
            ArithmeticIssueRequest(
                arithmeticOperationFor(operation),
                resolveRegisterOperand(workingState, sourceRegisterAddress),
                ReadyOperand(immediate),
                zeroInstructionAddress(),
                destinationRegisterAddress
            )

        is DecodedArithmeticRegisterInstruction ->
            ArithmeticIssueRequest(
                arithmeticOperationFor(operation),
                resolveRegisterOperand(workingState, leftSourceRegisterAddress),
                resolveRegisterOperand(workingState, rightSourceRegisterAddress),
                zeroInstructionAddress(),
                destinationRegisterAddress
            )

        is DecodedLoadUpperImmediateInstruction ->
            ArithmeticIssueRequest(
                LoadUpperImmediate,
                ReadyOperand(immediate),
                ReadyOperand(zeroWord()),
                zeroInstructionAddress(),
                destinationRegisterAddress
            )

        is DecodedAddUpperImmediateToProgramCounterInstruction ->
            ArithmeticIssueRequest(
                AddUpperImmediateToProgramCounter,
                ReadyOperand(immediate),
                ReadyOperand(zeroWord()),
                instructionAddress.asInstructionAddress(),
                destinationRegisterAddress
            )

        is DecodedJumpAndLinkInstruction ->
            JumpIssueRequest(
                branchlogic.JumpAndLink,
                ReadyOperand(zeroWord()),
                immediate,
                instructionAddress.asInstructionAddress(),
                predictedNextInstructionAddress.asInstructionAddress(),
                destinationRegisterAddress
            )

        is DecodedJumpAndLinkRegisterInstruction ->
            JumpIssueRequest(
                branchlogic.JumpAndLinkRegister,
                resolveRegisterOperand(workingState, sourceRegisterAddress),
                immediate,
                instructionAddress.asInstructionAddress(),
                predictedNextInstructionAddress.asInstructionAddress(),
                destinationRegisterAddress
            )

        is DecodedBranchInstruction ->
            BranchIssueRequest(
                branchOperationFor(operation),
                resolveRegisterOperand(workingState, leftSourceRegisterAddress),
                resolveRegisterOperand(workingState, rightSourceRegisterAddress),
                immediate,
                instructionAddress.asInstructionAddress(),
                predictedNextInstructionAddress.asInstructionAddress()
            )

        is DecodedLoadInstruction ->
            LoadIssueRequest(
                operation,
                resolveRegisterOperand(workingState, baseRegisterAddress),
                immediate,
                destinationRegisterAddress
            )

        is DecodedStoreInstruction ->
            StoreIssueRequest(
                operation,
                resolveRegisterOperand(workingState, baseRegisterAddress),
                resolveRegisterOperand(workingState, valueRegisterAddress),
                immediate
            )
    }

private fun resolveRegisterOperand(
    workingState: IssueWorkingState,
    registerAddress: RegisterAddress
): Operand =
    when (val operand = workingState.registerFile.operandFor(registerAddress)) {
        is ReadyOperand -> operand
        is PendingOperand -> workingState.reorderBuffer.resolveOperand(operand)
    }

private fun arithmeticOperationFor(operation: ArithmeticImmediateOperation): ArithmeticLogicOperation =
    when (operation) {
        AddImmediateOperation -> AddImmediate
        SetLessThanImmediateSignedOperation -> SetLessThanImmediateSigned
        SetLessThanImmediateUnsignedOperation -> SetLessThanImmediateUnsigned
        ExclusiveOrImmediateOperation -> ExclusiveOrImmediate
        OrImmediateOperation -> OrImmediate
        AndImmediateOperation -> AndImmediate
        ShiftLeftLogicalImmediateOperation -> ShiftLeftLogicalImmediate
        ShiftRightLogicalImmediateOperation -> ShiftRightLogicalImmediate
        ShiftRightArithmeticImmediateOperation -> ShiftRightArithmeticImmediate
    }

private fun arithmeticOperationFor(operation: ArithmeticRegisterOperation): ArithmeticLogicOperation =
    when (operation) {
        AddOperation -> Add
        SubtractOperation -> Subtract
        ShiftLeftLogicalOperation -> ShiftLeftLogical
        SetLessThanSignedOperation -> SetLessThanSigned
        SetLessThanUnsignedOperation -> SetLessThanUnsigned
        ExclusiveOrOperation -> ExclusiveOr
        ShiftRightLogicalOperation -> ShiftRightLogical
        ShiftRightArithmeticOperation -> ShiftRightArithmetic
        OrOperation -> Or
        AndOperation -> And
    }

private fun branchOperationFor(operation: decoder.BranchOperation): branchlogic.BranchOperation =
    when (operation) {
        BranchEqualOperation -> branchlogic.BranchEqual
        BranchNotEqualOperation -> branchlogic.BranchNotEqual
        BranchLessThanSignedOperation -> branchlogic.BranchLessThanSigned
        BranchLessThanUnsignedOperation -> branchlogic.BranchLessThanUnsigned
        BranchGreaterThanOrEqualSignedOperation -> branchlogic.BranchGreaterThanOrEqualSigned
        BranchGreaterThanOrEqualUnsignedOperation -> branchlogic.BranchGreaterThanOrEqualUnsigned
    }

private fun zeroInstructionAddress() = InstructionAddress(0)

private fun Word.asInstructionAddress() = InstructionAddress(value.toInt())

private fun zeroWord() = Word(0u)
