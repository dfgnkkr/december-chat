package ru.flamexander.december.chat.server.jdbc;

import ru.flamexander.december.chat.server.User;
import ru.flamexander.december.chat.server.UserService;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <P>Пример простого взаимодействия с реляционной базой данных через JDBC. Для упрощения материала в качестве СУБД
 * взята SQLite в файловом режиме работы
 */
public class JdbcUserService implements UserService {

    public JdbcUserService() {
        try {
            dropTable();
            createTable();
            fillTableByStatement(4);
            System.out.println(selectOperationFindAllUsers());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public User getUserByLoginAndPassword(String login, String password) throws SQLException {
        for (User u : selectOperationFindAllUsers()) {
            if (u.getLogin().equals(login) && u.getPassword().equals(password)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public User createNewUser(String login, String password, String username) throws SQLException {
        User newUser = new User(login, password, username);
        JdbcService.getStatement().executeUpdate(String.format("insert into users (login, password, username, role) values ('%s', '%s', '%s', '%s');", newUser.getLogin(), newUser.getPassword(), newUser.getUsername(), "user"));
        return newUser;
    }

    @Override
    public boolean isLoginAlreadyExist(String login) throws SQLException {
        for (User u : selectOperationFindAllUsers()) {
            if (u.getLogin().equals(login)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUsernameAlreadyExist(String username) throws SQLException {
        for (User u : selectOperationFindAllUsers()) {
            if (u.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Пример заполнения таблицы users через <code>Statement</code> в рамках одной транзакции
     * <P><B>Следует обратить внимание:</B> для подобных действий гораздо лучше подходит PreparedStatement
     *
     * @param count количество добавляемых студентов
     * @throws SQLException <code>SQLException</code> пробрасывается просто наверх (допустимо в учебном примере)
     */
    private static void fillTableByStatement(int count) throws SQLException {
        long time = System.currentTimeMillis();
        JdbcService.getConnection().setAutoCommit(false);
        JdbcService.getStatement().executeUpdate(String.format("insert into users (login, password, username, role) values ('admin', 'password', 'developer', 'admin');"));
        for (int i = 1; i <= count; i++) {
            JdbcService.getStatement().executeUpdate(String.format("insert into users (login, password, username, role) values ('%s', '%s', '%s', '%s');", "login" + i, "pass" + i, "user" + i, "user"));
        }
        JdbcService.getConnection().setAutoCommit(true);
        System.out.printf("Время выполнения: %d мс.\n", System.currentTimeMillis() - time);
    }

    /**
     * Создание таблицы users
     *
     * @throws SQLException <code>SQLException</code> пробрасывается просто наверх (допустимо в учебном примере)
     */
    private static void createTable() throws SQLException {
        JdbcService.getStatement().executeUpdate(
                "" +
                        "create table if not exists users (" +
                        "    id          integer primary key autoincrement," +
                        "    login       varchar(255)," +
                        "    password    varchar(255)," +
                        "    username    varchar(255)," +
                        "    role        varchar(255)" +
                        ")");
    }

    /**
     * <B>DROP</B> таблицы users
     *
     * @throws SQLException <code>SQLException</code> пробрасывается просто наверх (допустимо в учебном примере)
     */
    private static void dropTable() throws SQLException {
        JdbcService.getStatement().executeUpdate("drop table if exists users;");
    }

    /**
     * Получение списка студентов
     * <p>
     * <B>Следует обратить внимание:</B> В примере видно что в случае JDBC индексация столбцов начинается с 1, и существует возможность
     * обращаться к столбцам по имени
     *
     * @return неизменяемый список студентов
     * @throws SQLException <code>SQLException</code> пробрасывается просто наверх (допустимо в учебном примере)
     */
    private static List<User> selectOperationFindAllUsers() throws SQLException {
        try (ResultSet rs = JdbcService.getStatement().executeQuery("select * from users;")) {
            List<User> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new User(rs.getString("login"), rs.getString("password"), rs.getString("username"), rs.getString("role")));
            }
            return Collections.unmodifiableList(out);
        }
    }

}