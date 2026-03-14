package cpu

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isA

class ProcessorCreatorTest {

    @Test
    fun `creates a real processor from the supplied configuration`() {
        expectThat(RealProcessorCreator.create(testProcessorConfiguration()))
            .isA<RealProcessor>()
    }
}
