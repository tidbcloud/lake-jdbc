package com.tidbcloud.jdbc;

import com.tidbcloud.jdbc.internal.data.LakeDataType;

import java.sql.Types;

public class JdbcTypeMapping {
    /**
     * Converts {@link LakeColumnInfo} to generic SQL type defined in JDBC.
     *
     * @param column non-null column definition
     * @return generic SQL type defined in JDBC
     */
    public int toSqlType(LakeColumnInfo column) {
        LakeDataType dataType = column.getType().getDataType();
        int sqlType = Types.OTHER;
        switch (dataType) {
            case BOOLEAN:
                sqlType = Types.BOOLEAN;
                break;
            case INT_8:
                sqlType = Types.TINYINT;
                break;
            case INT_16:
                sqlType = Types.SMALLINT;
                break;
            case INT_32:
                sqlType = Types.INTEGER;
                break;
            case INT_64:
                sqlType = Types.BIGINT;
                break;
            case FLOAT:
                sqlType = Types.FLOAT;
                break;
            case DOUBLE:
                sqlType = Types.DOUBLE;
                break;
            case DECIMAL:
                sqlType = Types.DECIMAL;
                break;
            case STRING:
                sqlType = Types.VARCHAR;
                break;
            case DATE:
                sqlType = Types.DATE;
                break;
            case TIMESTAMP:
                sqlType = Types.TIMESTAMP;
                break;
            case TIMESTAMP_TZ:
                sqlType = Types.TIMESTAMP_WITH_TIMEZONE;
                break;
            case ARRAY:
                sqlType = Types.ARRAY;
                break;
            case VARIANT:
                sqlType = Types.VARCHAR;
                break;
            case TUPLE:
                sqlType = Types.STRUCT;
                break;
            case NULL:
                sqlType = Types.NULL;
                break;
            default:
                break;
        }
        return sqlType;
    }

    /**
     * Gets corresponding {@link LakeDataType} of the given {@link Types}.
     *
     * @param sqlType generic SQL types defined in JDBC
     * @return non-null Lake data type
     */
    protected LakeDataType getDataType(int sqlType) {
        LakeDataType dataType;

        switch (sqlType) {
            case Types.BOOLEAN:
                dataType = LakeDataType.UNSIGNED_INT_8;
                break;
            case Types.TINYINT:
                dataType = LakeDataType.INT_8;
                break;
            case Types.SMALLINT:
                dataType = LakeDataType.INT_16;
                break;
            case Types.INTEGER:
                dataType = LakeDataType.INT_32;
                break;
            case Types.BIGINT:
                dataType = LakeDataType.INT_64;
                break;
            case Types.FLOAT:
                dataType = LakeDataType.FLOAT;
                break;
            case Types.DOUBLE:
                dataType = LakeDataType.DOUBLE;
                break;
            case Types.DECIMAL:
                dataType = LakeDataType.DECIMAL;
                break;
            case Types.BIT:
            case Types.BLOB:
            case Types.BINARY:
            case Types.CHAR:
            case Types.CLOB:
            case Types.JAVA_OBJECT:
            case Types.LONGNVARCHAR:
            case Types.LONGVARBINARY:
            case Types.LONGVARCHAR:
            case Types.NCHAR:
            case Types.NCLOB:
            case Types.NVARCHAR:
            case Types.OTHER:
            case Types.SQLXML:
            case Types.VARBINARY:
            case Types.VARCHAR:
                dataType = LakeDataType.STRING;
                break;
            case Types.DATE:
                dataType = LakeDataType.DATE;
                break;
            case Types.TIME:
            case Types.TIMESTAMP:
                dataType = LakeDataType.TIMESTAMP;
                break;
            case Types.TIME_WITH_TIMEZONE:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                dataType = LakeDataType.TIMESTAMP_TZ;
                break;
            case Types.ARRAY:
                dataType = LakeDataType.ARRAY;
                break;
            case Types.STRUCT:
                dataType = LakeDataType.TUPLE;
                break;
            case Types.DATALINK:
            case Types.DISTINCT:
            case Types.REF:
            case Types.REF_CURSOR:
            case Types.ROWID:
            case Types.NULL:
            default:
                dataType = LakeDataType.NULL;
                break;
        }
        return dataType;
    }
}
