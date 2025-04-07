package org.example;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;

public class SocketClient {
    private Socket socket;
    private DataInputStream dataIn;
    private PrintWriter textOut;
    private BufferedReader textIn;

    public void connect(String host, int port) {
        try {
            socket = new Socket(host, port);

            // Mutual Authentication
            ECDH keyExchange = new ECDH();
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            // Send client's public key in DER format
            byte[] clientPublicKeyDER = keyExchange.getPublicKey().getEncoded();
            out.write(clientPublicKeyDER);
            out.flush();

            // Receive server's public key in DER format
            byte[] serverPublicKeyDER = new byte[1024];
            int bytesRead = in.read(serverPublicKeyDER);
            byte[] actualServerKey = Arrays.copyOf(serverPublicKeyDER, bytesRead);

            // Generate shared secret
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(actualServerKey);
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PublicKey serverPublicKey = keyFactory.generatePublic(keySpec);

            byte[] sharedSecret = keyExchange.generateSharedSecret(serverPublicKey);
            byte[] sessionKey = keyExchange.sessionKey(sharedSecret);



            // Create separate streams for text and binary
            InputStream rawIn = socket.getInputStream();
            OutputStream rawOut = socket.getOutputStream();

            // Binary stream for file downloads
            dataIn = new DataInputStream(new BufferedInputStream(rawIn));

            // Text streams for commands and menu
            textOut = new PrintWriter(new OutputStreamWriter(rawOut), true);
            textIn = new BufferedReader(new InputStreamReader(new PushbackInputStream(rawIn)));

            Scanner scanner = new Scanner(System.in);

            // Server reader thread for menu and text responses
            Thread serverReader = new Thread(() -> {
                try {
                    StringBuilder messageBuffer = new StringBuilder();
                    char[] buffer = new char[8192];
                    int charsRead;

                    while (true) {
                        // Read available data in chunks
                        if (textIn.ready()) {
                            charsRead = textIn.read(buffer);
                            if (charsRead == -1) break;

                            messageBuffer.append(buffer, 0, charsRead);

                            // If we have a complete message, print it
                            if (messageBuffer.toString().contains("\n")) {
                                System.out.print(messageBuffer);
                                messageBuffer.setLength(0);
                            }
                        }
                        Thread.sleep(50);
                    }
                } catch (IOException | InterruptedException e) {
                    if (!socket.isClosed()) {
                        System.out.println("Connection closed");
                    }
                }
            });
            serverReader.setDaemon(true);
            serverReader.start();

            // Handle user input
            while (true) {
                String input = scanner.nextLine().trim();
                textOut.println(input);

                if (input.equals("1")) {
                    receiveListFiles(in, keyExchange);
                    continue;
                }
                else if (input.equals("2")) {
                    System.out.print("Enter the filename to download: ");
                    String fileName = scanner.nextLine().trim();
                    textOut.println(fileName);
                    downloadFile(fileName, dataIn, keyExchange);
                    continue;
                }

                else if (input.equals("3")) {
                    disconnect();
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    private static void receiveListFiles(InputStream in, ECDH keyExchange) throws Exception {
        DataInputStream dataIn = new DataInputStream(in);

        // Read encrypted data length
        int encLen = dataIn.readInt();

        // Read encrypted data (includes nonce)
        byte[] encData = new byte[encLen];
        dataIn.readFully(encData);

        // Decrypt data (nonce handling is done inside decrypt method)
        byte[] decData = keyExchange.decrypt(encData);

    }

    private static void downloadFile(String fileName, InputStream in, ECDH keyExchange) throws Exception {
        try {
            DataInputStream dataIn = new DataInputStream(in);

            // Read and decrypt start marker
            int markerLen = dataIn.readInt();
            byte[] encMarker = new byte[markerLen];
            dataIn.readFully(encMarker);
            String marker = new String(keyExchange.decrypt(encMarker));

            if (!"START_BINARY_DATA".equals(marker)) {
                throw new IOException("Invalid file transfer start");
            }

            long originalSize = dataIn.readLong();

            if (originalSize == -1) {

                // Handle error case
                int errorLen = dataIn.readInt();
                byte[] encError = new byte[errorLen];
                dataIn.readFully(encError);
                System.out.println(new String(keyExchange.decrypt(encError)));

                // Read and decrypt end marker
                int endMarkerLen = dataIn.readInt();
                byte[] encEndMarker = new byte[endMarkerLen];
                dataIn.readFully(encEndMarker);
                String endMarker = new String(keyExchange.decrypt(encEndMarker));
                return;
            }

            // Read encrypted file data (includes nonce)
            int encLen = dataIn.readInt();
            byte[] encData = new byte[encLen];
            dataIn.readFully(encData);

            // Decrypt data
            byte[] decData = keyExchange.decrypt(encData);

            // Save decrypted file
            File outputDir = new File("downloads");
            outputDir.mkdirs();
            File file = new File(outputDir, fileName);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(decData);
            }

            // Read and verify end marker
            int endMarkerLen = dataIn.readInt();
            byte[] encEndMarker = new byte[endMarkerLen];
            dataIn.readFully(encEndMarker);
            String endMarker = new String(keyExchange.decrypt(encEndMarker));

            if (!"FILE_TRANSFER_COMPLETE".equals(endMarker)) {
                throw new IOException("File transfer incomplete");
            }

            System.out.println(" File downloaded: " + file.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("Error downloading file: " + e.getMessage());
            throw e;
        }
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
