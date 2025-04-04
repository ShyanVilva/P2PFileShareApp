package org.example;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
            try (OutputStream out = clientSocket.getOutputStream();
                 InputStream in = clientSocket.getInputStream();
                 BufferedReader textIn = new BufferedReader(new InputStreamReader(in));
                 PrintWriter textOut = new PrintWriter(new OutputStreamWriter(out), true)) {

                byte[] buffer = new byte[1024];
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                while (true) {
                    displayMenu(out);

                    int bytesRead = in.read(buffer);
                    if (bytesRead == -1) {
                        System.out.println("Client disconnected");
                        break;
                    }

                    String command = new String(buffer, 0, bytesRead, "UTF-8").trim();
                    System.out.println("Received command: '" + command + "'");

                    switch (command) {
                        case "1":
                            listFiles(out);
                            break;
                        case "2":
                            textOut.println("Please enter the filename you want to download:");
                            textOut.flush();
                            String filename = textIn.readLine().trim();
                            sendFile(out, filename);
                            break;
                        case "3":
                            return;
                        default:
                            handleInvalidOption(out);
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

    private static void sendFile(OutputStream out, String filename) {
        File file = new File("shared", filename);
        if (!file.exists() || file.isDirectory()) {
            try {
                out.write("File not found\n".getBytes("UTF-8"));
                out.flush();
            } catch (IOException e) {
                System.out.println("Error sending file not found message: " + e.getMessage());
            }
            return;
        }

        try (FileInputStream fileIn = new FileInputStream(file)) {
            out.write(("Starting download of " + filename + "\n").getBytes("UTF-8"));
            out.flush();

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();

            out.write("EOF\n".getBytes("UTF-8"));
            out.flush();
            System.out.println("File " + filename + " sent successfully");

        } catch (IOException e) {
            System.out.println("Error sending file: " + e.getMessage());
        }
    }

    private static void displayMenu(OutputStream out) throws IOException {
        String menuOptions = "\n=== Server Menu ===\n" +
                "1. List Files\n" +
                "2. Download File\n" +
                "3. Disconnect\n";
        out.write(menuOptions.getBytes("UTF-8"));
        out.flush();
    }

    private static void listFiles(OutputStream out) throws IOException {
        System.out.println("Processing list files command");
        String sharedDirectory = "shared";
        List<FileManager.FileInfo> files = FileManager.getDirectoryListing(sharedDirectory);

        StringBuilder response = new StringBuilder("Files available:\n");
        for (FileManager.FileInfo file : files) {
            response.append(file.toString()).append("\n");
        }
        response.append("End of list\n\nChoose an option: ");

        out.write(response.toString().getBytes("UTF-8"));
        out.flush();
        System.out.println("Files list sent");
    }

    private static void handleInvalidOption(OutputStream out) throws IOException {
        String error = "Invalid option.\n\nChoose an option: ";
        out.write(error.getBytes("UTF-8"));
        out.flush();
        System.out.println("Sent error response");
    }


}