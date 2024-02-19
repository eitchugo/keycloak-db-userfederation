package org.kewt.databaseprovider.database.callbacks;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface QueryReader<T> {
	
	public T onRowFound(ResultSet rs) throws SQLException;
	
}