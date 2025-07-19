package io.github.bitfist.github.release

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import java.net.URI
import javax.inject.Inject

/**
 * A Gradle extension to manage and configure GitHub repository settings.
 */
open class GitHubReleaseExtension @Inject constructor(project: Project) {

	private val objects: ObjectFactory = project.objects

	val repository: Property<String> = objects.property(String::class.java).convention(System.getenv("GITHUB_REPOSITORY"))
	val user: Property<String> = objects.property(String::class.java).convention(System.getenv("GITHUB_ACTOR"))
	val token: Property<String> = objects.property(String::class.java).convention(System.getenv("GITHUB_TOKEN"))
	val projectName: Property<String> = objects.property(String::class.java)
	val projectDescription: Property<String> = objects.property(String::class.java)
	val developer: Property<String> = objects.property(String::class.java)
	val licenseFile: RegularFileProperty = objects.fileProperty().convention(null as RegularFile?)
	val license: Property<String> = objects.property(String::class.java).convention(null as String?)
	val licenseUri: Property<URI> = objects.property(URI::class.java).convention(null as URI?)
}
