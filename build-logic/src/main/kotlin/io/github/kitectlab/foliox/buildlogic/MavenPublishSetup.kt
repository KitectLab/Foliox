package io.github.kitectlab.foliox.buildlogic

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.Project
import org.gradle.internal.Actions.with
import org.gradle.kotlin.dsl.findByType

internal fun Project.mavenPublishSetup() {
    val publishExtension = extensions.findByType<MavenPublishBaseExtension>() ?: return
    with(publishExtension) {
        publishToMavenCentral()
        signAllPublications()
        coordinates(
            artifactId = "foliox-${project.name.removePrefix("foliox-")}",
        )
        pomSetup(project)
    }
}

private fun MavenPublishBaseExtension.pomSetup(project: Project) {
    pom {
        name.set("KitectLab Foliox: ${project.name}")
        description.set("")
        inceptionYear.set("2025")
        url.set("https://github.com/KitectLab/Foliox/${project.relativePath(project.rootDir.path)}")

        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }

        developers {
            developer {
                id.set("Sanlorng")
                name.set("Sanlorng")
                url.set("https://github.com/sanlorng/")
            }
        }

        scm {
            url.set("https://github.com/KitectLab/Foliox/")
            connection.set("scm:git:git://github.com/KitectLab/Foliox.git")
            developerConnection.set("scm:git:ssh://git@github.com/KitectLab/Foliox.git")
        }
    }
}
