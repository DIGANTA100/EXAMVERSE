plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.examverse"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

javafx {
    version = "21.0.1"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml",
        "javafx.media",
        "javafx.graphics",
        "javafx.swing"
    )
}

dependencies {
    // JavaFX dependencies (handled by javafx plugin)

    // MySQL Database Connector
    implementation("com.mysql:mysql-connector-j:8.2.0")

    // Testing dependencies (optional)
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.examverse.app.Launcher")

    // JVM arguments for JavaFX - Enhanced with module fixes
    applicationDefaultJvmArgs = listOf(
        "--add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED",
        "--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED",
        "--add-opens=javafx.base/com.sun.javafx.runtime=ALL-UNNAMED",
        "--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
        "--add-modules=javafx.controls,javafx.fxml,javafx.media,javafx.graphics",
        "-Dprism.verbose=false",
        "-Djavafx.verbose=false"
    )
}

tasks {
    test {
        useJUnitPlatform()
    }

    // Custom task to run the application
    register("runApp") {
        dependsOn("run")
    }

    // Configure JAR task
    jar {
        manifest {
            attributes(
                "Main-Class" to "com.examverse.app.Launcher",
                "Implementation-Version" to project.version
            )
        }

        // Include dependencies in JAR (Fat JAR)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    }
}

// Configure source and resources directories
sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java"))
        }
        resources {
            setSrcDirs(listOf("src/main/resources"))
        }
    }
}