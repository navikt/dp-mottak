plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.9.10"
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.21.0")
}

kotlin {
    jvmToolchain(17)
}

