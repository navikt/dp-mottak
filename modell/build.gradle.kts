plugins {
    id("dagpenger.common")
}

dependencies {
    implementation(Jackson.core)
    implementation(Jackson.kotlin)

    testImplementation(Junit5.params)
}
