package org.dandoy.dbpop.database;

import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;

public abstract class ColumnType {
    public static final ColumnType VARCHAR = new ColumnType() {
        @Override
        public Integer toSqlType() {
            return Types.VARCHAR;
        }
    };

    public static final ColumnType INTEGER = new ColumnType() {
        @Override
        public void bind(PreparedStatement preparedStatement, int jdbcPos, String input) throws SQLException {
            if (input == null) {
                preparedStatement.setNull(jdbcPos, Types.INTEGER);
            } else {
                preparedStatement.setLong(jdbcPos, Long.parseLong(input));
            }
        }

        @Override
        public Integer toSqlType() {
            return Types.INTEGER;
        }
    };

    public static final ColumnType BIG_DECIMAL = new ColumnType() {
        @Override
        public void bind(PreparedStatement preparedStatement, int jdbcPos, String input) throws SQLException {
            if (input == null) {
                preparedStatement.setNull(jdbcPos, Types.DECIMAL);
            } else {
                preparedStatement.setBigDecimal(jdbcPos, new BigDecimal(input));
            }
        }

        @Override
        public Integer toSqlType() {
            return Types.DECIMAL;
        }
    };

    public static final ColumnType TIMESTAMP = new ColumnType() {
        @Override
        public void bind(PreparedStatement preparedStatement, int jdbcPos, String input) throws SQLException {
            SimpleDateFormat format_10 = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat format_19 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat format_21 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");

            if (input == null) {
                preparedStatement.setNull(jdbcPos, Types.TIMESTAMP);
            } else {
                try {
                    Date date;
                    int length = input.length();
                    if (length == 10) {
                        date = format_10.parse(input);
                    } else if (length == 19) {
                        date = format_19.parse(input);
                    } else if (length == 21 || length == 22 || length == 23) {
                        date = format_21.parse(input);
                    } else {
                        date = java.sql.Date.valueOf(input);
                    }
                    preparedStatement.setTimestamp(jdbcPos, new Timestamp(date.getTime()));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public Integer toSqlType() {
            return Types.TIMESTAMP;
        }
    };
    public static final ColumnType TIME = new ColumnType() {
        @Override
        public void bind(PreparedStatement preparedStatement, int jdbcPos, String input) throws SQLException {
            SimpleDateFormat format_8 = new SimpleDateFormat("HH:mm:ss");
            SimpleDateFormat format_10 = new SimpleDateFormat("HH:mm:ss.S");

            if (input == null) {
                preparedStatement.setNull(jdbcPos, Types.TIME);
            } else {
                try {
                    Date date;
                    int inputLength = input.length();
                    if (inputLength == 8) {
                        date = format_8.parse(input);
                    } else if (10 <= inputLength && inputLength <= 16) {
                        date = format_10.parse(input);
                    } else {
                        date = java.sql.Date.valueOf(input);
                    }
                    preparedStatement.setTime(jdbcPos, new Time(date.getTime()));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public Integer toSqlType() {
            return Types.TIME;
        }
    };
    public static final ColumnType DATE = new ColumnType() {
        @Override
        public void bind(PreparedStatement preparedStatement, int jdbcPos, String input) throws SQLException {
            SimpleDateFormat format_10 = new SimpleDateFormat("yyyy-MM-dd");

            if (input == null) {
                preparedStatement.setNull(jdbcPos, Types.DATE);
            } else {
                try {
                    Date date;
                    if (input.length() == 10) {
                        date = format_10.parse(input);
                    } else {
                        date = java.sql.Date.valueOf(input);
                    }
                    preparedStatement.setDate(jdbcPos, new java.sql.Date(date.getTime()));
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public Integer toSqlType() {
            return Types.DATE;
        }
    };

    public static final ColumnType BINARY = new ColumnType() {
        @Override
        public void bind(PreparedStatement preparedStatement, int jdbcPos, String input) throws SQLException {
            if (input == null) {
                preparedStatement.setNull(jdbcPos, Types.BINARY);
            } else {
                byte[] bytes = Base64.getDecoder().decode(input);
                preparedStatement.setBytes(jdbcPos, bytes);
            }
        }

        @Override
        public Integer toSqlType() {
            return Types.BINARY;
        }
    };

    public static final ColumnType INVALID = new ColumnType() {
        @Override
        public void bind(PreparedStatement preparedStatement, int jdbcPos, String input) {
            throw new RuntimeException("Cannot load this data type");
        }

        @Override
        public Integer toSqlType() {
            throw new RuntimeException("Cannot load this data type");
        }
    };

    public static ColumnType getColumnType(String typeName, int typePrecision) {
        if ("varchar".equals(typeName)) return VARCHAR;
        if ("nvarchar".equals(typeName)) return VARCHAR;
        if ("int".equals(typeName)) return INTEGER;
        if ("smallint".equals(typeName)) return INTEGER;
        if ("tinyint".equals(typeName)) return INTEGER;
        if ("bigint".equals(typeName)) return BIG_DECIMAL;
        if ("money".equals(typeName)) return BIG_DECIMAL;
        if ("text".equals(typeName)) return VARCHAR;
        if ("decimal".equals(typeName)) return typePrecision > 0 ? BIG_DECIMAL : INTEGER;
        if ("float".equals(typeName)) return typePrecision > 0 ? BIG_DECIMAL : INTEGER;
        if ("numeric".equals(typeName)) return typePrecision > 0 ? BIG_DECIMAL : INTEGER;
        if ("date".equals(typeName)) return DATE;
        if ("datetime".equals(typeName)) return TIMESTAMP;
        if ("datetime2".equals(typeName)) return TIMESTAMP;
        if ("time".equals(typeName)) return TIME;
        if ("binary".equals(typeName)) return BINARY;
        if ("bit".equals(typeName)) return INTEGER;
        if ("char".equals(typeName)) return VARCHAR;
        if ("nchar".equals(typeName)) return VARCHAR;
        if ("sysname".equals(typeName)) return VARCHAR;
        if ("image".equals(typeName)) return BINARY;
        if ("varbinary".equals(typeName)) return BINARY;
        if ("geometry".equals(typeName)) return INVALID;
        if ("geography".equals(typeName)) return INVALID;
        if ("hierarchyid".equals(typeName)) return INVALID;
        if ("uniqueidentifier".equals(typeName)) return VARCHAR;
        throw new RuntimeException("Unexpected type: " + typeName);
    }

    public void bind(PreparedStatement preparedStatement, int jdbcPos, String input) throws SQLException {
        preparedStatement.setString(jdbcPos, input);
    }

    public void bind(PreparedStatement preparedStatement, int jdbcPos, byte[] input) throws SQLException {
        preparedStatement.setBytes(jdbcPos, input);
    }

    public void bind(PreparedStatement preparedStatement, int jdbcPos, Object input) throws SQLException {
        preparedStatement.setObject(jdbcPos, input);
    }

    public abstract Integer toSqlType();
}
