import org.gradle.kotlin.dsl.invoke
import org.gradle.api.tasks.JavaExec

plugins {
    id("java")
}

group = "cn.sonata.vpn"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":module-common"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Convenience tasks to run the handshake sandbox without adding the application plugin.
// Usage:
//   gradlew :module-common-sandbox:runHandshakeServer
//   gradlew :module-common-sandbox:runHandshakeClient
tasks.register<JavaExec>("runHandshakeServer") {
    group = "application"
    description = "Run HandshakeDemo in server mode"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("cn.sonata.vpn.sandbox.handshake.HandshakeDemo")
    args("server", "localhost", "8081")
}

tasks.register<JavaExec>("runHandshakeClient") {
    group = "application"
    description = "Run HandshakeDemo in client mode"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("cn.sonata.vpn.sandbox.handshake.HandshakeDemo")
    args("client", "localhost", "8081")
}
