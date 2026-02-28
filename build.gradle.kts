plugins {
    kotlin("jvm") version "2.3.10"
    `java-test-fixtures`
}

group = "io.github.georgeherbert.processor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.forkhandles:result4k:2.25.1.0")
    testFixturesImplementation("dev.forkhandles:result4k:2.25.1.0")
    testFixturesImplementation("io.strikt:strikt-core:0.35.1")
    testImplementation(kotlin("test"))
    testImplementation("io.strikt:strikt-core:0.35.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
