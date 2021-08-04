import org.jetbrains.intellij.tasks.PrepareSandboxTask
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    id("java")
//    kotlin("jvm") version "1.5.21"
    id("org.jetbrains.kotlin.jvm") version "1.5.10"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "1.1.4"
    id("net.researchgate.release") version "2.8.1"
}

group = "org.gap.ijplugins.spring.ideaspringtools"

if (version.toString().endsWith("SNAPSHOT")) {
    version = version.toString().replace("SNAPSHOT", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HH.mm.ss.SSS")))
}

repositories {
    maven("https://repo.huaweicloud.com/repository/maven")
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.spring.io/libs-snapshot/")
}

val languageServer by configurations.creating

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.github.ballerina-platform:lsp4intellij:master-SNAPSHOT")
    implementation("org.springframework.ide.vscode:commons-java:1.27.0-SNAPSHOT")
    languageServer("org.springframework.ide.vscode:spring-boot-language-server:1.27.0-SNAPSHOT:exec") {
        isTransitive = false
    }
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    pluginName.set("idea-spring-tools")
    version.set("2021.2")
    type.set("IC")
    downloadSources.set(true)
    updateSinceUntilBuild.set(true)

    // Plugin Dependencies. Uses `platformPlugins` property from the gradle.properties file.
    plugins.set("IntelliLang,java".split(',').map(String::trim).filter(String::isNotEmpty))
}

tasks {
    compileJava {
        sourceCompatibility = "11"
        targetCompatibility = "11"
//        options.encoding = StandardCharsets.UTF_8.name()
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    patchPluginXml {
        version.set("1.2.7")
        sinceBuild.set("212")
        untilBuild.set("213.*")
    }
}

//tasks.buildSearchableOptions {
//    enabled = false
//}

tasks.getByName<PrepareSandboxTask>("prepareSandbox").doLast {
    val pluginServerDir = "${intellij.sandboxDir}/plugins/${intellij.pluginName}/lib/server"

    mkdir(pluginServerDir)
    copy {
        from(languageServer)
        into(pluginServerDir)
        rename("spring-boot-language-server.*\\.jar", "language-server.jar")
    }
}

tasks {
    buildPlugin {
        doLast() {
            val content = """
                <?xml version="1.0" encoding="UTF-8"?>
                <plugins>
                    <plugin id="org.gap.ijplugins.spring.idea-spring-tools" version="${archiveVersion}">
                        <idea-version since-build="212.4746.92" until-build="213.*" />
                    </plugin>
                </plugins>                
                
            """.trimIndent()
            file("build/distributions/updatePlugins.xml").writeText(content)
        }
    }
}


release {
    failOnUnversionedFiles = false
    failOnSnapshotDependencies = false
    tagTemplate = "$version"
    buildTasks = arrayListOf("buildPlugin")
}

//tasks {
//    afterReleaseBuild {
//        dependsOn("publishPlugin")
//    }
//
//    publishPlugin {
//        token.set(System.getenv("JB_API_KEY"))
//    }
//
//    runIde {
//        setJvmArgs(listOf("-Dsts4.jvmargs=-Xmx512m -Xms512m"))
//    }
//}
