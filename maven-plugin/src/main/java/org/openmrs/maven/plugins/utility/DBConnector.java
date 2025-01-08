package org.openmrs.maven.plugins.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.openmrs.maven.plugins.model.Server;

public class DBConnector implements AutoCloseable {

	Connection connection;

	String dbName;

	public DBConnector(String url, String user, String pass, String dbName) throws SQLException {
		DriverManager.setLoginTimeout(60);
		/*
		 * Connection attempts to a database in a newly created Docker container might fail on the first try due to the container not being fully ready
		 * This is to mitigate such errors.
		 */
		try {
			this.connection = DriverManager.getConnection(url, user, pass);
		} catch (SQLException e) {
			try {
				this.connection = DriverManager.getConnection(url, user, pass);
			} catch (SQLException e2) {
				if (e2.getMessage().contains("Access denied")) {
					throw new SQLException("Invalid database credentials. Please check your username and password.", e2);
				}
				throw e2;
			}
		}
		this.dbName = dbName;
	}

	/**
	 * Create db if not exists
	 *
	 * @throws SQLException
	 */
	public void checkAndCreate(Server server) throws SQLException {
		Statement stmt = connection.createStatement();
		String query = String.format("create database if not exists `%s` default character set utf8", dbName);
		if (server.isPostgreSqlDb()) {
			query = String.format("create database %s encoding 'utf8'", dbName);
		}
		stmt.executeUpdate(query);
	}

	/**
	 * Drop database
	 *
	 * @throws SQLException
	 */
	public void dropDatabase() throws SQLException {
		Statement stmt = connection.createStatement();
		String query = String.format("drop database if exists `%s`", dbName);
		stmt.executeUpdate(query);
	}

	/**
	 * Close connection
	 *
	 * @throws SQLException
	 */
	public void close() throws SQLException {
		if (connection != null) {
			connection.close();
		}
	}

	public Connection getConnection() {
		return connection;
	}
}
