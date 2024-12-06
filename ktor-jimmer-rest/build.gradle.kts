plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
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
    tasks.withType<GenerateModuleMetadata> {
        enabled = false
    }
    java {
        withJavadocJar()
    }
}

tasks.jar {
    from(subprojects.map { it.sourceSets.main.get().output })
}
publishing {
    publications {
        create<MavenPublication>("maven") {
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
    signing{ // 文件签名
        sign(publishing.publications["maven"])
    }
}
