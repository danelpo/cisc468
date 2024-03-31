package main;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import java.io.IOException;
import java.net.InetAddress;

public class PeerDiscovery {

    private static class MyServiceListener implements ServiceListener {
        private final JmDNS jmdns;

        public MyServiceListener(JmDNS jmdns) {
            this.jmdns = jmdns;
        }

        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("Service added: " + event.getName());
            // Delayed resolution request
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // Delay to ensure the service is ready for resolution
                    jmdns.requestServiceInfo(event.getType(), event.getName(), true);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("Resolution thread interrupted for " + event.getName());
                }
            }).start();
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getName());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            ServiceInfo info = event.getInfo();
            System.out.println("Service resolved: " + info.getName() + " at " + info.getHostAddresses()[0] + ":" + info.getPort());
            // Additional information from the ServiceInfo can be displayed here.
        }
    }

    public static void discoverDevices() {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            System.out.println("Service discovery started. Press Enter to stop...");
            
            jmdns.addServiceListener("_example._tcp.local.", new MyServiceListener(jmdns));

            System.in.read(); // Wait for user input to proceed with stopping discovery

            jmdns.close();
            System.out.println("Service discovery stopped.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        discoverDevices();
    }
}

