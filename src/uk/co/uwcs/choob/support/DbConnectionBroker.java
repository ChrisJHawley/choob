/**
 * DbConnectionBroker.
 * @version 1.0.13 3/12/02
 * @author Marc A. Mnich
 */
package uk.co.uwcs.choob.support;
import com.mchange.v2.c3p0.*;
import java.io.*;
import java.sql.*;
import java.util.*;

public final class DbConnectionBroker
{
	ComboPooledDataSource cpds;

	/**
	 * Create a new DbConnectionBroker.
	 *
	 * @param dbDriver 		JDBC driver. e.g. 'oracle.jdbc.driver.OracleDriver'
	 * @param dbServer		JDBC connect string. e.g. 'jdbc:oracle:thin:@203.92.21.109:1526:orcl'
	 * @param dbLogin		Database login name.  e.g. 'Scott'
	 * @param dbPassword	Database password.	e.g. 'Tiger'
	 * @param minConns		Minimum number of connections to start with.
	 * @param maxConns		Maximum number of connections in dynamic pool.
	 * @param logFile		PrintWriter to log to.
	 * @param maxCheckoutSeconds	Max time a connection can be checked out before being recycled. Zero value turns option off, default is 60 seconds.
	 */
	public DbConnectionBroker(String dbDriver, String dbServer, String dbLogin, String dbPassword, int minConns, int maxConns, PrintWriter logFile, int maxCheckoutSeconds) throws SQLException
	{
		setupBroker(dbDriver, dbServer, dbLogin, dbPassword, minConns, maxConns, logFile, maxCheckoutSeconds);
	}

	private void setupBroker(String dbDriver, String dbServer, String dbLogin, String dbPassword, int minConns, int maxConns, PrintWriter logFile, int maxCheckoutSeconds) throws SQLException	{
		cpds = new ComboPooledDataSource();
		try
		{
			cpds.setDriverClass(dbDriver);
		}
		catch (java.beans.PropertyVetoException e)
		{
			e.printStackTrace();
			throw new SQLException("Error setting driver class.");
		}
		cpds.setJdbcUrl(dbServer);
		cpds.setUser(dbLogin);
		cpds.setPassword(dbPassword);
		cpds.setMinPoolSize(minConns);
		cpds.setMaxPoolSize(maxConns);
		cpds.setLogWriter(logFile);
		cpds.setCheckoutTimeout(maxCheckoutSeconds*1000);

		cpds.setIdleConnectionTestPeriod(300); // Execute a a query after idle for 5 mins.
		//cpds.setTestConnectionOnCheckout(true); // Apparently very slow!
		cpds.setAutomaticTestTable("C3P0TestTable"); // C3P0 will create this.

		/*
			Doesn't do what we need: Connects AFTER throwing an exception.
			java.util.Properties props = new java.util.Properties();
			props.setProperty("autoReconnect", "true");
			props.setProperty("user", dbLogin);
			props.setProperty("password", dbPassword);
			cpds.setProperties(props);
		*/
	}

	/** Checkout a Connection. */
	public Connection getConnection() throws SQLException
	{
		// Switch to the 2nd line to get per-SQL-statement logging.
		return cpds.getConnection();
		//return new ChoobConnectionWrapper(cpds.getConnection());
	}

	/** Free (check-back-in) a checked-out Connection. */
	public void freeConnection(Connection conn)
	{
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	/** Free all of the resources allocated by this DbConnectionBroker */
	public void destroy() throws SQLException
	{
		DataSources.destroy(cpds);
	}

}