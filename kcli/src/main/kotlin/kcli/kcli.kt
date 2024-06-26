package hu.jlovas.kcli

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
                    option.value = nextArg
                } else {
                    error("Wrong value for option: $arg: $nextArg")
                }
            } else {
                option.value = true.toString()
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
    val cmdOptions: MutableList<CmdOption> = mutableListOf(),
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
    defaultValue: String? = null,
    withValue: Boolean = required || defaultValue != null,
    function: ((Cmd) -> Unit)? = null,
    valid: ((String) -> Boolean)? = null
): CmdOption {
    val option = CmdOption(name, longName, description, required, defaultValue, withValue, function, valid)
    cmdOptions.add(option)
    return option
}

class CmdOption(
    val name: String,
    val longName: String? = null,
    val description: String,
    val required: Boolean = false,
    val defaultValue: String? = null,
    val withValue: Boolean = required || defaultValue != null,
    val function: ((Cmd) -> Unit)? = null,
    val valid: ((String) -> Boolean)? = null,
) {
    var value: String? = defaultValue
        internal set

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? {
        return value
    }

    override fun toString(): String {
        val usage = StringBuilder()
        usage.append("    -${name} --${longName}  ")
        if (required) usage.append('*')
        usage.append(" ${description} ")
        if (defaultValue != null) usage.append("(default: '${defaultValue}')")
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
