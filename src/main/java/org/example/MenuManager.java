package org.example;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class MenuManager {
    private static final Scanner scanner = new Scanner(System.in);
    private static boolean isRunning = true;

    public static void showMainMenu() {
        while (isRunning) {
            System.out.println("\n=== P2P File Share Application ===");
            System.out.println("1. Self Advertising");
            System.out.println("2. Peer Discovery");
            System.out.println("3. Exit");
            System.out.print("Enter your choice: ");

            try {
                int choice = scanner.nextInt();
                switch (choice) {
                    case 1 -> {
                        SelfAdvertising.startAdvertising();
                        TimeUnit.SECONDS.sleep(10);
                    }
                    case 2 -> {
                        PeerDiscovery.startDiscovery();
                        TimeUnit.SECONDS.sleep(10);
                    }

                    case 3 -> {
                        System.out.println("Exiting...");
                        isRunning = false;
                        System.exit(0);
                    }
                    default -> System.out.println("Invalid choice. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
                scanner.nextLine(); // clear the buffer
            }
        }
    }
}