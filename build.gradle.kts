import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import dev.famer.build.CopyResourcesTask

plugins {
    kotlin("jvm") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
}

group = "dev.famer"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val ktor_version = "2.0.2"

dependencies {
    testImplementation(kotlin("test"))
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha16")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")
    implementation("org.slf4j:jcl-over-slf4j:2.0.0-alpha7")
    implementation(compose.desktop.currentOs)
    implementation("org.apache.pdfbox:pdfbox:3.0.0-RC1") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        jvmArgs += "-Dapp.data=\${LOCALAPPDATA}"
        nativeDistributions {
            includeAllModules = true
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "scissors"
            packageVersion = "1.0.7"

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            windows {
                shortcut = true
                console = false
                upgradeUuid = "10c0f00a-d0ed-46af-bfd9-4e02f7975bf8"
                iconFile.set(project.layout.projectDirectory.file("src/main/resources/images/scissors.ico"))
            }
        }
    }
}

tasks.register<CopyResourcesTask>("copyResources")