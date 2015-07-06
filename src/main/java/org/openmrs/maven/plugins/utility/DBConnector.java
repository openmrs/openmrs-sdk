package org.openmrs.maven.plugins.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnector {
    Connection conn;
    String dbName;

    public DBConnector() {}

    public DBConnector(String url, String user, String pass, String dbName) throws SQLException {
        this.conn = DriverManager.getConnection(url, user, pass);
        this.dbName = dbName;
    }

    public void checkAndCreate() throws SQLException {
        Statement stmt = conn.createStatement();
        String query = String.format("create database if not exists `%s` default character set utf8", dbName);
        stmt.executeQuery(query);
    }
}
