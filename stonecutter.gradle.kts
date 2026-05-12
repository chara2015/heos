plugins {
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false
}
stonecutter active "1.21.11"

stonecutter.tasks {
    order("publishMods")
}

val collectReleaseJars by tasks.registering(Sync::class) {
    group = "build"
    description = "Collects all built release jars into build/release-jars."
    into(layout.buildDirectory.dir("release-jars"))

    subprojects.forEach { versionProject ->
        from(versionProject.layout.buildDirectory.dir("libs")) {
            include("*.jar")
            exclude("*-sources.jar", "*-dev.jar", "*-all.jar")
        }
    }
}

gradle.projectsEvaluated {
    collectReleaseJars.configure {
        mustRunAfter(subprojects.map { it.tasks.named("build") })
    }

    tasks.matching { it.name == "build" }.configureEach {
        finalizedBy(collectReleaseJars)
    }
}
