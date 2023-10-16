plugins {
    id("common")
}

dependencies {
    implementation(libs.jackson.core)
    implementation(libs.jackson.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    api("no.bekk.bekkopen:nocommons:0.15.0")
    api(libs.kotlin.logging)

    testImplementation("org.junit.jupiter:junit-jupiter-params:${libs.versions.junit.get()}")
    testImplementation(libs.mockk)
    testImplementation("com.approvaltests:approvaltests:22.0.0")
}
