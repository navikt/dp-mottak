plugins {
    id("common")
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
    implementation(libs.dp.biblioteker.oauth2.klient)

    implementation(libs.rapids.and.rivers)

    implementation(libs.bundles.postgres)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.6.0")

    implementation(libs.bundles.jackson)

    implementation(libs.bundles.ktor.server)
    implementation(libs.bundles.ktor.client)
    implementation("de.slub-dresden:urnlib:2.0.1")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.mockk)
    testImplementation(libs.mock.oauth2.server)
    testImplementation(libs.bundles.postgres.test)
    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
}
