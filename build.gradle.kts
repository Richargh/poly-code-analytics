plugins {
    kotlin("jvm") version "2.0.21"
    id("application")
}

group = "de.richargh.sandbox"
version = "1.0-SNAPSHOT"

application.mainClass = "de.richargh.sandbox.treesitter.MainKt"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("io.github.bonede:tree-sitter:0.24.3")
    implementation("io.github.bonede:tree-sitter-java:0.21.0a")
    implementation("io.github.bonede:tree-sitter-json:0.23.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.strikt:strikt-core:0.34.0")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed")
    }
}