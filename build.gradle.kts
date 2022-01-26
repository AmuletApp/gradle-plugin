plugins {
    kotlin("jvm") version "1.6.10"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.github.redditvanced"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.10")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
    compileOnly(gradleApi())

	// Build utils
	compileOnly("com.android.tools:sdk-common:30.0.0")
    compileOnly("com.android.tools.build:gradle:7.0.0")

	// HTTP Lib
	implementation("com.github.kittinunf.fuel:fuel:2.3.1")
	implementation("com.github.kittinunf.fuel:fuel-gson:2.3.1")

	// APK utils
    implementation("com.github.RedditVanced.dex2jar:dex-translator:main-SNAPSHOT")
    implementation("com.github.Aliucord.jadx:jadx-core:master-SNAPSHOT")
    implementation("com.github.Aliucord.jadx:jadx-dex-input:master-SNAPSHOT")
    implementation("com.github.js6pak:jadb:fix-modified-time-SNAPSHOT")

	// GPlay API
	implementation("com.github.theapache64:google-play-api:0.0.9")
	implementation("com.google.protobuf:protobuf-java:3.19.3")
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
