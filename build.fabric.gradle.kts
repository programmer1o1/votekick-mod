plugins {
	id("mod-platform")
	id("fabric-loom")
}

platform {
	loader = "fabric"
	dependencies {
		required("minecraft") {
			versionRange = prop("deps.minecraft")
		}
		required("fabric-api") {
			slug("fabric-api")
			versionRange = ">=${prop("deps.fabric-api")}"
		}
		required("fabricloader") {
			versionRange = ">=${libs.fabric.loader.get().version}"
		}
		optional("modmenu") {
			slug("modmenu")
			versionRange = ">=${prop("deps.modmenu")}"
		}
	}
}

loom {
	val awPath = rootProject.file("src/main/resources/aw/${stonecutter.current.version}.accesswidener")
	if (awPath.exists()) {
		accessWidenerPath = awPath
	}

	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
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
	strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") { name = "TerraformersMC" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
	strictMaven("https://maven.nucleoid.xyz/", "eu.pb4") { name = "Nucleoid" }
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	mappings(loom.officialMojangMappings())
	modImplementation(libs.fabric.loader)
	modImplementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}")
	modCompileOnly("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
	modRuntimeOnly("com.terraformersmc:modmenu:${prop("deps.modmenu")}")
	modRuntimeOnly("maven.modrinth:carpet:${prop("deps.carpet")}")
	val fabricPermissionsVersion = prop("deps.fabric-permissions-api")
	if (fabricPermissionsVersion.isNotBlank()) {
		modRuntimeOnly("maven.modrinth:fabric-permissions-api:$fabricPermissionsVersion")
	}
	val luckPermsVersion = prop("deps.luckperms-fabric")
	if (luckPermsVersion.isNotBlank()) {
		modRuntimeOnly("maven.modrinth:luckperms:$luckPermsVersion")
	}
}
