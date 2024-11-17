package branchpredictor

import types.ProgramCounter

interface BranchPredictor {
    fun predict(programCounter: ProgramCounter): Boolean
}
