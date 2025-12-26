
plugins {
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
}

android {
    namespace = "io.github.kitectlab.foliox"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.kitectlab.foliox.sample"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(projects.sampleApp.sharedApp)
    debugImplementation(libs.compose.ui.tooling)
}
