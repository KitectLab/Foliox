plugins {
    `kotlin-dsl`
}

group = "io.github.kitectlab.foliox.buildlogic"

repositories {
    google {
        mavenContent {
            includeGroupAndSubgroups("androidx")
            includeGroupAndSubgroups("com.android")
            includeGroupAndSubgroups("com.google")
        }
    }
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(gradleApi())
    gradlePluginImplementation(libs.plugins.kotlinMultiplatform.get())
    gradlePluginImplementation(libs.plugins.androidKotlinMultiplatformLibrary.get())
    gradlePluginImplementation(libs.plugins.vanniktechMavenPublish.get())
}

gradlePlugin {
    plugins {
        register("Convention") {
            id = "io.github.kitectlab.foliox.build-logic"
            implementationClass = "io.github.kitectlab.foliox.buildlogic.ConventionPlugin"
        }
    }
}

fun DependencyHandler.gradlePluginImplementation(plugin: PluginDependency) {
    implementation(
        group = plugin.pluginId,
        name = "${plugin.pluginId}.gradle.plugin",
        version = plugin.version.toString()
    )
}
