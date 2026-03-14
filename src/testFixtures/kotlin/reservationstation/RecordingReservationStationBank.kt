package reservationstation

import commondatabus.CommonDataBus
import dev.forkhandles.result4k.asFailure
import dev.forkhandles.result4k.asSuccess
import types.*

data class ReservationStationEnqueueCall<T>(
    val operation: T,
    val leftOperand: Operand,
    val rightOperand: Operand,
    val immediate: Word,
    val robId: RobId,
    val instructionAddress: InstructionAddress
)

@ConsistentCopyVisibility
data class RecordingReservationStationBank<T> private constructor(
    private val enqueueFailure: ProcessorError?,
    val enqueueCalls: List<ReservationStationEnqueueCall<T>>
) : ReservationStationBank<T> {

    constructor() : this(null, emptyList())

    constructor(enqueueFailure: ProcessorError) : this(enqueueFailure, emptyList())

    override fun enqueue(
        operation: T,
        leftOperand: Operand,
        rightOperand: Operand,
        immediate: Word,
        robId: RobId,
        instructionAddress: InstructionAddress
    ) =
        enqueueFailure
            ?.asFailure()
            ?: copy(
                enqueueCalls = enqueueCalls + ReservationStationEnqueueCall(
                    operation,
                    leftOperand,
                    rightOperand,
                    immediate,
                    robId,
                    instructionAddress
                )
            ).asSuccess()

    override fun dispatchReady(maxCount: Int) =
        ReservationStationDispatchResult(
            this,
            enqueueCalls
                .mapIndexedNotNull { index, enqueueCall -> enqueueCall.toReadyEntryOrNull(index + 1) }
                .take(maxCount)
        )

    override fun acceptCommonDataBus(commonDataBus: CommonDataBus) =
        copy(
            enqueueCalls = enqueueCalls.map { enqueueCall ->
                enqueueCall.copy(
                    leftOperand = enqueueCall.leftOperand.resolved(commonDataBus),
                    rightOperand = enqueueCall.rightOperand.resolved(commonDataBus)
                )
            }
        )

    override fun clear() = copy(enqueueCalls = emptyList())

    private fun ReservationStationEnqueueCall<T>.toReadyEntryOrNull(reservationStationIdValue: Int) =
        when {
            leftOperand is ReadyOperand && rightOperand is ReadyOperand ->
                ReadyReservationStationEntry(
                    ReservationStationId(reservationStationIdValue),
                    operation,
                    leftOperand.value,
                    rightOperand.value,
                    immediate,
                    robId,
                    instructionAddress
                )

            else -> null
        }

    private fun Operand.resolved(commonDataBus: CommonDataBus): Operand =
        when (this) {
            is ReadyOperand -> this
            is PendingOperand -> commonDataBus.resolveOperand(this)
        }
}
