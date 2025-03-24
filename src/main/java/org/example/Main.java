package org.example;

import javax.jmdns.ServiceInfo;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        try {
            Map<String, String> properties = new HashMap<>();
            properties.put("version", "1.0");

            // First peer - only advertises
            System.out.println("\n=== Starting Peer 1 (Advertiser) ===");
            PeerDiscovery peer1 = new PeerDiscovery();
            // Note: Not calling startDiscovery() for peer1
            peer1.advertiseSelf(8081, "Peer1", properties);
            System.out.println("Peer1 is now advertising on port 8081");

            // Wait before starting second peer
            Thread.sleep(2000);

            // Second peer - only discovers
            System.out.println("\n=== Starting Peer 2 (Discoverer) ===");
            PeerDiscovery peer2 = new PeerDiscovery();
            peer2.startDiscovery();
            System.out.println("Peer2 is now searching for other peers...");

            // Wait for discovery
            Thread.sleep(2000);

            // Print only Peer 2's discovered peers
            System.out.println("\n=== Peer 2's discovered peers ===");
            for (ServiceInfo info : peer2.getDiscoveredPeers()) {
                printPeerInfo(info);
            }

            System.out.println("\nPress Enter to exit...");
            System.in.read();

            // Cleanup
            peer1.stopDiscovery(); // This will unregister the service
            peer2.stopDiscovery();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printPeerInfo(ServiceInfo info) {
        System.out.println("Peer Name: " + info.getName());
        System.out.println("  Address: " + info.getHostAddress());
        System.out.println("  Port: " + info.getPort());
    }
}