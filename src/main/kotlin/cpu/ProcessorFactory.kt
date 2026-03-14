package cpu

import address.RealAddressUnitSet
import arithmeticlogic.RealArithmeticLogicUnit
import arithmeticlogic.RealArithmeticLogicUnitSet
import branchlogic.RealBranchEvaluator
import branchlogic.RealBranchUnitSet
import branchpredictor.BranchTargetBuffer
import branchpredictor.SaturatingCounterBranchOutcomeBuffer
import commondatabus.RealCommonDataBus
import control.ControlState
import control.PredictedProgramCounterSelection
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import instructionqueue.RealInstructionQueue
import mainmemory.MainMemory
import memoryaccess.RealMemoryUnitSet
import memorybuffer.RealMemoryBufferQueue
import registerfile.RealRegisterFile
import reorderbuffer.RealReorderBuffer
import reservationstation.RealReservationStationBank
import types.*

interface ProcessorFactory {
    fun create(
        configuration: ProcessorConfiguration,
        mainMemory: MainMemory,
        mainMemorySize: Size,
        initialInstructionAddress: InstructionAddress
    ): ProcessorResult<ProcessorState>
}

data object RealProcessorFactory : ProcessorFactory {

    override fun create(
        configuration: ProcessorConfiguration,
        mainMemory: MainMemory,
        mainMemorySize: Size,
        initialInstructionAddress: InstructionAddress
    ) =
        SaturatingCounterBranchOutcomeBuffer
            .create(
                configuration.branchTargetBufferSize,
                configuration.branchOutcomeCounterBitWidth
            )
            .flatMap { branchOutcomePredictor ->
                BranchTargetBuffer.create(
                    configuration.branchTargetBufferSize,
                    branchOutcomePredictor
                )
            }
            .map { branchTargetBuffer ->
                ProcessorState(
                    ControlState(initialInstructionAddress, PredictedProgramCounterSelection),
                    ProcessorStatistics(0, 0, 0, 0),
                    false,
                    branchTargetBuffer,
                    RealCommonDataBus(
                        Size(
                            configuration.arithmeticLogicUnitCount.value +
                                    configuration.branchUnitCount.value +
                                    configuration.memoryUnitCount.value
                        )
                    ),
                    mainMemory,
                    RealInstructionQueue(configuration.instructionQueueSize),
                    seededRegisterFile(mainMemorySize),
                    RealReorderBuffer(configuration.reorderBufferSize),
                    RealReservationStationBank(configuration.arithmeticLogicReservationStationCount),
                    RealReservationStationBank(configuration.branchReservationStationCount),
                    RealMemoryBufferQueue(configuration.memoryBufferCount),
                    RealArithmeticLogicUnitSet(
                        configuration.arithmeticLogicUnitCount,
                        RealArithmeticLogicUnit,
                        configuration.arithmeticLogicLatencies
                    ),
                    RealBranchUnitSet(
                        configuration.branchUnitCount,
                        RealBranchEvaluator,
                        configuration.branchLatencies
                    ),
                    RealAddressUnitSet(configuration.addressUnitCount, configuration.addressLatency),
                    RealMemoryUnitSet(configuration.memoryUnitCount, configuration.memoryLatencies)
                )
            }

    private fun seededRegisterFile(mainMemorySize: Size) =
        stackStartAddress(mainMemorySize)
            .let { stackStartAddress ->
                RealRegisterFile()
                    .seed(RegisterAddress(2), stackStartAddress)
                    .seed(RegisterAddress(8), stackStartAddress)
            }

    private fun stackStartAddress(mainMemorySize: Size) =
        Word((mainMemorySize.value - stackGuardBytes(mainMemorySize)).toUInt())

    private fun stackGuardBytes(mainMemorySize: Size) =
        minOf(DEFAULT_STACK_GUARD_BYTES, mainMemorySize.value / 2)

    private const val DEFAULT_STACK_GUARD_BYTES = 1024
}
