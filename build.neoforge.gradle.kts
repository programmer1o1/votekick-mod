plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			forgeVersionRange = "[${prop("deps.minecraft")}]"
		}
		required("neoforge") {
			forgeVersionRange = "[1,)"
		}
	}
}

neoForge {
	version = property("deps.neoforge") as String

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.active?.version})"
			programArgument("--username=Dev")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.active?.version})"
		}
	}

	mods {
		register(property("mod.id") as String) {
			sourceSet(sourceSets["main"])
		}
	}
}

sourceSets {
	named("main") {
		java.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/main/java").get().asFile))
		resources.setSrcDirs(listOf(layout.buildDirectory.dir("generated/stonecutter/main/resources").get().asFile))
	}
}

tasks.withType<JavaCompile>().configureEach {
	dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.matching { it.name.startsWith("ksp") }.configureEach {
	dependsOn(tasks.named("stonecutterGenerate"))
}

repositories {
	mavenCentral()
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
}

dependencies {
	val carpetVersion = prop("deps.carpet-neoforge")
	if (carpetVersion.isNotBlank()) {
		runtimeOnly("maven.modrinth:neoforge-carpet:$carpetVersion")
	}
	val luckPermsVersion = prop("deps.luckperms-neoforge")
	if (luckPermsVersion.isNotBlank()) {
		runtimeOnly("maven.modrinth:luckperms:$luckPermsVersion")
	}
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
