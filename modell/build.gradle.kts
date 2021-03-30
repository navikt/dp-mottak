plugins {
    id("dagpenger.common")
}

dependencies {
    implementation(Jackson.core)
    implementation(Jackson.kotlin)
    implementation(Jackson.jsr310)

    testImplementation(Junit5.params)
}
