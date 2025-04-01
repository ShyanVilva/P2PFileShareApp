package org.example;

import java.io.IOException;
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;

public class SelfAdvertising {
    private static JmDNS jmdns;
    private static ServiceInfo serviceInfo;
    private static Thread serviceThread;
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
                jmdns = JmDNS.create(InetAddress.getByName("192.168.2.250"));
                int port = 49152 + (int) (Math.random() * (65535 - 49152 + 1));

                serviceInfo = ServiceInfo.create("_http._tcp.local.", "Shyan", port, "path=index.html");
                jmdns.registerService(serviceInfo);
                isAdvertising = true;

                System.out.println("Registered service: " + serviceInfo.getName() + " on port " + serviceInfo.getPort());

            } catch (IOException e) {
                System.out.println("Error in advertising: " + e.getMessage());
            }
        });

        serviceThread.start();
    }
}