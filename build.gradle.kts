import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import de.siphalor.jcyo.gradle.JcyoTask
import de.siphalor.minecraft_modding_toolkit.gradle.project_plugin.filter.JsonMergeFilterReader
import net.fabricmc.loom.task.RemapJarTask

plugins {
	java
	`maven-publish`
	alias(mcLibs.plugins.smcmtk)
	alias(mcLibs.plugins.fabric.loom)
	alias(libs.plugins.shadow) apply(false)
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

sourceSets {
	main {
		resources {
			val dataVersion = smcmtk.mcProps.getting("minecraft.data.version").get().toInt()
			val resourcesVersion = smcmtk.mcProps.getting("minecraft.resources.version").get().toInt()
			srcDirs(
				resolveDataDir(dataVersion, "src/main/recipes"),
				resolveDataDir(dataVersion, "src/main/advancements"),
				resolveDataDir(resourcesVersion, "src/main/item_models"),
			)
		}
	}
}

fun resolveDataDir(version: Int, base: String): String {
	return base + "/" + file(base).list()?.map { it.toInt() }?.filter { it <= version }?.max()
}

val shadow = configurations.register("shadow") {
	isTransitive = false
}

dependencies {
	annotationProcessor(libs.lombok)
	compileOnly(libs.lombok)
	annotationProcessor(libs.autoService)
	compileOnly(libs.autoService)

	minecraft(mcLibs.minecraft)
	"modImplementation"(libs.fabric.loader)

	compileOnly(libs.jspecify)

	"modImplementation"(mcLibs.fabric.api)
	
	shadow(libs.exp4j)
	implementation(libs.exp4j)

	include(mcLibs.bundles.config)
	"modApi"(mcLibs.bundles.config)

	include(mcLibs.capsaicin)
	"modApi"(mcLibs.capsaicin)

	"modImplementation"(mcLibs.bundles.polymer)
	"modLocalRuntime"(mcLibs.polymer.bundled)

	"modCompileOnly"(mcLibs.modmenu)
	"modRuntimeOnly"(mcLibs.modmenu)
	versionCatalogs.named("mcLibs").findLibrary("rei.api").ifPresent {
		"modCompileOnly"(it)
	}

	"modLocalRuntime"(mcLibs.bundles.compatibilityCheck)

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
	inputs.property("mixins.extra.common", smcmtk.mcProps.getting("mixins.extra.common").orElse(""))

	filesMatching("fabric.mod.json") {
		filter<JsonMergeFilterReader>(mapOf("merge" to mapOf(
			"version" to version
		)))
	}
	filesMatching("spiceoffabric.mixins.json") {
		filter<JsonMergeFilterReader>(mapOf("merge" to mapOf(
			"mixins" to smcmtk.mcProps.getting("mixins.extra.common").map { it.split(",") }.getOrElse(listOf())
		)))
	}
}

val jcyo = tasks.register<JcyoTask>("jcyo") {
	inputDirectory = file("src/main/java")
	variables = smcmtk.mcProps.map {
		it.filterKeys { key -> key.startsWith("preprocessor.") }.mapKeys { (key, _) -> key.substring("preprocessor.".length) }
	}
	importOrder = listOf(
		"",
		"com.mojang|net.minecraft",
		"\\#",
	)
}

java {
	sourceCompatibility = JavaVersion.toVersion(mcLibs.versions.java.get())
	targetCompatibility = JavaVersion.toVersion(mcLibs.versions.java.get())
}

tasks.compileJava {
	dependsOn(jcyo)
}

tasks.jar {
	archiveClassifier.set("dev")
}

tasks.findByName("remapJar")?.apply {
	this as RemapJarTask
	archiveClassifier = "remapped"
}

val shadowJar = tasks.register<ShadowJar>("shadowJar") {
	group = BasePlugin.BUILD_GROUP
	description = "Assembles a jar archive containing the classes and included dependencies of this project."

	val inputTask = tasks.findByName("remapJar") as RemapJarTask? ?: tasks.jar.get()
	inputs.file(inputTask.archiveFile)
	dependsOn(inputTask)
	from(inputTask.archiveFile.map { zipTree(it) })

	from("LICENSE")

	configurations = listOf(shadow.get())
	relocate("net.objecthunter", "de.siphalor.spiceoffabric.shadow.net.objecthunter")
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

	artifact.set(shadowJar)

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
