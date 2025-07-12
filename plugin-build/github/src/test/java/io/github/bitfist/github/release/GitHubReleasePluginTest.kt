package io.github.bitfist.github.release

import org.gradle.jvm.tasks.Jar
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class GitHubReleasePluginTest {

	@Test
	@DisplayName("🛠️ apply plugin adds gitHubRelease extension with default properties")
	fun `extension creation`() {
		val project = ProjectBuilder.builder().build()
		project.pluginManager.apply(GitHubReleasePlugin::class.java)

		val extension = project.extensions.findByName("gitHubRelease") as? GitHubReleaseExtension
		assertNotNull(extension, "Extension 'gitHubRelease' should be created")
		assertNotNull(extension.repository, "repository property should exist")
		assertEquals("", extension.user.get(), "user should default to empty string")
		assertEquals("", extension.token.get(), "token should default to empty string")
	}

	@Test
	@DisplayName("📦 apply plugin registers sourceJar and javadocJar tasks")
	fun `tasks registration`() {
		val project = ProjectBuilder.builder().build()
		project.pluginManager.apply(GitHubReleasePlugin::class.java)

		val sourceJar = project.tasks.getByName("sourceJar") as Jar
		assertEquals("documentation", sourceJar.group, "sourceJar should be in 'documentation' group")
		assertEquals("sources", sourceJar.archiveClassifier.get(), "sourceJar classifier should be 'sources'")

		val javadocJar = project.tasks.getByName("javadocJar") as Jar
		assertEquals("documentation", javadocJar.group, "javadocJar should be in 'documentation' group")
		assertEquals("javadoc", javadocJar.archiveClassifier.get(), "javadocJar classifier should be 'javadoc'")
	}
}
