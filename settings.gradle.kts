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
            from("no.nav.dagpenger:dp-version-catalog:20250313.146.aeb36f")
        }
    }
}
