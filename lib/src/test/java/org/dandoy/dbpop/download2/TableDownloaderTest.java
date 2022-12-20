package org.dandoy.dbpop.download2;

import org.dandoy.LocalCredentials;
import org.dandoy.dbpop.database.Database;
import org.dandoy.dbpop.database.TableName;
import org.dandoy.dbpop.download.TableDownloader;
import org.dandoy.dbpop.upload.Populator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

@EnabledIf("org.dandoy.TestUtils#hasSqlServer")
class TableDownloaderTest {
    @Test
    void testByPrimaryKey() throws SQLException {
        File datasetsDirectory = new File("src/test/resources/mssql");
        LocalCredentials localCredentials = LocalCredentials.from("mssql");
        try (Populator populator = localCredentials.populator()
                .setDirectory(datasetsDirectory)
                .build()) {
            populator.load("invoices");
        }

        try (Connection connection = localCredentials.createConnection()) {
            Database database = Database.createDatabase(connection);
            String dataset = "download";
            try (TableDownloader tableDownloader = TableDownloader.builder()
                    .setDatabase(database)
                    .setDatasetsDirectory(datasetsDirectory)
                    .setDataset(dataset)
                    .setTableName(new TableName("master", "dbo", "invoices"))
                    .setByPrimaryKey()
                    .build()) {

                Set<List<Object>> pks = Set.of(List.of(1001), List.of(1002));
                tableDownloader.download(pks);
            }
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    void testFull() throws SQLException {
        File datasetsDirectory = new File("src/test/resources/mssql");
        new File(datasetsDirectory, "download/master/dbo/invoices.csv").delete();
        LocalCredentials localCredentials = LocalCredentials.from("mssql");
        try (Populator populator = localCredentials.populator()
                .setDirectory(datasetsDirectory)
                .build()) {
            populator.load("invoices");
        }

        try (Connection connection = localCredentials.createConnection()) {
            Database database = Database.createDatabase(connection);
            String dataset = "download";
            try (TableDownloader tableDownloader = TableDownloader.builder()
                    .setDatabase(database)
                    .setDatasetsDirectory(datasetsDirectory)
                    .setDataset(dataset)
                    .setTableName(new TableName("master", "dbo", "invoices"))
                    .build()) {
                tableDownloader.download();
            }
        }
    }
}