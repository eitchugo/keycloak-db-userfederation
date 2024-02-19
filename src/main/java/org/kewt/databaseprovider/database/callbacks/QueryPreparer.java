package org.kewt.databaseprovider.database.callbacks;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface QueryPreparer {		
	
	public void onPrepare(PreparedStatement statement) throws SQLException;
	
}
