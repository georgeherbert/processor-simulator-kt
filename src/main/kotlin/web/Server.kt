package web

import cpu.RealProcessorFactory
import cpu.RealProcessorProgramRunner
import cpu.benchmarkConfiguration
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import mainmemory.RealMainMemoryProgramLoader
import toolchain.RealProgramBinaryPadder
import toolchain.RealRv32iCCompiler
import types.InstructionAddress
import types.Size

fun main() {
    embeddedServer(
        Netty,
        port(),
        host(),
        module = { processorSimulatorWebModule(realSimulationHttpApi()) }
    ).start(wait = true)
}

private fun realSimulationHttpApi() =
    RealSimulationHttpApi(
        RealSimulationService(
            RealBenchmarkProgramCatalog,
            RealRv32iCCompiler("riscv64-unknown-elf-gcc", "riscv64-unknown-elf-objcopy"),
            RealProgramBinaryPadder,
            RealTemporaryDirectoryFactory,
            RealProcessorProgramRunner(RealMainMemoryProgramLoader, RealProcessorFactory),
            benchmarkConfiguration(),
            Size(DEFAULT_MAIN_MEMORY_BYTES),
            InstructionAddress(0)
        )
    )

private fun port() =
    System.getenv("PORT")
        ?.toIntOrNull()
        ?: DEFAULT_PORT

private fun host() =
    System.getenv("HOST")
        ?: DEFAULT_HOST

private const val DEFAULT_PORT = 8080
private const val DEFAULT_HOST = "0.0.0.0"
private const val DEFAULT_MAIN_MEMORY_BYTES = 32 * 1024
