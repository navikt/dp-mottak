plugins {
    id("common")
}

dependencies {
    implementation(libs.bundles.jackson)
    api(libs.kotlin.logging)
    implementation("org.slf4j:slf4j-api:2.0.18")

    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.mockk)
    testImplementation("no.bekk.bekkopen:nocommons:0.17.0")
    testImplementation("com.approvaltests:approvaltests:31.0.0")
    testImplementation(libs.kotest.assertions.core)
}
