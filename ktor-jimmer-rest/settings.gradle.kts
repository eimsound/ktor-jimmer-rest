rootProject.name = "ktor-jimmer-rest"

dependencyResolutionManagement{
    repositories {
        if (System.getenv("JITPACK") == "true") {
            // JitPack 构建机访问国内镜像不稳定，仅使用 Maven Central 防止超时
            mavenCentral()
        } else {
            maven("https://mirrors.tencent.com/nexus/repository/maven-public/")
            mavenCentral()
        }
    }
}
include("ktor-jimmer-rest-route")
include("ktor-jimmer-rest-provider")
include("ktor-jimmer-rest-util")
include("ktor-jimmer-rest-validator")
include("ktor-jimmer-rest-config")
