plugins {
    `java-library`
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("io.freefair.lombok") version "8.4"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.bobolabs.net/releases")
}

dependencies {
    implementation("net.bobolabs.config:config:2.0.0")
    implementation("org.yaml:snakeyaml:2.2")

    implementation("com.google.code.gson:gson:2.10.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            project.shadow.component(this)
        }
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveVersion.set("${project.version}")
    }

    assemble {
        dependsOn(shadowJar)
    }

    jar {
        manifest {
            attributes["Main-Class"] = "it.renvins.downloadit.DownloadItMain"
        }
    }
}