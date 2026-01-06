package org.openmrs.maven.plugins.utility;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.openmrs.maven.plugins.model.Server;

private static final int  MAX_RETRIES = 15;
public class DBConnector implements AutoCloseable {

	Connection connection;

	String dbName;



	public DBConnector(String url, String user, String pass, String dbName) throws SQLException {
		DriverManager.setLoginTimeout(60);
		this.dbName = dbName;

		//  LOGIC: Retry up to 15 times (15 seconds)

		for (int i = 0; i < MAX_RETRIES; i++) {
			try {
				// Try to connect
				this.connection = DriverManager.getConnection(url, user, pass);

				break;

			} catch (SQLException e) {
				// If this was the last attempt, give up and crash
				if (i == maxRetries - 1) {
					if (e.getMessage().contains("Access denied")) {
						throw new SQLException("Invalid database credentials. Please check your username and password.", e);
					}
					throw e; // Throw the actual error
				}

				// If not the last attempt, Wait 1 second and loop again
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new SQLException("Thread interrupted while waiting for database connection", ie);
				}
			}
		}
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
