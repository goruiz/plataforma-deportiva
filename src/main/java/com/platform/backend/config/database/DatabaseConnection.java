package com.platform.backend.config.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if(connection == null || connection.isClosed()){
            String host = System.getenv("DB_HOST");
            String port = System.getenv("DB_PORT");
            String database = System.getenv("DB_NAME");
            String user = System.getenv("DB_USER");
            String password = System.getenv("DB_PASSWORD");

            String url = "JDBC:postgresql://" + host + ":" + port + "/" + database;
            connection = DriverManager.getConnection(url,user,password);
        }
        return connection;

    }
}