package org.example;

import java.io.*;
import java.net.Socket;
import java.security.*;

public class SocketClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final KeyPair keyPair;

    public SocketClient(KeyPair keyPair) {
        this.keyPair = keyPair;
    }

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connected to server at " + host + ":" + port);

            if (authenticate()) {
                System.out.println("Authentication successful!");
                // Read peer info
                String name = (String) in.readObject();
                Friend.addFriend(name, host, port);
            } else {
                System.out.println("Authentication failed!");
            }

        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private boolean authenticate() throws Exception {
        // Send challenge
        byte[] challenge = MutualAuthentication.generateChallenge();
        out.writeObject(challenge);

        // Get signature and verify it
        byte[] signature = (byte[]) in.readObject();
        PublicKey theirPublicKey = (PublicKey) in.readObject();

        // Verify their response
        if (!MutualAuthentication.verifyChallengeSignature(theirPublicKey, challenge, signature)) {
            return false;
        }

        // Handle their challenge
        byte[] theirChallenge = (byte[]) in.readObject();
        byte[] ourSignature = MutualAuthentication.signChallenge(keyPair.getPrivate(), theirChallenge);
        out.writeObject(ourSignature);
        out.writeObject(keyPair.getPublic());

        return true;
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