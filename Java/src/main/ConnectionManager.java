package main;

import javax.crypto.KeyAgreement;
import javax.crypto.spec.DHParameterSpec;
import javax.jmdns.*;
import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.*;
import java.util.ArrayList;
import java.util.List;

public class ConnectionManager {
    private static final int PORT = 3000;
    private static final String SERVICE_TYPE = "_broadcaster._tcp.local."; // Service we used
    private static final String SERVICE_NAME = "Service";
    private MessageEncryption messageEncryption;
    private List<MessageController> activeControllers = new ArrayList<>(); // To return controllers in discoverAndConnect
    
    private KeyAgreement keyAgreement;

    public ConnectionManager(MessageEncryption messageEncryption) {
        this.messageEncryption = messageEncryption;
    }

    public MessageController broadcastConnection() throws IOException {
      	/*
    	 * Method to broadcast signal, gets sicked up by python scanner
    	 * 
    	 * @param None
    	 * @return controller
    	 * */
        try (JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost())) {
            ServiceInfo serviceInfo = ServiceInfo.create(SERVICE_TYPE, SERVICE_NAME, PORT, "");
            jmdns.registerService(serviceInfo);
            System.out.println("Currently Broadcasting on port: " + PORT);

            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());
                
                // DHKE receive end
                performKeyExchangeRcv(clientSocket);
                
                // Controller
                MessageController controller = new MessageController(clientSocket, messageEncryption);
                return controller;
            } finally {
                jmdns.unregisterService(serviceInfo);
            }
        }
    }

    public MessageController discoverAndConnect() {
      	/*
    	 * Method to discover signal. Despite many many attempts I could never get this to work in any iteration of it.
    	 * The serviceRoseolved method would not work. One iteration was especially weird, it did not discover the 
    	 * java broadcaster with a second terminal opened, but if you opened a third on python and scanned, the java 
    	 * discoverer would also find it. This stopped working too after app development continued
    	 * 
    	 * @param None
    	 * @return MessageController
    	 * */
        try (JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost())) {
            jmdns.addServiceListener(SERVICE_TYPE, new ServiceListener() {
                @Override
                public void serviceResolved(ServiceEvent event) {
                    try {
                        Socket serviceSocket = connectToService(event.getInfo());

                        // DHKE Init side this is not going to be reaching as method cannot discover
                        performKeyExchangeInit(serviceSocket);

                        MessageController controller = new MessageController(serviceSocket, messageEncryption);
                        activeControllers.add(controller);  // Store the controller for later use
                    } catch (IOException e) {
                        System.err.println("Connection Failed: " + e.getMessage());
                    }
                }

                @Override
                public void serviceAdded(ServiceEvent event) {
                    jmdns.requestServiceInfo(event.getType(), event.getName(), true);
                }

                @Override
                public void serviceRemoved(ServiceEvent event) {
                    System.out.println("Service removed: " + event.getName());
                }
            });
            

            System.out.println("Service discovery running...");
            Thread.sleep(10000);  // Wait for services to be discovered
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return activeControllers.get(0);
    }
    private Socket connectToService(ServiceInfo serviceInfo) throws IOException {
      	/*
    	 * Method to Connect to service
    	 * 
    	 * @param ServiceInfo
    	 * @return socker
    	 * */
        Socket socket = new Socket(serviceInfo.getHostAddresses()[0], serviceInfo.getPort());
        System.out.println("Connected to service at " + serviceInfo.getHostAddresses()[0] + ":" + serviceInfo.getPort());
        return socket;
    }
    
    private void performKeyExchangeInit(Socket socket) throws IOException {
      	/*
    	 * Method to perform DHKE on initializing side. This method is not
    	 * used as java cannot discover.
    	 * 
    	 * Would have followed "call and response" protocol like in the python
    	 * implementation. 
    	 * Waits for p and g, then sends back as confirmation before creating
    	 * public key
    	 * 
    	 * 
    	 * @param String of file that saves file
    	 * @return void
    	 * */

    }

    // Method for key exchange reception
    private void performKeyExchangeRcv(Socket socket) throws IOException {
      	/*
    	 * Method to perform DHKE on receiving side. 
    	 * 
    	 * @param String of file that saves file
    	 * @return void
    	 * */
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            
            // Get p from python and send back
            BigInteger p = new BigInteger(in.readUTF());
            dos.writeUTF(p.toString());
            dos.flush();

            // Get g from python and send back
            BigInteger g = new BigInteger(in.readUTF());
            dos.writeUTF(g.toString());  
            dos.flush();

            initializeDHParameters(p, g);

            // get public key A from Python
            String A_v = in.readUTF();
            System.out.println("Received A's public key encoded: " + A_v);

            // Process received public key to get shared secret
            messageEncryption.processReceivedPublicKey(A_v);

            // get and send our public to give python shared secret
            String publicKeyB = messageEncryption.getDHPublicKeyEncoded();
            dos.writeUTF(publicKeyB);
            dos.flush();
            System.out.println("Java's public key sent");
        } catch (Exception e) {
            throw new IOException("Key exchange error: " + e.getMessage(), e);
        }
    }

    private void initializeDHParameters(BigInteger p, BigInteger g) throws GeneralSecurityException {
    	
        DHParameterSpec dhSpec = new DHParameterSpec(p, g);
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH");
        keyGen.initialize(dhSpec);
        KeyPair keyPair = keyGen.generateKeyPair();
        keyAgreement = KeyAgreement.getInstance("DH");
        keyAgreement.init(keyPair.getPrivate());
    }
}