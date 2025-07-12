package io.github.bitfist.github.release

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import java.net.URI

class GitHubReleasePluginTest {

	@Test
	@DisplayName("🛠️ apply plugin adds gitHubRelease extension with default properties")
	fun `extension creation`() {
		val project = ProjectBuilder.builder().build()
		project.pluginManager.apply(GitHubReleasePlugin::class.java)

		val extension = project.extensions.findByName("gitHubRelease") as? GitHubReleaseExtension
		assertNotNull(extension, "Extension 'gitHubRelease' should be created")
		assertNotNull(extension.repository, "repository property should exist")
		assertEquals(System.getenv("GITHUB_ACTOR"), extension.user.orNull, "user should default to GITHUB_ACTOR")
		assertEquals(System.getenv("GITHUB_TOKEN"), extension.token.orNull, "token should default to GITHUB_TOKEN")
	}

	@Test
	@DisplayName("🛠️ Java, Sources and Javadoc jars configured")
	fun `java plugin configured`() {
		val project: Project = ProjectBuilder.builder().build()
		project.pluginManager.apply(GitHubReleasePlugin::class.java)

		// Verify Java plugin is applied
		assertTrue(project.plugins.hasPlugin("java"), "Java plugin should be applied")

		// Verify sources and javadoc jars tasks exist
		val sourcesJar = project.tasks.findByName("sourcesJar")
		val javadocJar = project.tasks.findByName("javadocJar")
		assertNotNull(sourcesJar, "sourcesJar task should be created")
		assertNotNull(javadocJar, "javadocJar task should be created")
	}

	@Test
	@DisplayName("🚀 Semantic versioning, JaCoCo and publishing plugins applied")
	fun `semantic and publishing plugins configured`() {
		val project: Project = ProjectBuilder.builder().build()
		project.pluginManager.apply(GitHubReleasePlugin::class.java)

		// Verify semantic versioning plugin
		assertTrue(project.plugins.hasPlugin("com.github.jmongard.git-semver-plugin"),
			"Git Semver plugin should be applied")

		// Verify JaCoCo plugin
		assertTrue(project.plugins.hasPlugin("jacoco"), "JaCoCo plugin should be applied")

		// Verify Maven Publish plugin
		assertTrue(project.plugins.hasPlugin("maven-publish"), "Maven Publish plugin should be applied")
	}

	@Test
	@DisplayName("⚙️ Extension default values and conventions")
	fun `extension default values`() {
		val project: Project = ProjectBuilder.builder().build()
		project.pluginManager.apply(GitHubReleasePlugin::class.java)

		val extension = project.extensions.getByType(GitHubReleaseExtension::class.java)

		// Defaults from environment variables
		assertEquals(System.getenv("GITHUB_REPOSITORY"), extension.repository.get(),
			"repository should default to GITHUB_REPOSITORY env var")
		assertEquals(System.getenv("GITHUB_ACTOR"), extension.user.get(),
			"user should default to GITHUB_ACTOR env var")
		assertEquals(System.getenv("GITHUB_TOKEN"), extension.token.get(),
			"token should default to GITHUB_TOKEN env var")

		// No initial values set for required fields
		assertFalse(extension.projectName.isPresent, "projectName should not be set by default")
		assertFalse(extension.projectDescription.isPresent, "projectDescription should not be set by default")
		assertFalse(extension.developer.isPresent, "developer should not be set by default")
		assertFalse(extension.licenseFile.isPresent, "licenseFile should not be set by default")
	}

	@Test
	@DisplayName("🔗 Publication SCM and repository configured")
	fun `publication configuration`() {
		val project: ProjectInternal = ProjectBuilder.builder().build() as ProjectInternal
		// Create and configure extension before applying plugin
		project.pluginManager.apply(GitHubReleasePlugin::class.java)
		val extension = project.extensions.getByType(GitHubReleaseExtension::class.java)
		extension.repository.set("owner/repo")
		extension.user.set("actor")
		extension.token.set("token")
		extension.projectName.set("Test Project")
		extension.projectDescription.set("Description")
		extension.developer.set("devName")

		// Trigger afterEvaluate actions
		project.evaluate()

		val publishing = project.extensions.getByType(PublishingExtension::class.java)
		val repo = publishing.repositories
			.filterIsInstance<MavenArtifactRepository>()
			.first { it.name == "GitHubPackages" }

		assertEquals(URI("https://maven.pkg.github.com/owner/repo"), repo.url,
			"Repository URL should include configured repository path")
		assertEquals("actor", repo.credentials.username,
			"Repository credentials username should match extension user")

		val publicationName = if (project.plugins.hasPlugin("java-gradle-plugin")) "pluginMaven" else "mavenJava"
		val publication = publishing.publications.getByName(publicationName) as MavenPublication

		publication.pom.apply {
			assertEquals("Test Project", name.get(), "POM name should match extension.projectName")
			assertEquals("Description", description.get(), "POM description should match extension.projectDescription")

			scm {
				assertEquals("scm:git:git://github.com/owner/repo.git", it.connection.get(),
					"SCM connection URL should incorporate repository")
				assertEquals("scm:git:ssh://github.com:owner/repo.git", it.developerConnection.get(),
					"SCM developer connection URL should incorporate repository")
				assertEquals("https://github.com/owner/repo", it.url.get(),
					"SCM URL should point to GitHub repository")
			}
		}
	}
}
