package de.werwolf2303.xpairdebug;

import de.werwolf2303.xpairdebug.services.LaunchService;
import de.werwolf2303.xpairdebug.wireObjects.Message;
import de.werwolf2303.xpairdebug.wireObjects.MessageType;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class Receiver {
    JmDNS jmdns;
    final CompletableFuture<Boolean> ready;
    String jvmArgs = "";
    String progArgs = "";
    String filePath = null;
    Socket socket;
    Message response;
    Message received;
    InetAddress ip;
    int port;

    private class MDNSListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent serviceEvent) {}

        @Override
        public void serviceRemoved(ServiceEvent serviceEvent) {}

        @Override
        public void serviceResolved(ServiceEvent event) {
            if(event.getName().startsWith("xpairdebug")) {
                try {
                    socket = new Socket(event.getInfo().getInetAddresses()[0], event.getInfo().getPort());
                    ip = event.getInfo().getInetAddresses()[0];
                    port = event.getInfo().getPort();

                    Message message = new Message(MessageType.PING);
                    new ObjectOutputStream(socket.getOutputStream()).writeObject(message);

                    Message received = (Message) new ObjectInputStream(socket.getInputStream()).readObject();

                    if(received.getType() == MessageType.PONG) {
                        jmdns.removeServiceListener("_http._tcp.local.", this);
                        System.out.println("Ready to send commands");
                        ready.complete(true);
                    }
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public Receiver(String jvmArgs, String progArgs, String filePath) throws IOException {
        jmdns = JmDNS.create(InetAddress.getLocalHost());
        ready = new CompletableFuture<>();
        if(jvmArgs != null) {
            this.jvmArgs = jvmArgs;
        }
        if(progArgs != null) {
            this.progArgs = progArgs;
        }
        if(filePath == null) {
            System.err.println("No file path provided!");
            System.exit(-1);
        }else {
            this.filePath = filePath;
        }
    }

    void ready() throws IOException {

        try {
            response = new Message(MessageType.GETSTATE);
            new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
            received = (Message) new ObjectInputStream(socket.getInputStream()).readObject();
            if (received.getType() == MessageType.RETURNSTATE) {
                State state = (State) received.get("state");
                if (state == State.IDLE) {
                    sendReceive();
                } else {
                    sendStop();
                }
            } else {
                socket.close();
                System.err.println("Received wrong message! Type: " + received.getType());
                System.exit(-1);
            }
        }catch (StreamCorruptedException e) {
            socket.close();
            retry();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void retry() {
        try {
            socket = new Socket(ip, port);

            Message message = new Message(MessageType.PING);
            new ObjectOutputStream(socket.getOutputStream()).writeObject(message);

            Message received = (Message) new ObjectInputStream(socket.getInputStream()).readObject();

            if(received.getType() == MessageType.PONG) {
                ready();
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    void sendStop() throws StreamCorruptedException, Exception {
        response = new Message(MessageType.STOP);
        new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
        received = (Message) new ObjectInputStream(socket.getInputStream()).readObject();
        if(received.getType() == MessageType.STOPPED) {
            sendReceive();
        } else {
            System.err.println("Received wrong message! Type: " + received.getType());
            System.exit(-1);
        }
    }

    void sendReceive() throws StreamCorruptedException, Exception {
        response = new Message(MessageType.RECEIVE);
        response.put("filename", new File(filePath).getName());
        response.put("data", Files.readAllBytes(Paths.get(filePath)));
        new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
        received = (Message) new ObjectInputStream(socket.getInputStream()).readObject();
        if(received.getType() == MessageType.RECEIVED) {
            sendLaunch();
        } else {
            System.err.println("Received wrong message! Type: " + received.getType());
            System.exit(-1);
        }
    }

    void sendLaunch() throws Exception {
        response = new Message(MessageType.LAUNCH);
        response.put("jvmArgs", jvmArgs);
        response.put("progArgs", progArgs);
        new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
        received = (Message) new ObjectInputStream(socket.getInputStream()).readObject();
        if(received.getType() != MessageType.LAUNCHED) {
            System.err.println("Received wrong message! Type: " + received.getType());
            System.exit(-1);
        }
    }

    public void receive() throws IOException {
        jmdns.addServiceListener("_http._tcp.local.", new MDNSListener());
        System.out.println("Receiving");

        try {
            ready.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ready();
        socket.close();
        System.exit(0);
    }
}
