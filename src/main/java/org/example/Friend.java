package org.example;

import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;

public class Friend {
    private String name;
    private String ipAddress;
    private int port;
    private PublicKey publicKey;
    private static final ConcurrentHashMap<String, Friend> friends = new ConcurrentHashMap<>();

    public Friend(String name, String ipAddress, int port) {
        this.name = name;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public static void addFriend(String name, String ipAddress, int port) {
        friends.put(name, new Friend(name, ipAddress, port));
        System.out.println("Added friend: " + name);
    }

    public static Friend getFriend(String name) {
        return friends.get(name);
    }

    public static void listFriends() {
        if (friends.isEmpty()) {
            System.out.println("No friends discovered yet.");
            return;
        }
        System.out.println("\nDiscovered Friends:");
        friends.forEach((name, friend) ->
                System.out.printf("%s - %s:%d%n",
                        name, friend.ipAddress, friend.port));
    }

    // Getters
    public String getName() { return name; }
    public String getIpAddress() { return ipAddress; }
    public int getPort() { return port; }
}