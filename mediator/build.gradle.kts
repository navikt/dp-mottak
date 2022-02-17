plugins {
    id("dagpenger.rapid-and-rivers")
}

application {
    mainClass.set("no.nav.dagpenger.mottak.AppKt")
}

dependencies {
    implementation(project(":modell"))
    implementation("com.github.navikt.dp-biblioteker:aad-klient:2021.04.09-14.32.088c6dc10b69")
    implementation(Database.Flyway)
    implementation(Database.HikariCP)
    implementation(Database.Kotlinquery)
    implementation(Database.Postgres)
    implementation(Jackson.core)
    implementation(Jackson.jsr310)
    implementation(Jackson.kotlin)
    implementation(Ktor.library("client-cio-jvm"))
    implementation(Ktor.library("client-jackson"))
    implementation(Ktor.auth)
    implementation("de.slub-dresden:urnlib:2.0.1")

    // unleash
    implementation("no.finn.unleash:unleash-client-java:4.2.1") {
        exclude("org.apache.logging.log4j")
    }

    testImplementation(kotlin("test"))
    testImplementation(Mockk.mockk)
    testImplementation(TestContainers.postgresql)
    testImplementation(Junit5.params)
    testImplementation(Ktor.ktorTest)
}
