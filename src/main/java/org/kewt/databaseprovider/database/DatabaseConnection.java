package org.kewt.databaseprovider.database;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.kewt.databaseprovider.database.callbacks.QueryPreparer;
import org.kewt.databaseprovider.database.callbacks.QueryReader;

public class DatabaseConnection implements Closeable {
	
	private final Connection connection;
	
	public DatabaseConnection(String connectionUrl, String user, String password) {
		this(connectionUrl, user, password, true);
	}
	
	public DatabaseConnection(String connectionUrl, String user, String password, boolean autoCommit) {
		try {
			this.connection = DriverManager.getConnection(connectionUrl, user, password);
			this.connection.setAutoCommit(autoCommit);
		} catch (SQLException e) {
			throw new DatabaseException(e.getMessage(), e);
		}
	}
	
	public void commit() {
		try {
			connection.commit();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}
	
	public void rollback() {
		try {
			connection.rollback();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}
	
	public <T> T executeAndReturnGeneratedKeys(String sql, QueryPreparer prepare, QueryReader<T> read) {
		PreparedStatement statement = null;
		ResultSet rs = null;

		try {
			statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			prepare.onPrepare(statement);
			statement.executeUpdate();
			rs = statement.getGeneratedKeys();
			if (rs.next()) {
				return read.onRowFound(rs);
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) { }
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) { }
			}
		}
	}
	
	public int execute(String sql, QueryPreparer prepare) {
		PreparedStatement statement = null;
		try {
			statement = connection.prepareStatement(sql);
			prepare.onPrepare(statement);
			return statement.executeUpdate();
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) { }
			}
		}
	}

	public <T> T querySingle(String sql, QueryPreparer prepare, QueryReader<T> read) {
		PreparedStatement statement = null;
		ResultSet rs = null;
		try {
			statement = connection.prepareStatement(sql);
			if (prepare != null) {
				prepare.onPrepare(statement);
			}
			statement.execute();
			rs = statement.getResultSet();
			if (rs.next()) {
				return read.onRowFound(rs);
			} else {
				return null;
			}
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) { }
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) { }
			}
		}
	}
	
	public <T> List<T> queryList(String sql, QueryPreparer prepare, QueryReader<T> read) {
		PreparedStatement statement = null;
		ResultSet rs = null;
		try {
			statement = connection.prepareStatement(sql);
			if (prepare != null) {
				prepare.onPrepare(statement);
			}
			statement.execute();
			rs = statement.getResultSet();
			List<T> result = new ArrayList<T>();
			while (rs.next()) {
				T row = read.onRowFound(rs);
				if (row != null) {
					result.add(row);
				}
			}
			return result;
		} catch (SQLException e) {
			throw new DatabaseException(e);
		} finally {
			if (rs != null) {
				try {
					rs.close();
				} catch (Exception e) { }
			}
			if (statement != null) {
				try {
					statement.close();
				} catch (Exception e) { }
			}
		}
	}
	
	@Override
	public void close() {
		try {
			connection.close();
		} catch (Exception e) {
			throw new DatabaseException(e);
		}
	}

}
