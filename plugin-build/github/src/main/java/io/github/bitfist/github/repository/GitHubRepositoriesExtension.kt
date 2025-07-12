package io.github.bitfist.github.repository

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * A Gradle extension to manage and configure GitHub repository settings.
 */
open class GitHubRepositoriesExtension @Inject constructor(project: Project) {

	private val objects: ObjectFactory = project.objects

	val defaultUser: Property<String> = objects.property(String::class.java)
	val defaultToken: Property<String> = objects.property(String::class.java)
	val mavenRepositories: MavenRepositoriesConfig = objects.newInstance(MavenRepositoriesConfig::class.java)

	open class MavenRepositoriesConfig @Inject constructor(objects: ObjectFactory) {
		val repositories: NamedDomainObjectContainer<MavenRepositoryConfig> =
			objects.domainObjectContainer(MavenRepositoryConfig::class.java)

		fun mavenRepository(configure: Action<MavenRepositoryConfig>) {
			val name = "repo-${repositories.size + 1}"
			repositories.create(name, configure)
		}
	}

	open class MavenRepositoryConfig @Inject constructor(val name: String, objects: ObjectFactory) {
		/** e.g. "user/repo" */
		val repository: Property<String> = objects.property(String::class.java)
		val user: Property<String> = objects.property(String::class.java).convention("")
		val token: Property<String> = objects.property(String::class.java).convention("")
	}
}
