rootProject.name = "autotest-extension-dispatcher"

pluginManagement {
    val kotlinVersion: String by extra
    val atomicFuVersion: String by extra
    val jibVersion: String by extra
    val licenseVersion: String by extra

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("kotlinx-atomicfu") version atomicFuVersion
        id("com.google.cloud.tools.jib") version jibVersion
        id("com.github.johnrengelman.shadow") version "5.1.0"
        id("com.github.hierynomus.license") version licenseVersion

        repositories {
            gradlePluginPortal()
            maven(url = "https://oss.jfrog.org/artifactory/list/oss-release-local")
        }
    }
}

mapOf(
    "kotlinx-atomicfu" to "org.jetbrains.kotlinx:atomicfu-gradle-plugin"
).let { substitutions ->
    pluginManagement.resolutionStrategy.eachPlugin {
        substitutions["${requested.id}"]?.let { useModule("$it:${target.version}") }
    }
}
