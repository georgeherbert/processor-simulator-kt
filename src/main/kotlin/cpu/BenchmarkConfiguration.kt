package cpu

import fetch.FetchWidth
import types.BitWidth
import types.CycleCount
import types.Size

fun benchmarkConfiguration() =
    ProcessorConfiguration(
        FetchWidth(8),
        IssueWidth(8),
        CommitWidth(8),
        Size(64),
        Size(128),
        Size(8),
        BitWidth(2),
        Size(64),
        Size(16),
        Size(64),
        Size(4),
        Size(2),
        Size(2),
        Size(2),
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
