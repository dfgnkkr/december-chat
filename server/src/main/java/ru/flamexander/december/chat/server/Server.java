package ru.flamexander.december.chat.server;

import ru.flamexander.december.chat.server.jdbc.JdbcService;
import ru.flamexander.december.chat.server.jdbc.JdbcUserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
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
        broadcastMessage("Подключился новый клиент " + clientHandler.getLogin());
        clients.add(clientHandler);
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastMessage("Отключился клиент " + clientHandler.getLogin());
    }

    public synchronized boolean isUserBusy(String username) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isAdmin(String username) {
        for (ClientHandler c : clients) {
            if (c.getLogin().equals(username)) {
                return c.getUser().isAdmin();
            }
        }
        return false;
    }

    public synchronized void sendPrivateMessage(ClientHandler sender, List<String> recipients, String message) {
        for (String recipient : recipients) {
            for (ClientHandler client : clients) {
                if (recipient.equals(client.getLogin()) || recipient.equals(client.getUser().getRole())) {
                    client.sendMessage("Пользователь " + sender.getLogin() + " шепчет: " + message);
                }
            }
        }
    }

    public synchronized void onlineList(ClientHandler sender) {
        sender.sendMessage("_______начало_списка_пользователей_онлайн_______");
        for (ClientHandler client : clients) {
            sender.sendMessage(client.getUser().toString());
        }
        sender.sendMessage("_______конец_списка_пользователей_онлайн_______");
    }

    public synchronized void removeUser(String login, String attribute) throws SQLException {
        if (userService.isLoginExist(login) && attribute.equals("-del")) {
            if (isUserBusy(login)){
                broadcastMessage("СЕРВЕР: Админ пытался удалить УЗ пользователя с ником '" + login + "'. Но не вышло. Увы.");
                return;
            }
            userService.deleteUser(login);
            broadcastMessage("СЕРВЕР: Админ удалил УЗ пользователя с ником '" + login + "'. Увы.");
        }
    }

    public synchronized boolean changeRole(List<String> logins, String roleCommand) throws SQLException {
        String command = roleCommand.substring(0, 4);
        String role = "user";
        if (command.equals("-add")) {
            String roleName = roleCommand.substring(5);
            if (roleName.equals("admin") || roleName.equals("moderator")) {
                role = roleName;
            }
        } else if (command.equals("-del")){
            role = "user"; // типо удаляем роль, если она была
        } else {
            return false;
        }
        for (String login : logins) {
            if (login.equals("admin") && command.equals("-del")){
                broadcastMessage("СЕРВЕР: nice try.");
                return false;
            }
            if (userService.isLoginExist(login)) {
                userService.updateRole(login, role);
                // вот этот костыль нужен из-за того, что у нас тут пока нет нормального управления хранимыми Entity
                // БД живет своей жизнью, а приложение - своей
                for (ClientHandler clientHandler : clients){
                    if(clientHandler.getLogin().equals(login)) {
                        clientHandler.getUser().setRole(role);
                    }
                }
            }
        }
        return true;
    }

    public synchronized void kick(List<String> logins){
        for (String login : logins) {
            kick(login);
        }
    }

    public synchronized void kick(String login){
        if (isUserBusy(login)) {
            if (clients.stream().filter(c -> c.getLogin().equals(login)).findFirst().get().getUser().isAdmin()){
                broadcastMessage("СЕРВЕР: Админ пытался кикнуть пользователя с ником '"
                        + login + "', но кикнуть Админа нельзя.");
                return;
            }
            ClientHandler clientHandler = clients.stream().filter(c -> c.getLogin().equals(login)).findFirst().get();
            broadcastMessage("СЕРВЕР: Админ кикнул пользователя с ником '" + login + "'");
            clientHandler.disconnect();
        } else {
            broadcastMessage("СЕРВЕР: Админ пытался кикнуть пользователя с ником '"
                    + login + "', но такого пользователя нет в сети.");
        }
    }

    /**
     * Забанить пользователей на указанное время.
     *
     * Пример команды: /ban @user1 @user2 -d=1
     *  пользователи user1 и user2 забанены на 1 день
     */
    public synchronized void ban(List<String> logins, String banPeriodFormula) throws SQLException {
        int multiplier;
        String predicate = banPeriodFormula.substring(0, 2);
        long banPeriod = Integer.parseInt(banPeriodFormula.substring(3));
        switch (predicate) {
            case "-d":
                multiplier = 60 * 1000 * 60 * 24;
                break;
            case "-h":
                multiplier = 60 * 1000 * 60;
                break;
            case "-m":
                multiplier = 60 * 1000;
                break;
            default:
                multiplier = 1000 * 60 * 30;
        }
        for (String login : logins) {
            ban(login, banPeriod * multiplier);
        }
    }

    public synchronized void ban(String login, long banPeriod) throws SQLException {
        // банить пользователя, который не в сети - низя
        if (isUserBusy(login)) {
            if (isAdmin(login)) {
                broadcastMessage("СЕРВЕР: Админ пытался забанить пользователя с ником '"
                        + login + "', но забанить админа нельзя.");
                return;
            }
            Date unbanTime = new Date(System.currentTimeMillis() + banPeriod);
            ClientHandler clientHandler = clients.stream().filter(c -> c.getLogin().equals(login)).findFirst().get();
            clientHandler.getUser().setUnbanTime(unbanTime);
            userService.updateUnbanTime(login, unbanTime);
            broadcastMessage("СЕРВЕР: Админ забанил пользователя с ником '" + login + " до " + unbanTime.toString());
        } else {
            broadcastMessage("СЕРВЕР: Админ пытался забанить пользователя с ником '"
                    + login + "', но такого пользователя нет в сети.");
        }
    }
}
