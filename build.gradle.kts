import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import dev.famer.build.CopyResourcesTask

plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31"
    id("org.jetbrains.compose") version "1.0.0-rc2"
}

group = "dev.famer"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

val ktor_version = "1.6.5"

dependencies {
    testImplementation(kotlin("test"))
    implementation("ch.qos.logback:logback-classic:1.3.0-alpha10")
//    implementation("org.slf4j:slf4j-jdk14:1.7.32")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha5")
    implementation("org.slf4j:jcl-over-slf4j:2.0.0-alpha5")
    implementation(compose.desktop.currentOs)
    implementation("org.apache.pdfbox:pdfbox:3.0.0-RC1") {
        exclude(group = "commons-logging", module = "commons-logging")
    }
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
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
//        properties += Pair("", "")
        nativeDistributions {
            includeAllModules = true
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "scissors"
            packageVersion = "1.0.0"

            appResourcesRootDir.set(project.layout.projectDirectory.dir("resources"))
            windows {
                shortcut = true
                console = false
                iconFile.set(project.layout.projectDirectory.file("src/main/resources/images/scissors.ico"))
            }
        }
    }
}

tasks.register<CopyResourcesTask>("copyResources")