import java.util.Properties
import java.io.File
plugins { id("com.android.application"); id("org.jetbrains.kotlin.plugin.compose") }
android {
 namespace = "com.revolic.tscprint"
 compileSdk = 37
 defaultConfig { applicationId = "com.revolic.tscprint"; minSdk = 33; targetSdk = 37; versionCode = 8; versionName = "1.3.3"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildFeatures { compose = true; buildConfig = true }
 testOptions { animationsDisabled = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 val signingPath = System.getenv("TSC_SIGNING_PROPERTIES")
 if (signingPath != null) {
  val props = Properties().apply { File(signingPath).inputStream().use { load(it) } }
  signingConfigs { create("localRelease") { storeFile = file(props.getProperty("storeFile")); storePassword = props.getProperty("storePassword"); keyAlias = props.getProperty("keyAlias"); keyPassword = props.getProperty("keyPassword") } }
  buildTypes { getByName("release") { signingConfig = signingConfigs.getByName("localRelease") } }
 }
 lint { abortOnError = true }
}
dependencies {
 implementation(project(":core"))
 implementation("com.google.zxing:core:3.5.4")
 implementation("androidx.activity:activity-compose:1.13.0")
 implementation(platform("androidx.compose:compose-bom:2026.05.01"))
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.ui:ui-tooling-preview")
 debugImplementation("androidx.compose.ui:ui-tooling")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
 androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
