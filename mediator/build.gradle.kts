import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("common")
    alias(libs.plugins.shadow.jar)
    application
}

repositories {
    maven("https://jitpack.io")
}

application {
    mainClass.set("no.nav.dagpenger.mottak.AppKt")
}

dependencies {
    implementation(project(":modell"))
    implementation(project(":openapi"))
    implementation("no.nav.dagpenger:oauth2-klient:2025.07.23-08.30.31e64aee9725")
    implementation("io.prometheus:prometheus-metrics-core:1.4.1")
    implementation(libs.rapids.and.rivers)
    implementation("io.getunleash:unleash-client-java:11.1.0")

    implementation(libs.bundles.postgres)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.10.2")

    implementation("com.github.navikt.tbd-libs:naisful-app:2025.08.16-09.21-71db7cad")

    implementation(libs.bundles.jackson)

    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation("io.ktor:ktor-client-logging-jvm:${libs.versions.ktor.get()}")
    implementation("de.slub-dresden:urnlib:3.0.0")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.mockk)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
    testImplementation(libs.rapids.and.rivers.test)
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("no.bekk.bekkopen:nocommons:0.16.0")
    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
    testImplementation("io.ktor:ktor-client-mock-jvm:${libs.versions.ktor.get()}")
    testImplementation("com.github.navikt.tbd-libs:naisful-test-app:2025.08.16-09.21-71db7cad")
}

tasks.withType<ShadowJar> {
    mergeServiceFiles()
}
