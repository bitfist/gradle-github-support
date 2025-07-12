package io.github.bitfist.github.release

import git.semver.plugin.changelog.ChangeLogFormatter
import git.semver.plugin.changelog.ChangeLogTextFormatter
import git.semver.plugin.gradle.GitSemverPluginExtension
import git.semver.plugin.gradle.ReleaseTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.api.tasks.testing.Test
import org.gradle.external.javadoc.JavadocMemberLevel
import org.gradle.jvm.tasks.Jar
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.net.URI

@Suppress("UnnecessaryAbstractClass")
abstract class GitHubReleasePlugin : Plugin<Project> {

	override fun apply(project: Project) {
		val extension = project.extensions.create("gitHubRelease", GitHubReleaseExtension::class.java, project)

		project.pluginManager.apply("java")
		configureJaCoCo(project)
		configurePublication(project, extension)
		configureSemanticVersioning(project, extension)
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

		// Configure sourceJar task
		val sourceJar = project.tasks.register("sourceJar", Jar::class.java) { task ->
			task.group = "documentation"
			task.archiveClassifier.set("sources")
			task.from(
				project.extensions.getByType(SourceSetContainer::class.java).getByName("main").allJava
			)
		}

		// Configure javadoc task
		project.tasks.withType(Javadoc::class.java).configureEach { task ->
			task.source = project.extensions.getByType(SourceSetContainer::class.java).getByName("main").allJava
			task.classpath = project.configurations.getByName("compileClasspath")
			task.options.memberLevel = JavadocMemberLevel.PUBLIC
		}

		// Configure javadocJar task to bundle javadoc
		val javadocJar = project.tasks.register("javadocJar", Jar::class.java) { task ->
			task.group = "documentation"
			task.archiveClassifier.set("javadoc")
			task.from(project.tasks.named("javadoc"))
		}

		// Delay publication configuration until after evaluation
		project.afterEvaluate {
			val publishing = project.extensions.getByType(PublishingExtension::class.java)
			publishing.repositories { repositories ->
				repositories.maven { repository ->
					repository.url = URI("https://maven.pkg.github.com/${extension.getRepository()}")
					repository.credentials { credentials ->
						credentials.username = extension.user.orNull
								?: System.getenv("GITHUB_ACTOR")
								?: throw IllegalStateException("GitHub Release: GitHub Package Registry username missing")
						credentials.password = extension.token.orNull
							?: System.getenv("GITHUB_TOKEN")
								?: throw IllegalStateException("GitHub Release: GitHub Package Registry token missing")
					}
				}
			}
			publishing.publications.create("mavenJava", MavenPublication::class.java) { publication ->
				publication.from(project.components.getByName("java"))
				publication.artifact(sourceJar.get())
				publication.artifact(javadocJar.get())
			}
		}
	}

	private fun configureSemanticVersioning(project: Project, extension: GitHubReleaseExtension) {
		project.pluginManager.apply("com.github.jmongard.git-semver-plugin")
		project.tasks.withType(ReleaseTask::class.java) {
			it.dependsOn(project.tasks.withType(JacocoReport::class.java))
		}
		project.extensions.configure<GitSemverPluginExtension>("semver") { semver ->
			semver.patchPattern = "\\A(fix|chore|docs|refactor|ci|build|test|deps)(?:\\([^()]+\\))?:"

			fun ChangeLogTextFormatter.appendChangeLongEntry() {
				val longHash = hash().take(40)
				val shortHash = longHash.take(8)

				append("- ")
				append("[$shortHash](https://www.github.com/${extension.getRepository()}/commit/$longHash) ")
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
}
