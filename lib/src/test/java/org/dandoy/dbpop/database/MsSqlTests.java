package org.dandoy.dbpop.database;

import org.dandoy.LocalCredentials;
import org.dandoy.DbPopUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.List;

import static org.dandoy.DbPopUtils.customers;
import static org.dandoy.DbPopUtils.invoices;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnabledIf("org.dandoy.TestUtils#hasTargetMssql")
public class MsSqlTests {
    @BeforeEach
    void setUp() {
        DbPopUtils.prepareMssqlTarget();
    }

    @Test
    void name() {
        LocalCredentials localCredentials = LocalCredentials.from("mssql");
        try (Database targetDatabase = Database.createDatabase(localCredentials.targetConnectionBuilder())) {
            Table table = targetDatabase.getTable(invoices);
            {
                assertNotNull(table);
                assertEquals(3, table.columns().size());
                assertEquals(
                        List.of("invoice_id", "customer_id", "invoice_date"),
                        table.columns().stream().map(Column::getName).toList()
                );
                assertEquals(1, table.indexes().size());
                assertNotNull(table.indexes().get(0).getName());
                assertEquals("invoice_id", table.indexes().get(0).getColumns().get(0));
                assertNotNull(table.primaryKey());
                assertEquals(1, table.foreignKeys().size());
                assertEquals("customer_id", table.foreignKeys().get(0).getPkColumns().get(0));
                assertEquals("customers", table.foreignKeys().get(0).getPkTableName().getTable());
                assertEquals("invoices", table.foreignKeys().get(0).getFkTableName().getTable());
                assertNotNull(table.foreignKeys().get(0).getName());
            }

            List<ForeignKey> relatedForeignKeys = targetDatabase.getRelatedForeignKeys(customers);
            {
                assertEquals(1, relatedForeignKeys.size());
                ForeignKey foreignKey = relatedForeignKeys.get(0);
                assertEquals("invoices_customers_fk", foreignKey.getName());
                assertEquals("customers", foreignKey.getPkTableName().getTable());
                assertEquals("invoices", foreignKey.getFkTableName().getTable());
            }
        }
    }
}
