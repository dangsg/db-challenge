package db.challenge;

import java.sql.DatabaseMetaData;

import db.challenge.config.DatabaseConnection;
import db.challenge.service.ExtractMetadata;

public class App {
    public static void main(String[] args) {
        String connString = System.getProperty("connString", "jdbc:postgresql://127.0.0.1:35432/benerator");
        String username = System.getProperty("username", "benerator");
        String password = System.getProperty("password", "benerator");
        String output = System.getProperty("output", "schema.xml");

        System.out.println(connString);

        DatabaseConnection dbConn = new DatabaseConnection(connString, username, password);
        DatabaseMetaData databaseMetaData = dbConn.getDatabaseMetadata();

        ExtractMetadata extractMetadataService = new ExtractMetadata(databaseMetaData);
        extractMetadataService.exportSchemaToXml(output);

        System.out.println("Done");
    }

}
