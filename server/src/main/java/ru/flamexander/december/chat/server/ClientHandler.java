package ru.flamexander.december.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String username;

    private static int clientsCount = 0;

    public String getUsername() {
        return username;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        clientsCount++;
        this.username = "user" + clientsCount;
        new Thread(() -> {
            try {
                while (true) {
                    String rawMessage = in.readUTF();
                    if (rawMessage.startsWith("/")) {
                        if (rawMessage.equals("/exit")) {
                            break;
                        } else if (rawMessage.startsWith("/w ")) {
                            String[] elements = rawMessage.split(" ", 3);
                            String recipient = elements[1];
                            String message = elements[2];
                            server.sendPrivateMessage(this, recipient, message);
                        }
                    } else {
                        server.broadcastMessage(username + ": " + rawMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
