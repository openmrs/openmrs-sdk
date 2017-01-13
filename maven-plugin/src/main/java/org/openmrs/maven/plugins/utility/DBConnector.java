package org.openmrs.maven.plugins.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnector {
    Connection conn;
    String dbName;

    public DBConnector(String url, String user, String pass, String dbName) throws SQLException {
        DriverManager.setLoginTimeout(60);
        this.conn = DriverManager.getConnection(url, user, pass);
        this.dbName = dbName;
    }

    /**
     * Create db if not exists
     * @throws SQLException
     */
    public void checkAndCreate() throws SQLException {
        Statement stmt = conn.createStatement();
        String query = String.format("create database if not exists `%s` default character set utf8", dbName);
        stmt.executeUpdate(query);
    }

    /**
     * Drop database
     * @throws SQLException
     */
    public void dropDatabase() throws SQLException {
        Statement stmt = conn.createStatement();
        String query = String.format("drop database if exists `%s`", dbName);
        stmt.executeUpdate(query);
    }

    /**
     * Close connection
     * @throws SQLException
     */
    public void close() throws SQLException {
        if (conn != null) conn.close();
    }

    public Connection getConnection() {
        return conn;
    }
}
