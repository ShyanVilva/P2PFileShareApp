package org.example;

import java.security.*;
import java.util.Scanner;

public class Main {
    private static KeyPair keyPair;

    public static void main(String[] args) {
        try {
            // Generate key pair at startup
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            keyPair = keyGen.generateKeyPair();

            // Set the key pair in SelfAdvertising
            SelfAdvertising.setKeyPair(keyPair);

            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.println("\n=== LAN Service Menu ===");
                System.out.println("1. Start Advertising");
                System.out.println("2. Start Peer Discovery");
                System.out.println("3. Connect to Friend");
                System.out.println("4. Exit");
                System.out.print("Choose an option: ");

                String input = scanner.nextLine().trim();

                switch (input) {
                    case "1":
                        SelfAdvertising.startAdvertising();
                        sleep10Seconds();
                        break;
                    case "2":
                        PeerDiscovery.startDiscovery();
                        sleep10Seconds();
                        break;
                    case "3":
                        System.out.println("\n=== Friends List ===");
                        Friend.listFriends();
                        System.out.print("\nEnter friend's name to connect (or press Enter to go back): ");
                        String friendName = scanner.nextLine().trim();

                        if (!friendName.isEmpty()) {
                            Friend friend = Friend.getFriend(friendName);
                            if (friend != null) {
                                SocketClient client = new SocketClient(keyPair);
                                client.connect(friend.getIpAddress(), friend.getPort());
                            } else {
                                System.out.println("Friend not found.");
                            }
                        }
                        sleep10Seconds();
                        break;
                    case "4":
                        running = false;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                        sleep10Seconds();
                }
            }

            scanner.close();
            System.out.println("Program terminated.");

        } catch (NoSuchAlgorithmException e) {
            System.out.println("Error generating key pair: " + e.getMessage());
        }
    }

    private static void sleep10Seconds() {
        System.out.println("\nWaiting 10 seconds...");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}