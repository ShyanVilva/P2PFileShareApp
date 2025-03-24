package org.example;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PeerDiscovery {
    private JmDNS jmdns;
    private final List<ServiceInfo> discoveredPeers = new ArrayList<>();
    private static final String SERVICE_TYPE = "_p2pshare._tcp.local.";
    private ServiceInfo myServiceInfo;
    private String myPeerName;

    public PeerDiscovery() {
        try {
            jmdns = JmDNS.create(InetAddress.getLocalHost());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void startDiscovery() {
        jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
                // Debug print to see what's being added
                System.out.println("Service Added - Name: " + event.getName() +
                        ", My Name: " + myPeerName);

                if (!event.getName().equals(myPeerName)) {
                    jmdns.requestServiceInfo(event.getType(), event.getName(), 1000);
                } else {
                    System.out.println("Ignored self-discovery in serviceAdded: " + myPeerName);
                }
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                if (!event.getName().equals(myPeerName)) {
                    synchronized (discoveredPeers) {
                        discoveredPeers.removeIf(serviceInfo ->
                                serviceInfo.getName().equals(event.getName()));
                        System.out.println("Peer removed: " + event.getName());
                    }
                }
            }

            @Override
            public void serviceResolved(ServiceEvent event) {
                // Debug print to see what's being resolved
                System.out.println("Service Resolved - Name: " + event.getName() +
                        ", My Name: " + myPeerName);

                // Critical fix: Ensure myPeerName is set before comparing
                if (myPeerName != null && !event.getName().equals(myPeerName)) {
                    synchronized (discoveredPeers) {
                        // Check if we already have this peer
                        boolean alreadyExists = discoveredPeers.stream()
                                .anyMatch(info -> info.getName().equals(event.getName()));

                        if (!alreadyExists) {
                            discoveredPeers.add(event.getInfo());
                            ServiceInfo info = event.getInfo();
                            System.out.println("New peer discovered: " + info.getName());
                            System.out.println("  Address: " + info.getHostAddress());
                            System.out.println("  Port: " + info.getPort());
                        }
                    }
                } else {
                    System.out.println("Ignored self-discovery in serviceResolved: " + event.getName());
                }
            }
        });
        System.out.println("Started discovering peers...");
    }

    public void advertiseSelf(int port, String peerName, Map<String, String> properties) {
        try {
            // Set the peer name BEFORE registering the service
            this.myPeerName = peerName;
            System.out.println("Setting my peer name to: " + myPeerName);

            if (myServiceInfo != null) {
                jmdns.unregisterService(myServiceInfo);
            }

            myServiceInfo = ServiceInfo.create(
                    SERVICE_TYPE,
                    peerName,
                    port,
                    0,
                    0,
                    properties
            );

            jmdns.registerService(myServiceInfo);
            System.out.println("Started advertising service:");
            System.out.println("  Name: " + peerName);
            System.out.println("  Port: " + port);
            System.out.println("  Properties: " + properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopDiscovery() {
        if (myServiceInfo != null) {
            jmdns.unregisterService(myServiceInfo);
        }
        try {
            jmdns.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<ServiceInfo> getDiscoveredPeers() {
        synchronized (discoveredPeers) {
            return new ArrayList<>(discoveredPeers);
        }
    }

    // Add this method to help with debugging
    public String getMyPeerName() {
        return myPeerName;
    }
}