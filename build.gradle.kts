import java.net.*

plugins {
    application
    `maven-publish`
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("kotlinx-atomicfu")
    id("com.google.cloud.tools.jib")
    id("com.github.johnrengelman.shadow")
    id("com.github.hierynomus.license")
}

val scriptUrl: String by extra

apply(from = "$scriptUrl/git-version.gradle.kts")

repositories {
    mavenCentral()
    jcenter()
}

val ktorVersion: String by extra
val serializationVersion: String by extra
val atomicFuVersion: String by extra
val collectionImmutableVersion: String by extra

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:$collectionImmutableVersion")
    implementation("io.github.microutils:kotlin-logging:1.5.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")
}

val appMainClassName by extra("io.ktor.server.netty.EngineMain")

val defaultAppJvmArgs = listOf(
    "-server",
    "-Djava.awt.headless=true",
    "-XX:+UseG1GC",
    "-XX:+UseStringDeduplication"
)

java.targetCompatibility = JavaVersion.VERSION_1_8

application {
    mainClassName = appMainClassName
    applicationDefaultJvmArgs = defaultAppJvmArgs
}

jib {
    from {
        image = "gcr.io/distroless/java:8-debug"
    }
    to {
        image = "drill4j/autotest-extension-dispatcher"
        tags = setOf("${project.version}")
        auth {
            username = if (project.hasProperty("drillUsername"))
                project.property("drillUsername").toString()
            else System.getenv("DRILL_USERNAME")
            password =
                if (project.hasProperty("drillPassword"))
                    project.property("drillPassword").toString()
                else System.getenv("DRILL_PASSWORD")
        }
    }

    container {
        ports = listOf("5003")
        mainClass = appMainClassName

        jvmFlags = defaultAppJvmArgs
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    repositories {
        maven {
            url = uri("http://oss.jfrog.org/oss-release-local")
            credentials {
                username = if (project.hasProperty("bintrayUser"))
                    project.property("bintrayUser").toString()
                else System.getenv("BINTRAY_USER")
                password =
                    if (project.hasProperty("bintrayApiKey"))
                        project.property("bintrayApiKey").toString()
                    else System.getenv("BINTRAY_API_KEY")
            }

        }
    }

    publications {
        create<MavenPublication>("lib") {
            artifact(tasks.shadowJar.get())
            artifactId = "autotest-extension-dispatcher"
        }
    }
}

val licenseFormatSettings by tasks.registering(com.hierynomus.gradle.license.tasks.LicenseFormat::class) {
    source = fileTree(project.projectDir).also {
        include("**/*.kt", "**/*.java", "**/*.groovy")
        exclude("**/.idea")
    }.asFileTree
}
license{
    headerURI = URI("https://raw.githubusercontent.com/Drill4J/drill4j/develop/COPYRIGHT")
}

tasks["licenseFormat"].dependsOn(licenseFormatSettings)
