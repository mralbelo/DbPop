package org.dandoy.dbpop;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class TableName {
    private final String catalog;
    private final String schema;
    private final String table;

    TableName(String catalog, String schema, String table) {
        this.catalog = catalog;
        this.schema = schema;
        this.table = table;
    }

    @Override
    public String toString() {
        return "TableName{" +
                "catalog='" + catalog + '\'' +
                ", schema='" + schema + '\'' +
                ", table='" + table + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TableName tableName = (TableName) o;
        return Objects.equals(catalog, tableName.catalog) && Objects.equals(schema, tableName.schema) && table.equals(tableName.table);
    }

    @Override
    public int hashCode() {
        return Objects.hash(table);
    }

    String getCatalog() {
        return catalog;
    }

    String getSchema() {
        return schema;
    }

    String getTable() {
        return table;
    }

    String toQualifiedName() {
        return Stream.of(catalog, schema, table).filter(Objects::nonNull).collect(Collectors.joining("."));
    }
}
