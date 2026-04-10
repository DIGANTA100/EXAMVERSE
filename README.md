# ExamVerse

A JavaFX-based examination platform supporting student dashboards, contest rooms, exam management, and AI-powered evaluation.

---

## Prerequisites

Before running the project, ensure the following are installed:

| Requirement | Version |
|---|---|
| Java JDK | 17 |
| JavaFX SDK | 17 |
| MySQL Server | 8.0+ |
| MySQL Connector/J | 8.2.0 |
| Maven or Gradle | Latest stable |
| IntelliJ IDEA or VS Code | Latest stable |

- **Download JDK 17:** https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html  
- **Download JavaFX SDK 17:** https://gluonhq.com/products/javafx/  
- **Download MySQL Community Server:** https://dev.mysql.com/downloads/mysql/

---

## Getting the Source Code

The full project source is hosted on GitHub. Since the submitted file only contains this README, you must clone the repository first.

**Repository:** https://github.com/DIGANTA100/EXAMVERSE

Make sure **Git** is installed on your system: https://git-scm.com/downloads

Open a terminal and run:

```bash
git clone https://github.com/DIGANTA100/EXAMVERSE.git
```

This will create an `EXAMVERSE` folder in your current directory containing the full project.

---

## Installation & Setup

### Option 1 — IntelliJ IDEA

1. **Clone the repository**

   Open a terminal and run:

   ```bash
   git clone https://github.com/DIGANTA100/EXAMVERSE.git
   ```

   Or use IntelliJ's built-in Git support:
   - Launch IntelliJ IDEA
   - On the Welcome screen, click **Get from VCS**
   - Enter the URL: `https://github.com/DIGANTA100/EXAMVERSE.git`
   - Choose a local directory and click **Clone**

2. **Open the project**

   - If you cloned via terminal, go to **File → Open** and select the `EXAMVERSE` folder
   - Wait for IntelliJ to index the project

3. **Set the Project SDK to Java 17**

   - Go to **File → Project Structure → Project**
   - Set **SDK** to `Java 17`
   - Set **Language level** to `17`

4. **Add JavaFX SDK as a library**

   - Go to **File → Project Structure → Libraries**
   - Click **+** → **Java**
   - Navigate to your JavaFX SDK folder and select the `lib` directory
   - Click **OK** and apply

5. **Configure the Run Configuration for `Launcher.java`**

   - Go to **Run → Edit Configurations**
   - Click **+** → **Application**
   - Set **Main class** to `com.examverse.app.Launcher`
   - In the **VM options** field, add the following (replace the path with your actual JavaFX SDK `lib` path):

     ```
     --module-path /path/to/javafx-sdk-17/lib --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web
     ```

   - Click **Apply → OK**

6. **Run the project**

   - Click the green **Run** button, or press `Shift + F10`
   - The application will launch from `Launcher.java`

---

### Option 2 — Visual Studio Code

1. **Install required VS Code extensions**

   - [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) (includes Language Support, Debugger, Maven, and Project Manager for Java)

2. **Clone the repository**

   Open a terminal and run:

   ```bash
   git clone https://github.com/DIGANTA100/EXAMVERSE.git
   ```

   Or use VS Code's built-in Git support:
   - Open VS Code
   - Press `Ctrl + Shift + P` and search for **Git: Clone**
   - Enter the URL: `https://github.com/DIGANTA100/EXAMVERSE.git`
   - Choose a local folder and click **Open** when prompted

3. **Open the project**

   - If you cloned via terminal, go to **File → Open Folder** and select the `EXAMVERSE` folder

4. **Configure Java 17 in VS Code**


   - Open the Command Palette (`Ctrl + Shift + P`)
   - Search for **Java: Configure Java Runtime**
   - Make sure **JDK 17** is listed and selected for this project
   - If not detected, add the JDK path manually in `settings.json`:

     ```json
     "java.jdt.ls.java.home": "/path/to/jdk-17"
     ```

5. **Add the JavaFX VM arguments**

   - Create or edit `.vscode/launch.json` in the project root with the following content:

     ```json
     {
       "version": "0.2.0",
       "configurations": [
         {
           "type": "java",
           "name": "Launch ExamVerse",
           "request": "launch",
           "mainClass": "com.examverse.app.Launcher",
           "vmArgs": "--module-path /path/to/javafx-sdk-17/lib --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web"
         }
       ]
     }
     ```

   > Replace `/path/to/javafx-sdk-17/lib` with the actual path to the `lib` folder inside your JavaFX SDK installation.

6. **Add JavaFX to the classpath**

   - Open the Command Palette (`Ctrl + Shift + P`)
   - Search for **Java: Configure Classpath**
   - Under **Referenced Libraries**, click **+** and add all `.jar` files from the JavaFX SDK `lib` folder

7. **Run the project**

   - Open `src/main/java/com/examverse/app/Launcher.java`
   - Press `F5` or click **Run → Start Debugging**
   - Alternatively, click the **Run** button that appears above the `main` method

---

## Database Setup

ExamVerse uses **MySQL** as its database. You must have MySQL Server running before launching the application.

### Step 1 — Install & Start MySQL

- Download and install **MySQL Community Server 8.0+** from https://dev.mysql.com/downloads/mysql/
- During installation, set a root password — remember it, you will need it in the next step
- Make sure the MySQL service is running (port `3306` by default)

### Step 2 — Configure Your Credentials

Open the following file:

```
src/main/java/com/examverse/config/DatabaseConfig.java
```

Find **line 18** and update the password to match your MySQL root password:

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/examverse_db";
private static final String DB_USER = "root";
private static final String DB_PASSWORD = "your_mysql_password_here"; // Change this
```

> The username is `root` by default. If you use a different MySQL user, update `DB_USER` as well.

### Step 3 — Database and Tables Are Auto-Created

You do **not** need to manually create the database or tables. On first run, the application will automatically:

- Create the `examverse_db` database if it does not exist
- Create all required tables (`users`, `exams`, `questions`, `student_exam_attempts`, `student_answers`)
- Insert a default admin account and sample exam data

**Default admin credentials (auto-created on first run):**

```
Username : admin
Password : admin123
```

### Step 4 — Add MySQL Connector to Your Build

Make sure the MySQL JDBC driver is included in your dependencies.

In `build.gradle.kts` (Gradle):

```kotlin
implementation("com.mysql:mysql-connector-j:8.2.0")
```

Or in `pom.xml` (Maven):

```xml
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.2.0</version>
</dependency>
```

Initial contest data is additionally set up by `ContestDatabaseSetup.java` on first run.

---

## Project Entry Point

The application starts from:

```
src/main/java/com/examverse/app/Launcher.java
```

`Launcher.java` bootstraps the JavaFX application and delegates to `ExamVerseApp.java`.

---

## Project Structure Overview

```
EXAMVERSE/
├── src/main/java/com/examverse/
│   ├── app/          → Entry point (Launcher.java, ExamVerseApp.java)
│   ├── config/       → Database, email, and scene configuration
│   ├── controller/   → JavaFX controllers (admin, auth, contest, exam, dashboard)
│   ├── model/        → Data models (User, Exam, Contest, Question, etc.)
│   ├── service/      → Business logic and services (AI, auth, exam, storage)
│   └── util/         → Utilities (SessionManager, SceneManager, Validator)
└── src/main/resources/com/examverse/
    ├── assets/       → Images, music, and video assets
    ├── css/          → Stylesheets for all views
    └── fxml/         → FXML layout files for all screens
```

---

## Notes

- Make sure the **JavaFX module path is set correctly** in both the IDE and the run configuration. Missing VM arguments is the most common cause of `java.lang.module` errors on startup.
- If you see `Graphics Device initialization failed`, ensure JavaFX `lib` jars are on the classpath.
- The email feature requires valid SMTP credentials in `EmailConfig.java`.
