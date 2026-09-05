plugins {
    application
    id("java")
}

group = "org.rkg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val rdf4jVersion = "6.0.1"

dependencies {
    implementation("org.slf4j:slf4j-simple:2.0.12")
    implementation("org.xerial:sqlite-jdbc:3.53.4.0")
    implementation("info.picocli:picocli:4.7.7")
    implementation("org.eclipse.rdf4j:rdf4j-storage:$rdf4jVersion")
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.23.0")
    testImplementation("org.testcontainers:testcontainers:2.0.5")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
}

tasks.register<Test>("integrationTest") {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    shouldRunAfter("test")
}

tasks.register<Test>("e2eTest") {
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    systemProperty("rkg.project.directory", project.layout.projectDirectory.asFile.absolutePath)
    useJUnitPlatform {
        includeTags("e2e")
    }
    shouldRunAfter("integrationTest")
}

tasks.test {
    useJUnitPlatform {
        includeTags("unit")
    }
}

tasks.check {
    dependsOn("integrationTest")
}

application {
    mainClass = "org.rkg.cli.RkgCli"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}