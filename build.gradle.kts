import de.siphalor.jcyo.gradle.JcyoTask
import java.util.*

plugins {
	alias(libs.plugins.loom)
	java
	`maven-publish`
	alias(mcLibs.plugins.smcmtk)
	alias(libs.plugins.shadow)
	alias(libs.plugins.jcyo)
	alias(libs.plugins.modPublisher)

}

val minecraftVersionDescriptor = project.properties["minecraft.version.descriptor"] as String
val mcProps = Properties().apply {
	val propFile = project.layout.settingsDirectory.file("gradle/mc-${minecraftVersionDescriptor}/gradle.properties")
	load(propFile.asFile.inputStream())
}

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

loom {}

dependencies {
	annotationProcessor(libs.lombok)
	compileOnly(libs.lombok)
	annotationProcessor(libs.autoService)
	compileOnly(libs.autoService)

	minecraft(mcLibs.minecraft)
  	mappings(loom.layered {
  		officialMojangMappings()
  		parchment(variantOf(mcLibs.parchment) {
  			artifactType("zip")
  		})
  	})
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

// Mod sites
/*
static def getChangelog() {
	return 'git log -1 --format=format:##%x20%s%n%n%b%nRelease%x20by%x20%an --grep Version'.execute().text.trim()
}

tasks.register('uploadToModSites') {
	dependsOn build
	group = "upload"
}

if (project.hasProperty("curseforgeToken")) {
	curseforge {
		apiKey project.curseforgeToken
		def changes = project.getChangelog()
		project {
			id = "318416"
			releaseType = project.mod_release
			changelogType = "markdown"
			changelog = changes
			addGameVersion('Fabric')
			for (version in ((String) project.mod_mc_versions).split(";")) {
				addGameVersion(version)
			}
			relations {
				requiredDependency "fabric-api"
				optionalDependency "appleskin"
				optionalDependency "polymer"
				optionalDependency "roughly-enough-items"
			}
			mainArtifact(remapJar) {
				displayName = "[${project.mod_mc_version_specifier}] ${project.mod_version}"
			}
		}
	}
	uploadToModSites.finalizedBy(tasks.curseforge)
}

modrinth {
	if (project.hasProperty("modrinthToken")) {
		token = project.modrinthToken
		uploadToModSites.finalizedBy(tasks.modrinth, tasks.modrinthSyncBody)
	}

	projectId = "roxihOCb"
	versionName = "[$project.mod_mc_version_specifier] $project.mod_version"
	versionType = project.mod_release
	changelog = project.getChangelog()
	uploadFile = remapJar
	gameVersions = project.mod_mc_versions.split(";") as List<String>
	loaders = ["fabric"]
	syncBodyFrom = file("README.md").text

	dependencies {
		required.project("fabric-api")
		optional.project("appleskin")
		optional.project("polymer")
	}
}
tasks.modrinth.group = "upload"
tasks.modrinthSyncBody.group = "upload"

if (project.hasProperty("githubToken")) {
	githubRelease {
		token githubToken
		targetCommitish = minecraft_major_version
		releaseName = "Version $mod_version for $project.mod_mc_version_specifier"
		body = project.getChangelog()
		releaseAssets remapJar.getArchiveFile()
		prerelease = mod_release != "release"
		overwrite = true
	}
	uploadToModSites.finalizedBy(tasks.githubRelease)
}
 */
