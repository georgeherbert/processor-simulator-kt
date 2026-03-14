package cpu

import fetch.FetchWidth
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.BitWidth
import types.CycleCount
import types.Size

class ProcessorConfigurationFactoryTest {

    @Test
    fun `creates the reference processor configuration`() {
        expectThat(ReferenceProcessorConfigurationFactory.create())
            .isEqualTo(
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
            )
    }
}
