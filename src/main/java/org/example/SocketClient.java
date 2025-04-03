package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class SocketClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            System.out.println("Connected to server at " + host + ":" + port);

            // Command loop
            boolean connected = true;
            Scanner scanner = new Scanner(System.in);

            // Start a separate thread for reading server messages
            Thread serverReader = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    if (socket != null && !socket.isClosed()) {
                        System.out.println("Lost connection to server: " + e.getMessage());
                    }
                }
            });
            serverReader.setDaemon(true);
            serverReader.start();

            // Main loop for user input
            while (connected) {
                String input = scanner.nextLine().trim();
                out.println(input);

                if (input.equals("2")) {
                    connected = false;
                }
            }

        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    private void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
}