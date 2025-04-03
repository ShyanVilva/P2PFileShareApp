package org.example;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        while (running) {
            System.out.println("\n=== LAN Service Menu ===");
            System.out.println("1. Start Advertising");
            System.out.println("2. Start Peer Discovery");
            System.out.println("3. Connect to Friend");
            System.out.println("4. Check Server Status");
            System.out.println("5. Exit");
            System.out.print("Choose an option: ");

            String input = scanner.nextLine().trim();

            try {
                switch (input) {
                    case "1":
                        SelfAdvertising.startAdvertising();
                        Thread.sleep(5000);
                        break;
                    case "2":
                        PeerDiscovery.startDiscovery();
                        Thread.sleep(5000);
                        break;
                    case "3":
                        System.out.println("\n=== Friends List ===");
                        Friend.listFriends();
                        System.out.print("\nEnter friend's name to connect (or press Enter to go back): ");
                        String friendName = scanner.nextLine().trim();

                        if (!friendName.isEmpty()) {
                            Friend friend = Friend.getFriend(friendName);
                            if (friend != null) {
                                SocketClient client = new SocketClient();
                                client.connect(friend.getIpAddress(), friend.getPort());
                            } else {
                                System.out.println("Friend not found.");
                            }
                        }
                        break;
                    case "4":
                        SelfAdvertising.getServerInfo();
                        Thread.sleep(5000);
                        break;
                    case "5":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            } catch (InterruptedException e) {
                System.out.println("Operation interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        scanner.close();
        System.out.println("Program terminated.");
    }
}