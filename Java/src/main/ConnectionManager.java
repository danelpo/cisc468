package main;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.IOException;
import java.net.InetAddress;

public class ConnectionManager {

    public static void broadcastConnection() {
        try {
            // Preferably use a specific, non-loopback address if multicast has issues.
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

            // Simple service type and name for clear identification.
            String type = "_example._tcp.local.";
            String name = "ExampleService";
            int port = 12345;
            ServiceInfo serviceInfo = ServiceInfo.create(type, name, port, "path=index.html");

            jmdns.registerService(serviceInfo);
            System.out.println("Service broadcasting started. Press Enter to stop...");

            System.in.read(); // Wait for user to press Enter

            jmdns.unregisterService(serviceInfo);
            jmdns.close();
            System.out.println("Service broadcasting stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        broadcastConnection();
    }
}