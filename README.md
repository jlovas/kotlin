# Kotlin project (group)
Self-compiling Kotlin SDK add-on projects in a single repo.


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
