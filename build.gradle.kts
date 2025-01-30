plugins {
    id("java")
    kotlin("jvm") version "2.0.20"
    id("io.github.goooler.shadow") version "8.1.8"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.11"
    `java-library`
}

group = "io.github.rothes"
version = "2.0.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.codemc.org/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.1-R0.1-SNAPSHOT")
    compileOnlyApi("io.github.rothes.esu:bukkit:1.0.0")
    compileOnlyApi("io.github.rothes.esu:core:1.0.0")
    compileOnly("fr.xephi:authme:5.6.0-SNAPSHOT")
}

val fileName = rootProject.name
tasks.shadowJar {
    archiveFileName = "${fileName}-${project.version}-mojmap.jar"

    relocate("kotlin", "io.github.rothes.esu.lib.kotlin")
    relocate("org.incendo", "io.github.rothes.esu.lib.org.incendo")
    relocate("cc.carm.lib", "io.github.rothes.esu.lib.cc.carm.lib")
    relocate("info.debatty", "io.github.rothes.esu.lib.info.debatty")

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib"))
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-java-parameters") // Fix cloud-annotations
    }
}