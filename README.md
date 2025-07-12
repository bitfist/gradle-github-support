![Gradle Plugin](https://img.shields.io/static/v1?label=Gradle&message=Plugin&color=6DB33F&logo=gradle)
![GitHub Release Plugin](https://img.shields.io/static/v1?label=GitHub&message=Release&color=24292e&logo=github)
[![Gradle build](https://github.com/bitfist/gradle-github-support/actions/workflows/test.yml/badge.svg)](https://github.com/bitfist/os-conditions-spring-boot-starter/actions/workflows/test.yml)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

## ✨ Features

- **Automatic Release Management**: Configures GitHub Release tasks, ensuring your project is versioned and released according to semantic versioning rules based on Git tags and conventional commit messages.
- **Semantic Versioning**: Integrates with the Git-Semver plugin to compute versions automatically.
- **Changelog Generation**: Produces a formatted changelog, grouping commits by type (features, fixes, breaking changes) and linking to the corresponding GitHub commits.
- **Code Quality Gates**: Enforces test and code coverage thresholds by making Release tasks depend on test and Jacoco report tasks.
- **Maven Publication**: Publishes artifacts and metadata (including sources and Javadoc) to GitHub Packages or any Maven repository, with credentials and repository URL configurable via extension properties.
- **License Management**: Offers a `copyLicense` task to include your project's license file in the built resources automatically.
- **Repository Convenience**: Provides a `gitHub("owner/repo")` shortcut for adding GitHub Package repositories, handling authentication via Gradle properties or environment variables.

## 🚀 Usage Examples

## Setup

Use the Gradle plugin DSL in `settings.gradle.kts`:

```kotlin
pluginManagement {
	repositories {
		gradlePluginPortal()
		maven {
			name = "gradle-github-support"
			url = uri("https://maven.pkg.github.com/bitfist/gradle-github-support")
			credentials {
				try {
					username = settings.extra["GPR_USER"] as String?
				} catch (exception: ExtraPropertiesExtension.UnknownPropertyException) {
					username = System.getenv("GITHUB_ACTOR") ?: throw IllegalArgumentException("GITHUB_ACTOR not set")
				}
				try {
					password = settings.extra["GPR_KEY"] as String?
				} catch (exception: ExtraPropertiesExtension.UnknownPropertyException) {
					password = System.getenv("GITHUB_TOKEN") ?: throw IllegalArgumentException("GITHUB_TOKEN not set")
				}
			}
		}
	}
	plugins {
		id("io.github.bitfist.github.release") version "<version>"
		id("io.github.bitfist.github.repository") version "<version>"
	}
}
```

❌ Remember to set your GitHub user `GPR_USER` and GitHub Access Token `GPR_KEY` in `~/.gradle/gradle.properties`

### GitHub Release Plugin

```kotlin
plugins {
	id("io.github.bitfist.github.release")
}

gitHubRelease {
	repository.set("your-org/your-repo")
	user.set("your-github-username")   // defaults to GITHUB_ACTOR
	token.set("your-token")            // defaults to GITHUB_TOKEN
	projectName.set("AwesomeProject")
	projectDescription.set("An awesome project release")
	developer.set("your-github-username")
	licenseFile.set(file("path/to/file"))
}
```

### GitHub Repositories Plugin

```kotlin
import io.github.bitfist.github.repository.gitHub

plugins {
	id("io.github.bitfist.github.repository")
}

repositories {
	gitHub("user/repository") // uses GITHUB_ACTOR, GPR_USER, GITHUB_TOKEN, GPR_KEY for authentication
}
```

❌ Remember to set your GitHub user `GPR_USER` and GitHub Access Token `GPR_KEY` in `~/.gradle/gradle.properties`

## 📋 Plugin Properties

| Property                       | Plugin                              | Type      | Default                              | Description                                         |
|--------------------------------|-------------------------------------|-----------|--------------------------------------|-----------------------------------------------------|
| `repository`                   | Release (`gitHubRelease`)           | String    | `GITHUB_REPOSITORY` env var          | GitHub repo in `owner/repo` format                  |
| `user`                         | Release                             | String    | `GITHUB_ACTOR` env var               | GitHub username for authentication                  |
| `token`                        | Release                             | String    | `GITHUB_TOKEN` env var               | GitHub token for API access and publishing          |
| `projectName`                  | Release                             | String    | —                                    | Name to set in POM                                  |
| `projectDescription`           | Release                             | String    | —                                    | Description to set in POM                           |
| `developer`                    | Release                             | String    | —                                    | Developer ID/name for POM                           |

## 🌐 Environment Variables

| Variable            | Used By                | Description                                      |
|---------------------|------------------------|--------------------------------------------------|
| `GITHUB_REPOSITORY` | Release Extension      | Default GitHub repo (`owner/repo`)               |
| `GITHUB_ACTOR`      | Release & Repositories | GitHub username (actor)                          |
| `GITHUB_TOKEN`      | Release & Repositories | Token for GitHub API and Packages                |
| `GPR_USER`          | Repositories Plugin    | Fallback Gradle property for repository username |
| `GPR_KEY`           | Repositories Plugin    | Fallback Gradle property for repository token    |

## 🔧 Gradle Properties

| Property   | Used By             | Description                                |
|------------|---------------------|--------------------------------------------|
| `GPR_USER` | Repositories Plugin | Alternative to `GITHUB_ACTOR` for username |
| `GPR_KEY`  | Repositories Plugin | Alternative to `GITHUB_TOKEN` for token    |

