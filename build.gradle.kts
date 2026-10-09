@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import java.util.*

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kover)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.owasp.dependencycheck)
    id("signing")
}

val defaultGroupId = "cz.sazel.arch"
val nameStr = "arch-patterns"
val versionBase = "0.1.0"

val localProperties = Properties().apply {
    try {
        load(project.rootProject.file("local.properties").inputStream())
    } catch (e: java.io.IOException) {
        System.err.println("Can't read local.properties, skipping")
    }
}

group = System.getenv("GROUP_ID") ?: localProperties["groupId"] as String? ?: defaultGroupId
var versionStr = System.getenv("PACKAGE_VERSION")?.trim()?.ifBlank { null }
    ?: localProperties["packageVersion"] as String? ?: "$versionBase-SNAPSHOT"
if (versionStr.startsWith("v")) versionStr = versionStr.substring(1)
version = versionStr

println("Package version $version")

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/wojta/arch-patterns")
        credentials {
            username = System.getenv("GITHUB_USER") ?: localProperties["github.user"] as String?
            password = System.getenv("GITHUB_TOKEN") ?: localProperties["github.token"] as String?
        }
    }
}

kotlin {
    jvmToolchain(17)
    jvm()
    js {
        browser()
        nodejs()
    }
    
    linuxX64()
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(libs.kotlin.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlin.coroutines.test)
            }
        }
    }
}

val javadocJar: TaskProvider<Jar> = tasks.register<Jar>("javadocJar") {
    dependsOn(tasks.dokkaGenerate.get())
    archiveClassifier.set("javadoc")
    from(layout.buildDirectory.dir("dokka/html"))
}

mavenPublishing {
    repositories {
        val githubUserName = System.getenv("GITHUB_USER") ?: localProperties["github.user"] as String?
        if (githubUserName != null) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/wojta/arch-patterns")

                credentials {
                    username = githubUserName
                    password = System.getenv("GITHUB_TOKEN") ?: localProperties["github.token"] as String?
                }
            }
        }
        mavenLocal()
    }
    coordinates(group.toString(), nameStr, versionStr)

    signing {
        useInMemoryPgpKeys(
            System.getenv("GPG_KEY_SECRET") ?: localProperties["gpg.keySecret"] as String?,
            System.getenv("GPG_KEY_PASSWORD") ?: localProperties["gpg.keyPassword"] as String?
        )
        sign(publishing.publications)
    }

    publishToMavenCentral()

    pom {
        name.set("arch-patterns")
        description.set("Architecture patterns for Kotlin Multiplatform")
        licenses {
            license {
                name.set("Apache-2.0")
                url.set("https://opensource.org/licenses/Apache-2.0")
            }
        }
        url.set("https://github.com/wojta/arch-patterns")
        issueManagement {
            system.set("Github")
            url.set("https://github.com/wojta/arch-patterns/issues")
        }
        scm {
            connection.set("https://github.com/wojta/arch-patterns.git")
            url.set("https://github.com/wojta/arch-patterns")
        }
        developers {
            developer {
                name.set("Vojtěch Sázel")
                email.set("arch-patterns@sazel.cz")
            }
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/gradle/detekt/detekt.yml")
    source.setFrom(kotlin.sourceSets.flatMap { it.kotlin.srcDirs }.filter { it.startsWith(file("src")) })
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "17"
    reports {
        html.required.set(true)
    }
}

dependencyCheck {
    failBuildOnCVSS = 7.0f
    formats = listOf("HTML", "JSON")
    suppressionFile = "$projectDir/gradle/dependency-check/suppression.xml"
    scanConfigurations = listOf("jvmCompileClasspath", "jvmRuntimeClasspath")
    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: localProperties["nvd.apiKey"] as String?
    }
    analyzers {
        ossIndex {
            enabled = true
            password = System.getenv("OSS_INDEX_TOKEN") ?: localProperties["ossIndex.token"] as String?
            url = "https://api.guide.sonatype.com"
        }
    }
}
