package hu.jlovas.kcli

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter


class KcliTest {

    // Main command.
    val mainCmd = Cmd("callgraph", "Cobol, RPG, CL Source parser and call graph generator")
    val opIndexFile by mainCmd.option("i", "index", "Index file.", "INDEX.TXT")
    val opIntNumber   by mainCmd.optionInt("I", "Int", "A Int option", defaultValue = 1)
    val opFloatNumber   by mainCmd.optionFloat("F", "Float", "A Float option", defaultValue = 1.1f)
    val opDoubleNumber   by mainCmd.optionDouble("d", "double", "A Double option", defaultValue = 1.2)
    val opDateNumber   by mainCmd.optionDate("D", "Date", "A Date option", defaultValue = "2021-02-03")
    val opTimeNumber   by mainCmd.optionTime("T", "Time", "A Time option", defaultValue = "01:02:03")
    val opHelp    by mainCmd.optionBool("h", "help", "Prints help message", function = {
        println("helpFuncrion called")
        helpCalled = true
    })

    // Subcommand 1: index
    val cmdIndex = mainCmd.subCmd("index", "Parses and indexes program source files from the given directory").apply {
        val opDuration   by optionBool("r", "run-time", "Measure running time.")
        val opDryRun     by optionBool("d", "dry-run", "It runs without write.")
        val argSrcDir by argument("source_dir", minimum = 1, maximum = 1, defaults = listOf("./") )
        function {
            println("indexPrograms called")
            indexCalled = true
        }
    }

    // Subcommand 2: query
    val cmdQuery = mainCmd.subCmd("query", "Print call graph as list.").apply {
        val opFormat            by option("f", "format", "Output columns: n:name, l:line number, f:file_name, p:path, w:with_redundant_marker_*")
        val opThinColumn      by optionBool("t", "thin-column", "Print columns without padding them to the same size.")
        val argProgramName by argument("program_name", minimum = 1, maximum = 1)
        function {
            println("queryPrograms called: t=$opThinColumn, f=$opFormat, program_name=${argProgramName.first()}")
            queryCalled = true
        }
    }

//    @BeforeTest
//    fun setup() {
//        mainCmd.printUsage()
//    }

    @Test
    fun `GIVEN a Cmd object WHEN before parse arguments THEN the 'given' flag is false`() {
        assertFalse( mainCmd.cmdOptions.any { it.given } )
        assertFalse( cmdIndex.cmdOptions.any { it.given } )
        assertFalse( cmdQuery.cmdOptions.any { it.given } )
    }

        @Test
    fun `GIVEN a Cmd object WHEN before parse arguments THEN contains the init values`() {
        assertEquals(opIndexFile, "INDEX.TXT")
        assertEquals(opIntNumber, 1)
        assertEquals(opFloatNumber, 1.1f)
        assertEquals(opDoubleNumber, 1.2)
        assertEquals(opDateNumber, LocalDate.parse("2021-02-03", DateTimeFormatter.ISO_LOCAL_DATE))
        assertEquals(opTimeNumber, LocalTime.parse("01:02:03"))
        assertFalse(opHelp)

        assertFalse(helpCalled)
        assertFalse(indexCalled)
        assertFalse(queryCalled)

        assertThrows(IllegalStateException::class.java) {
            cmdQuery.cmdOptions[0].value
        }
        assertNull(cmdQuery.cmdOptions[0]._value)
        assertFalse(cmdQuery.cmdOptions[1].value as Boolean)
    }

    var helpCalled = false
    var indexCalled = false
    var queryCalled = false
    val cmdLineArgumentsMap = mapOf(
        "-i" to "INDEX2.TXT",
        "-I" to "11",
        "-F" to "11.1",
        "-d" to "11.2",
        "-D" to "2025-06-07",
        "-T" to "12:13:14",
        "-h" to null,
        "query" to null,
        "-t" to null,
        "-f" to "xxx",
        "ACCVTDAT" to null
    )
    val cmdLineArguments = mutableListOf<String>().apply {
        cmdLineArgumentsMap.forEach { (k, v) ->
            println("k=$k, v=$v")
            add(k)
            if (v != null) {
                this.add(v)
            }
        }
    }.toTypedArray()

    @Test
    fun `GIVEN a cli object WHEN args parsed THEN it contains the command line values values`() {
        assertDoesNotThrow {
            mainCmd(cmdLineArguments)
        }

        assertEquals(opIndexFile,   cmdLineArgumentsMap["-i"])
        assertEquals(opIntNumber,   cmdLineArgumentsMap["-I"]?.toInt())
        assertEquals(opFloatNumber, cmdLineArgumentsMap["-F"]?.toFloat())
        assertEquals(opDoubleNumber,cmdLineArgumentsMap["-d"]?.toDouble())
        assertEquals(opDateNumber,  cmdLineArgumentsMap["-D"]?.let { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) })
        assertEquals(opTimeNumber,  cmdLineArgumentsMap["-T"]?.let { LocalTime.parse(it) })
        assertTrue(opHelp)

        assertTrue(helpCalled)
        assertFalse(indexCalled)
        assertTrue(queryCalled)

        assertEquals(cmdQuery.cmdOptions[0].value, cmdLineArgumentsMap["-f"])
        assertTrue(cmdQuery.cmdOptions[1].value as Boolean)
    }

    @Test
    fun `GIVEN a cli object WHEN args parsed THEN the 'given' flag is true in parsed args`() {
        assertDoesNotThrow {
            mainCmd(cmdLineArguments)
        }

        assertTrue( mainCmd.cmdOptions.all { it.given } )
        assertFalse( cmdIndex.cmdOptions.any { it.given } )
        assertTrue( cmdQuery.cmdOptions.all { it.given } )
    }
}
