plugins {
    id("java")
    id("java-library")
    kotlin("jvm") version "2.2.0"
    id("fabric-loom") version "1.14-SNAPSHOT"
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("dev.kikugie.stonecutter")
    id("me.modmuss50.mod-publish-plugin") version "0.8.4"
}

val modMetadata = loadHeosmodMetadata(rootProject.file("src/main/java/heos/Heosmod.java"))
val modId = modMetadata.getValue("MOD_ID")
val modName = modMetadata.getValue("MOD_NAME")
val baseVersion = modMetadata.getValue("MOD_VERSION")
val modDescription = modMetadata.getValue("MOD_DESCRIPTION")
val modAuthor = modMetadata.getValue("MOD_AUTHOR")
val modLicense = modMetadata.getValue("MOD_LICENSE")
val modHomepage = modMetadata.getValue("MOD_HOMEPAGE")
val modSources = modMetadata.getValue("MOD_SOURCES")
val modIssues = modMetadata.getValue("MOD_ISSUES")
val dynamicVersion = if (baseVersion.endsWith("-SNAPSHOT")) {
    val lastReleaseTag = runGit("describe", "--tags", "--match", "[0-9]*.[0-9]*.[0-9]*", "--abbrev=0")
    if (lastReleaseTag != null) {
        val countStr = runGit("rev-list", "$lastReleaseTag..HEAD", "--count")
        val count = countStr?.toIntOrNull() ?: 0
        if (count > 0) "$baseVersion.$count" else baseVersion
    } else baseVersion
} else baseVersion
version = dynamicVersion

repositories {
    maven(url = "https://maven.nucleoid.xyz")
    maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
}

base.archivesName = "${modId}-mc${property("minecraft_version")}"

val awFile = when {
    stonecutter.eval(stonecutter.current.version, ">=1.21.11") -> "heos.1.21.11.accesswidener"
    stonecutter.eval(stonecutter.current.version, ">=1.21.5") -> "heos.1.21.5.accesswidener"
    stonecutter.eval(stonecutter.current.version, ">=1.21.2") -> "heos.1.21.2.accesswidener"
    stonecutter.eval(stonecutter.current.version, ">=1.20.3") -> "heos.1.20.3.accesswidener"
    stonecutter.eval(stonecutter.current.version, ">=1.19.4") -> "heos.1.19.4.accesswidener"
    else -> throw GradleException("Access widener is missing for Minecraft ${stonecutter.current.version})")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

loom {
    splitEnvironmentSourceSets()
    accessWidenerPath = rootProject.file("src/main/resources/accesswidener/$awFile")
    mods {
        create(modId) {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }

    runConfigs.all {
        ideConfigGenerated(true)
        runDir = "run"
        
        vmArgs(
            "--add-opens", "java.base/sun.misc=ALL-UNNAMED",
            "--add-opens", "java.base/java.lang=ALL-UNNAMED"
        )
    }
}

fabricApi.configureTests {
    createSourceSet = true
    modId = "${modId}-test"
    eula = true
    enableClientGameTests = false
    enableGameTests = false
}

afterEvaluate {
    tasks.findByName("runGameTest")?.apply {
        enabled = false
        usesService(semaphore)
    }
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")

    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    
    // Mixin Extras for advanced mixin features
    implementation("io.github.llamalad7:mixinextras-fabric:0.4.1")
    include("io.github.llamalad7:mixinextras-fabric:0.4.1")
}

tasks.jar {
    from("LICENSE")
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.processResources {
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "version" to dynamicVersion,
                "mod_id" to modId,
                "mod_name" to modName,
                "mod_description" to modDescription,
                "mod_author" to modAuthor,
                "mod_license" to modLicense,
                "mod_homepage" to modHomepage,
                "mod_sources" to modSources,
                "mod_issues" to modIssues,
                "supported_minecraft_version" to project.property("supported_minecraft_version")
            )
        )
        filter {
            it.replace("accesswidener/heos.1.21.2.accesswidener", "accesswidener/$awFile")
        }
    }

    filesMatching("heos.mixins.json") {
        filter {
            it.replace("\${refmap}", "${base.archivesName.get()}-refmap.json")
        }
    }
}

tasks.processTestResources {
    dependsOn("kspGametestKotlin")
}

tasks.named<Copy>("processGametestResources") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn("kspTestKotlin")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

java {
    withSourcesJar()
}

publishMods {
    val modrinthToken = System.getenv("MODRINTH_TOKEN") ?: ""
    val curseforgeToken = System.getenv("CURSEFORGE_TOKEN") ?: ""

    file = tasks.remapJar.get().archiveFile
    dryRun = true // Disabled by default - set your own project IDs

    displayName = "${modName} ${property("display_name")} $dynamicVersion"
    version = dynamicVersion

    changelog = file("../../RELEASE_NOTE.md").readText()
    type = STABLE
    modLoaders.add("fabric")

    val targets = property("supported_versions").toString().split(",")

    modrinth {
        projectId = "YOUR_MODRINTH_PROJECT_ID" // Replace with your project ID
        accessToken = modrinthToken

        targets.forEach(minecraftVersions::add)
        requires("fabric-api")
    }

    curseforge {
        projectId = "YOUR_CURSEFORGE_PROJECT_ID" // Replace with your project ID
        accessToken = curseforgeToken

        targets.forEach(minecraftVersions::add)
        requires("fabric-api")
    }
}

private abstract class ServerRunSemaphore : BuildService<BuildServiceParameters.None>

private val semaphore = gradle.sharedServices.registerIfAbsent("semaphore", ServerRunSemaphore::class.java) {
    maxParallelUsages.set(1)
}

fun runGit(vararg args: String): String? = try {
    val proc = ProcessBuilder("git", *args).redirectErrorStream(true).start()
    proc.waitFor(5, TimeUnit.SECONDS)
    if (proc.exitValue() == 0) proc.inputStream.bufferedReader().readText().trim().takeIf { it.isNotBlank() } else null
} catch (_: Exception) { null }

fun loadHeosmodMetadata(file: File): Map<String, String> {
    val pattern = Regex("""public\s+static\s+final\s+String\s+(\w+)\s*=\s*\"((?:\\.|[^\"])*)\";""")
    return pattern.findAll(file.readText()).associate { match ->
        val key = match.groupValues[1]
        val value = match.groupValues[2]
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .replace("\\\\", "\\")
        key to value
    }
}
