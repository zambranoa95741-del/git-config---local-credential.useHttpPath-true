import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class EnrollmentApp {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        ArrayList<Student> students = new ArrayList<>();
        ArrayList<Course> courses = new ArrayList<>();

        HashMap<String, ArrayList<String>> enrollments =
                new HashMap<>();

        String[] validPrograms = {"BSIT", "BSCS"};

        int choice = -1;

        while (choice != 0) {

            printMenu();
            choice = Integer.parseInt(sc.nextLine());

            switch (choice) {

                case 1:

                    System.out.println("\n--- REGISTER STUDENT ---");

                    System.out.print("Student ID: ");
                    String id = sc.nextLine();

                    System.out.print("Full Name: ");
                    String name = sc.nextLine();

                    System.out.print("Program: ");
                    String program = sc.nextLine();

                    boolean valid = false;

                    for (String p : validPrograms) {
                        if (p.equalsIgnoreCase(program)) {
                            valid = true;
                            program = p;
                            break;
                        }
                    }

                    if (!valid) {
                        System.out.println("Invalid Program!");
                        break;
                    }

                    System.out.print("Year Level: ");
                    int year = Integer.parseInt(sc.nextLine());

                    if (year < 1 || year > 4) {
                        System.out.println("Invalid Year Level!");
                        break;
                    }

                    students.add(new Student(id, name, program, year));

                    System.out.println("[OK] Student registered successfully!");

                    break;

                case 2:

                    System.out.println("\n--- ADD COURSE ---");

                    System.out.print("Course Code: ");
                    String code = sc.nextLine();

                    System.out.print("Title: ");
                    String title = sc.nextLine();

                    System.out.print("Units: ");
                    int units = Integer.parseInt(sc.nextLine());

                    System.out.print("Capacity: ");
                    int capacity = Integer.parseInt(sc.nextLine());

                    courses.add(new Course(code, title, units, capacity));

                    System.out.println("[OK] Course Added!");

                    break;

                case 3:

                    System.out.println("\n--- ENROLL STUDENT ---");

                    System.out.print("Student ID: ");
                    String sid = sc.nextLine();

                    Student student = findStudent(students, sid);

                    if (student == null) {
                        System.out.println("Student not found.");
                        break;
                    }

                    System.out.print("Course Code: ");
                    String ccode = sc.nextLine();

                    Course course = findCourse(courses, ccode);

                    if (course == null) {
                        System.out.println("Course not found.");
                        break;
                    }

                    if (course.isFull()) {
                        System.out.println("Course is already full.");
                        break;
                    }

                    enrollments.putIfAbsent(sid, new ArrayList<>());

                    ArrayList<String> studentCourses =
                            enrollments.get(sid);

                    if (studentCourses.contains(ccode)) {
                        System.out.println("Student already enrolled.");
                        break;
                    }

                    studentCourses.add(ccode);
                    course.addOneEnrollee();

                    System.out.println("[OK] " +
                            student.getFullName() +
                            " enrolled in " +
                            course.getCourseCode());

                    break;

                case 4:

                    System.out.println("\n--- STUDENTS ---");

                    if (students.isEmpty()) {
                        System.out.println("No students yet.");
                    } else {

                        for (Student s : students) {
                            System.out.println(s.describe());
                        }

                    }

                    break;

                case 5:

                    System.out.println("\n--- COURSES ---");

                    if (courses.isEmpty()) {
                        System.out.println("No courses.");
                    } else {

                        for (Course c : courses) {

                            System.out.println(
                                    c.getCourseCode() + " | " +
                                            c.getTitle() + " | " +
                                            c.getUnits() + " units | " +
                                            c.getEnrolledCount() +
                                            "/" +
                                            c.getCapacity()
                            );

                        }

                    }

                    break;

                case 6:

                    System.out.println("\n--- STUDENT LOAD ---");

                    System.out.print("Student ID: ");
                    String studentId = sc.nextLine();

                    Student st = findStudent(students, studentId);

                    if (st == null) {
                        System.out.println("Student not found.");
                        break;
                    }

                    ArrayList<String> list =
                            enrollments.get(studentId);

                    if (list == null || list.isEmpty()) {
                        System.out.println("No enrolled courses.");
                        break;
                    }

                    int totalUnits = 0;

                    System.out.println(st.getFullName());

                    for (String courseCode : list) {

                        Course c = findCourse(courses, courseCode);

                        if (c != null) {

                            System.out.println(
                                    c.getCourseCode() +
                                            " - " +
                                            c.getTitle() +
                                            " (" +
                                            c.getUnits() +
                                            " units)"
                            );

                            totalUnits += c.getUnits();

                        }

                    }

                    System.out.println("--------------------");
                    System.out.println("Total Units: " + totalUnits);

                    break;

                case 0:

                    System.out.println("Thank you for using the Liceo Enrollment System!");

                    break;

                default:

                    System.out.println("Invalid choice.");
            }
        }

        sc.close();
    }

    static Student findStudent(ArrayList<Student> list, String id) {

        for (Student s : list) {

            if (s.getStudentId().equals(id)) {
                return s;
            }

        }

        return null;
    }

    static Course findCourse(ArrayList<Course> list, String code) {

        for (Course c : list) {

            if (c.getCourseCode().equals(code)) {
                return c;
            }

        }

        return null;
    }

    static void printMenu() {

        System.out.println();
        System.out.println("========================================");
        System.out.println("      LICEO ENROLLMENT SYSTEM (CLI)");
        System.out.println("========================================");
        System.out.println("[1] Register Student");
        System.out.println("[2] Add Course Offering");
        System.out.println("[3] Enroll Student to Course");
        System.out.println("[4] View All Students");
        System.out.println("[5] View All Courses");
        System.out.println("[6] View Student Load (Courses + Total Units)");
        System.out.println("[0] Exit");
        System.out.println("----------------------------------------");
        System.out.print("Enter choice: ");
    }
}
public class Student {
    private String studentId;
    private String fullName;
    private String program;
    private int yearLevel;

    public Student(String studentId, String fullName,
                   String program, int yearLevel) {
        this.studentId = studentId;
        this.fullName = fullName;
        this.program = program;
        this.yearLevel = yearLevel;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getProgram() {
        return program;
    }

    public int getYearLevel() {
        return yearLevel;
    }

    public String describe() {
        return studentId + " | " + fullName + " | " +
                program + " | Year " + yearLevel;
    }
}
public class Course {

    private String courseCode;
    private String title;
    private int units;
    private int capacity;
    private int enrolledCount;

    public Course(String courseCode, String title,
                  int units, int capacity) {

        this.courseCode = courseCode;
        this.title = title;
        this.units = units;
        this.capacity = capacity;
        this.enrolledCount = 0;
    }

    public boolean isFull() {
        return enrolledCount >= capacity;
    }

    public void addOneEnrollee() {
        if (!isFull()) {
            enrolledCount++;
        }
    }

    public String getCourseCode() {
        return courseCode;
    }

    public String getTitle() {
        return title;
    }

    public int getUnits() {
        return units;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getEnrolledCount() {
        return enrolledCount;
    }
}
