import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        final int MAX = 10;

        int[] id = new int[MAX];
        String[] name = new String[MAX];
        int[] age = new int[MAX];
        String[] course = new String[MAX];
        double[] grade = new double[MAX];
        boolean[] enrolled = new boolean[MAX];

        int count = 0;
        int choice;

        do {
            System.out.println("\n===== STUDENT INFORMATION SYSTEM =====");
            System.out.println("[1] Add Student");
            System.out.println("[2] View All Students");
            System.out.println("[3] Search Student by ID");
            System.out.println("[4] View Statistics");
            System.out.println("[5] Exit");
            System.out.print("Enter choice: ");

            choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:

                    if (count == MAX) {
                        System.out.println("Student list is full!");
                        break;
                    }

                    System.out.print("\nEnter Student ID: ");
                    id[count] = sc.nextInt();
                    sc.nextLine();

                    System.out.print("Enter Full Name: ");
                    name[count] = sc.nextLine();

                    do {
                        System.out.print("Enter Age: ");
                        age[count] = sc.nextInt();

                        if (age[count] <= 0) {
                            System.out.println("Age must be positive.");
                        }
                    } while (age[count] <= 0);

                    sc.nextLine();

                    System.out.print("Enter Course: ");
                    course[count] = sc.nextLine();

                    do {
                        System.out.print("Enter Grade: ");
                        grade[count] = sc.nextDouble();

                        if (grade[count] < 0 || grade[count] > 100) {
                            System.out.println("Grade must be between 0 and 100.");
                        }
                    } while (grade[count] < 0 || grade[count] > 100);

                    System.out.print("Is Enrolled (true/false): ");
                    enrolled[count] = sc.nextBoolean();

                    count++;

                    System.out.println(">> Student added successfully!");
                    break;

                case 2:

                    if (count == 0) {
                        System.out.println("\nNo student records.");
                    } else {

                        System.out.println("\n--- STUDENT RECORDS ---");
                        System.out.printf("%-5s %-15s %-5s %-8s %-8s %-15s%n",
                                "ID", "NAME", "AGE", "COURSE", "GRADE", "STANDING");

                        for (int i = 0; i < count; i++) {

                            String standing;

                            if (grade[i] >= 90) {
                                standing = "Dean's Lister";
                            } else if (grade[i] >= 75) {
                                standing = "Passed";
                            } else {
                                standing = "Failed";
                            }

                            System.out.printf("%-5d %-15s %-5d %-8s %-8.1f %-15s%n",
                                    id[i],
                                    name[i],
                                    age[i],
                                    course[i],
                                    grade[i],
                                    standing);
                        }
                    }
                    break;

                case 3:

                    if (count == 0) {
                        System.out.println("No student records.");
                        break;
                    }

                    System.out.print("Enter Student ID: ");
                    int search = sc.nextInt();

                    boolean found = false;

                    for (int i = 0; i < count; i++) {

                        if (id[i] == search) {

                            System.out.println("\nStudent Found");
                            System.out.println("ID        : " + id[i]);
                            System.out.println("Name      : " + name[i]);
                            System.out.println("Age       : " + age[i]);
                            System.out.println("Course    : " + course[i]);
                            System.out.println("Grade     : " + grade[i]);
                            System.out.println("Enrolled  : " + enrolled[i]);

                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        System.out.println("Student not found.");
                    }

                    break;

                case 4:

                    if (count == 0) {
                        System.out.println("No student records.");
                    } else {

                        double total = 0;
                        double highest = grade[0];
                        String topStudent = name[0];

                        for (int i = 0; i < count; i++) {
                            total += grade[i];

                            if (grade[i] > highest) {
                                highest = grade[i];
                                topStudent = name[i];
                            }
                        }

                        double average = total / count;

                        System.out.println("\n--- STATISTICS ---");
                        System.out.println("Total Students : " + count);
                        System.out.printf("Average Grade  : %.2f%n", average);
                        System.out.printf("Top Student    : %s (%.1f)%n", topStudent, highest);
                    }

                    break;

                case 5:
                    System.out.println("Thank you for using the Student Information System. Goodbye!");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }

        } while (choice != 5);

        sc.close();
    }
}