package control

import types.InstructionAddress

sealed interface ProgramCounterSelection

data object PredictedProgramCounterSelection : ProgramCounterSelection

data class RedirectedProgramCounterSelection(val targetInstructionAddress: InstructionAddress) : ProgramCounterSelection

data class ControlState(
    val fetchInstructionAddress: InstructionAddress,
    val programCounterSelection: ProgramCounterSelection
)
