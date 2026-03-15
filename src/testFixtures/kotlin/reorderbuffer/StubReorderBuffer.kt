package reorderbuffer

import commondatabus.CommonDataBus
import decoder.StoreOperation
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

data class RegisterWriteAllocationCall(
    val destinationRegisterAddress: RegisterAddress,
    val category: RegisterWriteReorderBufferEntryCategory
)

data class JumpAllocationCall(
    val destinationRegisterAddress: RegisterAddress,
    val instructionAddress: InstructionAddress,
    val predictedNextInstructionAddress: InstructionAddress
)

data class BranchAllocationCall(
    val instructionAddress: InstructionAddress,
    val predictedNextInstructionAddress: InstructionAddress
)

data class StoreAllocationCall(
    val operation: StoreOperation,
    val valueOperand: Operand
)

@ConsistentCopyVisibility
data class StubReorderBuffer private constructor(
    private val nextRobIdValue: Int,
    private val resolvedValuesByRobId: Map<RobId, Word>,
    private val registerWriteUnavailable: Boolean,
    private val jumpUnavailable: Boolean,
    private val branchUnavailable: Boolean,
    private val storeUnavailable: Boolean,
    private val registerWriteFailure: ProcessorError?,
    private val jumpFailure: ProcessorError?,
    private val branchFailure: ProcessorError?,
    private val storeFailure: ProcessorError?,
    val registerWriteAllocations: List<RegisterWriteAllocationCall>,
    val jumpAllocations: List<JumpAllocationCall>,
    val branchAllocations: List<BranchAllocationCall>,
    val storeAllocations: List<StoreAllocationCall>
) : ReorderBuffer {

    constructor() : this(
        1,
        emptyMap(),
        false,
        false,
        false,
        false,
        null,
        null,
        null,
        null,
        emptyList(),
        emptyList(),
        emptyList(),
        emptyList()
    )

    fun withResolvedValue(robId: RobId, value: Word) =
        copy(resolvedValuesByRobId = resolvedValuesByRobId + (robId to value))

    fun withRegisterWriteUnavailable() =
        copy(registerWriteUnavailable = true)

    fun withJumpUnavailable() =
        copy(jumpUnavailable = true)

    fun withBranchUnavailable() =
        copy(branchUnavailable = true)

    fun withStoreUnavailable() =
        copy(storeUnavailable = true)

    fun withRegisterWriteFailure(error: ProcessorError) =
        copy(registerWriteFailure = error)

    fun withJumpFailure(error: ProcessorError) =
        copy(jumpFailure = error)

    fun withBranchFailure(error: ProcessorError) =
        copy(branchFailure = error)

    fun withStoreFailure(error: ProcessorError) =
        copy(storeFailure = error)

    override fun enqueueRegisterWrite(
        destinationRegisterAddress: RegisterAddress,
        category: RegisterWriteReorderBufferEntryCategory
    ) =
        when {
            registerWriteFailure != null -> registerWriteFailure.asFailure()
            registerWriteUnavailable -> ReorderBufferAllocationUnavailable.asSuccess()
            else ->
                allocationResult(
                    copy(
                        nextRobIdValue = nextRobIdValue + 1,
                        registerWriteAllocations = registerWriteAllocations + RegisterWriteAllocationCall(
                            destinationRegisterAddress,
                            category
                        )
                    )
                )
        }

    override fun enqueueJump(
        destinationRegisterAddress: RegisterAddress,
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress
    ) =
        when {
            jumpFailure != null -> jumpFailure.asFailure()
            jumpUnavailable -> ReorderBufferAllocationUnavailable.asSuccess()
            else ->
                allocationResult(
                    copy(
                        nextRobIdValue = nextRobIdValue + 1,
                        jumpAllocations = jumpAllocations + JumpAllocationCall(
                            destinationRegisterAddress,
                            instructionAddress,
                            predictedNextInstructionAddress
                        )
                    )
                )
        }

    override fun enqueueBranch(
        instructionAddress: InstructionAddress,
        predictedNextInstructionAddress: InstructionAddress
    ) =
        when {
            branchFailure != null -> branchFailure.asFailure()
            branchUnavailable -> ReorderBufferAllocationUnavailable.asSuccess()
            else ->
                allocationResult(
                    copy(
                        nextRobIdValue = nextRobIdValue + 1,
                        branchAllocations = branchAllocations + BranchAllocationCall(
                            instructionAddress,
                            predictedNextInstructionAddress
                        )
                    )
                )
        }

    override fun enqueueStore(
        operation: StoreOperation,
        valueOperand: Operand
    ) =
        when {
            storeFailure != null -> storeFailure.asFailure()
            storeUnavailable -> ReorderBufferAllocationUnavailable.asSuccess()
            else ->
                allocationResult(
                    copy(
                        nextRobIdValue = nextRobIdValue + 1,
                        storeAllocations = storeAllocations + StoreAllocationCall(operation, valueOperand)
                    )
                )
        }

    override fun acceptCommonDataBus(commonDataBus: CommonDataBus) = this

    override fun recordStoreAddress(robId: RobId, address: DataAddress) = this

    override fun recordBranchActualNextInstructionAddress(
        robId: RobId,
        actualNextInstructionAddress: InstructionAddress
    ) = this

    override fun resolveOperand(operand: PendingOperand) =
        resolvedValuesByRobId[operand.robId]
            ?.let { value -> ReadyOperand(value) }
            ?: operand

    override fun hasEarlierStore(robId: RobId, address: DataAddress) = false

    override fun commitReadyHeadIfPossible() = ReorderBufferCommitReadyHeadUnavailable

    override fun commitReadyHead() = ReorderBufferEmpty.asFailure()

    override fun clear() = StubReorderBuffer()

    private fun allocationResult(reorderBuffer: StubReorderBuffer) =
        ReorderBufferAllocationResult(reorderBuffer, RobId(nextRobIdValue)).asSuccess()
}
