plugins {
    id("dagpenger.rapid-and-rivers")
}

application {
    mainClass.set("no.nav.dagpenger.mottak.AppKt")
}

dependencies {
    implementation(project(":modell"))
    implementation("com.github.navikt.dp-biblioteker:oauth2-klient:2022.10.22-09.05.6fcf3395aa4f")
    implementation(Database.Flyway)
    implementation(Database.HikariCP)
    implementation(Database.Kotlinquery)
    implementation(Database.Postgres)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.6.0")
    implementation(Jackson.core)
    implementation(Jackson.jsr310)
    implementation(Jackson.kotlin)
    implementation(Ktor2.Client.library("cio"))
    implementation(Ktor2.Client.library("content-negotiation"))
    implementation("io.ktor:ktor-serialization-jackson:${Ktor2.version}")
    implementation(Ktor2.Server.library("auth"))
    implementation(Ktor2.Server.library("auth-jwt"))
    implementation(Ktor2.Server.library("status-pages"))
    implementation(Ktor2.Server.library("content-negotiation"))
    implementation(Ktor2.Server.library("call-logging"))
    implementation("de.slub-dresden:urnlib:2.0.1")

    // unleash
    implementation("no.finn.unleash:unleash-client-java:4.2.1") {
        exclude("org.apache.logging.log4j")
    }

    testImplementation(kotlin("test"))
    testImplementation("no.nav.security:mock-oauth2-server:1.0.0")
    testImplementation(Mockk.mockk)
    testImplementation(TestContainers.postgresql)
    testImplementation(Junit5.params)
    testImplementation(Ktor2.Server.library("test-host"))
}
