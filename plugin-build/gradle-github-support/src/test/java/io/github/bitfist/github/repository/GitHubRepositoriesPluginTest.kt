package io.github.bitfist.github.repository

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.repositories.PasswordCredentials
import org.gradle.api.credentials.Credentials
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import java.net.URI

class GitHubRepositoriesPluginTest {

	@Test
	@DisplayName("🔧 Configures valid GitHub repo")
	fun `gitHub extension configures repository correctly`() {
		val project = mock<Project>()
		whenever(project.findProperty("GPR_USER")).thenReturn("propUser")
		whenever(project.findProperty("GPR_KEY")).thenReturn("propKey")
		myProject = project

		val handler = mock<RepositoryHandler>()
		val repoActionCaptor = argumentCaptor<Action<MavenArtifactRepository>>()
		val repo = mock<MavenArtifactRepository>()
		doReturn(repo).`when`(handler).maven(repoActionCaptor.capture())

		handler.gitHub("octocat/hello-world")

		verify(handler, times(1)).maven(any<Action<MavenArtifactRepository>>())

		val repoAction = repoActionCaptor.firstValue
		val credActionCaptor = argumentCaptor<Action<PasswordCredentials>>()
		doNothing().`when`(repo).credentials(credActionCaptor.capture())

		repoAction.execute(repo)

		verify(repo).setName("GitHubPackages-octocat-hello-world")
		verify(repo).setUrl(URI("https://maven.pkg.github.com/octocat/hello-world"))
		verify(repo, times(1)).credentials(any<Action<Credentials>>())

		val creds = object : PasswordCredentials {
			private var _user: String? = null
			private var _pass: String? = null
			override fun getUsername(): String? = _user
			override fun setUsername(username: String?) { _user = username }
			override fun getPassword(): String? = _pass
			override fun setPassword(password: String?) { _pass = password }
		}
		credActionCaptor.firstValue.execute(creds)
		assertNotNull(creds.username, "username should be set")
		assertNotNull(creds.password, "password should be set")
	}

	@Test
	@DisplayName("❌ Throws on bad notation format")
	fun `gitHub extension with invalid notation throws`() {
		val handler = mock<RepositoryHandler>()
		assertThrows<IndexOutOfBoundsException> {
			handler.gitHub("invalidNotation")
		}
	}

	@Test
	@DisplayName("⚙️ Uses project properties over env vars")
	fun `gitHub extension uses project props first`() {
		val handler = mock<RepositoryHandler>()
		val project = mock<org.gradle.api.Project>()
		myProject = project
		whenever(project.findProperty("GPR_USER")).thenReturn("propUser")
		whenever(project.findProperty("GPR_KEY")).thenReturn("propKey")

		val repoActionCaptor = argumentCaptor<Action<MavenArtifactRepository>>()
		val repo = mock<MavenArtifactRepository>()
		doReturn(repo).`when`(handler).maven(repoActionCaptor.capture())

		handler.gitHub("owner/repo")
		val credActionCaptor = argumentCaptor<Action<PasswordCredentials>>()
		doNothing().`when`(repo).credentials(credActionCaptor.capture())

		repoActionCaptor.firstValue.execute(repo)
		val credentials = mock<PasswordCredentials>()
		credActionCaptor.firstValue.execute(credentials)

		verify(credentials).setUsername("propUser")
		verify(credentials).setPassword("propKey")
	}

	@Test
	@DisplayName("🔄 Env fallback when properties missing")
	fun `gitHub extension falls back to env vars`() {
		val handler = mock<RepositoryHandler>()
		val project = mock<org.gradle.api.Project>()
		myProject = project
		whenever(project.findProperty("GPR_USER")).thenReturn(null)
		whenever(project.findProperty("GPR_KEY")).thenReturn(null)

		val repoActionCaptor = argumentCaptor<Action<MavenArtifactRepository>>()
		val repo = mock<MavenArtifactRepository>()
		doReturn(repo).`when`(handler).maven(repoActionCaptor.capture())

		handler.gitHub("ownerX/repoY")
		val credActionCaptor = argumentCaptor<Action<PasswordCredentials>>()
		doNothing().`when`(repo).credentials(credActionCaptor.capture())

		repoActionCaptor.firstValue.execute(repo)
		val credentials = mock<PasswordCredentials>()
		credActionCaptor.firstValue.execute(credentials)

		verify(credentials).setUsername("test-user")
		verify(credentials).setPassword("<PASSWORD>")
	}

}
