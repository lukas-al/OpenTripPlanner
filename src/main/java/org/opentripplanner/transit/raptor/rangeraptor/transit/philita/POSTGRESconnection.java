package org.opentripplanner.transit.raptor.rangeraptor.transit.philita;

import java.sql.*;

/**
 * Connection class for connecting to JDBC
 *
 * @return a connection object
 */
public class POSTGRESconnection {

    public static Connection PGRSconnection() {

        Connection connection = null;
        if (connection == null) {
            String url = "jdbc:postgresql://127.0.0.1/Philita";
            String user = "Philita";
            String password = "HP2310i";

            try {
                connection = DriverManager.getConnection(url, user, password);
            } catch (SQLException ex) {
                System.err.println("Big oof in JDBC connection" + ex);
                System.err.println("Terminating");
                System.exit(3);
                /**
                 * OK just exiting when we are knee deep in the code is gonna hard crash the
                 * Java VM TODO: Improve the error handling by retrying the connection, moving
                 * to non-Philita mode for the current journey & logging the error.
                 */
            }
        }
        return connection;
    }

}