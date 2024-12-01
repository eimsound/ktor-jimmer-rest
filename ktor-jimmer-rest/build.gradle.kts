plugins {
    alias(libs.plugins.kotlin.jvm)
}

subprojects {
    apply(plugin = "kotlin")
    dependencies {
        implementation(rootProject.libs.bundles.dependencie)
        testImplementation(rootProject.libs.bundles.test)

    }
    tasks.test {
        useJUnitPlatform()
    }
}

tasks.jar{
    from(subprojects.map { it.sourceSets.main.get().output })
}