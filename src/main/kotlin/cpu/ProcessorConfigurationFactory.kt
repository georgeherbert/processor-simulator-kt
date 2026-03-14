package cpu

import fetch.FetchWidth
import types.BitWidth
import types.CycleCount
import types.Size

interface ProcessorConfigurationFactory {
    fun create(): ProcessorConfiguration
}

data object ReferenceProcessorConfigurationFactory : ProcessorConfigurationFactory {

    override fun create() =
        ProcessorConfiguration(
            FetchWidth(16),
            IssueWidth(16),
            CommitWidth(16),
            Size(128),
            Size(128),
            Size(64),
            BitWidth(2),
            Size(128),
            Size(128),
            Size(128),
            Size(8),
            Size(2),
            Size(8),
            Size(8),
            ArithmeticLogicLatencies(
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1),
                CycleCount(1)
            ),
            BranchLatencies(
                CycleCount(1),
                CycleCount(1),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2)
            ),
            CycleCount(1),
            MemoryLatencies(
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2),
                CycleCount(2)
            )
        )
}
