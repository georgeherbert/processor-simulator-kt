package registerfile

import types.*

data class RegisterReservation(
    val registerAddress: RegisterAddress,
    val robId: RobId
)

@ConsistentCopyVisibility
data class StubRegisterFile private constructor(
    private val operandsByRegisterAddress: Map<RegisterAddress, Operand>,
    private val committedValuesByRegisterAddress: Map<RegisterAddress, Word>,
    val reservations: List<RegisterReservation>
) : RegisterFile {

    constructor() : this(emptyMap(), emptyMap(), emptyList())

    fun withOperand(registerAddress: RegisterAddress, operand: Operand) =
        copy(operandsByRegisterAddress = operandsByRegisterAddress + (registerAddress to operand))

    override fun operandFor(registerAddress: RegisterAddress) =
        operandsByRegisterAddress[registerAddress]
            ?: when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
                true -> ReadyOperand(Word(0u))
                false -> ReadyOperand(committedValuesByRegisterAddress[registerAddress] ?: Word(0u))
            }

    override fun readCommitted(registerAddress: RegisterAddress) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> Word(0u)
            false -> committedValuesByRegisterAddress[registerAddress] ?: Word(0u)
        }

    override fun reserveDestination(registerAddress: RegisterAddress, robId: RobId) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> this
            false ->
                copy(
                    operandsByRegisterAddress = operandsByRegisterAddress + (registerAddress to PendingOperand(robId)),
                    reservations = reservations + RegisterReservation(registerAddress, robId)
                )
        }

    override fun commit(registerAddress: RegisterAddress, value: Word, robId: RobId) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> this
            false ->
                copy(
                    operandsByRegisterAddress = operandsByRegisterAddress - registerAddress,
                    committedValuesByRegisterAddress = committedValuesByRegisterAddress + (registerAddress to value)
                )
        }

    override fun seed(registerAddress: RegisterAddress, value: Word) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> this
            false ->
                copy(
                    operandsByRegisterAddress = operandsByRegisterAddress - registerAddress,
                    committedValuesByRegisterAddress = committedValuesByRegisterAddress + (registerAddress to value)
                )
        }

    override fun flushPendingDestinations() =
        copy(
            operandsByRegisterAddress = operandsByRegisterAddress.filterValues { operand ->
                operand is ReadyOperand
            }
        )

    private companion object {
        const val ZERO_REGISTER_ADDRESS = 0
    }
}
