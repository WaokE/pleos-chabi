plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

// Checked by every app build. Keep SDK dependencies inside their adapter module.
val verifyArchitecture by tasks.registering {
    group = "verification"
    description = "Checks module dependencies and forbidden imports across architecture boundaries."
    val sources = fileTree(rootDir) {
        include("core/src/**/*.kt", "route-map-api/src/**/*.kt", "route-map-maplibre/src/**/*.kt", "app/src/main/**/*.kt", "data-local/src/**/*.kt", "handoff-zxing/src/**/*.kt")
    }
    inputs.files(sources)
    doLast {
        val failures = mutableListOf<String>()
        val vendorNamespaces = listOf("org.maplibre.", "okhttp3.", "ai.pleos.playground.", "com.google.zxing.")
        sources.forEach { source ->
            val path = source.relativeTo(rootDir).invariantSeparatorsPath
            source.readLines().forEachIndexed { index, raw ->
                val line = raw.trim()
                val forbidden = when {
                    path.startsWith("core/") -> listOf("android.", "androidx.", "com.example.chabi.data.", "com.example.chabi.ui.", "com.example.chabi.map.") + vendorNamespaces
                    path.startsWith("route-map-api/") -> listOf("com.example.chabi.ui.", "com.example.chabi.data.", "com.example.chabi.maplibre.") + vendorNamespaces
                    path.startsWith("handoff-zxing/") -> listOf("android.", "androidx.", "com.example.chabi.ui.", "com.example.chabi.data.") + vendorNamespaces.filterNot { it == "com.google.zxing." }
                    path.startsWith("route-map-maplibre/") -> listOf("com.example.chabi.ui.", "com.example.chabi.data.")
                    path.contains("/ui/") -> listOf("com.example.chabi.data.", "com.example.chabi.maplibre.", "com.example.chabi.zxing.") + vendorNamespaces
                    path.contains("/data/") -> listOf("com.example.chabi.ui.", "com.example.chabi.map.", "com.example.chabi.maplibre.") + vendorNamespaces
                    else -> vendorNamespaces
                }
                if (!line.startsWith("//") && !line.startsWith("*") && forbidden.any { line.contains(it) }) {
                    failures += "$path:${index + 1}: forbidden dependency: $line"
                }
            }
        }
        val allowedProjects = mapOf(":core" to emptySet(), ":route-map-api" to setOf(":core"),
            ":route-map-maplibre" to setOf(":route-map-api"), ":data-local" to setOf(":core"), ":handoff-zxing" to setOf(":core"),
            ":app" to setOf(":core", ":route-map-api", ":route-map-maplibre", ":data-local", ":handoff-zxing"))
        subprojects.forEach { module ->
            module.configurations.forEach { configuration ->
                configuration.dependencies.forEach { dependency ->
                    if (dependency is org.gradle.api.artifacts.ProjectDependency && dependency.path != module.path && dependency.path !in allowedProjects.getValue(module.path)) {
                        failures += "${module.path}:${configuration.name}: unexpected project ${dependency.path}"
                    }
                    val sdkOwners = mapOf("org.maplibre" to ":route-map-maplibre", "com.squareup.okhttp3" to ":route-map-maplibre", "com.google.zxing" to ":handoff-zxing")
                    if (sdkOwners.any { (group, owner) -> dependency.group?.startsWith(group) == true && module.path != owner } || dependency.group?.startsWith("ai.pleos") == true) {
                        failures += "${module.path}:${configuration.name}: SDK dependency must stay in its registered adapter: $dependency"
                    }
                }
            }
        }
        check(failures.isEmpty()) { failures.joinToString("\n", prefix = "Architecture violations:\n") }
        logger.lifecycle("Architecture boundaries verified (${sources.files.size} Kotlin files).")
    }
}
subprojects {
    tasks.matching { it.name == "preBuild" || it.name == "check" }.configureEach { dependsOn(verifyArchitecture) }
}
