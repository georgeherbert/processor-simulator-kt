package memoryaccess

import cpu.CommonDataBusWrite
import cpu.MemoryLatencies
import decoder.*
import dev.forkhandles.result4k.asSuccess
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import mainmemory.RealMainMemory
import memorybuffer.MemoryBufferLoadWork
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import testfixtures.isFailure
import testfixtures.isSuccess
import types.*

class MemoryUnitSetTest {

    @Test
    fun `loads values after the configured latency for each load operation`() {
        val expectations = listOf(
            LoadExpectation(LoadWordOperation, 1, DataAddress(0), Word(0x12345678u)),
            LoadExpectation(LoadHalfWordOperation, 2, DataAddress(4), Word(0xfffffffeu)),
            LoadExpectation(LoadHalfWordUnsignedOperation, 3, DataAddress(4), Word(0x0000fffeu)),
            LoadExpectation(LoadByteOperation, 4, DataAddress(8), Word(0xffffff80u)),
            LoadExpectation(LoadByteUnsignedOperation, 5, DataAddress(8), Word(0x00000080u))
        )

        expectations.forEach { expectation ->
            val dispatchedResult = expectThat(
                RealMemoryUnitSet(Size(1), distinctLatencies()).step(
                    mainMemory(),
                    listOf(work(expectation.operation, expectation.address, RobId(expectation.latency)))
                )
            )
                .isSuccess()
                .subject
            val followUpResults = expectThat(
                followUpResults(
                    dispatchedResult.memoryUnitSet,
                    mainMemory(),
                    expectation.latency
                )
            )
                .isSuccess()
                .subject

            expectThat(dispatchedResult.commonDataBusWrites)
                .isEqualTo(emptyList())

            expectThat(followUpResults.dropLast(1).flatMap { stepResult -> stepResult.commonDataBusWrites })
                .isEqualTo(emptyList())

            expectThat(followUpResults.last().commonDataBusWrites)
                .isEqualTo(
                    listOf(
                        CommonDataBusWrite(RobId(expectation.latency), expectation.expectedValue)
                    )
                )
        }
    }

    @Test
    fun `fails when a memory load cannot be completed`() {
        expectThat(
            RealMemoryUnitSet(Size(1), distinctLatencies()).step(
                RealMainMemory(Size(4)),
                listOf(work(LoadWordOperation, DataAddress(12), RobId(1)))
            )
        )
            .isFailure()
            .isEqualTo(MainMemoryAddressOutOfBounds(12))
    }

    @Test
    fun `clear drops in flight memory work`() {
        val dispatchedResult = expectThat(
            RealMemoryUnitSet(Size(1), distinctLatencies()).step(
                mainMemory(),
                listOf(work(LoadWordOperation, DataAddress(0), RobId(1)))
            )
        )
            .isSuccess()
            .subject

        val clearedUnitSet = dispatchedResult.memoryUnitSet.clear()
        val nextResult = expectThat(clearedUnitSet.step(mainMemory(), emptyList()))
            .isSuccess()
            .subject

        expectThat(nextResult.commonDataBusWrites)
            .isEqualTo(emptyList())
    }

    private fun mainMemory() =
        RealMainMemory.fromProgramBytes(
            Size(16),
            byteArrayOf(
                0x78,
                0x56,
                0x34,
                0x12,
                0xfe.toByte(),
                0xff.toByte(),
                0x00,
                0x00,
                0x80.toByte(),
                0x7f,
                0x00,
                0x00
            )
        )

    private fun distinctLatencies() =
        MemoryLatencies(
            CycleCount(1),
            CycleCount(2),
            CycleCount(3),
            CycleCount(4),
            CycleCount(5)
        )

    private fun work(operation: LoadOperation, address: DataAddress, robId: RobId) =
        MemoryBufferLoadWork(MemoryBufferId(1), operation, address, robId)

    private fun followUpResults(
        memoryUnitSet: MemoryUnitSet,
        mainMemory: RealMainMemory,
        remainingSteps: Int
    ): ProcessorResult<List<MemoryUnitSetStepResult>> =
        when (remainingSteps == 0) {
            true -> emptyList<MemoryUnitSetStepResult>().asSuccess()
            false ->
                memoryUnitSet
                    .step(mainMemory, emptyList())
                    .flatMap { stepResult ->
                        followUpResults(stepResult.memoryUnitSet, mainMemory, remainingSteps - 1)
                            .map { stepResults -> listOf(stepResult) + stepResults }
                    }
        }

    private data class LoadExpectation(
        val operation: LoadOperation,
        val latency: Int,
        val address: DataAddress,
        val expectedValue: Word
    )
}
