import java.util.ArrayList;
import java.util.Scanner;

class Book {
    private String title;
    private String author;
    private boolean borrowed;

    public Book(String title, String author) {
        this.title = title;
        this.author = author;
        borrowed = false;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public boolean isBorrowed() {
        return borrowed;
    }

    public void borrow() {
        borrowed = true;
    }

    public void returnBook() {
        borrowed = false;
    }

    public String toString() {
        if (borrowed) {
            return title + " by " + author + " [Borrowed]";
        } else {
            return title + " by " + author + " [Available]";
        }
    }
}


class Library {
    private ArrayList<Book> bookList;

    public Library() {
        bookList = new ArrayList<>();
    }

    public void addBook(Book book) {
        bookList.add(book);
        System.out.println("Book added successfully.");
    }

    public void listBooks() {
        System.out.println("--- Library Catalog ---");

        if (bookList.size() == 0) {
            System.out.println("No books found.");
        }

        for (int i = 0; i < bookList.size(); i++) {
            System.out.println((i + 1) + ". " + bookList.get(i));
        }
    }

    public void borrowBook(String title) {
        for (Book book : bookList) {
            if (book.getTitle().equalsIgnoreCase(title)) {

                if (!book.isBorrowed()) {
                    book.borrow();
                    System.out.println("You borrowed " + title + ".");
                } else {
                    System.out.println("Book is already borrowed.");
                }

                return;
            }
        }

        System.out.println("Book not found.");
    }

    public void returnBook(String title) {
        for (Book book : bookList) {
            if (book.getTitle().equalsIgnoreCase(title)) {

                if (book.isBorrowed()) {
                    book.returnBook();
                    System.out.println("You returned " + title + ".");
                } else {
                    System.out.println("Book is already available.");
                }

                return;
            }
        }

        System.out.println("Book not found.");
    }

    public void searchBook(String title) {
        for (Book book : bookList) {
            if (book.getTitle().equalsIgnoreCase(title)) {
                System.out.println("Book found: " + book);
                return;
            }
        }

        System.out.println("Book does not exist.");
    }
}


public class Main {
    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);
        Library library = new Library();

        int choice;

        do {
            System.out.println("\n===== LIBRARY INFORMATION SYSTEM =====");
            System.out.println("1. Add a book");
            System.out.println("2. List all books");
            System.out.println("3. Borrow a book");
            System.out.println("4. Return a book");
            System.out.println("5. Search a book");
            System.out.println("0. Exit");

            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine();

            if (choice == 1) {

                System.out.print("Enter title: ");
                String title = scanner.nextLine();

                System.out.print("Enter author: ");
                String author = scanner.nextLine();

                library.addBook(new Book(title, author));

            } else if (choice == 2) {

                library.listBooks();

            } else if (choice == 3) {

                System.out.print("Enter title to borrow: ");
                library.borrowBook(scanner.nextLine());

            } else if (choice == 4) {

                System.out.print("Enter title to return: ");
                library.returnBook(scanner.nextLine());

            } else if (choice == 5) {

                System.out.print("Enter title to search: ");
                library.searchBook(scanner.nextLine());

            } else if (choice == 0) {

                System.out.println("Goodbye!");

            } else {

                System.out.println("Invalid choice.");

            }

        } while (choice != 0);

        scanner.close();
    }
}
