package io.github.bitfist.github.repository

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A Gradle plugin to configure and automate the setup of GitHub Maven repositories and publishing.
 *
 * This plugin provides the following functionality:
 * - Adds a `gitHubRepositories` extension to configure the default credentials and repository definitions.
 * - Automatically applies the Java plugin and configures project repositories based on the specified
 *   GitHub Maven repository details.
 * - Configures Maven publishing for the project, including tasks to generate source and Javadoc artifacts.
 * - Delays the configuration of the Maven publication until after the project's evaluation phase to ensure
 *   all necessary components are available.
 *
 * Key Features:
 * - Allows defining multiple GitHub Maven repositories with individual settings.
 * - Supports usage of environment variables or Gradle properties for credentials.
 * - Automates artifact generation for sources and Javadocs to include in publications.
 *
 * Usage of the `gitHubRepositories` extension includes configuring repository-specific details
 * and setting default credentials for GitHub Package Registry.
 */
@Suppress("UnnecessaryAbstractClass")
abstract class GitHubRepositoriesPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		// Add the 'template' extension object
		val extension =
			project.extensions.create("gitHubRepositories", GitHubRepositoriesExtension::class.java, project)

		project.pluginManager.apply("java")
		configureRepositories(project, extension)
	}

	private fun configureRepositories(project: Project, extension: GitHubRepositoriesExtension) {
		extension.mavenRepositories.repositories.forEach { config ->
			project.repositories.maven { repository ->
				repository.setUrl("https://maven.pkg.github.com/${config.repository.get()}")
				repository.credentials.username =
					config.user.get()
						?: extension.defaultUser.orNull
							?: project.findProperty("GPR_USER") as String?
							?: System.getenv("GITHUB_ACTOR")
							?: throw IllegalStateException("GitHub Repositories: GitHub Package Registry username missing")
				repository.credentials.password =
					config.token.get()
						?: extension.defaultToken.orNull
							?: project.findProperty("GPR_KEY") as String?
							?: System.getenv("GPR_KEY")
							?: throw IllegalStateException("GitHub Repositories: GitHub Package Registry token missing")
			}
		}
	}

}
