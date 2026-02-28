package register

import types.Word

interface Register {
    fun read(): Word
    fun write(value: Word): Register
}

@ConsistentCopyVisibility
data class RealRegister private constructor(
    private val value: Word
) : Register {

    constructor() : this(Word(0u))

    override fun read() = value

    override fun write(value: Word) = RealRegister(value)
}
