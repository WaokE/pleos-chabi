import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { alias(libs.plugins.kotlin.jvm) }
kotlin { jvmToolchain(17); compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
dependencies { testImplementation(libs.junit) }
