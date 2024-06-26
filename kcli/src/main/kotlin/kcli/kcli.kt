package hu.jlovas.kcli

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KProperty
import kotlin.system.exitProcess

fun Cmd.fromArgs(args: Array<String>) {
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
                    println("-Option name: $longName")
                    cmdOptions.find { it.longName == longName } ?: error("Unknown long option: $longName")
                } else {
                    // SHORT OPTION
                    val name = arg.substring(1)
                    println("-Option name: $name")
                    cmdOptions.find { it.name == name }
                        ?: error("Unknown short option: $name")
                }

            // OPTION VALUE
            if (option.withValue) {
                val nextArg = args.getOrNull(index++) ?: error("Missing value for option: $arg")
                if (try {
                        option.valid?.invoke(nextArg) != false
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
                println("-Values of <${argument.name}> argument: ${argument.values}")
                break
            } else {
                // SUBCOMMAND
                cmd.fromArgs(args.copyOfRange(index, args.size))
                break
            }
        }
    }

    if (isRootCmd() && subCmds.isNotEmpty()) {
        // TODO: how can I detect if there is no subcommand in the command line?
    }
    // CHECK REQUIRED ARGUMENTS
    cmdOptions.firstOrNull() { it.required && it.value == null }?.apply { error("Missing option: ${longName ?: name}") }
    // CHECK MINIMUM ARGUMENT NUMBER
    if (argument.values.size < argument.minimum) {
        error("Missing argument: ${argument.name}")
    }
    // CHECK MAXIMUM ARGUMENT NUMBER
    if (argument.values.size > argument.maximum) {
        error("Too many arguments: ${argument.name}")
    }
    function?.invoke()
}

fun Cmd.subCmd(name: String, description: String, function: (() -> Unit)? = null): Cmd {
    val cmd = Cmd(this, name, description, function = function)
    subCmds.add(cmd)
    return cmd
}

data class Cmd(
    val name: String,
    val description: String,
    val cmdOptions: MutableList<CmdOption<*>> = mutableListOf(),
    val subCmds: MutableList<Cmd> = mutableListOf(),
    val defaultArg: CmdArgument = CmdArgument(name = "argument", minimum = 0, maximum = 0),
    val function: (() -> Unit)? = null
) {
    private var rootCmd: Cmd? = null
    var argument: CmdArgument = defaultArg
        internal set

    fun isRootCmd() = rootCmd === null

    internal constructor(
        rootCmd: Cmd,
        name: String,
        description: String,
        function: (() -> Unit)? = null,
    ) : this(name, description, function = function) {
        this.rootCmd = rootCmd
    }

    fun printUsage() {
        if (rootCmd == null) {
            println()
            println("Description: ${description}")
            println()
            println("Usage: ${name} <option> <subcommand> $argument")
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
}

fun Cmd.option(
    name: String,
    longName: String? = null,
    description: String,
    required: Boolean = false,
    defaultValue: String = "",
    function: ((Cmd) -> Unit)? = null,
    valid: ((String) -> Boolean)? = null
): CmdOption<String> {
    val option = CmdOption<String>(name, longName, description, required, defaultValue, true, function, valid)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionInt(
    name: String,
    longName: String? = null,
    description: String,
    required: Boolean = false,
    defaultValue: Int = 0,
    function: ((Cmd) -> Unit)? = null,
    valid: ((String) -> Boolean)? = null
): CmdOption<Int> {
    val option = CmdOption<Int>(name, longName, description, required, defaultValue, true, function, valid)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionFloat(
    name: String,
    longName: String? = null,
    description: String,
    required: Boolean = false,
    defaultValue: Float = 0.0F,
    function: ((Cmd) -> Unit)? = null,
    valid: ((String) -> Boolean)? = null
): CmdOption<Float> {
    val option = CmdOption<Float>(name, longName, description, required, defaultValue, true, function, valid)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionDouble(
    name: String,
    longName: String? = null,
    description: String,
    required: Boolean = false,
    defaultValue: Double = 0.0,
    function: ((Cmd) -> Unit)? = null,
    valid: ((String) -> Boolean)? = null
): CmdOption<Double> {
    val option = CmdOption<Double>(name, longName, description, required, defaultValue, true, function, valid)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionDate(
    name: String,
    longName: String? = null,
    description: String,
    required: Boolean = false,
    defaultValue: String = "1970-01-01",
    function: ((Cmd) -> Unit)? = null,
    valid: ((String) -> Boolean)? = null
): CmdOption<LocalDate> {
    val defaultDateValue: LocalDate = LocalDate.parse(defaultValue, DateTimeFormatter.ISO_LOCAL_DATE)
    val option = CmdOption<LocalDate>(name, longName, description, required, defaultDateValue, true, function, valid)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionTime(
    name: String,
    longName: String? = null,
    description: String,
    required: Boolean = false,
    defaultValue: String = "00:00:00",
    function: ((Cmd) -> Unit)? = null,
    valid: ((String) -> Boolean)? = null
): CmdOption<LocalTime> {
    val defaultTimeValue = LocalTime.parse(defaultValue)
    val option = CmdOption<LocalTime>(name, longName, description, required, defaultTimeValue, true, function, valid)
    cmdOptions.add(option)
    return option
}

fun Cmd.optionBool(
    name: String,
    longName: String? = null,
    description: String,
    required: Boolean = false,
    defaultValue: Boolean = false,
    function: ((Cmd) -> Unit)? = null,
    valid: ((String) -> Boolean)? = null
): CmdOption<Boolean> {
    val option = CmdOption<Boolean>(name, longName, description, required, defaultValue, false, function, valid)
    cmdOptions.add(option)
    return option
}

class CmdOption<T>(
    val name: String,
    val longName: String? = null,
    val description: String,
    val required: Boolean = false,
    val defaultValue: T,
    val withValue: Boolean = required || defaultValue != null,
    val function: ((Cmd) -> Unit)? = null,
    val valid: ((String) -> Boolean)? = null,
) {
    private var _valueFromCmd: T? = null

    val value: T
        get() = _valueFromCmd ?: if (required) error("Missing required option: ${longName ?: name}") else defaultValue

    val given: Boolean
        get() = _valueFromCmd != null

    fun setStringValue(newValue: String) {
        when (defaultValue) {
            is String -> _valueFromCmd = newValue as T
            is LocalDate -> _valueFromCmd = LocalDate.parse(newValue, DateTimeFormatter.ISO_LOCAL_DATE) as T
            is LocalTime -> _valueFromCmd = LocalTime.parse(newValue) as T
            is Boolean -> _valueFromCmd = newValue.toBoolean() as T
            is Int -> _valueFromCmd = newValue.toInt() as T
            is Long -> _valueFromCmd = newValue.toLong() as T
            is Double -> _valueFromCmd = newValue.toDouble() as T
            else -> error("Unhandled option type of '$name', ${defaultValue!!::class.simpleName}")
        }
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    override fun toString(): String {
        val usage = StringBuilder()
        usage.append("    -${name} --${longName}  ")
        if (required) usage.append('*')
        usage.append(" ${description} ")
        if (defaultValue.toString().isNotEmpty()) usage.append("(default: '${defaultValue}')")
        return usage.toString()
    }
}

fun Cmd.argument(name: String, minimum: Int = 1, maximum: Int = 1, defaults: List<String> = emptyList()): CmdArgument {
    argument = CmdArgument(name = name, minimum = minimum, maximum = maximum, defaultArgs = defaults)
    return argument
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
