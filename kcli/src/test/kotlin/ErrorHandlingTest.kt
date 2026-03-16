package hu.jlovas.kcli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Critical: Tests error handling for invalid conversions and missing required options.
 * This ensures type safety and proper error reporting.
 */
class ErrorHandlingTest {

    @Test
    fun `GIVEN invalid integer value WHEN parsing option THEN conversion fails gracefully`() {
        val cmd = Cmd("test", "Test command")
        val opCount by cmd.optionInt("c", "count", "Item count")

        assertThrows<Throwable> {
            cmd(arrayOf("-c", "not-a-number"))
        }
    }

    @Test
    fun `GIVEN invalid date format WHEN parsing option THEN conversion fails gracefully`() {
        val cmd = Cmd("test", "Test command")
        val opDate by cmd.optionDate("d", "date", "Date value", defaultValue = "2021-01-01")

        assertThrows<Throwable> {
            cmd(arrayOf("-d", "invalid-date"))
        }
    }

    @Test
    fun `GIVEN missing required argument WHEN parsing THEN error is thrown`() {
        val cmd = Cmd("test", "Test command")
        val arg by cmd.arguments("files", count = 1..Int.MAX_VALUE)

        assertThrows<Throwable> {
            cmd(arrayOf())
        }
    }

    @Test
    fun `GIVEN too many arguments WHEN parsing THEN error contains correct max constraint`() {
        val cmd = Cmd("test", "Test command")
        val arg by cmd.arguments("files", count = 1..2)

        val exception = assertThrows<Throwable> {
            cmd(arrayOf("file1", "file2", "file3"))
        }

        // Error message should reference maximum constraint
        assert("Too many" in exception.message.orEmpty() || "2" in exception.message.orEmpty())
    }
}
