package ru.flamexander.december.chat.server;

import ru.flamexander.december.chat.server.jdbc.JdbcService;
import ru.flamexander.december.chat.server.jdbc.JdbcUserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private int port;
    private List<ClientHandler> clients;
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }

    public Server(int port) {
        this.port = port;
        this.clients = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.printf("Сервер запущен на порту %d. Ожидание подключения клиентов\n", port);
            JdbcService.connect();
            System.out.println("Сервер подключен к локальной БД");
            userService = new JdbcUserService();
            System.out.println("Запущен сервис для работы с пользователями");
            while (true) {
                Socket socket = serverSocket.accept();
                try {
                    new ClientHandler(this, socket);
                } catch (IOException e) {
                    System.out.println("Не удалось подключить клиента");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            JdbcService.disconnect();
        }
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clients) {
            clientHandler.sendMessage(message);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        broadcastMessage("Подключился новый клиент " + clientHandler.getUsername());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Отключился клиент " + clientHandler.getUsername());
    }

    public synchronized boolean isUserBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, String receiverUsername, String message) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(receiverUsername)){
                client.sendMessage("Пользователь " + sender.getUsername() + " шепчет: " + message);
            }
        }
    }

    public synchronized void kickUser(String kickedUserName){
        if (isUserBusy(kickedUserName)) {
            ClientHandler clientHandler = clients.stream().filter(c -> c.getUsername().equals(kickedUserName)).findFirst().get();
            broadcastMessage("СЕРВЕР: Админ кикнул пользователя с ником '" + kickedUserName + "'");
            clientHandler.disconnect();
        } else {
            broadcastMessage("СЕРВЕР: Админ пытался кикнуть пользователя с ником '"
                    + kickedUserName + "', но такого пользователя нет в сети.");
        }
    }
}
