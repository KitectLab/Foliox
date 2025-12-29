package io.github.kitectlab.foliox.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Action
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import kotlin.jvm.optionals.getOrNull

@OptIn(ExperimentalKotlinGradlePluginApi::class)
fun KotlinMultiplatformExtension.applyTargets(namespaceModule: String) {
    val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
    val compileSdk = libs.findVersion("android-compileSdk")
        .getOrNull()?.requiredVersion?.toInt() ?: throw IllegalStateException("android-compileSdk is missing from libs.versions.toml")
    val minSdk = libs.findVersion("android-minSdk")
        .getOrNull()?.requiredVersion?.toInt()

    android {
        this.compileSdk = compileSdk
        this.minSdk = minSdk
        namespace = "${BuildConfig.packageName}.$namespaceModule"
        androidResources.enable = true
        withHostTestBuilder {
        }

        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = namespaceModule
        }
    }

    jvm()

    wasmJs { browser() }
    js { browser() }

    macosArm64()

    applyDefaultHierarchyTemplate {
        sourceSetTrees(KotlinSourceSetTree.main, KotlinSourceSetTree.test)
        common {
            group("android") {
                withCompilations { it.platformType == KotlinPlatformType.androidJvm }
            }

            group("jvm") {
                withJvm()
            }

            group("nonAndroid") {
                group("jvm")
                group("native")
                group("web")
            }

            group("jvmAndAndroid") {
                group("jvm")
                group("android")
            }

            group("desktop") {
                group("linux")
                group("macos")
                group("mingw")
                group("jvm")
            }
        }
    }
}

fun KotlinMultiplatformExtension.applyKotlinOptions() {
    compilerOptions {
        compilerOptions {
            freeCompilerArgs.apply {
                add("-Xcontext-sensitive-resolution")
                add("-Xcontext-parameters")
                add("-Xexpect-actual-classes")
            }
        }
    }
}

internal fun KotlinMultiplatformExtension.android(
    action: Action<KotlinMultiplatformAndroidLibraryTarget>
) {
    val androidPlugin = "com.android.kotlin.multiplatform.library"
    if (!project.plugins.hasPlugin(androidPlugin)) {
        throw IllegalStateException("plugin $androidPlugin is not applied")
    }
    extensions.configure("android", action)
}
