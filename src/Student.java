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
