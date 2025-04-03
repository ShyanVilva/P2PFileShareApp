package org.example;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.util.List;

public class SelfAdvertising {
    private static JmDNS jmdns;
    private static ServiceInfo serviceInfo;
    private static Thread serviceThread;
    private static ServerSocket serverSocket;
    private static boolean isAdvertising = false;

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
            try {
                OutputStream out = clientSocket.getOutputStream();
                InputStream in = clientSocket.getInputStream();
                byte[] buffer = new byte[1024];

                System.out.println("Client connected: " + clientSocket.getInetAddress());

                while (true) {
                    // Send menu
                    String menu = "\n=== Server Menu ===\n" +
                            "1. List Files\n" +
                            "2. Disconnect\n" +
                            "Choose an option: ";
                    out.write(menu.getBytes("UTF-8"));
                    out.flush();
                    System.out.println("Menu sent to client");

                    // Read client's choice
                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) {
                        System.out.println("Client disconnected");
                        break;
                    }

                    String command = new String(buffer, 0, bytesRead, "UTF-8").trim();
                    System.out.println("Received command: '" + command + "'");

                    switch (command) {
                        case "1":
                            System.out.println("Processing list files command");
                            String sharedDirectory = "shared";
                            List<FileManager.FileInfo> files = FileManager.getDirectoryListing(sharedDirectory);

                            StringBuilder response = new StringBuilder("Files available:\n");
                            for (FileManager.FileInfo file : files) {
                                response.append(file.toString()).append("\n");
                            }
                            response.append("End of list\n");

                            out.write(response.toString().getBytes("UTF-8"));
                            out.flush();
                            System.out.println("Files list sent");
                            break;

                        case "2":
                            System.out.println("Client requested disconnect");
                            return;

                        default:
                            String error = "Invalid option.\n";
                            out.write(error.getBytes("UTF-8"));
                            out.flush();
                            System.out.println("Sent error response");
                            break;
                    }
                }

            } catch (Exception e) {
                System.out.println("Error handling client: " + e.getMessage());
                e.printStackTrace();
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