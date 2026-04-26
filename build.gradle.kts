import de.siphalor.jcyo.gradle.JcyoTask

plugins {
	java
	`maven-publish`
	alias(mcLibs.plugins.smcmtk)
	alias(mcLibs.plugins.fabric.loom)
	alias(libs.plugins.shadow)
	alias(libs.plugins.jcyo)
	alias(libs.plugins.modPublisher)
}

val minecraftVersionDescriptor = project.properties["minecraft.version.descriptor"] as String

group = "de.siphalor.${project.name}"
val archivesBaseName = "${project.name}-mc${minecraftVersionDescriptor}"
val shortVersion = "${properties["version"]}"
version = "${shortVersion}+mc${mcLibs.versions.minecraft.get()}"

repositories {
	mavenCentral()
	maven {
		name = "ParchmentMC"
		url = uri("https://maven.parchmentmc.org")
		mavenContent {
			includeGroup("org.parchmentmc.data")
		}
	}
	maven {
		name = "Nucleoid"
		url = uri("https://maven.nucleoid.xyz")
		mavenContent {
			includeGroup("xyz.nucleoid")
			includeGroup("eu.pb4")
		}
	}
	maven {
		name = "Siphalor"
		url = uri("https://maven.siphalor.de/")
		mavenContent {
			includeGroupAndSubgroups("de.siphalor")
		}
	}
	maven {
		name = "TerraformersMC"
		url = uri("https://maven.terraformersmc.com/releases")
	}
	maven {
		name = "shedaniel"
		url = uri("https://maven.shedaniel.me/")
	}
	maven { url = uri("https://maven.ryanliptak.com") }
	mavenLocal()
}

smcmtk {
	useMojangMappings()
	createModConfigurations(listOf(sourceSets.main.get()))
}

dependencies {
	annotationProcessor(libs.lombok)
	compileOnly(libs.lombok)
	annotationProcessor(libs.autoService)
	compileOnly(libs.autoService)

	minecraft(mcLibs.minecraft)
	modImplementation(libs.fabric.loader)

	compileOnly(libs.jspecify)

	modImplementation(mcLibs.fabric.api)
	
	shadow(libs.exp4j)
	implementation(libs.exp4j)

	include(mcLibs.bundles.config)
	modApi(mcLibs.bundles.config)

	include(mcLibs.capsaicin)
	modApi(mcLibs.capsaicin)

	modImplementation(mcLibs.bundles.polymer)

	modCompileOnly(mcLibs.modmenu)
	modRuntimeOnly(mcLibs.modmenu)
	modCompileOnly(mcLibs.rei.api)

	modLocalRuntime(mcLibs.bundles.compatibilityCheck)

	testImplementation(platform(libs.junit.platform))
	testImplementation(libs.bundles.junit)
}

configurations.configureEach {
	resolutionStrategy {
		force("net.fabricmc:fabric-loader:${libs.versions.fabric.loader.get()}")
	}
}

tasks.processResources {
	inputs.property("version", version)

	filesMatching("fabric.mod.json") {
		expand("version" to version)
	}
}

val jcyo = tasks.register<JcyoTask>("jcyo") {
	inputDirectory = file("src/main/java")
	variables = smcmtk.mcProps.map {
		it.filterKeys { key -> key.startsWith("preprocessor.") }.mapKeys { (key, _) -> key.substring("preprocessor.".length) }
	}
}

tasks.compileJava {
	dependsOn(jcyo)
}

tasks.jar {
	from("LICENSE")
	archiveClassifier.set("dev")
}

tasks.shadowJar {
	configurations = listOf(project.configurations.shadow.get())
	archiveClassifier.set("dev")
	relocate("net.objecthunter", "de.siphalor.spiceoffabric.shadow.net.objecthunter")
}

tasks.remapJar {
	dependsOn(tasks.shadowJar)
	inputFile = tasks.shadowJar.get().archiveFile
}

tasks.test {
	useJUnitPlatform()
}

// configure the maven publication
publishing {
	publications {
		create<MavenPublication>("mod") {
			artifactId = archivesBaseName
			version = shortVersion

			from(components["java"])
		}
	}
	repositories {
		if (project.hasProperty("siphalorMavenUser")) {
			maven {
				name = "Siphalor"
				url = uri("https://maven.siphalor.de/upload.php")
				credentials {
					username = project.property("siphalor.maven.user") as String
					password = project.property("siphalor.maven.password") as String
				}
			}
		}
	}
}

publisher {
	apiKeys {
		project.findProperty("modrinth.token")?.let { modrinth(it as String) }
		project.findProperty("curseforge.token")?.let { curseforge(it as String) }
		project.findProperty("github.token")?.let { github(it as String) }
	}

	curseID = "318416"
	modrinthID = "roxihOCb"

	artifact.set(tasks.findByName("remapJar") ?: tasks.jar)

	projectVersion = project.version as String
	versionType = project.property("version.type") as String
	loaders = listOf("fabric")
	curseEnvironment = "client"

	gameVersions = smcmtk.mcProps.getting("mc.version.supported").map { it.split(", ") }

	displayName = "[${smcmtk.mcProps.getting("mc.version.title").get()}] $shortVersion"
	changelog.set(providers.exec {
		commandLine("git", "log", "-1", "--format=format:##%x20%s%n%n%b", "--grep", "Version")
	}.standardOutput.asText.map { it.trim() })

	curseDepends {
		required("fabric-api")
		optional("polymer")
		optional("appleskin")
	}
	modrinthDepends {
		required("fabric-api")
		optional("polymer")
		optional("appleskin")
	}

	github {
		repo("Siphalor/spiceoffabric")
		tag(shortVersion)
		displayName(shortVersion)
		createTag(true)
		createRelease(true)
	}
}
