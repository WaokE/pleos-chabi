import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(17); compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
dependencies {
    implementation(project(":core"))
    implementation(libs.zxing.core)
    implementation(libs.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
