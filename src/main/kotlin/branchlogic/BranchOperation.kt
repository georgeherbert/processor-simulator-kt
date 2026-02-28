package branchlogic

sealed interface BranchOperation

data object JumpAndLink : BranchOperation
data object JumpAndLinkRegister : BranchOperation
data object BranchEqual : BranchOperation
data object BranchNotEqual : BranchOperation
data object BranchLessThanSigned : BranchOperation
data object BranchLessThanUnsigned : BranchOperation
data object BranchGreaterThanOrEqualSigned : BranchOperation
data object BranchGreaterThanOrEqualUnsigned : BranchOperation
