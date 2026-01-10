plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "xyz.kohara"
version = ""

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://maven.lavalink.dev/releases")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("net.dv8tion:JDA:5.3.1") {}
    implementation("org.yaml:snakeyaml:2.4")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("org.apache.commons:commons-text:1.13.0")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("dev.arbjerg:lavaplayer:2.2.6")
    implementation("dev.lavalink.youtube:common:1.16.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2")
    implementation("org.apache.httpcomponents.core5:httpcore5:5.2")
    implementation("com.github.minndevelopment:emoji-java:master-SNAPSHOT")
    implementation("org.reflections:reflections:0.10.2")
    implementation("com.sparkjava:spark-core:2.9.4")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "xyz.kohara.Aroki"
    }
}

tasks.shadowJar {
    archiveBaseName.set("Aroki")
    archiveClassifier.set("")
}
