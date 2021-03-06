plugins {
    id("dagpenger.common")
}

dependencies {
    implementation(Jackson.core)
    implementation(Jackson.kotlin)
    implementation(Jackson.jsr310)
    api("no.bekk.bekkopen:nocommons:0.12.0")

    testImplementation(Junit5.params)
    testImplementation(Mockk.mockk)
    testImplementation("com.approvaltests:approvaltests:11.2.3")
}
