pluginManagement {
    repositories {
        // 1. 优先尝试 Google 官方源（解决 com.android.application 找不到的核心）
        google()
        gradlePluginPortal()
        mavenCentral()

        // 2. 如果官方源慢，再尝试阿里云作为备选
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 优先使用阿里云镜像（国内访问更快更稳定）
        maven { 
            url = uri("https://maven.aliyun.com/repository/public")
            isAllowInsecureProtocol = false
        }
        maven { 
            url = uri("https://maven.aliyun.com/repository/google")
            isAllowInsecureProtocol = false
        }
        // 添加腾讯云镜像作为备选
        maven {
            url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
            isAllowInsecureProtocol = false
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "YIXIU_1"
include(":app")