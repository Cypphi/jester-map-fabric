import org.gradle.api.GradleException
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar

plugins {
	id("dev.kikugie.stonecutter")
	id("fabric-loom") version "1.17.13"
	id("maven-publish")
}

val modId = "jester_map"
val minecraftVersion = property("minecraft_version").toString()
val loaderVersion = property("loader_version").toString()
val fabricVersion = property("fabric_version").toString()
val modMenuVersion = property("modmenu_version").toString()
val modVersion = property("mod_version").toString()
val mavenGroup = property("maven_group").toString()
val archivesBaseName = property("archives_base_name").toString()
val mappingsProvider = findProperty("mappings_provider")?.toString() ?: "identity"
val minecraftRange = findProperty("minecraft_range")?.toString() ?: minecraftVersion
val minecraftDependency = findProperty("minecraft_dependency")?.toString() ?: minecraftVersion
val fabricDependency = findProperty("fabric_dependency")?.toString() ?: ">=$fabricVersion"
val javaRelease = findProperty("java_release")?.toString()?.toInt() ?: 21
val javaDependency = findProperty("java_dependency")?.toString() ?: ">=21"
val versionDigits = findProperty("version_digits")?.toString() ?: minecraftVersion.filter(Char::isDigit)
val entrypointPackage = findProperty("version_entrypoint_package")?.toString()
	?: "dev.cypphi.jestermap.versions.v$versionDigits"

version = "$modVersion+mc$minecraftRange"
group = mavenGroup

base {
	archivesName.set(archivesBaseName)
}

repositories {
	maven {
		name = "Fabric"
		url = uri("https://maven.fabricmc.net/")
	}
	mavenCentral()
	maven {
		name = "TerraformersMC"
		url = uri("https://maven.terraformersmc.com/releases")
		content {
			includeGroup("com.terraformersmc")
		}
	}
}

loom {
	mods {
		create(modId) {
			sourceSet(sourceSets["main"])
		}
	}
}

dependencies {
	minecraft("com.mojang:minecraft:$minecraftVersion")
	when (mappingsProvider) {
		"mojmap", "official" -> mappings(loom.officialMojangMappings())
		"none", "identity" -> mappings(loom.layered {
			mappings(rootProject.file("versions/$minecraftVersion/identity.tiny")) {
				fallbackNamespaces("official", "named")
			}
		})
		else -> throw GradleException("Unsupported mappings_provider '$mappingsProvider'")
	}

	modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
	modImplementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
	implementation("com.google.code.gson:gson:2.13.1")
	modCompileOnly("com.terraformersmc:modmenu:$modMenuVersion")

	testImplementation(platform("org.junit:junit-bom:5.12.2"))
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

sourceSets {
	named("main") {
		java.srcDirs(
			rootProject.file("src/mixin/java"),
			rootProject.file("src/versions/$versionDigits/java"),
		)
		resources.srcDir(rootProject.file("src/mixin/resources"))
	}
}

tasks.processResources {
	dependsOn(tasks.named("stonecutterGenerate"))
	val resourceProperties = mapOf(
		"version" to version.toString(),
		"entrypoint_package" to entrypointPackage,
		"loader_dependency" to ">=$loaderVersion",
		"minecraft_dependency" to minecraftDependency,
		"java_dependency" to javaDependency,
		"fabric_dependency" to fabricDependency,
	)
	inputs.properties(resourceProperties)

	filesMatching("fabric.mod.json") {
		expand(resourceProperties)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release.set(javaRelease)
}

tasks.named<JavaCompile>("compileJava") {
	dependsOn(tasks.named("stonecutterGenerate"))
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
}

if (mappingsProvider in setOf("none", "identity")) {
	tasks.matching { it.name in setOf("sourcesJar", "remapSourcesJar") }.configureEach {
		enabled = false
	}
}

java {
	withSourcesJar()
	sourceCompatibility = JavaVersion.VERSION_21
	targetCompatibility = JavaVersion.VERSION_21
}

tasks.named<Jar>("jar") {
	from("LICENSE") {
		rename("LICENSE", "LICENSE_$archivesBaseName")
	}
}

publishing {
	publications {
		create<MavenPublication>("mavenJava") {
			artifactId = archivesBaseName
			from(components["java"])
		}
	}
}
