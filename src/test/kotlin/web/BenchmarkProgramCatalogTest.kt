package web

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.startsWith
import strikt.assertions.isTrue
import testfixtures.isFailure
import testfixtures.isSuccess
import types.BenchmarkProgramMaterializationFailed
import types.BenchmarkProgramNotFound
import java.nio.file.Files
import java.nio.file.Path

class BenchmarkProgramCatalogTest {

    @Test
    fun `programs lists the bundled benchmark kernels`() {
        expectThat(RealBenchmarkProgramCatalog.programs().map { program -> program.name })
            .contains("arithmetic.c")
            .contains("matrix_multiply.c")
    }

    @Test
    fun `materializeSource writes a bundled benchmark to the target directory`(@TempDir temporaryDirectory: Path) {
        val sourcePath = expectThat(
            RealBenchmarkProgramCatalog.materializeSource("arithmetic.c", temporaryDirectory)
        )
            .isSuccess()
            .subject

        expectThat(Files.exists(sourcePath))
            .isTrue()

        expectThat(Files.size(sourcePath))
            .isGreaterThan(0)

        expectThat(Files.readString(sourcePath).contains("int32_t main"))
            .isTrue()
    }

    @Test
    fun `source reads the bundled benchmark text`() {
        val source = expectThat(RealBenchmarkProgramCatalog.source("arithmetic.c"))
            .isSuccess()
            .subject

        expectThat(source)
            .startsWith("#include <stdint.h>")

        expectThat(source)
            .contains("int32_t main")
    }

    @Test
    fun `materializeSource fails for unknown benchmark names`(@TempDir temporaryDirectory: Path) {
        expectThat(RealBenchmarkProgramCatalog.materializeSource("missing.c", temporaryDirectory))
            .isFailure()
            .isEqualTo(BenchmarkProgramNotFound("missing.c"))
    }

    @Test
    fun `source fails for unknown benchmark names`() {
        expectThat(RealBenchmarkProgramCatalog.source("missing.c"))
            .isFailure()
            .isEqualTo(BenchmarkProgramNotFound("missing.c"))
    }

    @Test
    fun `materializeSource fails when the target directory is missing`(@TempDir temporaryDirectory: Path) {
        val missingDirectory = temporaryDirectory.resolve("missing")

        expectThat(RealBenchmarkProgramCatalog.materializeSource("arithmetic.c", missingDirectory))
            .isFailure()
            .isEqualTo(
                BenchmarkProgramMaterializationFailed(
                    "arithmetic.c",
                    missingDirectory.resolve("arithmetic.c").toString()
                )
            )
    }
}
