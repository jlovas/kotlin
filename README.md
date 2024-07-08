# Kotlin project (group)
Self-compiling Kotlin SDK add-on projects in a single repo.

## kcli: Kotlin Command Line Interface

Example command line help output:
```
Description: Cobol, RPG, CL Source parser and call graph generator

 Usage:
   with Gradle: ./gradlew run --args="<option> <subcommand>"
   as FatJar  : java -jar <app_fatjar>.jar <option> <subcommand>

Options:
    -i --index  Index file. (default: './QSYS_IDX.TXT')
    -I --Int  A Int option (default: '1')
    -F --Float  A Float option (default: '1.0')
    -d --double  A Double option (default: '1.0')
    -D --Date  A Date option (default: '2021-01-01')
    -T --Time  A Time option (default: '00:00')
    -h --help  Prints help message (default: 'false')

Subcommands:
  index <options> <source_dir>    Parses and indexes program source files from the given directory
    -r --run-time  Measure running time. (default: 'false')
    -d --dry-run  It runs without write. (default: 'false')
    <source_dir>    Default: ./

  query <options> <program_names>    Print call graph as list.
    -f --format  Output columns: n:name, l:line number, f:file_name, p:path, w:with_redundant_marker_(default: 'n l f p w')
    -t --thin-column  Print columns without padding them to the same size. (default: 'false')
```

Example implementation:

```kotlin
    val mainCmd = Cmd("callgraph", "Cobol, RPG, CL Source parser and call graph generator")
    val opIndexFile    by mainCmd.option("i", "index", "Index file.", "./QSYS_IDX.TXT")
    val opIntNumber    by mainCmd.optionInt("I", "Int", "A Int option", defaultValue = 1)
    val opFloatNumber  by mainCmd.optionFloat("F", "Float", "A Float option", defaultValue = 1.0f)
    val opDoubleNumber by mainCmd.optionDouble("D", "Double", "A Double option", defaultValue = 1.0)
    val opDateNumber   by mainCmd.optionDate("D", "Date", "A Date option", defaultValue = "2021-01-01")
    val opTimeNumber   by mainCmd.optionTime("T", "Time", "A Time option", defaultValue = "00:00:00")
    val opHelp         by mainCmd.optionBool("h", "help", "Prints help message") { mainCmd.printUsage() }

    // Subcommand 1.
    val cmdIndex = mainCmd.subCmd("index", "Parses and indexes program source files from the given directory").apply {
        val opDuration   by optionBool("r", "run-time", "Measure running time.")
        val opDryRun     by optionBool("d", "dry-run", "It runs without write.")
        val argSrcDir    by argument("source_dir", "./" )
        function { indexPrograms(argSrcDir.first(), opIndexFile, opDryRun, opDuration) }
    }

    // Subcommand 2.
    val cmdQuery = mainCmd.subCmd("query", "Print call graph as list.").apply {
        val opFormat        by option("f", "format", "Output columns: n:name, l:line number, f:file_name, p:path, w:with_redundant_marker_", defaultValue = "n l f p w")
        val opThinColumn    by optionBool("t", "thin-column", "Print columns without padding them to the same size.")
        val argProgramNames by argument("program_names", 1..99)
        function { queryPrograms(
            programName = argProgramNames,
            format = opFormat,
            paddingColumns = !opThinColumn,
        ) }
    }
```


__INFO:__ 
If your submodule status is: 
    
- HEAD detached from b9fc903

...then do the followings:
```shell
# in the submodule directory (stash your changes first)
git checkout master
git pull

# ...and then in the main project
git submodule update --remote --rebase  # or --merge; updates the FETCH_HEAD file
git add <submodule>                     # add the updated submodule to the main project
git commit -m "Update <submodule> to latest commit"
```


## How to include it in your main project
### include in your Git project as a submodule:
```sh
git submodule add git@github.com:jlovas/kotlin.git lib/github.com/jlovas/kotlin
git add .gitmodules lib/github.com/jlovas/kotlin/
git commit -m "kotlin submodule added"
```


### Include in your build (example: "kcli" subproject)
#### settings.gradle.kts
```kotlin
include(":kcli")
project(":kcli").projectDir = File("./lib/github.com/jlovas/kotlin/kcli")
```

#### build.gradle.kts
```kotlin
dependencies {
    implementation( project(":kcli") )
}
```


## How to clone the main project with all submodules
### All steps at once:
```sh
git clone --recurse-submodules <main_repo>
```

### Or step by step:
```sh
git clone <main_repo>
cd <main_repo>
git submodule init
git submodule update
```
