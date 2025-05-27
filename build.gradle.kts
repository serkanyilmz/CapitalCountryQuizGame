// build.gradle.kts


plugins {
    java
    application
}

group = "com.example"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
    google()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.jsoup:jsoup:1.16.1")
    implementation("org.json:json:20231013")
    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    testImplementation("junit:junit:4.13.2")
}

application {
    // Replace with your actual main class package/name
    mainClass.set("org.example.QuizGame")
}

tasks.jar {
    // Exclude any duplicate META-INF entries
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }

    // Unpack all runtime‐classpath jars into this fat‐jar
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    }) {
        // Also exclude duplicates here just in case
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
