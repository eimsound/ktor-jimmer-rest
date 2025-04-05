plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
}

subprojects {
    apply(plugin = "kotlin")
    dependencies {
        implementation(rootProject.libs.kotlinx.coroutines.core)
        implementation(rootProject.libs.kotlin.reflect)
        compileOnly(rootProject.libs.ktor.server.core)
        compileOnly(rootProject.libs.jimmer)
        testImplementation(rootProject.libs.bundles.test)
    }
    tasks.test {
        useJUnitPlatform()
    }
    java {
        withJavadocJar()
        withSourcesJar()
    }
}

// 整合源代码jar
tasks.kotlinSourcesJar {
    archiveClassifier.set("sources")
    from(subprojects.map { it.sourceSets.main.get().allSource })
}
// 整合源文档jar
val javadocJarMerger = tasks.register<Jar>("javadocJarMerger") {
    archiveClassifier.set("javadoc")
    from(subprojects.map { it.tasks.javadoc.get().outputs })
}

tasks.jar {
    dependsOn(tasks.kotlinSourcesJar)
    dependsOn(javadocJarMerger)

    dependsOn(tasks.withType(GenerateMavenPom::class))
    into("META-INF") {
        from("${project.layout.buildDirectory.get()}/publications/mavenJava")
        exclude("*.asc")
        rename { it.replace("pom-default.xml", "pom.xml") }
    }
    from(subprojects.map { it.sourceSets.main.get().output })
}



tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = group.toString()
            artifactId = rootProject.name
            version = version

            from(components["java"])

            pom {
                name.set(project.name)
                description.set("A Ktor plugin that provides a concise DSL-style API for building RESTful web services based on Ktor and Jimmer")
                url.set("https://www.eimsound.com")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("SparrowAndSnow")
                        name.set("SparrowAndSnow")
                        email.set("maqueyuxue@outlook.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/eimsound/ktor-jimmer-rest.git")
                    developerConnection.set("scm:git:ssh://github.com/eimsound/ktor-jimmer-rest.git")
                    url.set("https://github.com/eimsound/ktor-jimmer-rest.git")
                }
            }
        }
    }

    repositories {
        maven {
            name = "mavenCentral"
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = System.getProperty("SONATYPE_NEXUS_USERNAME")
                password = System.getProperty("SONATYPE_NEXUS_PASSWORD")
            }
        }
    }
    signing {
        sign(publishing.publications["mavenJava"])
    }

    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }
    tasks.javadoc {
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
        (options as StandardJavadocDocletOptions).encoding("UTF-8")
    }
}
