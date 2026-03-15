plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    application
    `java-test-fixtures`
}

group = "io.github.georgeherbert.processor"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("dev.forkhandles:result4k:2.25.1.0")
    implementation("io.ktor:ktor-server-core-jvm:3.4.1")
    implementation("io.ktor:ktor-server-netty-jvm:3.4.1")
    implementation("io.ktor:ktor-server-cors-jvm:3.4.1")
    implementation("io.ktor:ktor-server-status-pages-jvm:3.4.1")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.4.1")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:3.4.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:3.4.1")
    implementation("org.slf4j:slf4j-simple:2.0.17")
    testFixturesImplementation("dev.forkhandles:result4k:2.25.1.0")
    testFixturesImplementation("io.strikt:strikt-core:0.35.1")
    testImplementation(kotlin("test"))
    testImplementation("io.strikt:strikt-core:0.35.1")
    testImplementation("io.ktor:ktor-server-test-host-jvm:3.4.1")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass = "web.ServerKt"
}

val frontendDirectory = layout.projectDirectory.dir("frontend")

val frontendInstall = tasks.register<Exec>("frontendInstall") {
    workingDir(frontendDirectory.asFile)
    commandLine("npm", "ci")
    inputs.file(frontendDirectory.file("package.json"))
    inputs.file(frontendDirectory.file("package-lock.json"))
    outputs.dir(frontendDirectory.dir("node_modules"))
}

val frontendTest = tasks.register<Exec>("frontendTest") {
    dependsOn(frontendInstall)
    workingDir(frontendDirectory.asFile)
    commandLine("npm", "run", "test", "--", "--run")
    inputs.dir(frontendDirectory.dir("src"))
    inputs.file(frontendDirectory.file("package.json"))
    inputs.file(frontendDirectory.file("package-lock.json"))
    inputs.file(frontendDirectory.file("vite.config.ts"))
}

val frontendBuild = tasks.register<Exec>("frontendBuild") {
    dependsOn(frontendInstall)
    workingDir(frontendDirectory.asFile)
    commandLine("npm", "run", "build")
    inputs.dir(frontendDirectory.dir("src"))
    inputs.file(frontendDirectory.file("package.json"))
    inputs.file(frontendDirectory.file("package-lock.json"))
    inputs.file(frontendDirectory.file("vite.config.ts"))
    inputs.file(frontendDirectory.file("index.html"))
    inputs.file(frontendDirectory.file("tsconfig.json"))
    outputs.dir(frontendDirectory.dir("dist"))
}

tasks.processResources {
    dependsOn(frontendBuild)
    from(frontendDirectory.dir("dist")) {
        into("web")
    }
}

tasks.check {
    dependsOn(frontendTest)
}
