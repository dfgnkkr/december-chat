package ru.flamexander.december.chat.server;

import java.sql.SQLException;
import java.util.Date;

public interface UserService {
    User getUserByLoginAndPassword(String login, String password) throws SQLException;
    User createNewUser(String login, String password) throws SQLException;
    boolean isLoginExist(String login) throws SQLException;
    void updateUnbanTime(String login, Date date) throws SQLException;
    void updateRole(String login, String role) throws SQLException;
    void deleteUser(String login) throws SQLException;
}