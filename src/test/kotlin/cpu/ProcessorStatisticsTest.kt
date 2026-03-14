package cpu

import commit.CommitStatisticsDelta
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ProcessorStatisticsTest {

    @Test
    fun `updated with increments the cycle count and applies the commit deltas`() {
        expectThat(
            ProcessorStatistics(10, 20, 30, 40).updatedWith(
                CommitStatisticsDelta(1, 2, 3)
            )
        )
            .isEqualTo(ProcessorStatistics(11, 21, 32, 43))
    }
}
