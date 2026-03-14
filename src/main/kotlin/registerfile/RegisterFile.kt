package registerfile

import types.*

interface RegisterFile {
    fun operandFor(registerAddress: RegisterAddress): Operand
    fun readCommitted(registerAddress: RegisterAddress): Word
    fun reserveDestination(registerAddress: RegisterAddress, robId: RobId): RegisterFile
    fun commit(registerAddress: RegisterAddress, value: Word, robId: RobId): RegisterFile
    fun seed(registerAddress: RegisterAddress, value: Word): RegisterFile
    fun flushPendingDestinations(): RegisterFile
}

@ConsistentCopyVisibility
data class RealRegisterFile private constructor(
    private val entries: List<Entry>
) : RegisterFile {

    constructor() : this(List(REGISTER_COUNT) { Entry(Word(0u), null) })

    override fun operandFor(registerAddress: RegisterAddress) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> ReadyOperand(Word(0u))
            false ->
                entries[registerAddress.value].pendingRobId
                    ?.let { pendingRobId -> PendingOperand(pendingRobId) }
                    ?: ReadyOperand(entries[registerAddress.value].value)
        }

    override fun readCommitted(registerAddress: RegisterAddress) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> Word(0u)
            false -> entries[registerAddress.value].value
        }

    override fun reserveDestination(registerAddress: RegisterAddress, robId: RobId) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> this
            false -> copy(entries = entries.withEntry(registerAddress, entries[registerAddress.value].copy(pendingRobId = robId)))
        }

    override fun commit(registerAddress: RegisterAddress, value: Word, robId: RobId) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> this
            false ->
                entries[registerAddress.value]
                    .let { currentEntry ->
                        copy(
                            entries = entries.withEntry(
                                registerAddress,
                                currentEntry.copy(
                                    value = value,
                                    pendingRobId = when (currentEntry.pendingRobId == robId) {
                                        true -> null
                                        false -> currentEntry.pendingRobId
                                    }
                                )
                            )
                        )
                    }
        }

    override fun seed(registerAddress: RegisterAddress, value: Word) =
        when (registerAddress.value == ZERO_REGISTER_ADDRESS) {
            true -> this
            false -> copy(entries = entries.withEntry(registerAddress, Entry(value, null)))
        }

    override fun flushPendingDestinations() =
        copy(entries = entries.mapIndexed { index, entry -> flushedEntryFor(index, entry) })

    private fun List<Entry>.withEntry(registerAddress: RegisterAddress, entry: Entry) =
        toMutableList().apply {
            this[registerAddress.value] = entry
        }

    private fun flushedEntryFor(index: Int, entry: Entry) =
        when (index == ZERO_REGISTER_ADDRESS) {
            true -> Entry(Word(0u), null)
            false -> entry.copy(pendingRobId = null)
        }

    private data class Entry(
        val value: Word,
        val pendingRobId: RobId?
    )

    private companion object {
        const val REGISTER_COUNT = 32
        const val ZERO_REGISTER_ADDRESS = 0
    }
}
