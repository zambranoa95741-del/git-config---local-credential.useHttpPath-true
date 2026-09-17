# Lab Activity 10 — Liceo Student Portal (JavaFX)

**Object-Oriented Programming · Mr. Ernesto Razo Jr.**
**Branch: `lab-activity-10`**

Build a two-screen JavaFX desktop application: a **login screen**, and — after a
successful login — a **student profile screen** showing that student's details.

---

## Before you write any code

```bash
git checkout -b lab-activity-10
git branch          # the * must be on lab-activity-10
```

Everything you commit for this activity must be on `lab-activity-10`.
Work committed to `main` will not be checked.

## Running it

Open the Maven tool window in IntelliJ → **Plugins → javafx → javafx:run**, or:

```bash
mvn clean javafx:run
```

## The starter does NOT compile yet — this is normal

Before you finish the TODOs you will see errors such as:

```
Student.java:33: error: missing return statement
StudentDirectory.java:57: error: cannot find symbol
        return students.size();
               ^
  symbol:   variable students
```

They disappear as you complete the TODOs. Do not panic at the red marks.

## What you must finish — 12 TODOs

| # | File | What to do |
|---|------|-----------|
| 1–3 | `model/Student.java` | fields, constructor, getters |
| 4–6 | `model/StudentDirectory.java` | the list, the three accounts, `login()` |
| 7–8 | `login.fxml` | `fx:id` on the two input controls and the message label, `onAction` on the button |
| 9–10 | `controller/LoginController.java` | the `@FXML` fields, and `handleLogin()` |
| 11 | `profile.fxml` | the five `fx:id` attributes |
| 12 | `controller/ProfileController.java` | `setStudent()` |

**Given complete — do not change:** `MainApp.java`, `module-info.java`,
`pom.xml`, `style.css`, and the `handleLogout()` / `initialsOf()` methods.

## Test accounts

| Student number | Password | Name |
|---|---|---|
| 2026-00123 | liceo123 | Ana Marie Dela Cruz |
| 2026-00456 | gcash456 | Jerome Bacaltos |
| 2026-00789 | maya789 | Liza Manalo |

## Screenshots to submit

Put six PNG files in `screenshots/`:

```
01-login-screen.png     the login screen when the program starts
02-empty-fields.png     LOG IN clicked with both fields empty
03-wrong-password.png   a real student number with a wrong password
04-profile-ana.png      logged in as 2026-00123
05-profile-liza.png     logged in as 2026-00789
06-logout.png           back on the login screen after LOG OUT
```

## Submitting

```bash
git add .
git commit -m "Lab Activity 10: login and student profile"
git push -u origin lab-activity-10
```

See the handout for the full instructions, the wireframes and the rubric.
