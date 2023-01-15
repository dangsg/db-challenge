package db.challenge.service;

import java.io.FileOutputStream;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import db.challenge.entity.Column;
import db.challenge.entity.Database;
import db.challenge.entity.Table;
import db.challenge.util.XmlUtil;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ExtractMetadata {
    private DatabaseMetaData databaseMetaData;

    private static Map<String, String> typeMap = Stream.of(new String[][] {
            { "VARCHAR", "string" },
            { "VARBINARY", "binary" },
            { "TINYINT", "byte" },
            { "TIMESTAMP", "timestamp" },
            { "TIME", "date" },
            { "SMALLINT", "short" },
            { "REAL", "double" },
            { "NUMERIC", "double" },
            { "LONGVARCHAR", "string" },
            { "JAVA_OBJECT", "object" },
            { "INTEGER", "integer" },
            { "FLOAT", "float" },
            { "DOUBLE", "double" },
            { "DECIMAL", "big_decimal" },
            { "DATE", "date" },
            { "CLOB", "string" },
            { "CHAR", "string" },
            { "BOOLEAN", "boolean" },
            { "BLOB", "binary" },
            { "BIT", "byte" },
            { "BINARY", "binary" },
            { "BIGINT", "big_integer" },
    }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

    /**
     * Get schema of database tables
     * 
     * @return
     */
    public Map<String, Table> getTableSchema() {
        try {
            Map<String, Table> tableMap = new HashMap<>();

            ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[] { "TABLE" });
            while (resultSet.next()) {
                final String tableName = resultSet.getString("TABLE_NAME");

                // Get primary keys
                Set<String> primaryKeySet = new HashSet<>();
                ResultSet primaryKeys = databaseMetaData.getPrimaryKeys(null, null, tableName);
                while (primaryKeys.next()) {
                    primaryKeySet.add(primaryKeys.getString("COLUMN_NAME"));
                }

                // Get foreign keys
                Map<String, String[]> foreignKeyMap = new HashMap<>();
                ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, tableName);
                while (foreignKeys.next()) {
                    String pkTableName = foreignKeys.getString("PKTABLE_NAME");
                    String pkColumnName = foreignKeys.getString("PKCOLUMN_NAME");
                    String fkColumnName = foreignKeys.getString("FKCOLUMN_NAME");
                    foreignKeyMap.put(fkColumnName, new String[] { pkTableName, pkColumnName });
                }

                List<Column> columns = new ArrayList<>();

                // Get columns
                ResultSet column = databaseMetaData.getColumns(null, null, tableName, null);
                while (column.next()) {
                    String columnName = column.getString("COLUMN_NAME");
                    String datatype = typeMap
                            .get(JDBCType.valueOf(Integer.parseInt(column.getString("DATA_TYPE")))
                                    .getName());

                    // Specify column attribute
                    String attribute = "attribute";
                    String selector = null;
                    if (primaryKeySet.contains(columnName)) {
                        attribute = "id";
                    } else if (foreignKeyMap.keySet().contains(columnName)) {
                        attribute = "reference";
                        selector = String.format("select %s from %s", foreignKeyMap.get(columnName)[1],
                                foreignKeyMap.get(columnName)[0]);
                    } else {
                        // Predict attribute based on column name
                        Matcher matcherOfEncrypt = Pattern
                                .compile("^(name|last_name|email|house_number|street|city|country|zip_code)$")
                                .matcher(columnName);
                        boolean isEncrypt = matcherOfEncrypt.find();
                        Matcher matcherOfHash = Pattern.compile("(password)").matcher(columnName);
                        boolean isHash = matcherOfHash.find();
                        Matcher matcherOfMask = Pattern.compile("(credit)").matcher(columnName);
                        boolean isMask = matcherOfMask.find();

                        attribute = isEncrypt ? "encrypt" : isHash ? "hash" : isMask ? "mask" : attribute;
                    }
                    Column newColumn = new Column(attribute, columnName, datatype, selector);
                    columns.add(newColumn);
                }
                tableMap.put(tableName, new Table(tableName, columns));
            }

            return tableMap;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Get table schema failed!!!");
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return null;
    }

    /**
     * Get BFS traversal of database tables (table as vertex, foreign key as edge)
     * 
     * @return
     */
    public LinkedList<String> getBfsTraversal() {
        try {
            // Set of tables which don't have foreign keys referencing to other tables
            Set<String> notReferencingTable = new HashSet<>();
            // Map of table and its referencing tables
            Map<String, Set<String>> referencingTableMap = new HashMap<>();
            // BFS not visited nodes
            Set<String> notVisitedNodes = new HashSet<>();

            ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[] { "TABLE" });

            while (resultSet.next()) {
                final String tableName = resultSet.getString("TABLE_NAME");

                notVisitedNodes.add(tableName);
                // Suppose this table is NOT referencing table
                notReferencingTable.add(tableName);

                ResultSet foreignKeys = databaseMetaData.getImportedKeys(null, null, tableName);
                while (foreignKeys.next()) {
                    String pkTableName = foreignKeys.getString("PKTABLE_NAME");

                    // Ignore if it is a self reference
                    if (tableName.equals(pkTableName))
                        continue;

                    // Because table has foreign key to another table, it is specified as a
                    // referencing table
                    notReferencingTable.remove(tableName);
                    Set<String> referencingTables = referencingTableMap.containsKey(pkTableName)
                            ? referencingTableMap.get(pkTableName)
                            : new HashSet<>();
                    referencingTables.add(tableName);
                    referencingTableMap.put(pkTableName, referencingTables);
                }
            }

            LinkedList<String> bfsResult = new LinkedList<>();
            // First, start traversing with not referencing tables (as root nodes of BFS
            // graph)
            LinkedList<String> bfsQueue = new LinkedList<>(notReferencingTable);
            notVisitedNodes.removeAll(notReferencingTable);
            while (bfsQueue.size() != 0) {
                String tableName = bfsQueue.poll();

                notVisitedNodes.remove(tableName);
                bfsResult.add(tableName);

                // Check if this node has any sub nodes
                if (!referencingTableMap.containsKey(tableName))
                    continue;

                // Add sub nodes to queue
                Set<String> addedTable = referencingTableMap.get(tableName);
                addedTable.retainAll(notVisitedNodes);
                bfsQueue.addAll(addedTable);
                notVisitedNodes.removeAll(addedTable);
            }
            bfsResult.addAll(notVisitedNodes);

            return bfsResult;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Getting BFS traversal failed");
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
        return null;
    }

    /**
     * Export database schema to XML
     * 
     * @param output
     */
    public void exportSchemaToXml(String output) {
        try {
            LinkedList<String> bfsTables = getBfsTraversal();
            Map<String, Table> tableMap = getTableSchema();
            Database dbSchema = new Database(
                    bfsTables.stream().map(tableMap::get).collect(Collectors.toList()));
            XmlUtil.writeXml(dbSchema.toXmlDocument(), new FileOutputStream(output));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Export schema to XML failed");
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }
}