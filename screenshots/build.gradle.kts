plugins {
    id("com.android.test")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.dayscounter.screenshots"
    compileSdk = 36

    targetProjectPath = ":app"

    defaultConfig {
        minSdk = 26
        targetSdk = 36

        testInstrumentationRunner = "com.dayscounter.screenshots.ScreenshotTestRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":app"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.test.rules)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.screengrab)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
}
