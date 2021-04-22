plugins {
    id("dagpenger.rapid-and-rivers")
}

application {
    mainClass.set("no.nav.dagpenger.mottak.AppKt")
}

dependencies {
    implementation(project(":modell"))
    implementation("com.github.navikt.dp-biblioteker:aad-klient:2021.04.09-14.32.088c6dc10b69")
    implementation(Ktor.library("client-cio-jvm"))
    implementation(Ktor.library("client-jackson"))
    implementation(Jackson.core)
    implementation(Jackson.kotlin)
    implementation(Jackson.jsr310)

    testImplementation(Mockk.mockk)
}
