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

    testImplementation(Mockk.mockk)
    testImplementation(TestContainers.postgresql)
}
