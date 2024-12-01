rootProject.name = "ktor-jimmer-rest"

dependencyResolutionManagement{
    repositories {
        maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
        mavenCentral()
    }
}
include("ktor-jimmer-rest-route")
include("ktor-jimmer-rest-provider")
include("ktor-jimmer-rest-util")
include("ktor-jimmer-rest-validator")
include("ktor-jimmer-rest-config")

