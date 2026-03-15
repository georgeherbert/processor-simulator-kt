package decoder

import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.map
import types.*

data object RealInstructionDecoder : InstructionDecoder {

    override fun decode(
        instruction: Word,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        when (val opcode = opcodeOf(instruction)) {
            OpcodeOperationImmediate -> decodeOperationImmediate(iTypeFieldsOf(instruction))
            OpcodeLoadUpperImmediate -> decodeLoadUpperImmediate(uTypeFieldsOf(instruction))
            OpcodeAddUpperImmediateToProgramCounter ->
                decodeAddUpperImmediateToProgramCounter(uTypeFieldsOf(instruction), instructionAddress)

            OpcodeOperation -> decodeOperationRegister(rTypeFieldsOf(instruction))
            OpcodeJumpAndLink ->
                decodeJumpAndLink(jTypeFieldsOf(instruction), instructionAddress, predictedNextInstructionAddress)

            OpcodeJumpAndLinkRegister ->
                decodeJumpAndLinkRegister(iTypeFieldsOf(instruction), instructionAddress, predictedNextInstructionAddress)

            OpcodeBranch -> decodeBranch(bTypeFieldsOf(instruction), instructionAddress, predictedNextInstructionAddress)
            OpcodeLoad -> decodeLoad(iTypeFieldsOf(instruction))
            OpcodeStore -> decodeStore(sTypeFieldsOf(instruction))
            OpcodeMiscMemory -> DecoderUnknownOpcode(opcode).asFailure()
            OpcodeSystem -> DecoderUnknownOpcode(opcode).asFailure()
            else -> DecoderUnknownOpcode(opcode).asFailure()
        }

    private fun decodeOperationImmediate(iTypeFields: ITypeFields) =
        decodeArithmeticImmediateOperation(iTypeFields)
            .map { operationAndImmediate ->
                DecodedArithmeticImmediateInstruction(
                    operationAndImmediate.operation,
                    iTypeFields.destinationRegisterAddress,
                    iTypeFields.sourceRegisterAddress,
                    operationAndImmediate.immediate
                )
            }

    private fun decodeArithmeticImmediateOperation(iTypeFields: ITypeFields) =
        when (iTypeFields.funct3) {
            Funct3AddImmediate ->
                ArithmeticImmediateOperationAndImmediate(AddImmediateOperation, iTypeFields.immediate).asSuccess()

            Funct3SetLessThanImmediateSigned ->
                ArithmeticImmediateOperationAndImmediate(
                    SetLessThanImmediateSignedOperation,
                    iTypeFields.immediate
                ).asSuccess()

            Funct3SetLessThanImmediateUnsigned ->
                ArithmeticImmediateOperationAndImmediate(
                    SetLessThanImmediateUnsignedOperation,
                    iTypeFields.immediate
                ).asSuccess()

            Funct3ExclusiveOrImmediate ->
                ArithmeticImmediateOperationAndImmediate(
                    ExclusiveOrImmediateOperation,
                    iTypeFields.immediate
                ).asSuccess()

            Funct3OrImmediate ->
                ArithmeticImmediateOperationAndImmediate(OrImmediateOperation, iTypeFields.immediate).asSuccess()

            Funct3AndImmediate ->
                ArithmeticImmediateOperationAndImmediate(AndImmediateOperation, iTypeFields.immediate).asSuccess()

            Funct3ShiftLeftLogicalImmediate ->
                decodeShiftLeftImmediateOperation(shiftImmediateFieldsOf(iTypeFields))

            Funct3ShiftRightLogicalOrArithmeticImmediate ->
                decodeShiftRightImmediateOperation(shiftImmediateFieldsOf(iTypeFields))

            else -> DecoderUnknownFunct3(OpcodeOperationImmediate, iTypeFields.funct3).asFailure()
        }

    private fun decodeShiftLeftImmediateOperation(shiftImmediateFields: ShiftImmediateFields) =
        when (shiftImmediateFields.functionCode7) {
            Funct7ShiftLeftLogical ->
                ArithmeticImmediateOperationAndImmediate(
                    ShiftLeftLogicalImmediateOperation,
                    shiftImmediateFields.shiftAmount
                ).asSuccess()

            else ->
                DecoderUnknownFunct7(
                    OpcodeOperationImmediate,
                    Funct3ShiftLeftLogicalImmediate,
                    shiftImmediateFields.functionCode7
                ).asFailure()
        }

    private fun decodeShiftRightImmediateOperation(shiftImmediateFields: ShiftImmediateFields) =
        when (shiftImmediateFields.functionCode7) {
            Funct7ShiftRightLogical ->
                ArithmeticImmediateOperationAndImmediate(
                    ShiftRightLogicalImmediateOperation,
                    shiftImmediateFields.shiftAmount
                ).asSuccess()

            Funct7ShiftRightArithmetic ->
                ArithmeticImmediateOperationAndImmediate(
                    ShiftRightArithmeticImmediateOperation,
                    shiftImmediateFields.shiftAmount
                ).asSuccess()

            else ->
                DecoderUnknownFunct7(
                    OpcodeOperationImmediate,
                    Funct3ShiftRightLogicalOrArithmeticImmediate,
                    shiftImmediateFields.functionCode7
                ).asFailure()
        }

    private fun decodeLoadUpperImmediate(uTypeFields: UTypeFields) =
        DecodedLoadUpperImmediateInstruction(
            uTypeFields.destinationRegisterAddress,
            uTypeFields.immediate
        ).asSuccess()

    private fun decodeAddUpperImmediateToProgramCounter(
        uTypeFields: UTypeFields,
        instructionAddress: Word
    ) =
        DecodedAddUpperImmediateToProgramCounterInstruction(
            uTypeFields.destinationRegisterAddress,
            uTypeFields.immediate,
            instructionAddress
        ).asSuccess()

    private fun decodeOperationRegister(rTypeFields: RTypeFields) =
        decodeArithmeticRegisterOperation(rTypeFields)
            .map { operation ->
                DecodedArithmeticRegisterInstruction(
                    operation,
                    rTypeFields.destinationRegisterAddress,
                    rTypeFields.leftSourceRegisterAddress,
                    rTypeFields.rightSourceRegisterAddress
                )
            }

    private fun decodeArithmeticRegisterOperation(rTypeFields: RTypeFields) =
        when (rTypeFields.funct3) {
            Funct3AddOrSubtract -> decodeAddOrSubtractOperation(rTypeFields)
            Funct3ShiftLeftLogical -> decodeFixedFunct7RegisterOperation(rTypeFields, ShiftLeftLogicalOperation)
            Funct3SetLessThanSigned -> decodeFixedFunct7RegisterOperation(rTypeFields, SetLessThanSignedOperation)
            Funct3SetLessThanUnsigned -> decodeFixedFunct7RegisterOperation(rTypeFields, SetLessThanUnsignedOperation)
            Funct3ExclusiveOr -> decodeFixedFunct7RegisterOperation(rTypeFields, ExclusiveOrOperation)
            Funct3ShiftRightLogicalOrArithmetic -> decodeShiftRightRegisterOperation(rTypeFields)
            Funct3Or -> decodeFixedFunct7RegisterOperation(rTypeFields, OrOperation)
            Funct3And -> decodeFixedFunct7RegisterOperation(rTypeFields, AndOperation)
            else -> DecoderUnknownFunct3(OpcodeOperation, rTypeFields.funct3).asFailure()
        }

    private fun decodeFixedFunct7RegisterOperation(
        rTypeFields: RTypeFields,
        operation: ArithmeticRegisterOperation
    ) =
        when (rTypeFields.funct7) {
            Funct7Add -> operation.asSuccess()
            else -> DecoderUnknownFunct7(OpcodeOperation, rTypeFields.funct3, rTypeFields.funct7).asFailure()
        }

    private fun decodeAddOrSubtractOperation(rTypeFields: RTypeFields) =
        when (rTypeFields.funct7) {
            Funct7Add -> AddOperation.asSuccess()
            Funct7Subtract -> SubtractOperation.asSuccess()
            else -> DecoderUnknownFunct7(OpcodeOperation, Funct3AddOrSubtract, rTypeFields.funct7).asFailure()
        }

    private fun decodeShiftRightRegisterOperation(rTypeFields: RTypeFields) =
        when (rTypeFields.funct7) {
            Funct7ShiftRightLogical -> ShiftRightLogicalOperation.asSuccess()
            Funct7ShiftRightArithmetic -> ShiftRightArithmeticOperation.asSuccess()
            else ->
                DecoderUnknownFunct7(
                    OpcodeOperation,
                    Funct3ShiftRightLogicalOrArithmetic,
                    rTypeFields.funct7
                ).asFailure()
        }

    private fun decodeJumpAndLink(
        jTypeFields: JTypeFields,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        DecodedJumpAndLinkInstruction(
            jTypeFields.destinationRegisterAddress,
            jTypeFields.immediate,
            instructionAddress,
            predictedNextInstructionAddress
        ).asSuccess()

    private fun decodeJumpAndLinkRegister(
        iTypeFields: ITypeFields,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        when (iTypeFields.funct3) {
            Funct3JumpAndLinkRegister ->
                DecodedJumpAndLinkRegisterInstruction(
                    iTypeFields.destinationRegisterAddress,
                    iTypeFields.sourceRegisterAddress,
                    iTypeFields.immediate,
                    instructionAddress,
                    predictedNextInstructionAddress
                ).asSuccess()

            else -> DecoderUnknownFunct3(OpcodeJumpAndLinkRegister, iTypeFields.funct3).asFailure()
        }

    private fun decodeBranch(
        bTypeFields: BTypeFields,
        instructionAddress: Word,
        predictedNextInstructionAddress: Word
    ) =
        decodeBranchOperation(bTypeFields)
            .map { operation ->
                DecodedBranchInstruction(
                    operation,
                    bTypeFields.leftSourceRegisterAddress,
                    bTypeFields.rightSourceRegisterAddress,
                    bTypeFields.immediate,
                    instructionAddress,
                    predictedNextInstructionAddress
                )
            }

    private fun decodeBranchOperation(bTypeFields: BTypeFields) =
        when (bTypeFields.funct3) {
            Funct3BranchEqual -> BranchEqualOperation.asSuccess()
            Funct3BranchNotEqual -> BranchNotEqualOperation.asSuccess()
            Funct3BranchLessThanSigned -> BranchLessThanSignedOperation.asSuccess()
            Funct3BranchGreaterThanOrEqualSigned -> BranchGreaterThanOrEqualSignedOperation.asSuccess()
            Funct3BranchLessThanUnsigned -> BranchLessThanUnsignedOperation.asSuccess()
            Funct3BranchGreaterThanOrEqualUnsigned -> BranchGreaterThanOrEqualUnsignedOperation.asSuccess()
            else -> DecoderUnknownFunct3(OpcodeBranch, bTypeFields.funct3).asFailure()
        }

    private fun decodeLoad(iTypeFields: ITypeFields) =
        decodeLoadOperation(iTypeFields)
            .map { operation ->
                DecodedLoadInstruction(
                    operation,
                    iTypeFields.destinationRegisterAddress,
                    iTypeFields.sourceRegisterAddress,
                    iTypeFields.immediate
                )
            }

    private fun decodeLoadOperation(iTypeFields: ITypeFields) =
        when (iTypeFields.funct3) {
            Funct3LoadByte -> LoadByteOperation.asSuccess()
            Funct3LoadHalfWord -> LoadHalfWordOperation.asSuccess()
            Funct3LoadWord -> LoadWordOperation.asSuccess()
            Funct3LoadByteUnsigned -> LoadByteUnsignedOperation.asSuccess()
            Funct3LoadHalfWordUnsigned -> LoadHalfWordUnsignedOperation.asSuccess()
            else -> DecoderUnknownFunct3(OpcodeLoad, iTypeFields.funct3).asFailure()
        }

    private fun decodeStore(sTypeFields: STypeFields) =
        decodeStoreOperation(sTypeFields)
            .map { operation ->
                DecodedStoreInstruction(
                    operation,
                    sTypeFields.baseRegisterAddress,
                    sTypeFields.valueRegisterAddress,
                    sTypeFields.immediate
                )
            }

    private fun decodeStoreOperation(sTypeFields: STypeFields) =
        when (sTypeFields.funct3) {
            Funct3StoreByte -> StoreByteOperation.asSuccess()
            Funct3StoreHalfWord -> StoreHalfWordOperation.asSuccess()
            Funct3StoreWord -> StoreWordOperation.asSuccess()
            else -> DecoderUnknownFunct3(OpcodeStore, sTypeFields.funct3).asFailure()
        }
}
