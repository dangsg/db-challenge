package db.challenge.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DatabaseConnection {
    private String connString;
    private String username;
    private String password;

    /**
     * Get PostgreSQL database metadata
     * 
     * @return
     */
    public DatabaseMetaData getDatabaseMetadata() {
        try {
            Connection connection = null;

            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(connString, username, password);
            DatabaseMetaData databaseMetaData = connection.getMetaData();
            return databaseMetaData;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Connect to PostgreSQL failed!!!");
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return null;
    }
}
