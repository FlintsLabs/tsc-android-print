plugins {
 id("com.android.application") version "9.2.1" apply false
 id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}

// Keep generated classes outside Documents/iCloud to avoid synchronized duplicate class files.
allprojects {
    val buildRoot = System.getenv("TSC_BUILD_ROOT") ?: "${System.getProperty("user.home")}/.cache/tsc-android-print/build"
    layout.buildDirectory.set(file("$buildRoot/${project.name}"))
}
