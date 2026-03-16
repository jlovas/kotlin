package hu.jlovas.kcli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

/**
 * Critical: Tests argument constraints (min/max count) and default values.
 * This ensures proper validation of positional arguments and defaults work correctly.
 */
class ArgumentConstraintsTest {

    @Test
    fun `GIVEN argument with default WHEN no arguments provided THEN default is used`() {
        val cmd = Cmd("app", "Test")
        val files by cmd.arguments("files", count = 0..10, defaults = listOf("default.txt"))

        assertDoesNotThrow {
            cmd(arrayOf())
        }

        assertEquals(listOf("default.txt"), files)
    }

    @Test
    fun `GIVEN variable argument count WHEN correct number provided THEN all captured`() {
        val cmd = Cmd("app", "Test")
        var capturedFiles: List<String> = emptyList()

        cmd.apply {
            val files by arguments("files", count = 1..5)
            function {
                capturedFiles = files
            }
        }

        assertDoesNotThrow {
            cmd(arrayOf("file1", "file2", "file3"))
        }

        assertEquals(listOf("file1", "file2", "file3"), capturedFiles)
    }

    @Test
    fun `GIVEN exact argument count requirement WHEN different count provided THEN error thrown`() {
        val cmd = Cmd("app", "Test")
        val files by cmd.arguments("files", count = 3..3)

        assertThrows<Throwable> {
            cmd(arrayOf("file1", "file2"))
        }
    }

    @Test
    fun `GIVEN minimum argument count WHEN fewer provided THEN error thrown`() {
        val cmd = Cmd("app", "Test")
        val files by cmd.arguments("files", count = 2..10)

        val exception = assertThrows<Throwable> {
            cmd(arrayOf("file1"))
        }

        assert("Too few" in exception.message.orEmpty())
    }

    @Test
    fun `GIVEN maximum argument count WHEN more provided THEN error thrown with correct max value`() {
        val cmd = Cmd("app", "Test")
        val files by cmd.arguments("files", count = 1..3)

        val exception = assertThrows<Throwable> {
            cmd(arrayOf("file1", "file2", "file3", "file4"))
        }

        assert("Too many" in exception.message.orEmpty() && "3" in exception.message.orEmpty())
    }

    @Test
    fun `GIVEN zero minimum arguments WHEN no arguments provided THEN valid`() {
        val cmd = Cmd("app", "Test")
        var capturedFiles: List<String> = emptyList()

        cmd.apply {
            val files by arguments("files", count = 0..10, defaults = emptyList())
            function {
                capturedFiles = files
            }
        }

        assertDoesNotThrow {
            cmd(arrayOf())
        }

        assertEquals(emptyList<String>(), capturedFiles)
    }
}
