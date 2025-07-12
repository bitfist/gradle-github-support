plugins {
    java
	id("io.github.bitfist.github.release")
	id("io.github.bitfist.github.repository")
}

gitHubRepositories {
    defaultUser.set("user")
    defaultToken.set("token")
    mavenRepositories.mavenRepository {
        repository.set("user/repository")
    }
}

gitHubRelease {
	repository.set("user/repository")
	user.set("user")
	token.set("token")
}
