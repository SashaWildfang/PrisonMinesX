plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "2.2.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.intellectualsites.com/content/groups/public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:5.1.0")

    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.9.2")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.9.2")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    runServer {
        minecraftVersion("1.20.4")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    build {
        dependsOn(shadowJar)
    }
}