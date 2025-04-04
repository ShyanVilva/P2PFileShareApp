package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class SocketClient {
    private Socket socket;
    private DataInputStream dataIn;
    private PrintWriter textOut;
    private BufferedReader textIn;

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            setupStreams();

            System.out.println("Connected to server at " + host + ":" + port);
            Scanner scanner = new Scanner(System.in);


            Thread serverReader = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = textIn.readLine()) != null) {
                        if (!serverMessage.trim().isEmpty()) {
                            System.out.println(serverMessage);
                        }
                    }
                } catch (IOException e) {
                    System.out.println("Connection closed");
                }
            });
            serverReader.setDaemon(true);
            serverReader.start();

            // Handle user input
            while (true) {
                String input = scanner.nextLine().trim();
                textOut.println(input);

                if (input.equals("2")) {
                    String filename = scanner.nextLine().trim();
                    textOut.println(filename);
                    receiveFile(filename);
                    continue;
                }

                if (input.equals("3")) {
                    disconnect();
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    private void receiveFile(String filename) {
        try (FileOutputStream fileOut = new FileOutputStream("downloads/" + filename)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = dataIn.read(buffer)) != -1) {
                String chunk = new String(buffer, 0, bytesRead, "UTF-8");
                if (chunk.contains("EOF")) {
                    fileOut.write(buffer, 0, chunk.indexOf("EOF"));
                    break;
                }
                fileOut.write(buffer, 0, bytesRead);
            }
            System.out.println("File " + filename + " downloaded successfully");
        } catch (IOException e) {
            System.out.println("Error receiving file: " + e.getMessage());
        }
    }

    private void setupStreams() throws IOException {

        dataIn = new DataInputStream(socket.getInputStream());
        textOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        textIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    private void disconnect() {
        try {
            if (textIn != null) textIn.close();
            if (textOut != null) textOut.close();
            if (dataIn != null) dataIn.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
}