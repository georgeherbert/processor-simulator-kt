package cpu

data class ProcessorStatistics(
    val cycleCount: Int,
    val committedInstructionCount: Int
)

fun ProcessorStatistics.updatedWith(statisticsDelta: commit.CommitStatisticsDelta) =
    ProcessorStatistics(
        cycleCount + 1,
        committedInstructionCount + statisticsDelta.committedInstructionCount
    )
