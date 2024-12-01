
dependencies {
    implementation(project(":ktor-jimmer-rest-validator"))
    implementation(project(":ktor-jimmer-rest-config"))
    implementation(project(":ktor-jimmer-rest-util"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}