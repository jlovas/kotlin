import hu.jlovas.kcli.*

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.test.BeforeTest

class KcliTest {

    val mainCmd = Cmd("callgraph", "Cobol, RPG, CL Source parser and call graph generator")
    val opIndexFile by mainCmd.option("i", "index", "Index file.", false, "./QSYS_IDX.TXT")
    val opDryRun   by mainCmd.option("d", "dry-run", "It runs without write.")
    val opQuiet by mainCmd.option("q", "quiet", "It does not print extra information.")
    val opHelp  by mainCmd.option("h", "help", "Prints help message", function = { mainCmd.printUsage() })

    val cmdIndex = mainCmd.subCmd("index", "Parse program source files from the given directory")
    val opDuration by cmdIndex.option("t", "time", "Measure running time.")
    val argSrcDir by cmdIndex.argument("source_dir")

    val cmdList = mainCmd.subCmd("list", "Print call graph as list.") {} // printListResult() }
    val opUpward by cmdList.option("r", "caller", "Caller direction.")
    val opFormat by cmdList.option("f", "format", "Output columns: n:name, s:size, l:line number, p:path, f:file name", defaultValue = "nlsp")
    val argPrgName by cmdList.argument("program_name", minimum= 1, maximum = 2)

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

