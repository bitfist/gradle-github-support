package io.github.bitfist.github.release

import git.semver.plugin.changelog.ChangeLogFormatter
import git.semver.plugin.changelog.ChangeLogTextFormatter
import git.semver.plugin.gradle.GitSemverPluginExtension
import git.semver.plugin.gradle.ReleaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI

@Suppress("UnnecessaryAbstractClass")
abstract class GitHubReleasePlugin : Plugin<Project> {

	private val gitTagVersion = getVersionFromGitTag()

	override fun apply(project: Project) {
		val extension = project.extensions.create("gitHubRelease", GitHubReleaseExtension::class.java, project)

		configureJava(project)
		configureJaCoCo(project)
		project.afterEvaluate {
			configureSemanticVersioning(project, extension)
			configurePublication(project, extension)
			configureCopyLicenseTask(project, extension)
		}
	}

	private fun configureJava(project: Project) {
		project.pluginManager.apply("java")
		if (!project.pluginManager.hasPlugin("java-gradle-plugin")) {
			project.extensions.getByType(JavaPluginExtension::class.java).apply {
				withJavadocJar()
				withSourcesJar()
			}
		}
	}

	/**
	 * Configures the semantic versioning for the given project using the Git-Semver plugin.
	 *
	 * This method applies the Git-Semver plugin to the project, sets up a dependency on
	 * Jacoco report tasks for release tasks, and configures semantic versioning rules,
	 * including versioning patterns and changelog formatting.
	 *
	 * @param project The Gradle project where semantic versioning is being configured.
	 * @param extension An instance of [GitHubReleaseExtension] containing repository-related configurations.
	 */
	private fun configureSemanticVersioning(project: Project, extension: GitHubReleaseExtension) {
		project.pluginManager.apply("com.github.jmongard.git-semver-plugin")
		project.tasks.withType(ReleaseTask::class.java) {
			it.dependsOn(project.tasks.withType(JacocoReport::class.java))
		}
		project.extensions.configure<GitSemverPluginExtension>("semver") { semver ->
			project.version = determineVersion(semver)
			semver.patchPattern = "\\A(fix|chore|docs|refactor|ci|build|test|deps)(?:\\([^()]+\\))?:"

			fun ChangeLogTextFormatter.appendChangeLongEntry() {
				val longHash = hash().take(40)
				val shortHash = longHash.take(8)

				append("- ")
				if (extension.repository.isPresent) {
					append("[$shortHash](https://www.github.com/${extension.repository.get()}/commit/$longHash) ")
				}
				append(scope())

				val commitMessage = header()
				appendLine(commitMessage)
			}

			semver.changeLogFormat = ChangeLogFormatter {
				appendLine(constants.header).appendLine()

				withType("release") {
					skip()
				}

				// breaking
				withBreakingChanges {
					appendLine(constants.breakingChange)
					formatChanges {
						appendChangeLongEntry()
					}
					appendLine()
				}

				// fix, feat, refactor
				withType(types = arrayOf("feat", "fix", "refactor")) {
					appendLine(constants.headerTexts[groupKey])
					with({ constants.headerTexts.containsKey(it.scope) }) {
						formatChanges {
							appendChangeLongEntry()
						}
					}
					formatChanges {
						appendChangeLongEntry()
					}
					appendLine()
				}

				// chores and other known changes
				groupBySorted({ constants.headerTexts[it.scope] ?: constants.headerTexts[it.type] }) {
					appendLine(groupKey)
					with({ constants.headerTexts.containsKey(it.scope) }) {
						formatChanges {
							appendChangeLongEntry()
						}
					}
					formatChanges {
						appendChangeLongEntry()
					}
					appendLine()
				}

				// other unknown changes
				otherwise {
					appendLine(constants.otherChange)
					formatChanges {
						appendChangeLongEntry()
					}
					appendLine()
				}

				appendLine(constants.footer)
			}
		}
	}

	private fun determineVersion(semver: GitSemverPluginExtension): String {
		val semanticVersionRegex = "\\d+\\.\\d+\\.\\d+".toRegex()

		// if we are on a tag-commit, we use the tag version
		return if (gitTagVersion.matches(semanticVersionRegex)) {
			gitTagVersion // version provided by git tag
		} else {
			semver.version // version provided by the semantic versioning plugin
		}
	}

	private fun configureJaCoCo(project: Project) {
		project.pluginManager.apply("jacoco")
		val testTasks = project.tasks.withType(Test::class.java)
		project.tasks.withType(JacocoReport::class.java).configureEach { report ->
			report.dependsOn(testTasks)
			report.reports.apply {
				csv.required.set(true)
				xml.required.set(true)
				html.required.set(true)
			}
		}
	}

	private fun configurePublication(project: Project, extension: GitHubReleaseExtension) {
		project.pluginManager.apply("maven-publish")
		val publishing = project.extensions.getByType(PublishingExtension::class.java)
		publishing.repositories { repositories ->
			repositories.maven { repository ->
				repository.name = "GitHubPackages"

				if (extension.repository.isPresent) {
					repository.url = URI("https://maven.pkg.github.com/${extension.repository.get()}")
				}
				repository.credentials { credentials ->
					if (extension.user.isPresent) {
						credentials.username = extension.user.get()
					}
					if (extension.token.isPresent) {
						credentials.password = extension.token.get()
					}
				}
			}
		}
		if (project.pluginManager.hasPlugin("java-gradle-plugin")) {
			publishing.publications.register("pluginMaven", MavenPublication::class.java) { publication ->
				publication.from(project.components.getByName("java"))
				configurePublication(publication, extension)
			}
		} else {
			publishing.publications.register("mavenJava", MavenPublication::class.java) { publication ->
				publication.from(project.components.getByName("java"))
				configurePublication(publication, extension)
			}
		}
	}

	private fun configurePublication(publication: MavenPublication, extension: GitHubReleaseExtension) {
		publication.pom { pom ->
			pom.name.set(extension.projectName.orNull)
			pom.description.set(extension.projectDescription.orNull)
			if (extension.developer.isPresent) {
				pom.developers { developers ->
					developers.developer { developer ->
						developer.id.set(extension.developer.get())
						developer.name.set(extension.developer.get())
					}
				}
			}
			if (extension.repository.isPresent) {
				pom.scm { scm ->
					scm.connection.set("scm:git:git://github.com/${extension.repository.get()}.git")
					scm.developerConnection.set("scm:git:ssh://github.com:${extension.repository.get()}.git")
					scm.url.set("https://github.com/${extension.repository.get()}")
				}
			}
		}
	}

	private fun configureCopyLicenseTask(project: Project, extension: GitHubReleaseExtension) {
		if (!extension.licenseFile.isPresent) {
			return
		}
		val copyLicenseTask = project.tasks.register("copyLicense", Copy::class.java) { task ->
			task.from(extension.licenseFile.get().asFile)
			task.into(project.layout.buildDirectory.dir("resources/main/META-INF/"))
			task.description = "Copies the license file into build/resources/main/META-INF/"
			task.group = "build setup"
		}
		project.tasks.named("processResources").configure {
			it.dependsOn(copyLicenseTask)
		}
	}


	private fun getVersionFromGitTag(): String {
		val process = ProcessBuilder("git", "tag", "--points-at", "HEAD")
			.redirectErrorStream(true)
			.start()

		val output = StringBuilder()
		BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
			lines.forEach { output.appendLine(it) }
		}

		val exitCode = process.waitFor()
		if (exitCode != 0) {
			throw RuntimeException("Command git tag --points-at HEAD failed with exit code $exitCode")
		}

		return output.toString().trimEnd()
	}
}
