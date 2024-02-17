package ru.flamexander.december.chat.server;

import java.sql.SQLException;

public interface UserService {
    User getUserByLoginAndPassword(String login, String password) throws SQLException;
    User createNewUser(String login, String password, String username) throws SQLException;
    boolean isLoginAlreadyExist(String login) throws SQLException;
    boolean isUsernameAlreadyExist(String username) throws SQLException;
}