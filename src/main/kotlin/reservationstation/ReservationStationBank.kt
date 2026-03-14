package reservationstation

import commondatabus.CommonDataBus
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import types.*

interface ReservationStationBank<Operation> {
    fun freeSlotCount(): Int
    fun entryCount(): Int
    fun enqueue(
        operation: Operation,
        leftOperand: Operand,
        rightOperand: Operand,
        immediate: Word,
        robId: RobId,
        instructionAddress: InstructionAddress
    ): ProcessorResult<ReservationStationBank<Operation>>

    fun dispatchReady(maxCount: Int): ReservationStationDispatchResult<Operation>
    fun acceptCommonDataBus(commonDataBus: CommonDataBus): ReservationStationBank<Operation>
    fun clear(): ReservationStationBank<Operation>
}

data class ReservationStationEntry<Operation>(
    val reservationStationId: ReservationStationId,
    val operation: Operation,
    val leftOperand: Operand,
    val rightOperand: Operand,
    val immediate: Word,
    val robId: RobId,
    val instructionAddress: InstructionAddress
) {
    fun isReady() =
        leftOperand is ReadyOperand && rightOperand is ReadyOperand
}

data class ReservationStationDispatchResult<Operation>(
    val reservationStationBank: ReservationStationBank<Operation>,
    val entries: List<ReadyReservationStationEntry<Operation>>
)

data class ReadyReservationStationEntry<Operation>(
    val reservationStationId: ReservationStationId,
    val operation: Operation,
    val leftValue: Word,
    val rightValue: Word,
    val immediate: Word,
    val robId: RobId,
    val instructionAddress: InstructionAddress
)

@ConsistentCopyVisibility
data class RealReservationStationBank<Operation> private constructor(
    private val capacity: Int,
    private val nextReservationStationIdValue: Int,
    private val entries: List<ReservationStationEntry<Operation>>
) : ReservationStationBank<Operation> {

    constructor(size: Size) : this(size.value, 1, emptyList())

    override fun freeSlotCount() = capacity - entries.size

    override fun entryCount() = entries.size

    override fun enqueue(
        operation: Operation,
        leftOperand: Operand,
        rightOperand: Operand,
        immediate: Word,
        robId: RobId,
        instructionAddress: InstructionAddress
    ) =
        when (entries.size >= capacity) {
            true -> ReservationStationFull.asFailure()
            false ->
                copy(
                    entries = entries + ReservationStationEntry(
                        ReservationStationId(nextReservationStationIdValue),
                        operation,
                        leftOperand,
                        rightOperand,
                        immediate,
                        robId,
                        instructionAddress
                    ),
                    nextReservationStationIdValue = nextReservationStationIdValue + 1
                ).asSuccess()
        }

    override fun dispatchReady(maxCount: Int): ReservationStationDispatchResult<Operation> {
        val readyEntries = entries
            .mapNotNull { entry -> entry.toReadyEntryOrNull() }
            .take(maxCount)

        val dispatchedIds = readyEntries.map { entry -> entry.reservationStationId }

        return ReservationStationDispatchResult(
            copy(entries = entries.filter { entry -> entry.reservationStationId !in dispatchedIds }),
            readyEntries
        )
    }

    override fun acceptCommonDataBus(commonDataBus: CommonDataBus) =
        copy(
            entries = entries.map { entry ->
                entry.copy(
                    leftOperand = entry.leftOperand.resolved(commonDataBus),
                    rightOperand = entry.rightOperand.resolved(commonDataBus)
                )
            }
        )

    override fun clear() = copy(entries = emptyList())

    private fun Operand.resolved(commonDataBus: CommonDataBus): Operand =
        when (this) {
            is ReadyOperand -> this
            is PendingOperand ->
                when (commonDataBus.isValueReady(robId)) {
                    true ->
                        commonDataBus.valueFor(robId)
                            .map { value -> ReadyOperand(value) }
                            .recover { this@resolved }

                    false -> this
                }
        }

    private fun ReservationStationEntry<Operation>.toReadyEntryOrNull() =
        when {
            leftOperand is ReadyOperand && rightOperand is ReadyOperand ->
                ReadyReservationStationEntry(
                    reservationStationId,
                    operation,
                    leftOperand.value,
                    rightOperand.value,
                    immediate,
                    robId,
                    instructionAddress
                )

            else -> null
        }
}
