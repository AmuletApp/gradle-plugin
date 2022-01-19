plugins {
    kotlin("jvm") version "1.6.10"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.github.redditvanced"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
    compileOnly(gradleApi())

	compileOnly("com.android.tools:sdk-common:30.0.0")
    compileOnly("com.android.tools.build:gradle:7.0.0")

    implementation("com.github.Aliucord.dex2jar:dex-translator:d5a5efb06c")
    implementation("com.github.Aliucord.jadx:jadx-core:1a213e978d")
    implementation("com.github.Aliucord.jadx:jadx-dex-input:1a213e978d")
    implementation("com.github.js6pak:jadb:fix-modified-time-SNAPSHOT")
}

gradlePlugin {
    plugins {
        create("com.github.redditvanced.gradle") {
            id = "redditvanced-gradle"
            implementationClass = "com.github.redditvanced.gradle.RedditVancedPlugin"
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
