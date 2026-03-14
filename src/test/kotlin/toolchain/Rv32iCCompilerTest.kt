package toolchain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotEmpty
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.ExternalToolExecutionFailed
import types.ExternalToolExecutionInterrupted
import types.ExternalToolProcessLaunchFailed
import types.TemporaryPathCreateFailed
import java.nio.file.Files
import java.nio.file.Path

class Rv32iCCompilerTest {

    private val compilerExecutable = "riscv64-unknown-elf-gcc"
    private val objcopyExecutable = "riscv64-unknown-elf-objcopy"

    @Test
    fun `compiles rv32i c source into a raw binary`(@TempDir temporaryDirectory: Path) {
        val compiler = RealRv32iCCompiler(compilerExecutable, objcopyExecutable)
        val sourceFilePath = writeSource(
            temporaryDirectory.resolve("program.c"),
            """
            #include <stdint.h>

            uint32_t _start()
            {
                return 42;
            }
            """.trimIndent()
        )

        val binaryFilePath = expectThat(compiler.compile(sourceFilePath, temporaryDirectory.resolve("out")))
            .isSuccess()
            .subject

        expectThat(Files.isRegularFile(binaryFilePath))
            .isTrue()

        expectThat(Files.readAllBytes(binaryFilePath).toList())
            .isNotEmpty()
    }

    @Test
    fun `fails when the output directory cannot be created`(@TempDir temporaryDirectory: Path) {
        val compiler = RealRv32iCCompiler(compilerExecutable, objcopyExecutable)
        val sourceFilePath = writeSource(
            temporaryDirectory.resolve("program.c"),
            """
            #include <stdint.h>

            uint32_t _start()
            {
                return 42;
            }
            """.trimIndent()
        )
        val outputDirectoryPath = temporaryDirectory.resolve("occupied")
        Files.writeString(outputDirectoryPath, "not-a-directory")

        expectThat(compiler.compile(sourceFilePath, outputDirectoryPath))
            .isFailure()
            .isEqualTo(TemporaryPathCreateFailed(outputDirectoryPath.toString()))
    }

    @Test
    fun `fails when the compiler executable cannot be launched`(@TempDir temporaryDirectory: Path) {
        val compiler = RealRv32iCCompiler("missing-rv32i-compiler", objcopyExecutable)
        val sourceFilePath = writeSource(
            temporaryDirectory.resolve("program.c"),
            """
            #include <stdint.h>

            uint32_t _start()
            {
                return 42;
            }
            """.trimIndent()
        )

        expectThat(compiler.compile(sourceFilePath, temporaryDirectory.resolve("out")))
            .isFailure()
            .isEqualTo(ExternalToolProcessLaunchFailed("missing-rv32i-compiler"))
    }

    @Test
    fun `fails when the compiler process exits unsuccessfully`(@TempDir temporaryDirectory: Path) {
        val failingCompilerPath = writeExecutableScript(
            temporaryDirectory.resolve("fail-compiler.sh"),
            """
            #!/bin/sh
            echo compiler-failed 1>&2
            exit 7
            """.trimIndent()
        )
        val compiler = RealRv32iCCompiler(failingCompilerPath.toString(), objcopyExecutable)
        val sourceFilePath = writeSource(
            temporaryDirectory.resolve("program.c"),
            """
            #include <stdint.h>

            uint32_t _start()
            {
                return 42;
            }
            """.trimIndent()
        )

        expectThat(compiler.compile(sourceFilePath, temporaryDirectory.resolve("out")))
            .isFailure()
            .isEqualTo(ExternalToolExecutionFailed(failingCompilerPath.toString(), 7, "compiler-failed\n"))
    }

    @Test
    fun `fails when the objcopy executable cannot be launched`(@TempDir temporaryDirectory: Path) {
        val compiler = RealRv32iCCompiler(compilerExecutable, "missing-rv32i-objcopy")
        val sourceFilePath = writeSource(
            temporaryDirectory.resolve("program.c"),
            """
            #include <stdint.h>

            uint32_t _start()
            {
                return 42;
            }
            """.trimIndent()
        )

        expectThat(compiler.compile(sourceFilePath, temporaryDirectory.resolve("out")))
            .isFailure()
            .isEqualTo(ExternalToolProcessLaunchFailed("missing-rv32i-objcopy"))
    }

    @Test
    fun `fails when the tool execution is interrupted`(@TempDir temporaryDirectory: Path) {
        val interruptibleCompilerPath = writeExecutableScript(
            temporaryDirectory.resolve("interruptible-compiler.sh"),
            """
            #!/bin/sh
            sleep 1
            exit 0
            """.trimIndent()
        )
        val compiler = RealRv32iCCompiler(interruptibleCompilerPath.toString(), objcopyExecutable)
        val sourceFilePath = writeSource(
            temporaryDirectory.resolve("program.c"),
            """
            #include <stdint.h>

            uint32_t _start()
            {
                return 42;
            }
            """.trimIndent()
        )

        Thread.currentThread().interrupt()

        expectThat(compiler.compile(sourceFilePath, temporaryDirectory.resolve("out")))
            .isFailure()
            .isEqualTo(ExternalToolExecutionInterrupted(interruptibleCompilerPath.toString()))

        expectThat(Thread.interrupted())
            .isTrue()
    }

    private fun writeSource(path: Path, source: String): Path {
        Files.writeString(path, source)
        return path
    }

    private fun writeExecutableScript(path: Path, content: String): Path {
        Files.writeString(path, content)
        expectThat(path.toFile().setExecutable(true))
            .isTrue()
        return path
    }
}
