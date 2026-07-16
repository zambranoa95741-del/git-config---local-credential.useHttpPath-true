import java.util.ArrayList;
import java.util.Scanner;

class GradeTracker {

    static double[] cutoffs = {90, 80, 70, 60};
    static char[] letters = {'A', 'B', 'C', 'D'};

    static char letterFor(double grade) {
        for (int i = 0; i < cutoffs.length; i++) {
            if (grade >= cutoffs[i]) {
                return letters[i];
            }
        }
        return 'F';
    }
    
    static class Student {
        String name;
        double grade;

        Student(String name, double grade) {
            this.name = name;
            this.grade = grade;
        }
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        ArrayList<Student> roster = new ArrayList<>();

        boolean running = true;

        while (running) {

            System.out.println("\n===== GRADE TRACKER =====");
            System.out.println("1. Add Student");
            System.out.println("2. View All Students");
            System.out.println("3. Class Average");
            System.out.println("4. Exit");
            System.out.print("Choice: ");

            int choice = sc.nextInt();

            switch (choice) {

                case 1:
                    System.out.print("Name: ");
                    String name = sc.next();

                    System.out.print("Grade (0-100): ");
                    double grade = sc.nextDouble();

                    if (grade < 0 || grade > 100) {
                        System.out.println("Invalid grade. Enter a value from 0 to 100.");
                    } else {
                        roster.add(new Student(name, grade));
                        System.out.println("Added " + name + " (" + grade + ") " + letterFor(grade));
                    }
                    break;

                case 2:
                    if (roster.isEmpty()) {
                        System.out.println("No students yet.");
                    } else {
                        System.out.println("\nStudent List:");
                        for (Student s : roster) {
                            System.out.println(s.name + " - " + s.grade + " - " + letterFor(s.grade));
                        }
                    }
                    break;

                case 3:
                    if (roster.isEmpty()) {
                        System.out.println("No students yet.");
                    } else {
                        double total = 0;

                        for (Student s : roster) {
                            total += s.grade;
                        }

                        double average = total / roster.size();

                        System.out.printf("Class Average: %.2f (%c)%n",
                                average, letterFor(average));
                    }
                    break;

                case 4:
                    running = false;
                    System.out.println("Goodbye!");
                    break;

                default:
                    System.out.println("Invalid choice. Pick 1-4.");
            }
        }

        sc.close();
    }
}