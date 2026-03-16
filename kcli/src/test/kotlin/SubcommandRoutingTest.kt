package hu.jlovas.kcli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.*

/**
 * Critical: Tests subcommand routing ensures correct command execution.
 * This verifies hierarchical command structure and option scoping work correctly.
 */
class SubcommandRoutingTest {

    @Test
    fun `GIVEN multiple subcommands WHEN parsing THEN only the selected subcommand executes`() {
        var buildExecuted = false
        var testExecuted = false
        var deployExecuted = false

        val main = Cmd("app", "Main app")

        val build = main.subCmd("build", "Build the project").apply {
            function { buildExecuted = true }
        }

        val test = main.subCmd("test", "Run tests").apply {
            function { testExecuted = true }
        }

        val deploy = main.subCmd("deploy", "Deploy app").apply {
            function { deployExecuted = true }
        }

        assertDoesNotThrow {
            main(arrayOf("build"))
        }

        assertTrue(buildExecuted)
        assertFalse(testExecuted)
        assertFalse(deployExecuted)
    }

    @Test
    fun `GIVEN subcommand with options WHEN parsing THEN subcommand options are available to subcommand function`() {
        val main = Cmd("app", "Main app")
        var capturedVerbose = false

        val build = main.subCmd("build", "Build").apply {
            val verbose by optionBool("v", "verbose", "Verbose output")
            function {
                capturedVerbose = verbose
            }
        }

        assertDoesNotThrow {
            main(arrayOf("build", "-v"))
        }

        assertTrue(capturedVerbose)
    }

    @Test
    fun `GIVEN subcommand with arguments WHEN parsing THEN subcommand receives correct arguments`() {
        val main = Cmd("app", "Main app")
        var capturedFile: String? = null

        val clone = main.subCmd("clone", "Clone repo").apply {
            val file by arguments("repo_name", count = 1..1)
            function {
                capturedFile = file.firstOrNull()
            }
        }

        assertDoesNotThrow {
            main(arrayOf("clone", "my-repo"))
        }

        assertEquals("my-repo", capturedFile)
    }

    @Test
    fun `GIVEN subcommand options WHEN not passed to other subcommand THEN not available`() {
        val main = Cmd("app", "Main app")

        val build = main.subCmd("build", "Build").apply {
            val verbose by optionBool("v", "verbose", "Build verbose")
            function { }
        }

        val test = main.subCmd("test", "Test").apply {
            val verbose by optionBool("v", "verbose", "Test verbose")
            function { }
        }

        assertDoesNotThrow {
            main(arrayOf("build", "-v"))
        }

        assertTrue(build.options[0].value as Boolean)
        // test's verbose option should not be affected (default is false)
        assertFalse(test.options[0].value as Boolean)
    }
}
