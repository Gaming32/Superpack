import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    java
    kotlin("jvm") version "1.8.21"
    id("io.ktor.plugin") version "2.1.1" // It builds fat JARs
    id("io.freefair.lombok") version "8.0.1"
}

group = "io.github.gaming32"
version = "0.3"

java.sourceCompatibility = JavaVersion.VERSION_17
java.targetCompatibility = JavaVersion.VERSION_17

application {
    mainClass.set("io.github.gaming32.superpack.SuperpackKt")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Multi-Release"] = true
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.gaming32:mrpacklib:9d979177f9")

    implementation("com.formdev:flatlaf:3.1.1")

    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.8")

    implementation("net.java.dev.jna:jna:5.13.0")
    implementation("net.java.dev.jna:jna-platform:5.13.0")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")

    implementation("io.github.matyrobbrt:curseforgeapi:1.7.4")

    implementation("com.github.steos.jnafilechooser:jnafilechooser-api:f512011434")

    compileOnly("org.jetbrains:annotations:24.0.1")

    compileOnly("org.projectlombok:lombok:1.18.26")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
