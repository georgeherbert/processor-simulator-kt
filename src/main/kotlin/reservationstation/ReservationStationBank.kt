package reservationstation

import commondatabus.CommonDataBus
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

interface ReservationStationBank<T> {
    fun enqueue(
        operation: T,
        leftOperand: Operand,
        rightOperand: Operand,
        immediate: Word,
        robId: RobId,
        instructionAddress: InstructionAddress,
    ): ProcessorResult<ReservationStationBank<T>>

    fun dispatchReady(maxCount: Int): ReservationStationDispatchResult<T>
    fun acceptCommonDataBus(commonDataBus: CommonDataBus): ReservationStationBank<T>
    fun clear(): ReservationStationBank<T>
}

data class ReservationStationEntry<T>(
    val reservationStationId: ReservationStationId,
    val operation: T,
    val leftOperand: Operand,
    val rightOperand: Operand,
    val immediate: Word,
    val robId: RobId,
    val instructionAddress: InstructionAddress,
)

data class ReservationStationDispatchResult<T>(
    val reservationStationBank: ReservationStationBank<T>,
    val entries: List<ReadyReservationStationEntry<T>>,
)

data class ReadyReservationStationEntry<T>(
    val reservationStationId: ReservationStationId,
    val operation: T,
    val leftValue: Word,
    val rightValue: Word,
    val immediate: Word,
    val robId: RobId,
    val instructionAddress: InstructionAddress,
)

@ConsistentCopyVisibility
data class RealReservationStationBank<T> private constructor(
    private val capacity: Int,
    private val nextReservationStationIdValue: Int,
    private val entries: List<ReservationStationEntry<T>>,
) : ReservationStationBank<T> {

    constructor(size: Size) : this(size.value, 1, emptyList())

    override fun enqueue(
        operation: T,
        leftOperand: Operand,
        rightOperand: Operand,
        immediate: Word,
        robId: RobId,
        instructionAddress: InstructionAddress,
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

    override fun dispatchReady(maxCount: Int): ReservationStationDispatchResult<T> {
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
            is PendingOperand -> commonDataBus.resolveOperand(this)
        }

    private fun ReservationStationEntry<T>.toReadyEntryOrNull() =
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
