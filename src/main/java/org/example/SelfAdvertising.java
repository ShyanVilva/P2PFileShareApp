package org.example;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.security.PublicKey;
import java.io.DataOutputStream;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

public class SelfAdvertising {
    private static JmDNS jmdns;
    private static ServiceInfo serviceInfo;
    private static Thread serviceThread;
    private static ServerSocket serverSocket;
    private static boolean isAdvertising = false;
    private static ECDH keyExchange;

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

                // Mutual Authentication
                ECDH keyExchange = new ECDH();
                OutputStream out = clientSocket.getOutputStream();
                InputStream in = clientSocket.getInputStream();

                // Send server's public key in DER format
                byte[] serverPublicKeyDER = keyExchange.getPublicKey().getEncoded();
                out.write(serverPublicKeyDER);
                out.flush();

                // Receive client's public key in DER format
                byte[] clientPublicKeyDER = new byte[1024];
                int bytesRead = in.read(clientPublicKeyDER);
                byte[] actualClientKey = Arrays.copyOf(clientPublicKeyDER, bytesRead);

                // Generate shared secret
                X509EncodedKeySpec keySpec = new X509EncodedKeySpec(actualClientKey);
                KeyFactory keyFactory = KeyFactory.getInstance("EC");
                PublicKey clientPublicKey = keyFactory.generatePublic(keySpec);

                byte[] sharedSecret = keyExchange.generateSharedSecret(clientPublicKey);
                byte[] sessionKey = keyExchange.sessionKey(sharedSecret);




                // Setup regular communication streams
                BufferedReader textIn = new BufferedReader(new InputStreamReader(in));
                PrintWriter textOut = new PrintWriter(new OutputStreamWriter(out), true);

                byte[] buffer = new byte[1024];
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                while (true) {
                    displayMenu(out);

                    bytesRead = in.read(buffer);
                    if (bytesRead == -1) {
                        System.out.println("Client disconnected");
                        break;
                    }

                    String command = new String(buffer, 0, bytesRead, "UTF-8").trim();
                    System.out.println("Received command: '" + command + "'");

                    switch (command) {
                        case "1":
                            listFiles(out, keyExchange);
                            break;
                        case "2":
                            byte[] fileNameBuffer = new byte[1024];
                            int fileNameBytes = in.read(fileNameBuffer);
                            String fileName = new String(fileNameBuffer, 0, fileNameBytes, "UTF-8").trim();
                            sendFile(fileName, out, keyExchange);
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


    private static void displayMenu(OutputStream out) throws IOException {
        String menuOptions = "\n=== Server Menu ===\n" +
                "1. List Files\n" +
                "2. Download File\n" +
                "3. Disconnect\n"
                + "Choose an option: ";
        out.write(menuOptions.getBytes("UTF-8"));
        out.flush();
    }

    private static void listFiles(OutputStream out, ECDH keyExchange) throws Exception {
        System.out.println("\nFunction to send list of files called");
        String sharedDirectory = "shared";
        List<FileManager.FileInfo> files = FileManager.getDirectoryListing(sharedDirectory);

        StringBuilder response = new StringBuilder("Files available:\n");
        for (FileManager.FileInfo file : files) {
            response.append(file.toString()).append("\n");
        }

        byte[] originalData = response.toString().getBytes("UTF-8");
        byte[] encryptedData = keyExchange.encrypt(originalData);

        // Send length followed by encrypted data
        DataOutputStream dataOut = new DataOutputStream(out);
        dataOut.writeInt(encryptedData.length);
        dataOut.write(encryptedData);
        dataOut.flush();

        System.out.println("Files list sent to the client");
    }

    private static void sendFile(String fileName, OutputStream out, ECDH keyExchange) throws Exception {
        String filePath = "shared/" + fileName;
        File file = new File(filePath);

        DataOutputStream fileOut = new DataOutputStream(new BufferedOutputStream(out));

        if (!file.exists()) {
            // Encrypt and send error message
            byte[] startMarker = keyExchange.encrypt("START_BINARY_DATA".getBytes());
            fileOut.writeInt(startMarker.length);
            fileOut.write(startMarker);
            fileOut.writeLong(-1);

            byte[] errorMsg = keyExchange.encrypt("ERROR: File not found".getBytes());
            fileOut.writeInt(errorMsg.length);
            fileOut.write(errorMsg);

            byte[] endMarker = keyExchange.encrypt("FILE_TRANSFER_COMPLETE".getBytes());
            fileOut.writeInt(endMarker.length);
            fileOut.write(endMarker);
            fileOut.flush();
            return;
        }

        // Encrypt and send file data
        byte[] fileData = FileManager.getFileData(filePath);
        byte[] encryptedData = keyExchange.encrypt(fileData);

        // Send start marker
        byte[] startMarker = keyExchange.encrypt("START_BINARY_DATA".getBytes());
        fileOut.writeInt(startMarker.length);
        fileOut.write(startMarker);

        // Send file size and encrypted data
        fileOut.writeLong(fileData.length);
        fileOut.writeInt(encryptedData.length);
        fileOut.write(encryptedData);

        // Send end marker
        byte[] endMarker = keyExchange.encrypt("FILE_TRANSFER_COMPLETE".getBytes());
        fileOut.writeInt(endMarker.length);
        fileOut.write(endMarker);
        fileOut.flush();
    }

    private static void handleInvalidOption(OutputStream out) throws IOException {
        String error = "Invalid option.\n\nChoose an option: ";
        out.write(error.getBytes("UTF-8"));
        out.flush();
        System.out.println("Sent error response");
    }


}
