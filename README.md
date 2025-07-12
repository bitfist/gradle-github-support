# GitHub Gradle Plugins 🛠️

This repository provides two Gradle plugins to streamline publishing and dependency management with GitHub Package Registry.

## ⭐️ Plugins

- **GitHub Release Plugin** (`io.github.bitfist.github.release`): Automates publishing your artifacts and generating changelogs based on semantic versioning.
- **GitHub Repositories Plugin** (`io.github.bitfist.github.repository`): Simplifies configuring GitHub Maven repositories and managing credentials.

## 🛠️ Installation

Add the plugins to your `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.bitfist.github.release")
    id("io.github.bitfist.github.repository")
}
```

And using the Gradle plugin DSL in `settings.gradle.kts`:

```kotlin
pluginManagement {
	repositories {
		gradlePluginPortal()
		maven {
			name = "github-support"
			url = uri("https://maven.pkg.github.com/bitfist/github-support")
			credentials {
				username = "<username>"
				password = "<token>"
			}
		}
	}
	plugins {
		id("io.github.bitfist.github.release") version "<version>"
		id("io.github.bitfist.github.repository") version "<version>"
	}
}
```

## ⚙️ Configuration

### GitHub Release Plugin

Configure the `gitHubRelease` extension in your `build.gradle.kts`:

```kotlin
gitHubRelease {
    repository.set("OWNER/REPO")    // GitHub repository identifier
    user.set("USERNAME")           // GitHub username (optional, see env vars)
    token.set("PERSONAL_TOKEN")    // GitHub token (optional, see env vars)
}
```

### GitHub Repositories Plugin

Configure the `gitHubRepositories` extension:

```kotlin
gitHubRepositories {
	defaultUser.set("USERNAME")    // Default GitHub user for all repos (optional)
	defaultToken.set("ACCESS_KEY") // Default token for all repos (optional)

	mavenRepositories.repository {
		repository.set("OWNER/REPO") // GitHub Maven repository
		user.set("USERNAME")         // Repo-specific user (optional)
		token.set("ACCESS_KEY")      // Repo-specific token (optional)
		// Add more repositories as needed...
	}
}
```

## 🔑 Environment Variables & Gradle Properties

The plugins fallback to environment variables or project properties if values are not set:

| Property                            | Env Variable         | Gradle Property (`-P`) |
|-------------------------------------|----------------------|-------------------------|
| `gitHubRelease.repository`          | `GITHUB_REPOSITORY`  | -                       |
| `gitHubRelease.user`                | `GITHUB_ACTOR`       | -                       |
| `gitHubRelease.token`               | `GITHUB_TOKEN`       | -                       |
| `gitHubRepositories.defaultUser`    | `GITHUB_ACTOR`       | `GPR_USER`              |
| `gitHubRepositories.defaultToken`   | `GPR_KEY`            | `GPR_KEY`               |
| `mavenRepositories[].user`          | `GITHUB_ACTOR`       | `GPR_USER`              |
| `mavenRepositories[].token`         | `GPR_KEY`            | `GPR_KEY`               |

## 🚀 Usage

Publish artifacts to GitHub Package Registry and create a release:

```bash
./gradlew publish # Publishes artifacts
```

```bash
./gradlew release # Creates GitHub release and changelog
```

Run tests and generate coverage report:

```bash
./gradlew test jacocoTestReport
```

## 📖 Documentation

- **GitHubReleaseExtension**: `io.github.bitfist.github.release.GitHubReleaseExtension`
- **GitHubRepositoriesExtension**: `io.github.bitfist.github.repository.GitHubRepositoriesExtension`
