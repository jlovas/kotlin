package hu.jlovas.kcli

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest

class KcliTest {

    val mainCmd = Cmd("callgraph", "Cobol, RPG, CL Source parser and call graph generator")
    val opIndexFile by mainCmd.option("i", "index", "Index file.", false, "./QSYS_IDX.TXT")
    val opDryRun  by mainCmd.optionBool("d", "dry-run", "It runs without write.")
    val opQuiet   by mainCmd.optionBool("q", "quiet", "It does not print extra information.")
    val opHelp    by mainCmd.optionBool("h", "help", "Prints help message", function = { mainCmd.printUsage() })

    val cmdIndex = mainCmd.subCmd("index", "Parse program source files from the given directory")
    val opDuration by cmdIndex.option("t", "time", "Measure running time.")
    val argSrcDir by cmdIndex.argument("source_dir")

    val cmdList = mainCmd.subCmd("list", "Print call graph as list.") {} // printListResult() }
    val opString by cmdList.option("s", "string", "Output columns: n:name, s:size, l:line number, p:path, f:file name", defaultValue = "nlsp")
    val opInt by cmdList.optionInt("i", "int", "A date.", defaultValue = 1)
    val opFloat by cmdList.optionFloat("f", "float", "A date.", defaultValue = 1.0f)
    val opDouble by cmdList.optionDouble("d", "double", "A date.", defaultValue = 1.0)
    val opDate by cmdList.optionDate("D", "Date", "A date.", defaultValue = "2022-01-01")
    val opTime by cmdList.optionTime("T", "Time", "A time.", defaultValue = "12:00:00")
    val opBoolean by cmdList.optionBool("b", "boolean", "Caller direction.")
    val argPrgName by cmdList.argument("program_name", minimum= 1, maximum = 2, defaults = listOf("ACCBTDAT", "ACCBTDAT2"))

    @BeforeTest
    fun setup() {
    }

    @Test
    fun `GIVEN a perfect callgraph command line, WHEN parse it, THEN there is no exception`() {
        val cmdArg = arrayOf("-i", "QSYS_IDX.TXT", "-d", "list", "-r", "-f", "nlsp", "ACCBTDAT")
        assertDoesNotThrow {
            mainCmd.fromArgs(cmdArg)
        }
    }
}

