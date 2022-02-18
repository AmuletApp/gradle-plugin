plugins {
	kotlin("jvm") version "1.6.10"
	id("java-gradle-plugin")
	id("maven-publish")
}

group = "com.github.redditvanced"
version = "1.0.3"

repositories {
	google()
	mavenCentral()
	maven("https://jitpack.io")
}

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
	compileOnly(gradleApi())

	// Build utils
	compileOnly("com.android.tools:sdk-common:30.0.0")
	compileOnly("com.android.tools.build:gradle:7.0.0")
	compileOnly("com.google.guava:guava:31.0.1-jre")

	// HTTP Lib
	implementation("com.github.kittinunf.fuel:fuel:2.3.1")
	implementation("com.github.kittinunf.fuel:fuel-gson:2.3.1")

	// APK utils
	implementation("com.github.RedditVanced.dex2jar:dex-translator:main-SNAPSHOT")
	implementation("com.github.RedditVanced.jadx:jadx-core:master-SNAPSHOT")
	implementation("com.github.RedditVanced.jadx:jadx-dex-input:master-SNAPSHOT")
	implementation("com.github.js6pak:jadb:fix-modified-time-SNAPSHOT")

	// GPlay API
	implementation("com.github.theapache64:google-play-api:0.0.9")
	implementation("com.google.protobuf:protobuf-java:3.19.4")
}

gradlePlugin {
	plugins {
		create("com.github.redditvanced.gradle") {
			id = "redditvanced"
			implementationClass = "com.github.redditvanced.gradle.Plugin"
		}
	}
}

java {
	sourceCompatibility = JavaVersion.VERSION_11
	targetCompatibility = JavaVersion.VERSION_11
}

publishing {
	repositories {
		val username = System.getenv("MAVEN_USERNAME")
		val password = System.getenv("MAVEN_PASSWORD")

		if (username == null || password == null) {
			project.logger.lifecycle("Maven username/password missing, publishing to mavenLocal...")
			mavenLocal()
		} else maven {
			credentials {
				this.username = username
				this.password = password
			}
			setUrl("https://redditvanced.ddns.net/releases")
		}
	}
}
