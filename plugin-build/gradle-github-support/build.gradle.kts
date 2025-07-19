import git.semver.plugin.gradle.GitSemverPluginExtension
import git.semver.plugin.gradle.ReleaseTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
	kotlin("jvm")
	`java-gradle-plugin`
	`maven-publish`
	jacoco
	alias(libs.plugins.semver)
}

group = "io.github.bitfist"

var gitTagVersion = execCommand("git", "tag", "--points-at", "HEAD")
val semanticVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()
// if we are on a tag-commit, we use the tag version
val versionToUse = if (gitTagVersion.matches(semanticVersionRegex)) {
	gitTagVersion // version provided by git tag
} else {
	semver.version // version provided by the semantic versioning plugin
}
version = versionToUse

repositories {
	mavenLocal()
	mavenCentral()
	gradlePluginPortal()
}

dependencies {
	implementation(kotlin("stdlib"))
	implementation(gradleApi())
	implementation(libs.semver)

	testImplementation(platform(libs.junit5Bom))
	testImplementation(libs.mockito)
	testImplementation(libs.mockitoKotlin)
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val javaVersion = JavaVersion.VERSION_17
java {
	sourceCompatibility = javaVersion
	targetCompatibility = javaVersion
	withSourcesJar()
	withJavadocJar()
}

tasks.withType<KotlinCompile> {
	compilerOptions {
		jvmTarget.set(JvmTarget.fromTarget(javaVersion.majorVersion))
	}
}

tasks.test {
	useJUnitPlatform()
	testLogging {
		events("passed", "skipped", "failed")
	}
	environment("GITHUB_USERNAME", "test-user")
	environment("GITHUB_ACTOR", "test-actor")
	environment("GITHUB_TOKEN", "<PASSWORD>")
	environment("GITHUB_REPOSITORY", "test-user/test-repository")
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		csv.required = true
	}
}

val repository = "bitfist/gradle-github-support"

gradlePlugin {
	website.set("https://github.com/$repository")
	vcsUrl.set("https://github.com/$repository")

	plugins {
		create("io.github.bitfist.github.repository") {
			id = "io.github.bitfist.github.repository"
			implementationClass = "io.github.bitfist.github.repository.GitHubRepositoriesPlugin"
			version = project.version.toString()
			description = "Gradle plugin to support GitHub package repositories"
			displayName = "Gradle plugin to support GitHub package repositories"
			// Note: tags cannot include "plugin" or "gradle" when publishing
			tags.set(listOf("GitHub", "Repository", "Maven"))
		}
		create("io.github.bitfist.github.release") {
			id = "io.github.bitfist.github.release"
			implementationClass = "io.github.bitfist.github.release.GitHubReleasePlugin"
			version = project.version.toString()
			description = "Gradle plugin to release to a GitHub package repository"
			displayName = "Gradle plugin to release to a GitHub package repository"
			// Note: tags cannot include "plugin" or "gradle" when publishing
			tags.set(listOf("GitHub", "Release", "Repository", "Maven"))
		}
	}
}

publishing {
	publications {
		register<MavenPublication>("pluginMaven") {
			groupId = project.group.toString()
			artifactId = "github-gradle-support"
			version = versionToUse

			pom {
				name.set("Gradle GitHub Support Plugin")
				description.set("Gradle plugin to support GitHub package repositories")
				url.set("https://github.com/$repository")
				licenses {
					license {
						name.set("The Apache License, Version 2.0")
						url.set("https://www.apache.org/licenses/LICENSE-2.0")
					}
				}
				developers {
					developer {
						id.set("bitfist")
						name.set("bitfist")
					}
				}
				scm {
					connection.set("scm:git:git://github.com/$repository.git")
					developerConnection.set("scm:git:ssh://github.com:$repository.git")
					url.set("https://github.com/$repository")
				}
			}
		}
	}

	repositories {
		maven {
			name = "GitHubPackages"
			url = uri("https://maven.pkg.github.com/$repository")
			credentials {
				username = System.getenv("GITHUB_ACTOR")
				password = System.getenv("GITHUB_TOKEN")
			}
		}
	}
}

tasks.register<ReleaseTask>("releaseVersion", ReleaseTask::class.java, extensions.getByType<GitSemverPluginExtension>())

// look up git tag
fun execCommand(vararg command: String): String {
	val process = ProcessBuilder(*command)
		.redirectErrorStream(true)
		.start()

	val output = StringBuilder()
	BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
		lines.forEach { output.appendLine(it) }
	}

	val exitCode = process.waitFor()
	if (exitCode != 0) {
		throw RuntimeException("Command ${command.joinToString(" ")} failed with exit code $exitCode")
	}

	return output.toString().trimEnd()
}

val copyLicenseTask = tasks.register<Copy>("Copy license") {
	from(rootProject.rootDir.resolve("../LICENSE.txt"))
	into(project.layout.buildDirectory.dir("resources/main/META-INF"))
}

project.tasks.named("processResources").configure {
	dependsOn(copyLicenseTask)
}
