
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

group = "dev.famer"
version = "1.0"

val ktor_version = "1.6.5"

dependencies {
    implementation(gradleApi())
    implementation("io.ktor:ktor-client-cio:$ktor_version")
}