package io.github.bitfist.github.release

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * A Gradle extension to manage and configure GitHub repository settings.
 */
open class GitHubReleaseExtension @Inject constructor(project: Project) {

	private val objects: ObjectFactory = project.objects

	val repository: Property<String> = objects.property(String::class.java).convention("")
	val user: Property<String> = objects.property(String::class.java).convention("")
	val token: Property<String> = objects.property(String::class.java).convention("")

	fun getRepository(): String = repository.get()
		?: System.getenv("GITHUB_REPOSITORY")
		?: throw IllegalStateException("GitHub Release: GitHub Repository missing")
}
