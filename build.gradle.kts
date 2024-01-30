import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform

plugins {
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Kotlin
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Apache
    implementation("org.freemarker:freemarker")
    implementation("commons-codec:commons-codec")
    implementation("org.apache.maven:maven-repository-metadata:3.9.6")

    // JGit
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

fun String.runCommand(workingDirectory: File = layout.projectDirectory.asFile, env: List<Pair<String, Any>> = emptyList()): Int {
    println("> $this")
    return project.exec {
        environment(*env.toTypedArray())
        workingDir = workingDirectory
        commandLine = this@runCommand.split("\\s".toRegex())
    }.exitValue
}

tasks.register("docker-build") {
    val scripts = File(layout.projectDirectory.asFile, "scripts${File.separator}docker")

    doLast {
        val os = DefaultNativePlatform.getCurrentOperatingSystem()

        if(os.isLinux) {
            "./build.sh".runCommand(scripts, listOf(
                "PROJECT_NAME" to rootProject.name,
                "PROJECT_VERSION" to rootProject.version
            ))
        } else {
            throw IllegalStateException("Unsupported Operating System: ${os.name}")
        }
    }
}


tasks.test {
    useJUnitPlatform()
}
kotlin {
    explicitApi()
    jvmToolchain(21)
}

application {
    mainClass.set("org.ivcode.mvn.MainKt")
}