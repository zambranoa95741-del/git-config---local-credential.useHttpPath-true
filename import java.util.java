import java.util.Scanner;

public class Main {

    // Node class
    static class Node {
        int data;
        Node next;

        Node(int data) {
            this.data = data;
            this.next = null;
        }
    }

    // Linked List class
    static class LinkedList {
        Node head;

        // Constructor
        LinkedList() {
            head = null;
        }

        // Insert at the end
        void insert(int data) {
            Node newNode = new Node(data);

            // If the list is empty
            if (head == null) {
                head = newNode;
                return;
            }

            Node current = head;

            // Find the last node
            while (current.next != null) {
                current = current.next;
            }

            // Connect last node to new node
            current.next = newNode;
        }

        // Insert at a specific position
        void insertAtPosition(int data, int position) {

            Node newNode = new Node(data);

            // Case 1: Insert at the beginning
            if (position == 1) {
                newNode.next = head;
                head = newNode;
                return;
            }

            Node current = head;

            // Move to the node before the position
            for (int i = 1; i < position - 1; i++) {

                if (current == null) {
                    System.out.println("Invalid position.");
                    return;
                }

                current = current.next;
            }

            // Check if position is valid
            if (current == null) {
                System.out.println("Invalid position.");
                return;
            }

            // Insert the new node
            newNode.next = current.next;
            current.next = newNode;
        }

        // Delete at a specific position
        void deleteAtPosition(int position) {
            // Case 1: Empty list
            if (head == null) {
                System.out.println("List is empty.");
                return;
            }

            // Case 2: Delete the head node
            if (position == 1) {
                head = head.next;
                return;
            }

            Node current = head;

            // Move to the node right before the targeted node
            for (int i = 1; current != null && i < position - 1; i++) {
                current = current.next;
            }

            // Case 3: Position is out of bounds
            if (current == null || current.next == null) {
                System.out.println("Invalid position.");
                return;
            }

            // Unlink the node at 'position'
            current.next = current.next.next;
        }

        // Display all nodes
        void display() {

            if (head == null) {
                System.out.println("The list is empty.");
                return;
            }

            Node current = head;

            while (current != null) {
                System.out.print(current.data + " -> ");
                current = current.next;
            }

            System.out.println("null");
        }
    }

    public static void main(String[] args) {

        Scanner input = new Scanner(System.in);

        LinkedList list = new LinkedList();

        // Create initial nodes
        list.insert(10);
        list.insert(20);
        list.insert(30);
        list.insert(40);

        System.out.println("Original Linked List:");
        list.display();

        // Ask user for data
        System.out.print("\nEnter data to insert: ");
        int data = input.nextInt();

        // Ask user for position
        System.out.print("Enter position to insert: ");
        int position = input.nextInt();

        // Insert data
        list.insertAtPosition(data, position);

        // Display updated list
        System.out.println("\nLinked List after insertion:");
        list.display();

        // Ask user for position to delete
        System.out.print("\nEnter position to delete: ");
        int deletePos = input.nextInt();

        // Delete node
        list.deleteAtPosition(deletePos);

        // Display updated list
        System.out.println("\nLinked List after deletion:");
        list.display();

        input.close();
    }
}