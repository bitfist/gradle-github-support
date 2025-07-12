package io.github.bitfist.github.repository

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

lateinit var myProject: Project

@Suppress("UnnecessaryAbstractClass")
abstract class GitHubRepositoriesPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		myProject = project
		// Register the extension function so it's available in build scripts
		project.extensions.extraProperties["gitHub"] = project.repositories::gitHub
	}
}

fun RepositoryHandler.gitHub(notation: String) {
	val (owner, repo) = notation.split("/")
	maven {
		it.name = "GitHubPackages-$owner-$repo"
		it.url = URI("https://maven.pkg.github.com/$owner/$repo")
		it.credentials {
			it.username = myProject.findProperty("GPR_USER") as String?
				?: System.getenv("GITHUB_USERNAME")
			it.password = myProject.findProperty("GPR_KEY")  as String?
				?: System.getenv("GITHUB_TOKEN")
		}
	}
}
