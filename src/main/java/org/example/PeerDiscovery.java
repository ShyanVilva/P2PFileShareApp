package org.example;

import java.io.IOException;
import java.net.InetAddress;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;

public class PeerDiscovery {
    private static JmDNS jmdns;
    private static Thread discoveryThread;
    private static boolean isDiscovering = false;

    private static class SampleListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            // Required by interface but left empty
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.printf("%s disconnected%n",
                    event.getName());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            String name = event.getName();
            String ip = event.getInfo().getInetAddresses()[0].getHostAddress();
            int port = event.getInfo().getPort();

            Friend.addFriend(name, ip, port);
            System.out.printf("%s connected from IP %s on port %d%n",
                    name, ip, port);
        }
    }

    public static void startDiscovery() {
        if (isDiscovering) {
            System.out.println("Already discovering. Press Enter to return to menu...");
            try {
                System.in.read();
            } catch (IOException e) {
                System.out.println("Error reading input: " + e.getMessage());
            }
            return;
        }

        discoveryThread = new Thread(() -> {
            try {
                jmdns = JmDNS.create(InetAddress.getByName("192.168.2.75"));
                jmdns.addServiceListener("_secureshare._tcp.local.", new SampleListener());
                isDiscovering = true;


            } catch (Exception e) {
                System.out.println("Error in peer discovery: " + e.getMessage());
            }
        });

        discoveryThread.start();
    }
}