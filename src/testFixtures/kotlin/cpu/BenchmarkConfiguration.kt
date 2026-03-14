package cpu

import fetch.FetchWidth
import types.Size

fun benchmarkConfiguration() =
    testProcessorConfiguration().copy(
        fetchWidth = FetchWidth(8),
        issueWidth = IssueWidth(8),
        commitWidth = CommitWidth(8),
        instructionQueueSize = Size(64),
        reorderBufferSize = Size(128),
        arithmeticLogicReservationStationCount = Size(64),
        branchReservationStationCount = Size(16),
        memoryBufferCount = Size(64),
        arithmeticLogicUnitCount = Size(4),
        branchUnitCount = Size(2),
        addressUnitCount = Size(2),
        memoryUnitCount = Size(2)
    )
