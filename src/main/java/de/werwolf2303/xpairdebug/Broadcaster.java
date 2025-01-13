package de.werwolf2303.xpairdebug;

import de.werwolf2303.xpairdebug.services.LaunchService;
import de.werwolf2303.xpairdebug.wireObjects.Message;
import de.werwolf2303.xpairdebug.wireObjects.MessageType;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceInfo;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Broadcaster {
    JmDNS jmdns;
    ServiceInfo serviceInfo;
    ServerSocket tcpSocket;
    volatile boolean running = true;
    File cwd;
    File tmpDir;
    State state = State.IDLE;
    File latestJarPath = null;
    LaunchService launchService;

    public Broadcaster(
            String cwd,
            String tmpDir
    ) throws IOException {
        jmdns = JmDNS.create(InetAddress.getLocalHost());
        serviceInfo = ServiceInfo.create("_http._tcp.local.", "xpairdebug", 6970, "Windows XP Wireless Java Debugger");
        tcpSocket = new ServerSocket(6970, 0);
        if(cwd != null) {
            this.cwd = new File(cwd);
        }
        if(tmpDir != null) {
            this.tmpDir = new File(tmpDir);
        }
        launchService = new LaunchService();
        launchService.setProcessListener(new LaunchService.ProcessListener() {
            @Override
            public void onProcessStopped() {
                state = State.IDLE;
            }
        });
    }

    private void listen() {
        new Thread(() -> {
            while (running) {
                Socket socket;

                try {
                    socket = tcpSocket.accept();
                    socket.setSoTimeout(6000);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                while(socket.isConnected()) {
                    try {
                        Message message = (Message) new ObjectInputStream(socket.getInputStream()).readObject();
                        Message response;

                        System.out.println(message.getType());

                        switch (message.getType()) {
                            case PING:
                                response = new Message(MessageType.PONG);
                                new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
                                break;
                            case LAUNCH:
                                launchService.launch(cwd, latestJarPath.getAbsolutePath(), (String) message.get("jvmArgs"), (String) message.get("progArgs"));
                                response = new Message(MessageType.LAUNCHED);
                                new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
                                state = State.LAUNCHED;
                                socket.close();
                                break;
                            case STOP:
                                launchService.stop();
                                response = new Message(MessageType.STOPPED);
                                new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
                                state = State.IDLE;
                                break;
                            case RECEIVE:
                                latestJarPath = new File(tmpDir, (String) message.get("filename"));
                                Files.write(Paths.get(latestJarPath.getAbsolutePath()), (byte[]) message.get("data"));
                                response = new Message(MessageType.RECEIVED);
                                new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
                                break;
                            case GETSTATE:
                                response = new Message(MessageType.RETURNSTATE);
                                response.put("state", state);
                                new ObjectOutputStream(socket.getOutputStream()).writeObject(response);
                        }
                    } catch (SocketTimeoutException | SocketException ignored) {
                        break;
                    } catch (Exception e) {
                        try {
                            socket.close();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();
    }

    public void broadcast() throws IOException {
        jmdns.registerService(serviceInfo);
        listen();
        System.out.println("Broadcasting");
    }

    public void stop() {
        jmdns.unregisterAllServices();
        running = false;
    }
}
