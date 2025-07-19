import io.github.bitfist.github.repository.gitHub
import java.net.URI

plugins {
    java
//	`java-gradle-plugin`
	id("io.github.bitfist.gradle-github-support.release")
	id("io.github.bitfist.gradle-github-support.repository")
}

group = "test"
version = "test-version"

repositories {
	gitHub("user/repository")
}

gitHubRelease {
	repository.set("user/repository")
	user.set("user")
	token.set("token")
	projectName.set("test-project")
	projectDescription.set("Test project for the GitHub Release Plugin")
	developer.set("test")
	licenseFile.set(rootProject.rootDir.resolve("LICENSE.txt"))
	license.set("The Apache License, Version 2.0")
	licenseUri.set(URI("https://www.apache.org/licenses/LICENSE-2.0"))
}
