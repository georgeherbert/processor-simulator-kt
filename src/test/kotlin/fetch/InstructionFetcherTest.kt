package fetch

import branchpredictor.BranchTargetPredictor
import branchpredictor.StubBranchTargetPredictor
import instructionqueue.InstructionQueueSlots
import instructionqueue.InstructionQueueSlotsSource
import instructionqueue.StubInstructionQueueSlotsSource
import mainmemory.RealMainMemory
import mainmemory.StubMainMemoryLoader
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*

class InstructionFetcherTest {

    @Test
    fun `step fails when instruction queue slot source returns negative slot count`() {
        val fetchUnit = fetchUnit(
            2,
            mapOf(0 to 0x11u),
            16,
            sequentialBranchTargetPredictor(),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(-1))
        )

        expectThat(fetchUnit.step(InstructionAddress(0)))
            .isFailure()
            .isEqualTo(InstructionQueueSlotCountInvalid(-1))
    }

    @Test
    fun `step returns no instructions when fetch width is zero`() {
        val mainMemoryLoader = StubMainMemoryLoader(emptyMap(), setOf(4))
        val fetchUnit = RealInstructionFetcher(
            FetchWidth(0),
            mainMemoryLoader,
            StubBranchTargetPredictor(emptyMap()),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )
        val stepResult = expectThat(fetchUnit.step(InstructionAddress(4)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions)
            .isEqualTo(emptyList())

        expectThat(stepResult.nextInstructionAddress)
            .isEqualTo(InstructionAddress(4))
    }

    @Test
    fun `step returns no instructions when available queue slots are zero`() {
        val mainMemoryLoader = StubMainMemoryLoader(emptyMap(), setOf(8))
        val fetchUnit = RealInstructionFetcher(
            FetchWidth(2),
            mainMemoryLoader,
            StubBranchTargetPredictor(emptyMap()),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(0))
        )
        val stepResult = expectThat(fetchUnit.step(InstructionAddress(8)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions)
            .isEqualTo(emptyList())

        expectThat(stepResult.nextInstructionAddress)
            .isEqualTo(InstructionAddress(8))
    }

    @Test
    fun `step uses fetch width when available queue slots are larger`() {
        val fetchUnit = fetchUnit(
            2,
            mapOf(0 to 0x11u, 4 to 0x22u, 8 to 0x33u),
            32,
            sequentialBranchTargetPredictor(0, 4),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )
        val stepResult = expectThat(fetchUnit.step(InstructionAddress(0)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions.size)
            .isEqualTo(2)
    }

    @Test
    fun `step uses available queue slots when smaller than fetch width`() {
        val fetchUnit = fetchUnit(
            4,
            mapOf(0 to 0x11u, 4 to 0x22u, 8 to 0x33u),
            32,
            sequentialBranchTargetPredictor(0, 4),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(2))
        )
        val stepResult = expectThat(fetchUnit.step(InstructionAddress(0)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions.size)
            .isEqualTo(2)
    }

    @Test
    fun `step starts from provided instruction address`() {
        val fetchUnit = fetchUnit(
            1,
            mapOf(16 to 0x99u),
            32,
            sequentialBranchTargetPredictor(16),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )
        val stepResult = expectThat(fetchUnit.step(InstructionAddress(16)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions.first().instructionAddress)
            .isEqualTo(InstructionAddress(16))

        expectThat(stepResult.nextInstructionAddress)
            .isEqualTo(InstructionAddress(20))
    }

    @Test
    fun `step fetches sequential batch and can continue with a new fetch unit instance`() {
        val mainMemory = mainMemory(32, mapOf(0 to 0x11u, 4 to 0x22u, 8 to 0x33u))
        val capacitySource = StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        val firstFetchUnit = RealInstructionFetcher(
            FetchWidth(2),
            mainMemory,
            sequentialBranchTargetPredictor(0, 4),
            capacitySource
        )
        val firstStep = expectThat(firstFetchUnit.step(InstructionAddress(0)))
            .isSuccess()
            .subject

        expectThat(firstStep.fetchedInstructions.size)
            .isEqualTo(2)

        expectThat(firstStep.fetchedInstructions[0].instructionAddress)
            .isEqualTo(InstructionAddress(0))

        expectThat(firstStep.fetchedInstructions[0].instruction)
            .isEqualTo(Word(0x11u))

        expectThat(firstStep.fetchedInstructions[1].instructionAddress)
            .isEqualTo(InstructionAddress(4))

        expectThat(firstStep.fetchedInstructions[1].instruction)
            .isEqualTo(Word(0x22u))

        expectThat(firstStep.nextInstructionAddress)
            .isEqualTo(InstructionAddress(8))

        val secondFetchUnit = RealInstructionFetcher(
            FetchWidth(2),
            mainMemory,
            sequentialBranchTargetPredictor(8, 12),
            capacitySource
        )
        val secondStep = expectThat(secondFetchUnit.step(firstStep.nextInstructionAddress))
            .isSuccess()
            .subject

        expectThat(secondStep.fetchedInstructions.first().instructionAddress)
            .isEqualTo(InstructionAddress(8))
    }

    @Test
    fun `step stops after first taken prediction in a batch`() {
        val fetchUnit = fetchUnit(
            3,
            mapOf(0 to 0x11u, 4 to 0x22u, 8 to 0x33u, 16 to 0x44u),
            64,
            StubBranchTargetPredictor(
                mapOf(
                    InstructionAddress(0) to InstructionAddress(16),
                    InstructionAddress(4) to InstructionAddress(8),
                    InstructionAddress(8) to InstructionAddress(12)
                )
            ),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )
        val stepResult = expectThat(fetchUnit.step(InstructionAddress(0)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions.size)
            .isEqualTo(1)

        expectThat(stepResult.fetchedInstructions.first().predictedNextInstructionAddress)
            .isEqualTo(InstructionAddress(16))

        expectThat(stepResult.nextInstructionAddress)
            .isEqualTo(InstructionAddress(16))
    }

    @Test
    fun `step keeps all candidates when redirect happens on final candidate`() {
        val fetchUnit = fetchUnit(
            3,
            mapOf(0 to 0x11u, 4 to 0x22u, 8 to 0x33u, 20 to 0x44u),
            64,
            StubBranchTargetPredictor(
                mapOf(
                    InstructionAddress(0) to InstructionAddress(4),
                    InstructionAddress(4) to InstructionAddress(8),
                    InstructionAddress(8) to InstructionAddress(20)
                )
            ),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )
        val stepResult = expectThat(fetchUnit.step(InstructionAddress(0)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions.size)
            .isEqualTo(3)

        expectThat(stepResult.nextInstructionAddress)
            .isEqualTo(InstructionAddress(20))
    }

    @Test
    fun `step follows first predicted redirect and returns redirect address as next instruction address`() {
        val fetchUnit = fetchUnit(
            4,
            mapOf(0 to 0x11u, 4 to 0x22u, 8 to 0x33u, 12 to 0x44u, 20 to 0x55u),
            64,
            StubBranchTargetPredictor(
                mapOf(
                    InstructionAddress(0) to InstructionAddress(4),
                    InstructionAddress(4) to InstructionAddress(20),
                    InstructionAddress(8) to InstructionAddress(12),
                    InstructionAddress(12) to InstructionAddress(16)
                )
            ),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )

        val stepResult = expectThat(fetchUnit.step(InstructionAddress(0)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions.size)
            .isEqualTo(2)

        expectThat(stepResult.nextInstructionAddress)
            .isEqualTo(InstructionAddress(20))
    }

    @Test
    fun `step propagates main memory failure on first instruction`() {
        val mainMemoryLoader = StubMainMemoryLoader(emptyMap(), setOf(0))
        val fetchUnit = RealInstructionFetcher(
            FetchWidth(2),
            mainMemoryLoader,
            sequentialBranchTargetPredictor(0, 4),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )
        val stepResult = fetchUnit.step(InstructionAddress(0))

        expectThat(stepResult)
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(0))
    }

    @Test
    fun `step propagates main memory failure after earlier successful fetch`() {
        val mainMemoryLoader = StubMainMemoryLoader(mapOf(0 to 0x11u), setOf(4))
        val fetchUnit = RealInstructionFetcher(
            FetchWidth(3),
            mainMemoryLoader,
            sequentialBranchTargetPredictor(0, 4, 8),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )
        val stepResult = fetchUnit.step(InstructionAddress(0))

        expectThat(stepResult)
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(4))
    }

    @Test
    fun `step does not load instructions after predicted redirect`() {
        val mainMemoryLoader = StubMainMemoryLoader(mapOf(0 to 0x11u), setOf(4, 8))
        val fetchUnit = RealInstructionFetcher(
            FetchWidth(3),
            mainMemoryLoader,
            StubBranchTargetPredictor(
                mapOf(
                    InstructionAddress(0) to InstructionAddress(16),
                    InstructionAddress(4) to InstructionAddress(8),
                    InstructionAddress(8) to InstructionAddress(12)
                )
            ),
            StubInstructionQueueSlotsSource(InstructionQueueSlots(8))
        )
        val stepResult = expectThat(fetchUnit.step(InstructionAddress(0)))
            .isSuccess()
            .subject

        expectThat(stepResult.fetchedInstructions.size)
            .isEqualTo(1)
    }

    private fun fetchUnit(
        width: Int,
        words: Map<Int, UInt>,
        sizeInBytes: Int,
        branchTargetPredictor: BranchTargetPredictor,
        instructionQueueSlotsSource: InstructionQueueSlotsSource
    ) =
        RealInstructionFetcher(
            FetchWidth(width),
            mainMemory(sizeInBytes, words),
            branchTargetPredictor,
            instructionQueueSlotsSource
        )

    private fun mainMemory(sizeInBytes: Int, words: Map<Int, UInt>): RealMainMemory {
        val programBytes = ByteArray(sizeInBytes)

        words.forEach { entry ->
            val address = entry.key
            val value = entry.value.toInt()
            programBytes[address] = (value and 0xff).toByte()
            programBytes[address + 1] = ((value ushr 8) and 0xff).toByte()
            programBytes[address + 2] = ((value ushr 16) and 0xff).toByte()
            programBytes[address + 3] = ((value ushr 24) and 0xff).toByte()
        }

        return RealMainMemory.fromProgramBytes(Size(sizeInBytes), programBytes)
    }

    private fun sequentialBranchTargetPredictor(vararg addresses: Int) =
        StubBranchTargetPredictor(
            addresses.associate { address ->
                InstructionAddress(address) to InstructionAddress(address).next
            }
        )
}
