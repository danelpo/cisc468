package main;
import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionManager {
    private List<Socket> clients = new ArrayList<>();

    public static void main(String[] args) {
        ConnectionManager manager = new ConnectionManager();
        manager.BroadcastConnection();
    }

    public void BroadcastConnection() {
        try {
            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());
            String type = "_broadcaster._tcp.local.";
            String name = "ExampleService._example._tcp.local.";
            int port = 3000;
            
            ServiceInfo serviceInfo = ServiceInfo.create(type, name, port, "path=index.html");
            jmdns.registerService(serviceInfo);
            System.out.println("Currently Broadcasting...");

            try (ServerSocket sock = new ServerSocket(port)) {
                while (true) {
                    Socket clientSock = sock.accept();
                    System.out.println("Connection from: " + clientSock.getRemoteSocketAddress());
                    clients.add(clientSock);
                    new Thread(() -> handleClientConnection(clientSock)).start();
                }
            } finally {
                jmdns.unregisterService(serviceInfo);
                jmdns.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void handleClientConnection(Socket clientSock) {
    	
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSock.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received: " + line);
            }
        } catch (IOException e) {
            System.out.println("Closing connection with: " + clientSock.getRemoteSocketAddress());
        } finally {
            clients.remove(clientSock);
            try {
                clientSock.close();
            } catch (IOException e) {
            }
        }
    }
}