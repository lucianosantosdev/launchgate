plugins {
    alias(libs.plugins.kotlinMultiplatform)
    // AGP 9 refuses to apply `com.android.library` alongside the Kotlin Multiplatform
    // plugin; the dedicated KMP library plugin is the replacement. Consumers on AGP 8
    // can swap this back for `libs.plugins.androidLibrary` and restore the `android { }`
    // block below.
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    `maven-publish`
}

group = "dev.lssoftware"
version = "0.1.0"

kotlin {
    android {
        namespace = "dev.lssoftware.launchgate"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        // Deliberately below the catalog's floor so the library never raises a
        // consumer's own minSdk.
        minSdk = 24
    }

    // Matches :composeApp — Compose Multiplatform 1.11+ no longer publishes iosX64.
    iosArm64()
    iosSimulatorArm64()

    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            // Preferences DataStore is the only storage the gate needs, and consumers hand it
            // their own instance — see VersionGate.create.
            api(libs.androidx.datastore.preferences)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))

            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        // The Compose UI tests render through Skiko, whose native runtime ships with the
        // desktop artifact. Test-only: consumers never see it.
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

publishing {
    repositories {
        // `./gradlew :launchgate:publishToMavenLocal` needs no configuration; the remote is only
        // resolved when publishing to it, so a missing token never breaks a local build.
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/lucianosantosdev/launchgate")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: providers.gradleProperty("gpr.user").orNull
                password = System.getenv("GITHUB_TOKEN") ?: providers.gradleProperty("gpr.token").orNull
            }
        }
    }
}
