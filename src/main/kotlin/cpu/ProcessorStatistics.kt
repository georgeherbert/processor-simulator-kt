package cpu

data class ProcessorStatistics(
    val cycleCount: Int,
    val committedInstructionCount: Int,
    val branchInstructionCount: Int,
    val mispredictionCount: Int
)

fun ProcessorStatistics.updatedWith(statisticsDelta: commit.CommitStatisticsDelta) =
    ProcessorStatistics(
        cycleCount + 1,
        committedInstructionCount + statisticsDelta.committedInstructionCount,
        branchInstructionCount + statisticsDelta.branchInstructionCount,
        mispredictionCount + statisticsDelta.mispredictionCount
    )
