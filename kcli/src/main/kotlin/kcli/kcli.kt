package hu.jlovas.kcli

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

/**
 * # Kotlin Command Line Interface (KCLI)
 *
 * Example command line help output:
 * ```
 * Description: Cobol, RPG, CL Source parser and call graph generator
 *
 *  Usage:
 *    with Gradle: ./gradlew run --args="<option> <subcommand>"
 *    as FatJar  : java -jar <app_fatjar>.jar <option> <subcommand>
 *
 * Options:
 *     -i --index  Index file. (default: './QSYS_IDX.TXT')
 *     -I --Int  A Int option (default: '1')
 *     -F --Float  A Float option (default: '1.0')
 *     -d --double  A Double option (default: '1.0')
 *     -D --Date  A Date option (default: '2021-01-01')
 *     -T --Time  A Time option (default: '00:00')
 *     -h --help  Prints help message (default: 'false')
 *
 * Subcommands:
 *   index <options> <source_dir>    Parses and indexes program source files from the given directory
 *     -r --run-time  Measure running time. (default: 'false')
 *     -d --dry-run  It runs without write. (default: 'false')
 *     <source_dir>    Default: ./
 *
 *   query <options> <program_name>    Print call graph as list.
 *     -f --format  Output columns: n:name, l:line number, f:file_name, p:path, w:with_redundant_marker_* (default: 'n l f p w')
 *     -t --thin-column  Print columns without padding them to the same size. (default: 'false')
 * ```
 *
 * Example implementation:
 *
 * ```kotlin
 *     val mainCmd = Cmd("callgraph", "Cobol, RPG, CL Source parser and call graph generator")
 *     val opIndexFile by mainCmd.option("i", "index", "Index file.", "./QSYS_IDX.TXT")
 *     val opIntNumber   by mainCmd.optionInt("I", "Int", "A Int option", defaultValue = 1)
 *     val opFloatNumber   by mainCmd.optionFloat("F", "Float", "A Float option", defaultValue = 1.0f)
 *     val opDoubleNumber   by mainCmd.optionDouble("d", "double", "A Double option", defaultValue = 1.0)
 *     val opDateNumber   by mainCmd.optionDate("D", "Date", "A Date option", defaultValue = "2021-01-01")
 *     val opTimeNumber   by mainCmd.optionTime("T", "Time", "A Time option", defaultValue = "00:00:00")
 *     val opHelp    by mainCmd.optionBool("h", "help", "Prints help message", function = { mainCmd.printUsage() })
 *
 *     // Subcommand 1.
 *     val cmdIndex = mainCmd.subCmd("index", "Parses and indexes program source files from the given directory").apply {
 *         val opDuration   by optionBool("r", "run-time", "Measure running time.")
 *         val opDryRun     by optionBool("d", "dry-run", "It runs without write.")
 *         val argSrcDir by argument("source_dir", minimum = 1, maximum = 1, defaults = listOf("./") )
 *         function { indexPrograms(argSrcDir.first(), opIndexFile, opDryRun, opDuration) }
 *     }
 *
 *     // Subcommand 2.
 *     val cmdQuery = mainCmd.subCmd("query", "Print call graph as list.").apply {
 *         val opFormat            by option("f", "format", "Output columns: n:name, l:line number, f:file_name, p:path, w:with_redundant_marker_*", defaultValue = "n l f p w")
 *         val opThinColumn      by optionBool("t", "thin-column", "Print columns without padding them to the same size.")
 *         val argProgramName by argument("program_name", minimum = 1, maximum = 1)
 *         function { queryPrograms(
 *             programName = argProgramName.first().uppercase(),
 *             format = opFormat,
 *             paddingColumns = !opThinColumn,
 *         ) }
 *     }
 * ```
 */
open class Cmd(
    val name: String,
    val description: String,
    val defaultArg: CmdArgument = CmdArgument(name = "argument", minimum = 0, maximum = 0),
    var function: (Cmd.() -> Unit)? = null
) {
    internal var rootCmd: Cmd? = null
    internal val cmdOptions: MutableList<CmdOption<*>> = mutableListOf()
    internal val subCmds: MutableList<Cmd> = mutableListOf()
    var argument: CmdArgument = defaultArg
        internal set

    fun isRootCmd() = rootCmd === null

    fun subCmd(name: String, description: String, function: (Cmd.() -> Unit)? = null): Cmd {
        val cmd = Cmd(name, description, function = function)
        cmd.rootCmd = this
        subCmds.add(cmd)
        return cmd
    }

    fun argument(name: String, minimum: Int = 1, maximum: Int = 1, defaults: List<String> = emptyList()) =
        CmdArgument(name = name, minimum = minimum, maximum = maximum, defaultArgs = defaults).apply {
            argument = this
        }

    fun function(function: Cmd.() -> Unit): Cmd {
        this.function = function
        return this
    }

    operator fun invoke(args: Array<String>) = fromArgs(args)

    fun printUsage() {
        if ( isRootCmd() ) {
            println()
            println("Description: ${description}")
            println()
            println("Usage: ${name} <option> <subcommand> $argument")
            println("  with Gradle: ./gradlew run --args='<option> <subcommand>'")
            println("  as FatJar  : java -jar build/libs/callgraph-1.0-app.jar <option> <subcommand>")
            println()
            println("Options:")
        } else {
            print("  $name ")
            if (cmdOptions.isNotEmpty()) print("<options> ")
            println("$argument    $description")
        }
        // OPTION LIST
        cmdOptions.forEach { option -> println(option) }
        // ARGUMENT description
        if (argument.values.isNotEmpty()) {
            println("    <${argument.name}>    Default: ${argument.values.joinToString(" ")}")
        }
        // SUBCOMMAND LIST
        println()
        if (subCmds.isNotEmpty()) {
            println("Subcommands:")
            subCmds.forEach { it.printUsage() }
        }
    }

    fun fromArgs(args: Array<String>) {
        if (args.isEmpty()) {
            printUsage()
            exitProcess(0)
        }

        var index = 0
        while (index < args.size) {
            val arg = args[index++]
            // OPTION
            if (arg.startsWith("-")) {
                val option =
                    if (arg.startsWith("--")) {
                        // LONG OPTION
                        val longName = arg.substring(2)
                        cmdOptions.find { it.longName == longName } ?: error("Unknown long option: $longName")
                    } else {
                        // SHORT OPTION
                        val name = arg.substring(1)
                        cmdOptions.find { it.name == name }
                            ?: error("Unknown short option: $name")
                    }

                // OPTION VALUE
                if (option.withValue) {
                    val nextArg = args.getOrNull(index++) ?: error("Missing value for option: $arg")
                    if (try {
                            option.converter(nextArg) != null
                        } catch (_: Throwable) {
                            false
                        }
                    ) {
                        option.setStringValue(nextArg)
                    } else {
                        error("Wrong value for option: $arg: $nextArg")
                    }
                } else {
                    option.setStringValue(true.toString())
                }

                // OPTION FUNCTION
                option.function?.invoke(this)
            } else {
                val cmd = subCmds.find { it.name == arg }
                if (cmd == null) {
                    // ARGUMENT
                    argument.values = args.slice((index - 1)..<args.size)
                    break
                } else {
                    // SUBCOMMAND
                    cmd.fromArgs(args.copyOfRange(index, args.size))
                    break
                }
            }
        }

        // CHECK REQUIRED ARGUMENTS
        cmdOptions.firstOrNull() { it.required && !it.given }?.apply { error("Missing option: ${longName ?: name}") }
        // CHECK MINIMUM ARGUMENT NUMBER
        if (argument.values.size < argument.minimum) {
            error("Missing argument: ${argument.name}")
        }
        // CHECK MAXIMUM ARGUMENT NUMBER
        if (argument.values.size > argument.maximum) {
            error("Too many arguments: ${argument.name} ${argument.minimum} ${argument.maximum} ${argument.values.size}")
        }
        function?.invoke(this)
    }
}


fun Cmd.option(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: String? = null,
    function: ((Cmd) -> Unit)? = null,
): CmdOption<String> {
    val option = CmdOption<String>(name, longName, description, true, defaultValue, function= function)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionInt(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: Int? = null,
    function: ((Cmd) -> Unit)? = null,
    converter: (String) -> Int = { it.toInt() }
): CmdOption<Int> {
    val option = CmdOption<Int>(name, longName, description, true, defaultValue, function= function, converter= converter)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionFloat(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: Float? = null,
    function: ((Cmd) -> Unit)? = null,
    converter: (String) -> Float = { it.toFloat() }
): CmdOption<Float> {
    val option = CmdOption<Float>(name, longName, description, true, defaultValue, function= function, converter= converter)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionDouble(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: Double? = null,
    function: ((Cmd) -> Unit)? = null,
    converter: (String) -> Double = { it.toDouble() }
): CmdOption<Double> {
    val option = CmdOption<Double>(name, longName, description, true, defaultValue, function= function, converter= converter)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionDate(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: String? = null,
    function: ((Cmd) -> Unit)? = null,
    converter: (String) -> LocalDate = { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
): CmdOption<LocalDate> {
    val defaultDateValue = defaultValue?.let {
        LocalDate.parse(defaultValue, DateTimeFormatter.ISO_LOCAL_DATE)
    }
    val option = CmdOption<LocalDate>(name, longName, description, true, defaultDateValue, function= function, converter= converter)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionTime(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: String? = null,
    function: ((Cmd) -> Unit)? = null,
    converter: ((String) -> LocalTime) = {  LocalTime.parse(it) }
): CmdOption<LocalTime> {
    val defaultTimeValue = defaultValue?.let {
        LocalTime.parse(defaultValue)
    }
    val option = CmdOption<LocalTime>(name, longName, description, true, defaultTimeValue, function= function, converter= converter)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionBool(
    name: String,
    longName: String? = null,
    description: String,
    function: ((Cmd) -> Unit)? = null,
    converter: (String) -> Boolean = { it.toBoolean() }
): CmdOption<Boolean> {
    val option = CmdOption<Boolean>(name, longName, description, false, false, function=function, converter=converter)
    cmdOptions.add(option)
    return option
}


class CmdOption<T>(
    val name: String,
    val longName: String? = null,
    val description: String,
    val withValue: Boolean,
    val defaultValue: T? = null,
    val function: ((Cmd) -> Unit)? = null,
    val converter: ((String) -> T) = { it as? T ?: error("Incompatible converter for option: ${longName ?: name}") },
) {
    internal var _value: T? = null

    val value: T
        get() = _value ?: defaultValue ?: error("Missing both given and default value for option: ${longName ?: name}")

    val given: Boolean
        get() = _value != null

    val required: Boolean
        get() = defaultValue == null

    fun setStringValue(newValue: String) {
        _value = converter(newValue)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun toString(): String {
        val usage = StringBuilder()
        usage.append("    -${name}")
        if (longName != null ) usage.append(" --$longName ") else usage.append("        ")
        if (required) usage.append('*')
        usage.append(" ${description} ")
        if (defaultValue != null) usage.append("(default: '${defaultValue}')")
        return usage.toString()
    }
}


class CmdArgument(
    val name: String,
    val minimum: Int = 1,
    val maximum: Int = 1,
    val defaultArgs: List<String> = emptyList(),
) {
    var values: List<String> = defaultArgs
        internal set

    init {
        if (minimum < 0 || maximum < 0 || minimum > maximum)
            error("CmdArgument: invalid minimum ($minimum) or maximum ($maximum) value.")
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): List<String> {
        return values
    }

    override fun toString(): String {
        val usage: StringBuilder = StringBuilder()
        if (maximum > 0) {
            val MAX_ARGUMENT_SAMPLE = kotlin.math.min(2, maximum)
            for (i in 1..MAX_ARGUMENT_SAMPLE) {
                if (i > 1) usage.append(' ')
                if (i == minimum + 1) usage.append('[')
                usage.append("<${name}>")
            }
            if (MAX_ARGUMENT_SAMPLE < maximum) usage.append(" ...")
            if (minimum < maximum) usage.append(']')
        }
        return usage.toString()
    }
}
