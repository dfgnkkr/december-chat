package ru.flamexander.december.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * сейчас там есть фича авторизации по данным из БД, регистрация новых УЗ с сохраниением в БД, условные права "юзер" и "админ".
 *
 * можно допилить:
 * - фича "бан" (запрет читать/писать сообщения в течении Х секунд)
 * - расширение ролей (добавление модераторов, наделение ролью "модератор" с помощью консольной команды)
 * - фича "удаление учетной записи"
 * - фича "шепнуть нескольким участникам"
 */
public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private User user;

    public String getLogin() {
        return user.getLogin();
    }

    public User getUser() {
        return user;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        new Thread(() -> {
            try {
                authentication();
                listenUserChatMessages();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    private void listenUserChatMessages() throws IOException, SQLException {
        boolean isExitCommand = false;
        while (!isExitCommand) {
            String rawMessage = in.readUTF();
            if (rawMessage.startsWith("/")) {
                isExitCommand = recognizeCommandMessage(rawMessage);
            } else if (user.isBanned()){
                sendMessage("СЕРВЕР: вас забанили и вы не можете писать сообщения в чат до " + user.getUnbanTime().toString());
            } else {
                server.broadcastMessage(getLogin() + ": " + rawMessage);
            }
        }
    }

    private boolean recognizeCommandMessage(String rawCommandMessage) throws SQLException {
        String[] elements = rawCommandMessage.split(" ");
        String command = "";
        List<String> users = new ArrayList<>();
        List<String> attributes = new ArrayList<>();
        StringBuilder message = new StringBuilder();
        for (String element : elements) {
            if (element.startsWith("/")) {
                command = element;
                continue;
            }
            if (element.startsWith("@")) {
                users.add(element.replace("@", ""));
                continue;
            }
            if (element.startsWith("-")) {
                attributes.add(element);
                continue;
            }
            message.append(element).append(" ");
        }
        switch (command){
            case("/exit"):
                return true;
            case("/w"):
                server.sendPrivateMessage(this, users, message.toString());
                break;
            case("/kick"):
                if (user.isAdmin()) {
                    server.kick(users);
                } else {
                    sendMessage("СЕРВЕР: у вас нет прав на выполнение команды " + command);
                }
                break;
            case("/ban"):
                if (user.isAdmin() || user.isModerator()) {
                    server.ban(users, attributes.get(0));
                } else {
                    sendMessage("СЕРВЕР: у вас нет прав на выполнение команды " + command);
                }
                break;
            case("/help"):
                sendMessage("_______начало_справки_______");
                sendMessage("/exit - отключиться");
                sendMessage("/w @login text - написать приватное сообщение пользователю(лям)");
                sendMessage("/ban @login -d=1 - забанить пользователя(лей) на указанное число дней/минут/часов");
                sendMessage("/kick @login - отключить пользователя(лей) от сервера");
                sendMessage("/help - вызов справки");
                sendMessage("/role @login -add=moderator - выдать пользователю указанную роль");
                sendMessage("/role @login -del - разжаловать пользователя(лей) до обычного юзера (низя разжаловать admin)");
                sendMessage("/user @login -del - удалить УЗ пользователя (только если он не подключен)");
                sendMessage("/online - список подключенных пользователей");
                sendMessage("_______конец_справки_______");
                break;
            case("/role"):
                if (user.isAdmin()) {
                    if (!server.changeRole(users, attributes.get(0))) {
                        sendMessage("СЕРВЕР: неизвестная команда " + command + " " + attributes.get(0));
                    }
                } else {
                    sendMessage("СЕРВЕР: у вас нет прав на выполнение команды " + command);
                }
                break;
            case("/user"):
                server.removeUser(users.get(0), attributes.get(0));
                break;
            case("/online"):
                server.onlineList(this);
                break;
        }
        return false;
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

    private boolean tryToAuthenticate(String message) throws SQLException {
        String[] elements = message.split(" "); // /auth login1 pass1
        if (elements.length != 3) {
            sendMessage("СЕРВЕР: некорректная команда аутентификации");
            return false;
        }
        String login = elements[1];
        String password = elements[2];
        User userFromUserService = server.getUserService().getUserByLoginAndPassword(login, password);
        if (userFromUserService == null) {
            sendMessage("СЕРВЕР: пользователя с указанным логин/паролем не существует");
            return false;
        }
        if (server.isUserBusy(userFromUserService.getLogin())) {
            sendMessage("СЕРВЕР: учетная запись уже занята");
            return false;
        }
        user = userFromUserService;
        server.subscribe(this);
        sendMessage("/authok " + getLogin());
        sendMessage("СЕРВЕР: " + getLogin() + ", добро пожаловать в чат!");
        return true;
    }

    private boolean register(String message) throws SQLException {
        String[] elements = message.split(" "); // /auth login1 pass1 user1
        if (elements.length != 3) {
            sendMessage("СЕРВЕР: некорректная команда аутентификации");
            return false;
        }
        String login = elements[1];
        String password = elements[2];
        if (server.getUserService().isLoginExist(login)) {
            sendMessage("СЕРВЕР: указанный login уже занят");
            return false;
        }
        user = server.getUserService().createNewUser(login, password);
        sendMessage("/authok " + getLogin());
        sendMessage("СЕРВЕР: " + getLogin() + ", вы успешно прошли регистрацию, добро пожаловать в чат!");
        server.subscribe(this);
        return true;
    }

    private void authentication() throws IOException, SQLException {
        while (true) {
            String message = in.readUTF();
            boolean isSucceed = false;
            if (message.startsWith("/auth ")) {
                isSucceed = tryToAuthenticate(message);
            } else if (message.startsWith("/register ")) {
                isSucceed = register(message);
            } else {
                sendMessage("СЕРВЕР: требуется войти в учетную запись или зарегистрироваться");
            }
            if (isSucceed) {
                break;
            }
        }
    }
}