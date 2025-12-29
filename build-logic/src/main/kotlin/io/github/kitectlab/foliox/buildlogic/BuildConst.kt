package io.github.kitectlab.foliox.buildlogic

object BuildConst {
    private val basicPackage = this::class.java.`package`.name.removeSuffix(".foliox.buildlogic")

    val group = basicPackage

    val snapshotVersion = "0.1.0-SNAPSHOT"

    val packageName = "${basicPackage}.foliox"

    val isRelease = System.getenv("KITECTLAB_PROJECT_BUILD_TYPE") == "release"

}