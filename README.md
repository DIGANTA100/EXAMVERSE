<div align="center">

<!-- ============================================================ -->
<!--  BANNER IMAGE                                                 -->
<!--  📌 Replace the src below with your actual banner image URL  -->
<!--  Recommended: 1280×640px, upload to /assets/images/ in repo  -->
<!-- ============================================================ -->
<img src="src/main/resources/com.examverse/assets/images/banner.png" alt="ExamVerse Banner" width="100%" />

<br/>

# ⚡ ExamVerse

### *A Next-Generation JavaFX Examination & Contest Platform*

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/JavaFX-17-blue?style=for-the-badge&logo=java&logoColor=white" />
  <img src="https://img.shields.io/badge/MySQL-8.0+-4479A1?style=for-the-badge&logo=mysql&logoColor=white" />
  <img src="https://img.shields.io/badge/Gemini_AI-Integrated-8E75B2?style=for-the-badge&logo=google&logoColor=white" />
  <img src="https://img.shields.io/badge/Gradle-Build-02303A?style=for-the-badge&logo=gradle&logoColor=white" />
  <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" />
</p>

<br/>

<!-- ============================================================ -->
<!--  ACTION BUTTONS — replace # with your actual links           -->
<!-- ============================================================ -->

[![▶ Watch Demo](https://img.shields.io/badge/▶%20Watch%20Demo-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://youtu.be/tm7iBEOTL0A)
&nbsp;
[![⭐ Star this Repo](https://img.shields.io/badge/⭐%20Star%20this%20Repo-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/DIGANTA100/EXAMVERSE)
&nbsp;
[![🔗 LinkedIn](https://img.shields.io/badge/🔗%20LinkedIn-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/ajmainfayekdiganta/)
&nbsp;
[![🍴 Fork](https://img.shields.io/badge/🍴%20Fork-24292F?style=for-the-badge&logo=github&logoColor=white)](https://github.com/DIGANTA100/EXAMVERSE/fork)

<br/>

</div>

---

## 📖 About ExamVerse

**ExamVerse** is a feature-rich desktop examination platform built with **JavaFX**, designed for educational institutions and competitive exam environments. It combines a polished UI experience with real-time contest rooms, AI-powered answer evaluation, and a full admin management suite — all backed by a MySQL database.

> 🎯 Built as a full-stack desktop application — from animated intro screens and 3D dashboards to live leaderboards and AI-graded written exams.

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 🎓 Student Side
- Personalized student dashboard with 3D landing view
- Take scheduled exams with live timer
- Join contest rooms and compete in real-time
- View leaderboard rankings & detailed results
- Written answer submission with AI evaluation
- Discussion forum for peer interaction
- Notifications for upcoming exams & contests

</td>
<td width="50%">

### 🛠️ Admin Side
- Full admin dashboard with analytics
- Create, edit & manage exams and questions
- Organize and schedule contests
- View per-student exam reports
- Manage student accounts
- Send broadcast notifications
- Moderate the discussion forum

</td>
</tr>
</table>

### 🤖 AI & Extra
- **Gemini AI** integration for evaluating written/subjective answers
- Cinematic intro video with animated transitions
- Background music per contest arena (8 unique tracks)
- Email-based password reset flow
- Role-based access control (Admin / Student)

---

## 🖼️ Screenshots

<!-- ============================================================ -->
<!--  SCREENSHOTS — replace placeholder paths with real ones      -->
<!--  Put your screenshots inside assets/images/screenshots/      -->
<!--  Recommended size: ~1200×700px per screenshot                -->
<!-- ============================================================ -->

<div align="center">

| Dashboard Landing | Contest Room |
|:-----------------:|:------------:|
| ![Dashboard](assets/images/screenshots/dashboard.png) | ![Contest Room](assets/images/screenshots/contest-room.png) |

| Admin Panel | Exam Taking |
|:-----------:|:-----------:|
| ![Admin](assets/images/screenshots/admin-dashboard.png) | ![Exam](assets/images/screenshots/exam-taking.png) |

| Leaderboard | Results |
|:-----------:|:-------:|
| ![Leaderboard](assets/images/screenshots/leaderboard.png) | ![Results](assets/images/screenshots/results.png) |

</div>

---

## 🎬 Demo Video

<!-- ============================================================ -->
<!--  VIDEO THUMBNAIL — replace with your YouTube thumbnail link  -->
<!--  To get thumbnail: https://i.ytimg.com/vi/YOUR_VIDEO_ID/maxresdefault.jpg -->
<!-- ============================================================ -->

<div align="center">

[![ExamVerse Demo Video](https://img.youtube.com/vi/tm7iBEOTL0A/maxresdefault.jpg)](https://youtu.be/tm7iBEOTL0A)

*👆 Click to watch the full demo on YouTube*

</div>

---

## 🗂️ Project Structure

```
EXAMVERSE/
├── src/main/java/com/examverse/
│   ├── app/            → Entry point (Launcher.java, ExamVerseApp.java)
│   ├── config/         → Database, email, and scene configuration
│   ├── controller/
│   │   ├── admin/      → Admin dashboard, exams, contests, students
│   │   ├── auth/       → Login, signup, password reset
│   │   ├── contest/    → Contest lobby, room, leaderboard, results
│   │   ├── dashboard/  → Student & landing dashboard sections
│   │   ├── exam/       → Exam taking & result display
│   │   └── forum/      → Discussion forum (admin & student)
│   ├── model/          → Data models (User, Exam, Contest, Question…)
│   ├── service/
│   │   ├── ai/         → Gemini AI integration
│   │   ├── auth/       → Auth, email, password reset, UserDAO
│   │   └── exam/       → Exam, contest, evaluation, timer services
│   └── util/           → SessionManager, SceneManager, Validator
└── src/main/resources/com/examverse/
    ├── assets/         → Images, music (8 tracks), intro videos
    ├── css/            → Stylesheets for all views
    └── fxml/           → FXML layout files for all screens
```

---

## ⚙️ Prerequisites

| Requirement | Version |
|---|---|
| Java JDK | 17 |
| JavaFX SDK | 17 |
| MySQL Server | 8.0+ |
| MySQL Connector/J | 8.2.0 |
| Gradle | Latest stable |
| IntelliJ IDEA / VS Code | Latest stable |

<details>
<summary>📥 Download Links</summary>

- [JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
- [JavaFX SDK 17](https://gluonhq.com/products/javafx/)
- [MySQL Community Server](https://dev.mysql.com/downloads/mysql/)
- [Git](https://git-scm.com/downloads)

</details>

---

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/DIGANTA100/EXAMVERSE.git
cd EXAMVERSE
```

### 2. Database Setup

> **MySQL must be running before you launch the app.**

**a)** Open `src/main/java/com/examverse/config/DatabaseConfig.java` and update your credentials:

```java
private static final String DB_URL      = "jdbc:mysql://localhost:3306/examverse_db";
private static final String DB_USER     = "root";
private static final String DB_PASSWORD = "your_mysql_password_here"; // ← change this
```

**b)** That's it — on first run, ExamVerse **automatically**:
- Creates the `examverse_db` database
- Creates all required tables
- Seeds a default admin account and sample exam data

> 🔑 **Default admin login:** `admin` / `admin123`

### 3. IDE Setup

<details>
<summary>🟦 IntelliJ IDEA</summary>

1. Open the cloned folder via **File → Open**
2. Set SDK to Java 17: **File → Project Structure → Project**
3. Add JavaFX lib as a library: **File → Project Structure → Libraries → + → Java → select JavaFX `lib/` folder**
4. Create a Run Configuration (**Run → Edit Configurations → + → Application**):
   - **Main class:** `com.examverse.app.Launcher`
   - **VM options:**
     ```
     --module-path /path/to/javafx-sdk-17/lib --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.web
     ```
5. Press **Shift + F10** to run

</details>

<details>
<summary>🟦 Visual Studio Code</summary>

1. Install the **Extension Pack for Java**
2. Open the cloned folder via **File → Open Folder**
3. Create `.vscode/launch.json`:

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

4. Open `Launcher.java` and press **F5**

</details>

### 4. Gradle Dependency

Make sure your `build.gradle.kts` includes the MySQL connector:

```kotlin
implementation("com.mysql:mysql-connector-j:8.2.0")
```

---

## 🧠 Tech Stack

| Layer | Technology |
|---|---|
| UI Framework | JavaFX 17 + FXML |
| Styling | CSS (custom per module) |
| Backend Logic | Java 17 |
| Database | MySQL 8.0+ via JDBC |
| AI Evaluation | Google Gemini API |
| Email Service | JavaMail (SMTP) |
| Build Tool | Gradle (Kotlin DSL) |
| Media | MP4 intro videos, MP3 arena music |

---

## 🤝 Contributing

Contributions are welcome! Feel free to open issues or submit pull requests.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Commit your changes: `git commit -m "Add your feature"`
4. Push and open a Pull Request

---

## 👨‍💻 Author

<div align="center">

<!-- ============================================================ -->
<!--  AUTHOR AVATAR — replace with your GitHub profile image URL  -->
<!-- ============================================================ -->

<img src="assets/images/diganta.jpg" width="100" style="border-radius:50%" alt="Diganta"/>

**Diganta**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Connect-0A66C2?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/ajmainfayekdiganta/)
&nbsp;
[![GitHub](https://img.shields.io/badge/GitHub-Follow-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/DIGANTA100)
&nbsp;
[![YouTube](https://img.shields.io/badge/YouTube-Watch%20Demo-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://youtu.be/tm7iBEOTL0A)

</div>

---

## ⚠️ Troubleshooting

| Issue | Fix |
|---|---|
| `java.lang.module` error on startup | Check VM args — missing `--module-path` or `--add-modules` |
| `Graphics Device initialization failed` | Ensure all JavaFX `.jar` files are on the classpath |
| MySQL connection refused | Make sure MySQL service is running on port 3306 |
| Email not sending | Add valid SMTP credentials in `EmailConfig.java` |

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<div align="center">

*If you found this project helpful, please consider giving it a ⭐ — it means a lot!*

[![⭐ Star on GitHub](https://img.shields.io/badge/⭐%20Star%20on%20GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/DIGANTA100/EXAMVERSE)

</div>
