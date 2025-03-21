plugins {
    id("common")
}

dependencies {
    implementation(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    api(libs.kotlin.logging)

    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.mockk)
    testImplementation("no.bekk.bekkopen:nocommons:0.16.0")
    testImplementation("com.approvaltests:approvaltests:24.19.0")
    testImplementation(libs.kotest.assertions.core)
}
