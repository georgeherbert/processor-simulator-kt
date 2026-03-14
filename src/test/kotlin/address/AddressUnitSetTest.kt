package address

import cpu.LoadAddressResolution
import cpu.StoreAddressResolution
import decoder.LoadWordOperation
import decoder.StoreWordOperation
import memorybuffer.LoadAddressComputationWork
import memorybuffer.StoreAddressComputationWork
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import types.*

class AddressUnitSetTest {

    @Test
    fun `resolves load and store addresses immediately when latency is one`() {
        val stepResult = RealAddressUnitSet(Size(2), CycleCount(1)).step(
            listOf(
                LoadAddressComputationWork(
                    MemoryBufferId(1),
                    LoadWordOperation,
                    Word(8u),
                    Word(4u),
                    RobId(11)
                ),
                StoreAddressComputationWork(
                    MemoryBufferId(2),
                    StoreWordOperation,
                    Word(12u),
                    Word(8u),
                    RobId(12)
                )
            )
        )

        expectThat(stepResult.addressResolutions)
            .isEqualTo(
                listOf(
                    LoadAddressResolution(MemoryBufferId(1), DataAddress(12)),
                    StoreAddressResolution(MemoryBufferId(2), RobId(12), DataAddress(20))
                )
            )

        expectThat(stepResult.addressUnitSet.availableLaneCount())
            .isEqualTo(2)
    }

    @Test
    fun `delays address resolution until a later cycle when latency is greater than one`() {
        val firstStepResult = RealAddressUnitSet(Size(1), CycleCount(2)).step(
            listOf(
                LoadAddressComputationWork(
                    MemoryBufferId(1),
                    LoadWordOperation,
                    Word(16u),
                    Word(4u),
                    RobId(21)
                )
            )
        )

        expectThat(firstStepResult.addressResolutions)
            .isEqualTo(emptyList())

        expectThat(firstStepResult.addressUnitSet.availableLaneCount())
            .isEqualTo(0)

        val secondStepResult = firstStepResult.addressUnitSet.step(emptyList())

        expectThat(secondStepResult.addressResolutions)
            .isEqualTo(
                listOf(
                    LoadAddressResolution(MemoryBufferId(1), DataAddress(20))
                )
            )
    }

    @Test
    fun `clear drops in flight address work`() {
        val firstStepResult = RealAddressUnitSet(Size(1), CycleCount(2)).step(
            listOf(
                StoreAddressComputationWork(
                    MemoryBufferId(3),
                    StoreWordOperation,
                    Word(16u),
                    Word(4u),
                    RobId(31)
                )
            )
        )

        val clearedUnitSet = firstStepResult.addressUnitSet.clear()
        val secondStepResult = clearedUnitSet.step(emptyList())

        expectThat(secondStepResult.addressResolutions)
            .isEqualTo(emptyList())

        expectThat(secondStepResult.addressUnitSet.availableLaneCount())
            .isEqualTo(1)
    }
}
