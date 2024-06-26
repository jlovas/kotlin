plugins {
    kotlin("jvm") version "1.9.23"
}

group = "hu.jlovas"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java {
            srcDir("src/main/java")
            srcDir("src/main/kotlin")
        }
        resources.srcDir("src/main/resources")
    }
    test {
        java.srcDir("src/test/kotlin")
        resources.srcDir("src/test/resources")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}