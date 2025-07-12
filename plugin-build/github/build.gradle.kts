import git.semver.plugin.gradle.PrintTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
	`maven-publish`
	alias(libs.plugins.semver)
}

version = semver.version
group = "io.github.bitfist"

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
	testImplementation("org.junit.jupiter:junit-jupiter")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val javaVersion = JavaVersion.VERSION_17
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
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
}

gradlePlugin {
    website.set("https://github.com/bitfist/github-support")
    vcsUrl.set("https://github.com/bitfist/github-support")

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
		create<MavenPublication>("maven") {
			groupId = project.group.toString()
			artifactId = "jcef-gradle-plugin"
			version = project.version.toString()

			from(components["java"])
		}
	}
}
