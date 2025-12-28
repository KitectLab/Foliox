import io.github.kitectlab.foliox.buildlogic.applyKotlinOptions
import io.github.kitectlab.foliox.buildlogic.applyTargets
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.jetbrainsCompose)
}

kotlin {

    applyTargets("sampleApp")
    applyKotlinOptions()

    sourceSets {

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.ui.tooling.preview)
            implementation("androidx.paging:paging-common:3.3.6")
            implementation(projects.folioxCore)
        }

    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.ui.tooling)
}
