package io.github.bitfist.github.release

import io.github.bitfist.github.repository.GitHubRepositoriesExtension
import io.github.bitfist.github.repository.GitHubRepositoriesPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class GitHubRepositoriesPluginTest {

	@Test
	@DisplayName("🔌 apply plugin adds gitHubRepositories extension with default properties")
	fun `repositories extension creation`() {
		val project = ProjectBuilder.builder().build()
		project.pluginManager.apply(GitHubRepositoriesPlugin::class.java)

		val extension = project.extensions.findByName("gitHubRepositories") as? GitHubRepositoriesExtension
		assertNotNull(extension, "Extension 'gitHubRepositories' should be created")
		assertNotNull(extension.defaultUser, "defaultUser property should exist")
		assertNotNull(extension.defaultToken, "defaultToken property should exist")
		assertNotNull(extension.mavenRepositories, "mavenRepositories configuration should exist")
	}

	@Test
	@DisplayName("⚙️ should allow defining maven repositories")
	fun `define maven repository`() {
		val project = ProjectBuilder.builder().build()
		project.pluginManager.apply(GitHubRepositoriesPlugin::class.java)

		val extension = project.extensions.getByName("gitHubRepositories") as GitHubRepositoriesExtension
		extension.defaultUser.set("defaultUser")
		extension.defaultToken.set("defaultToken")
		extension.mavenRepositories.mavenRepository {
			it.repository.set("owner/repo")
			it.user.set("user1")
			it.token.set("token1")
		}

		assertEquals(1, extension.mavenRepositories.repositories.size, "Should contain one repository config")
		val config = extension.mavenRepositories.repositories.getByName("repo-1")
		assertEquals("owner/repo", config.repository.get())
		assertEquals("user1", config.user.get())
		assertEquals("token1", config.token.get())
	}
}
