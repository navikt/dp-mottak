rootProject.name = "dp-mottak"

include(
    "modell",
    "mediator",
)

dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20250306.142.3d919e")
        }
    }
}
