package branchpredictor

import types.ProgramCounter

interface BranchListener {
    fun outcome(programCounter: ProgramCounter, taken: Boolean): BranchListener
}
