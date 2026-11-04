import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    useLibrary("android.car")
    namespace = "com.example.chabi"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.chabi"
        minSdk = 28
        targetSdk = 34
        versionCode = 6
        versionName = "0.6.0"
    }

    buildTypes { debug { versionNameSuffix = "-debug" } }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(project(":core"))
    implementation(project(":route-map-api"))
    implementation(project(":route-map-maplibre"))
    implementation(project(":data-local"))
    implementation(project(":handoff-zxing"))
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.coroutines.android)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
