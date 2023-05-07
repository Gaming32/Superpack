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

java.sourceCompatibility = JavaVersion.VERSION_11
java.targetCompatibility = JavaVersion.VERSION_11

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

    implementation("com.formdev:flatlaf:3.0")

    implementation("com.github.Dansoftowner:jSystemThemeDetector:3.8")

    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("net.java.dev.jna:jna-platform:5.12.1")

    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.19.0")
    implementation("org.apache.logging.log4j:log4j-core:2.19.0")
    implementation("io.github.oshai:kotlin-logging-jvm:4.0.0-beta-22")

    implementation("com.github.TheRandomLabs:CurseApi:cdc5d48d5e")
    implementation("org.jsoup:jsoup:1.16.1") // Upgrade from old jsoup version that has CVEs

    compileOnly("org.jetbrains:annotations:24.0.0")

    compileOnly("org.projectlombok:lombok:1.18.26")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}
