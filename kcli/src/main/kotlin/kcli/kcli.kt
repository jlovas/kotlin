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
 *   query <options> <program_names>    Print call graph as list.
 *     -f --format  Output columns: n:name, l:line number, f:file_name, p:path, w:with_redundant_marker_* (default: 'n l f p w')
 *     -t --thin-column  Print columns without padding them to the same size. (default: 'false')
 * ```
 *
 * Example implementation:
 *
 * ```kotlin
 *     val mainCmd = Cmd("callgraph", "Cobol, RPG, CL Source parser and call graph generator")
 *     val opIndexFile    by mainCmd.option("i", "index", "Index file.", "./QSYS_IDX.TXT")
 *     val opIntNumber    by mainCmd.optionInt("I", "Int", "A Int option", defaultValue = 1)
 *     val opFloatNumber  by mainCmd.optionFloat("F", "Float", "A Float option", defaultValue = 1.0f)
 *     val opDoubleNumber by mainCmd.optionDouble("d", "double", "A Double option", defaultValue = 1.0)
 *     val opDateNumber   by mainCmd.optionDate("D", "Date", "A Date option", defaultValue = "2021-01-01")
 *     val opTimeNumber   by mainCmd.optionTime("T", "Time", "A Time option", defaultValue = "00:00:00")
 *     val opHelp         by mainCmd.optionBool("h", "help", "Prints help message") { mainCmd.printUsage() }
 *
 *     // Subcommand 1.
 *     val cmdIndex = mainCmd.subCmd("index", "Parses and indexes program source files from the given directory").apply {
 *         val opDuration   by optionBool("r", "run-time", "Measure running time.")
 *         val opDryRun     by optionBool("d", "dry-run", "It runs without write.")
 *         val argSrcDir    by argument("source_dir", "./" )
 *         function { indexPrograms(argSrcDir.first(), opIndexFile, opDryRun, opDuration) }
 *     }
 *
 *     // Subcommand 2.
 *     val cmdQuery = mainCmd.subCmd("query", "Print call graph as list.").apply {
 *         val opFormat          by option("f", "format", "Output columns: n:name, l:line number, f:file_name, p:path, w:with_redundant_marker_*", defaultValue = "n l f p w")
 *         val opThinColumn      by optionBool("t", "thin-column", "Print columns without padding them to the same size.")
 *         val argProgramNames   by arguments("program_names", count = 0..3, defaults = listOf("a_prg", "b_prg") )
 *         function { queryPrograms(
 *             programNames = argProgramNames,
 *             format = opFormat,
 *             paddingColumns = !opThinColumn,
 *         ) }
 *     }
 * ```
 */

/**
 * Main entry point for building type-safe CLI applications.
 *
 * Manages options, subcommands, and positional arguments. Parses command-line arguments
 * via [fromArgs], prints help via [printUsage], and executes the configured command function.
 *
 * Use Kotlin property delegation with the `by` operator to declare options and arguments
 * that are automatically parsed and validated.
 *
 * @property name The command name displayed in usage output and help messages.
 * @property description The command description shown in help output.
 *
 * **Example:**
 * ```kotlin
 * val cmd = Cmd("myapp", "My CLI Application")
 * val opFile by cmd.option("f", "file", "Input file", "./default.txt")
 * val opVerbose by cmd.optionBool("v", "verbose", "Verbose output")
 *
 * cmd.function {
 *     println("Processing file: $opFile (verbose=$opVerbose)")
 * }
 *
 * cmd(args)  // Parse and execute
 * ```
 *
 * @see CmdOption for type-safe option definitions
 * @see CmdArgument for positional argument definitions
 */
open class Cmd(
    val name: String,
    val description: String,
    defaultArg: CmdArgument = CmdArgument(name = "no_arg", minimum = 0, maximum = 0),
    protected var function: (Cmd.() -> Unit)? = null
) {
    protected var rootCmd: Cmd? = null
    protected val cmdOptions: MutableList<CmdOption<*>> = mutableListOf()
    protected val subCmds: MutableList<Cmd> = mutableListOf()
    protected var argument: CmdArgument = defaultArg

    fun isRootCmd() = rootCmd === null

    val options: List<CmdOption<*>>
        get() = cmdOptions

    val subcommands: Map<String,Cmd>
        get() = subCmds.associateBy { it.name }

    val arguments: List<String>
        get() = argument.values

    operator fun plusAssign(option: CmdOption<*>) {
        cmdOptions.forEach { 
            if (it.name == option.name) error("Option already exists: ${option.name}") 
            if (it.longName != null && it.longName == option.longName) error("Option already exists: ${option.longName}") 
        }
        cmdOptions.add(option)
    }

    operator fun plusAssign(cmd: Cmd) {
        subCmds.add(cmd)
        cmd.rootCmd = this
    }

    fun subCmd(name: String, description: String, function: (Cmd.() -> Unit)? = null): Cmd {
        require(subCmds.none { it.name == name }) { "Subcommand already exists: $name" }
        val cmd = Cmd(name, description, function = function)
        cmd.rootCmd = this
        subCmds.add(cmd)
        return cmd
    }

    fun argument(name: String, default: String? = null) =
        arguments(name, count = 1, defaults = if (default == null) emptyList() else listOf(default) )

    fun arguments(name: String, count: Int, defaults: List<String> = emptyList()) =
        arguments(name, count..count, defaults)

    fun arguments(name: String, count: IntRange, defaults: List<String> = emptyList()): CmdArgument {
        argument = CmdArgument(name, count.start, count.endInclusive, defaults)
        return argument
    }

    fun function(function: Cmd.() -> Unit) {
        this.function = function
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
            val missingOptions = cmdOptions.filter { it.required && !it.given }
            if (missingOptions.isNotEmpty()){
                println("Error: Missing required options: ${missingOptions.joinToString(", ") { it.longName ?: it.name }}")
                printUsage()
                exitProcess(1)
            }
            if (subCmds.isNotEmpty()){
                println("Error: Missing subcommand. Expected one of: ${subCmds.joinToString(", ") { it.name }}")
                printUsage()
                exitProcess(1)
            }
            if (argument.minimum > 0){
                println("Error: Missing arguments: expected at least ${argument.minimum}")
                printUsage()
                exitProcess(1)
            }
            function?.invoke(this)
            return
        }

        var index = 0
        // OPTION PARSE: options must come before subcommand or arguments
        while (index < args.size && args[index].startsWith("-")) {
            val arg = args[index++]
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

            // OPTION VALUE IF REQUIRED
            if (option.withValue) {
                val nextArg = args.getOrNull(index++) ?: error("Missing value for option: $arg")
                try { 
                    option.setStringValue(nextArg)
                }catch (e: Throwable) { 
                    when (e) {
                        is NumberFormatException -> error("Invalid number format for option: $arg: $nextArg")
                        is IllegalArgumentException -> error("Invalid value for option: $arg: $nextArg (${e.message})")
                        else -> throw e
                    }                        
                }
            } else {
                option.setStringValue(true.toString())
            }

            // OPTION FUNCTION
            option.function?.invoke(this)
        }
        
        // CHECK REQUIRED OPTIONS BEFORE CONTINUING
        cmdOptions.filter { it.required && !it.given }.takeIf { it.isNotEmpty() }?.let {
            error("Missing required options: ${it.joinToString(", ") { it.longName ?: it.name }}")
        }

        // NO MORE OPTIONS, PARSE SUBCOMMAND OR ARGUMENTS, CHECK SUBCOMMAND FIRST
        if( subCmds.isNotEmpty() ) {
            if (index >= args.size) {
                println("Error: Missing required subcommand.\n")
                printUsage()
                exitProcess(1)
            }
            val subCmd = subCmds.find { it.name == args[index] }
            if (subCmd == null) {
                error("Unknown subcommand: '${args[index]}'. Expected one of: ${subCmds.joinToString(", ") { it.name }}")
            }
            subCmd.fromArgs(args.copyOfRange(index + 1, args.size))
            return
        }

        // Every remaining arg is considered as positional argument value
        argument.values = args.drop(index)
        require(argument.values.size >= argument.minimum) {
            "Too few arguments: ${argument.values.size} (min: ${argument.minimum}, max: ${argument.maximum})"
        }
        require(argument.values.size <= argument.maximum) {
            "Too many arguments: ${argument.values.size} (min: ${argument.minimum}, max: ${argument.maximum})"
        }
        // ALWAYS CALL FUNCTION IF DEFINED AND EXIT
        function?.invoke(this)
    }
}

fun Cmd.option(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: String? = null,
    function: ((Cmd) -> Unit)? = null,
) = 
    CmdOption<String>(name, longName, description, true, defaultValue, function= function, converter= { it }).also { this += it }

fun Cmd.optionInt(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: Int? = null,
    converter: (String) -> Int = { it.toInt() },
    function: ((Cmd) -> Unit)? = null,
) =
    CmdOption<Int>(name, longName, description, true, defaultValue, function= function, converter= converter).also { this += it }

fun Cmd.optionFloat(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: Float? = null,
    converter: (String) -> Float = { it.toFloat() },
    function: ((Cmd) -> Unit)? = null,
) =
    CmdOption<Float>(name, longName, description, true, defaultValue, function= function, converter= converter).also { this += it }

fun Cmd.optionDouble(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: Double? = null,
    converter: (String) -> Double = { it.toDouble() },
    function: ((Cmd) -> Unit)? = null,
) =
    CmdOption<Double>(name, longName, description, true, defaultValue, function= function, converter= converter).also { this += it }

fun Cmd.optionDate(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: String? = null,
    converter: (String) -> LocalDate = { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) },
    function: ((Cmd) -> Unit)? = null,
): CmdOption<LocalDate> {
    val defaultDateValue = defaultValue?.let {
        LocalDate.parse(defaultValue, DateTimeFormatter.ISO_LOCAL_DATE)
    }
    val option = CmdOption<LocalDate>(name, longName, description, true, defaultDateValue, function= function, converter= converter)
    this += option
    return option
}

fun Cmd.optionTime(
    name: String,
    longName: String? = null,
    description: String,
    defaultValue: String? = null,
    converter: ((String) -> LocalTime) = {  LocalTime.parse(it) },
    function: ((Cmd) -> Unit)? = null,
): CmdOption<LocalTime> {
    val defaultTimeValue = defaultValue?.let {
        LocalTime.parse(defaultValue)
    }
    val option = CmdOption<LocalTime>(name, longName, description, true, defaultTimeValue, function= function, converter= converter)
    this += option
    return option
}

fun Cmd.optionBool(
    name: String,
    longName: String? = null,
    description: String,
    converter: (String) -> Boolean = { it.toBoolean() },
    function: ((Cmd) -> Unit)? = null,
) =
    CmdOption<Boolean>(name, longName, description, false, false, function=function, converter=converter).also { this += it }

/**
 * Generic typed command-line option with conversion, validation, and callback support.
 *
 * Supports both short (-f) and long (--file) option formats. Automatically converts
 * string input to the target type [T] using a provided converter function.
 *
 * Implements Kotlin's property delegation pattern for seamless use with the `by` operator:
 * `val option by cmd.option(...)` automatically registers and provides access to the parsed value.
 *
 * @param T The target type after conversion (String, Int, Boolean, LocalDate, etc.)
 * @property name Short option name without dash (e.g., "f" for -f). Single character recommended.
 * @property longName Optional long option name without dashes (e.g., "file" for --file).
 * @property description Help text shown in usage output.
 * @property withValue Whether this option expects a value. False for boolean flags (no value required).
 * @property defaultValue Default value if option not provided on command line. Null means option is required.
 * @property converter Function to convert string input to target type [T]. Throws exception on conversion failure.
 * @property function Optional callback lambda invoked when this option is parsed. Receives the command object.
 *
 * **Example:**
 * ```kotlin
 * val cmd = Cmd("app", "My app")
 * val count by cmd.optionInt("c", "count", "Item count", defaultValue = 10)
 * val verbose by cmd.optionBool("v", "verbose", "Verbose output")
 * ```
 *
 * @see Cmd for main command configuration
 * @see getValue for property delegation implementation
 */
class CmdOption<T>(
    val name: String,
    val longName: String? = null,
    val description: String,
    val withValue: Boolean,
    val defaultValue: T? = null,
    val converter: ((String) -> T) = { throw NotImplementedError("Converter not implemented for option ${longName ?: name}") },
    val function: ((Cmd) -> Unit)? = null,
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


/**
 * Positional command-line arguments with cardinality constraints and default values.
 *
 * Manages a variable-length list of positional arguments (non-option values) parsed from
 * the command line. Enforces minimum and maximum argument count constraints.
 *
 * Implements Kotlin's property delegation pattern for seamless use with the `by` operator:
 * `val args by cmd.arguments(...)` automatically registers and provides access to the parsed values.
 *
 * @property name Argument name shown in usage output (e.g., "files" displays as <files>).
 * @property minimum Minimum number of arguments required. Validation fails if fewer provided.
 * @property maximum Maximum number of arguments accepted. Validation fails if more provided.
 * @property values The list of parsed argument values. Initially empty, populated after parsing.
 *
 * **Example:**
 * ```kotlin
 * val cmd = Cmd("copy", "Copy files")
 * val files by cmd.arguments("source_files", count = 1..Int.MAX_VALUE)
 * // Requires 1 or more arguments
 * ```
 *
 * @throws IllegalStateException if minimum/maximum constraints are invalid (negative or min > max).
 * @see Cmd.arguments for creating argument instances
 * @see getValue for property delegation implementation
 */
class CmdArgument(
    internal val name: String,
    internal val minimum: Int,
    internal val maximum: Int,
    defaultArgs: List<String> = emptyList(),
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
