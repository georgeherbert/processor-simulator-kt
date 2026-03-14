package cpu

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class WidthsTest {

    @Test
    fun `issue widths retain the provided value`() {
        expectThat(IssueWidth(0))
            .isEqualTo(IssueWidth(0))

        expectThat(IssueWidth(4))
            .isEqualTo(IssueWidth(4))

        expectThat(IssueWidth(-1))
            .isEqualTo(IssueWidth(-1))
    }

    @Test
    fun `commit widths retain the provided value`() {
        expectThat(CommitWidth(0))
            .isEqualTo(CommitWidth(0))

        expectThat(CommitWidth(4))
            .isEqualTo(CommitWidth(4))

        expectThat(CommitWidth(-1))
            .isEqualTo(CommitWidth(-1))
    }
}
