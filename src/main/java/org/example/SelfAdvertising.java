package org.example;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class SelfAdvertising {
    private static JmDNS jmdns;
    private static ServiceInfo serviceInfo;
    private static Thread serviceThread;
    private static ServerSocket serverSocket;
    private static boolean isAdvertising = false;
    private static KeyPair keyPair;

    public static void setKeyPair(KeyPair kp) {
        keyPair = kp;
    }

    public static void startAdvertising() {
        if (isAdvertising) {
            System.out.println("Already advertising. Press Enter to return to menu...");
            try {
                System.in.read();
            } catch (IOException e) {
                System.out.println("Error reading input: " + e.getMessage());
            }
            return;
        }

        serviceThread = new Thread(() -> {
            try {
                jmdns = JmDNS.create(InetAddress.getByName("192.168.2.75"));
                int port = 49152 + (int) (Math.random() * (65535 - 49152 + 1));

                // Start socket server first
                serverSocket = new ServerSocket(port);
                System.out.println("Socket server started on port " + port);

                // Then register the service
                serviceInfo = ServiceInfo.create("_secureshare._tcp.local.", "Shyan", port, "path=index.html");
                jmdns.registerService(serviceInfo);
                isAdvertising = true;

                System.out.println("Registered service: " + serviceInfo.getName() + " on port " + serviceInfo.getPort());

                // Accept connections
                while (isAdvertising) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New connection from: " + clientSocket.getInetAddress());
                        // Handle connection in new thread
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            System.out.println("Error accepting connection: " + e.getMessage());
                        }
                    }
                }

            } catch (IOException e) {
                System.out.println("Error in advertising: " + e.getMessage());
            }
        });

        serviceThread.start();
    }

    private static void handleClient(Socket clientSocket) {
        Thread clientThread = new Thread(() -> {
            try (ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                 ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

                // Get their challenge
                byte[] theirChallenge = (byte[]) in.readObject();

                // Sign and send back
                byte[] signature = MutualAuthentication.signChallenge(keyPair.getPrivate(), theirChallenge);
                out.writeObject(signature);

                // Get their public key
                PublicKey theirPublicKey = (PublicKey) in.readObject();

                // Send our challenge
                byte[] ourChallenge = MutualAuthentication.generateChallenge();
                out.writeObject(ourChallenge);

                // Get their signature
                byte[] theirSignature = (byte[]) in.readObject();

                // Send our public key
                out.writeObject(keyPair.getPublic());

                if (MutualAuthentication.verifyChallengeSignature(theirPublicKey, ourChallenge, theirSignature)) {
                    // Send our service name
                    out.writeObject(serviceInfo.getName());
                    System.out.println("Authentication successful with " + clientSocket.getInetAddress());
                } else {
                    System.out.println("Authentication failed with " + clientSocket.getInetAddress());
                }

            } catch (Exception e) {
                System.out.println("Error handling client: " + e.getMessage());
            }
        });
        clientThread.start();
    }

    public static boolean isServerRunning() {
        return isAdvertising && serverSocket != null && !serverSocket.isClosed();
    }

    public static void getServerInfo() {
        if (isServerRunning()) {
            System.out.println("Server Status:");
            System.out.println("- Running: Yes");
            System.out.println("- Port: " + serverSocket.getLocalPort());
            System.out.println("- Bound Address: " + serverSocket.getInetAddress());
            if (serviceInfo != null) {
                System.out.println("- Service Name: " + serviceInfo.getName());
                System.out.println("- Service Type: " + serviceInfo.getType());
            }
        } else {
            System.out.println("Server is not running");
        }
    }

    public static void stopAdvertising() {
        isAdvertising = false;
        try {
            if (serverSocket != null) serverSocket.close();
            if (jmdns != null) {
                jmdns.unregisterAllServices();
                jmdns.close();
            }
        } catch (IOException e) {
            System.out.println("Error stopping advertising: " + e.getMessage());
        }
    }
}