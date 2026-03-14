package cpu

import fetch.FetchWidth
import types.BitWidth
import types.CycleCount
import types.Size

fun testProcessorConfiguration() =
    ProcessorConfiguration(
        FetchWidth(2),
        IssueWidth(2),
        CommitWidth(2),
        Size(8),
        Size(8),
        Size(8),
        BitWidth(2),
        Size(8),
        Size(8),
        Size(8),
        Size(1),
        Size(1),
        Size(1),
        Size(1),
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
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1)
        ),
        CycleCount(1),
        MemoryLatencies(
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1),
            CycleCount(1)
        )
    )
