/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tidbcloud.client.data;


import java.sql.Types;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.tidbcloud.client.data.LakeRawType.startsWithIgnoreCase;
import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * data type that reflect java.sql.type
 */
public enum LakeDataType {

    BOOLEAN(Types.BOOLEAN, LakeTypes.BOOLEAN, false, 1, false, "Boolean", "BOOL"),

    // int8 -> TINYINT -> -128~127
    INT_8(Types.TINYINT, LakeTypes.INT8, true, 3, false, "Int8", "TINYINT"),
    UNSIGNED_INT_8(Types.TINYINT, LakeTypes.UINT8, false, 3, false, "UInt8", "TINYINT UNSIGNED"),

    // int16 -> SMALLINT -> -32768~32767
    INT_16(Types.SMALLINT, LakeTypes.INT16, true, 5, false, "Int16", "SMALLINT"),
    UNSIGNED_INT_16(Types.SMALLINT, LakeTypes.UINT16, false, 5, false, "UInt16", "SMALLINT UNSIGNED"),

    // int32 -> INT -> -2147483648~2147483647
    INT_32(Types.INTEGER, LakeTypes.INT32, true, 10, false, "Int32", "INTEGER"),
    UNSIGNED_INT_32(Types.INTEGER, LakeTypes.UINT32, false, 10, false, "UInt32", "INTEGER UNSIGNED"),

    // INT64 -> BIGINT -> -9223372036854775808~9223372036854775807
    INT_64(Types.BIGINT, LakeTypes.INT64, true, 19, false, "Int64", "BIGINT"),
    UNSIGNED_INT_64(Types.BIGINT, LakeTypes.UINT64, false, 20, false, "UInt64", "BIGINT UNSIGNED"),

    DOUBLE(Types.DOUBLE, LakeTypes.FLOAT64, true, 22, false, "Float64", "DOUBLE"),
    FLOAT(Types.FLOAT, LakeTypes.FLOAT32, true, 12, false, "Float32", "FLOAT"),
    DECIMAL(Types.DECIMAL, LakeTypes.DECIMAL, true, 65, false, "Decimal"),

    STRING(Types.VARCHAR, LakeTypes.STRING, false, Integer.MAX_VALUE, false, "String", "VARCHAR"),

    DATE(Types.DATE, LakeTypes.DATE, false, 10, true, "Date"),
    TIMESTAMP(Types.TIMESTAMP, LakeTypes.TIMESTAMP, false, 26, true, "DateTime", "TIMESTAMP"),
    TIMESTAMP_TZ(Types.TIMESTAMP_WITH_TIMEZONE, LakeTypes.TIMESTAMP, false, 32, true, "TIMESTAMP_TZ"),

    ARRAY(Types.ARRAY, LakeTypes.ARRAY, false, 0, false, "Array"),
    MAP(Types.OTHER, LakeTypes.MAP, false, 0, false, "Map"),
    BITMAP(Types.OTHER, LakeTypes.MAP, false, 0, false, "Bitmap"),
    TUPLE(Types.OTHER, LakeTypes.TUPLE, false, 0, false, "Tuple"),
    VARIANT(Types.VARCHAR, LakeTypes.VARIANT, false, 0, false, "Variant", "Json"),

    BINARY(Types.BINARY, LakeTypes.BINARY, false, 0, false, "Binary"),

    GEOMETRY(Types.OTHER, LakeTypes.GEOMETRY, false, 0, false, "Geometry"),

    NULL(Types.NULL, LakeTypes.NULL, false, 0, false, "NULL"),
    ;

    private static final Map<String, LakeDataType> typeNameOrAliasToType;

    static {
        typeNameOrAliasToType = new HashMap<>();
        for (LakeDataType dataType : values()) {
            Arrays.stream(dataType.aliases).forEach(alias -> typeNameOrAliasToType.put(alias.toUpperCase(), dataType));
        }
    }

    private final int sqlType;
    private final String displayName;
    private final boolean signed;
    private final int length;
    private final boolean time;
    private final String[] aliases;

    /**
     * Get Lake data type by full type name
     *
     * @param typeName full Lake data type name
     * @return {@link LakeDataType}
     */
    public static LakeDataType getByTypeName(String typeName) {
        // the order of checks is important because some short names could match parts of longer names
        if (LakeTypes.BOOLEAN.equalsIgnoreCase(typeName)) {
            return BOOLEAN;
        } else if (LakeTypes.INT8.equalsIgnoreCase(typeName)) {
            return INT_8;
        } else if (LakeTypes.UINT8.equalsIgnoreCase(typeName)) {
            return UNSIGNED_INT_8;
        } else if (LakeTypes.INT16.equalsIgnoreCase(typeName)) {
            return INT_16;
        } else if (LakeTypes.UINT16.equalsIgnoreCase(typeName)) {
            return UNSIGNED_INT_16;
        } else if (LakeTypes.INT32.equalsIgnoreCase(typeName) || "int".equalsIgnoreCase(typeName)) {
            return INT_32;
        } else if (LakeTypes.UINT32.equalsIgnoreCase(typeName)) {
            return UNSIGNED_INT_32;
        } else if (LakeTypes.INT64.equalsIgnoreCase(typeName) || "bigint".equalsIgnoreCase(typeName)) {
            return INT_64;
        } else if (LakeTypes.UINT64.equalsIgnoreCase(typeName)) {
            return UNSIGNED_INT_64;
        } else if (LakeTypes.FLOAT32.equalsIgnoreCase(typeName)) {
            return FLOAT;
        } else if (LakeTypes.FLOAT64.equalsIgnoreCase(typeName)) {
            return DOUBLE;
        } else if (LakeTypes.DATE.equalsIgnoreCase(typeName)) {
            return DATE;
        } else if (LakeTypes.TIMESTAMP.equalsIgnoreCase(typeName)) {
            return TIMESTAMP;
        } else if (LakeTypes.TIMESTAMP_TZ.equalsIgnoreCase(typeName)) {
            return TIMESTAMP_TZ;
        } else if (LakeTypes.VARIANT.equalsIgnoreCase(typeName)) {
            return VARIANT;
        } else if (LakeTypes.BITMAP.equalsIgnoreCase(typeName)) {
            return BITMAP;
        } else if (startsWithIgnoreCase(typeName, LakeTypes.DECIMAL)) {
            return DECIMAL;
        } else if (startsWithIgnoreCase(typeName, LakeTypes.STRING)) {
            return STRING;
        } else if (startsWithIgnoreCase(typeName, LakeTypes.ARRAY)) {
            return ARRAY;
        } else if (startsWithIgnoreCase(typeName, LakeTypes.MAP)) {
            return MAP;
        } else if (startsWithIgnoreCase(typeName, LakeTypes.TUPLE)) {
            return TUPLE;
        } else if (startsWithIgnoreCase(typeName, LakeTypes.BINARY)) {
            return BINARY;
        } else if (startsWithIgnoreCase(typeName, LakeTypes.GEOMETRY)) {
            return GEOMETRY;
        }
        // Fallback: check SQL-standard aliases (e.g., bigint, tinyint, smallint, varchar, etc.)
        LakeDataType aliasMatch = typeNameOrAliasToType.get(typeName.toUpperCase());
        if (aliasMatch != null) {
            return aliasMatch;
        }
        return NULL;
    }

    LakeDataType(int sqlType, String displayName, boolean signed, int length, boolean isTime, String... aliases) {
        this.sqlType = sqlType;
        this.displayName = displayName;
        this.signed = signed;
        this.length = length;
        this.aliases = aliases;
        this.time = isTime;
    }

    public int getSqlType() {
        return sqlType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isSigned() {
        return signed;
    }

    public int getLength() {
        return length;
    }

    public boolean isTime() {
        return time;
    }

    public String[] getAliases() {
        return aliases;
    }

    public static LakeDataType ofType(String type) {
        String formattedType = type.trim().toUpperCase();
        return Optional.ofNullable(typeNameOrAliasToType.get(formattedType)).orElse(NULL);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("sqlType", sqlType)
                .add("displayName", displayName)
                .add("signed", signed)
                .add("length", length)
                .add("time", time)
                .add("aliases", Arrays.asList(aliases))
                .toString();
    }
}
