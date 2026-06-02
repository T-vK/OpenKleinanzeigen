import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val versionProps = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    } else {
        setProperty("VERSION_CODE", "1")
        setProperty("VERSION_NAME", "0.1.0")
    }
}

android {
    namespace = "de.openkleinanzeigen"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.openkleinanzeigen"
        minSdk = 26
        targetSdk = 35
        versionCode = versionProps.getProperty("VERSION_CODE", "1").toInt()
        versionName = versionProps.getProperty("VERSION_NAME", "0.1.0")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "FDROID_REPO_URL", "\"https://t-vk.github.io/OpenKleinanzeigen/fdroid/repo\"")
        buildConfigField("String", "GITHUB_REPO", "\"T-vK/OpenKleinanzeigen\"")
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file(
                providers.gradleProperty("RELEASE_STORE_FILE").orNull ?: "release.keystore",
            )
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = providers.gradleProperty("RELEASE_STORE_PASSWORD").orNull
                    ?: System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = providers.gradleProperty("RELEASE_KEY_ALIAS").orNull
                    ?: System.getenv("RELEASE_KEY_ALIAS")
                    ?: "release"
                keyPassword = providers.gradleProperty("RELEASE_KEY_PASSWORD").orNull
                    ?: System.getenv("RELEASE_KEY_PASSWORD")
                    ?: storePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release").takeIf {
                signingConfigs.getByName("release").storeFile?.exists() == true
            } ?: signingConfigs.getByName("debug")
        }
        debug {
            // Same applicationId as release; only the signing certificate differs.
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:api"))
    implementation(project(":core:data"))

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
}
