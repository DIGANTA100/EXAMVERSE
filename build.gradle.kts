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
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

/* =======================
   JavaFX Configuration
   ======================= */
javafx {
    version = "21.0.1"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml",
        "javafx.graphics",
        "javafx.media",
        "javafx.web"      // ✅ REQUIRED for WebView
    )
}

/* =======================
   Dependencies
   ======================= */
dependencies {

    // MySQL Connector
    implementation("com.mysql:mysql-connector-j:8.2.0")

    // Java Mail (Email / OTP / Reset Password)
    implementation("com.sun.mail:javax.mail:1.6.2")
    implementation("javax.activation:activation:1.1.1")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}

/* =======================
   Application Config
   ======================= */
application {
    mainClass.set("com.examverse.app.Launcher")

    // Minimal JVM args (JavaFX plugin already handles module-path)
    applicationDefaultJvmArgs = listOf(
        "--add-modules=javafx.controls,javafx.fxml,javafx.graphics,javafx.media,javafx.web",
        "-Xmx4G",  // Increase heap to 4GB
        "-Xms2G"   // Start with 2GB
    )
}

/* =======================
   Tasks
   ======================= */
tasks {

    test {
        useJUnitPlatform()
    }

    // Run app using Gradle
    register<JavaExec>("runApp") {
        group = "application"
        description = "Run ExamVerse Application"
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set("com.examverse.app.Launcher")
    }

    // Fat JAR (optional but useful)
    jar {
        manifest {
            attributes(
                "Main-Class" to "com.examverse.app.Launcher",
                "Implementation-Version" to project.version
            )
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(
            configurations.runtimeClasspath.get().map {
                if (it.isDirectory) it else zipTree(it)
            }
        )
    }
}

/* =======================
   Source Sets
   ======================= */
sourceSets {
    main {
        java.setSrcDirs(listOf("src/main/java"))
        resources.setSrcDirs(listOf("src/main/resources"))
    }
}
