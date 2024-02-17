package ru.flamexander.december.chat.server.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcService {

    private static Connection connection;
    private static Statement statement;

    public static Connection getConnection() {
        return connection;
    }

    public static Statement getStatement() {
        return statement;
    }


    /**
     * Открытие соединения с БД
     * <p>
     * <B>Следует обратить внимание:</B> При использовании старых версий драйверов требовалось вручную выполнять
     * загрузку класса драйвера через, сейчас это делать необязательно
     * Class.forName()
     *
     * @throws SQLException <code>SQLException</code> пробрасывается просто наверх (допустимо в учебном примере)
     */
    public static void connect() throws SQLException {
        // Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:db2.db");
        statement = connection.createStatement();
        // connection.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        // DatabaseMetaData dbMetaData = connection.getMetaData();
    }

    /**
     * Закрытие соединения с БД
     * <p>
     * <B>Следует обратить внимание:</B> как орагнизована защита от NullPointerException и порядок закрытия ресурсов
     */
    public static void disconnect() {
        try {
            if (statement != null) {
                statement.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
//        try {
//            if (psInsert != null) {
//                psInsert.close();
//            }
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
