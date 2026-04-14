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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Types;

public class TestLakeTypes {

    @Test(groups = {"Unit"})
    public void testTypeNullable() {
        LakeRawType nullUnit8 = new LakeRawType("Nullable(Uint8)");
        Assert.assertEquals(nullUnit8.getType(), "Uint8");
        Assert.assertEquals(nullUnit8.isNullable(), true);

        LakeRawType nullTuple = new LakeRawType("Nullable(Tuple(String, Nullable(Int8)))");
        Assert.assertEquals(nullTuple.getDataType().getDisplayName(), "tuple");
        Assert.assertEquals(nullTuple.isNullable(), true);
        Assert.assertTrue(nullTuple.getColumnSize() == 2);

        LakeRawType map = new LakeRawType("MAP(STRING, STRING)");
        Assert.assertEquals(map.getDataType().getDisplayName(), "map");
        Assert.assertEquals(map.isNullable(), false);

        LakeRawType variant = new LakeRawType("VARIANT");
        Assert.assertEquals(variant.getDataType().getDisplayName(), "variant");

        LakeRawType geometry = new LakeRawType("Geometry");
        Assert.assertEquals(geometry.getDataType().getDisplayName(), "geometry");
    }

    // =========================================================================
    // Tests for SQL-standard type aliases returned by Lake >= v1.2.889
    // Prior to v1.2.889, information_schema.columns returned internal names
    // like "Nullable(Int64)". After v1.2.889, it returns SQL-standard names
    // like "bigint".
    // =========================================================================

    /**
     * Verify that getByTypeName resolves both old (v1.2.723) and new (v1.2.889)
     * type names to the same LakeDataType enum value.
     */
    @Test(groups = {"Unit"})
    public void testOldAndNewTypeNamesResolveToSameType() {
        // Integer types
        Assert.assertEquals(LakeDataType.getByTypeName("int8"), LakeDataType.getByTypeName("tinyint"));
        Assert.assertEquals(LakeDataType.getByTypeName("uint8"), LakeDataType.getByTypeName("tinyint unsigned"));
        Assert.assertEquals(LakeDataType.getByTypeName("int16"), LakeDataType.getByTypeName("smallint"));
        Assert.assertEquals(LakeDataType.getByTypeName("uint16"), LakeDataType.getByTypeName("smallint unsigned"));
        Assert.assertEquals(LakeDataType.getByTypeName("int32"), LakeDataType.getByTypeName("integer"));
        Assert.assertEquals(LakeDataType.getByTypeName("uint32"), LakeDataType.getByTypeName("integer unsigned"));
        Assert.assertEquals(LakeDataType.getByTypeName("int64"), LakeDataType.getByTypeName("bigint"));
        Assert.assertEquals(LakeDataType.getByTypeName("uint64"), LakeDataType.getByTypeName("bigint unsigned"));

        // Float types
        Assert.assertEquals(LakeDataType.getByTypeName("float32"), LakeDataType.getByTypeName("float"));
        Assert.assertEquals(LakeDataType.getByTypeName("float64"), LakeDataType.getByTypeName("double"));

        // String / Boolean
        Assert.assertEquals(LakeDataType.getByTypeName("string"), LakeDataType.getByTypeName("varchar"));
        Assert.assertEquals(LakeDataType.getByTypeName("boolean"), LakeDataType.getByTypeName("bool"));
    }

    /**
     * Verify SQL type codes are correct for new SQL-standard type names.
     * This is the core regression: bigint was returning Types.NULL (0) instead
     * of Types.BIGINT (-5).
     */
    @Test(groups = {"Unit"})
    public void testSqlTypeCodesForNewTypeNames() {
        // Signed integers
        Assert.assertEquals(LakeDataType.getByTypeName("tinyint").getSqlType(), Types.TINYINT);
        Assert.assertEquals(LakeDataType.getByTypeName("smallint").getSqlType(), Types.SMALLINT);
        Assert.assertEquals(LakeDataType.getByTypeName("integer").getSqlType(), Types.INTEGER);
        Assert.assertEquals(LakeDataType.getByTypeName("bigint").getSqlType(), Types.BIGINT);

        // Unsigned integers
        Assert.assertEquals(LakeDataType.getByTypeName("tinyint unsigned").getSqlType(), Types.TINYINT);
        Assert.assertEquals(LakeDataType.getByTypeName("smallint unsigned").getSqlType(), Types.SMALLINT);
        Assert.assertEquals(LakeDataType.getByTypeName("integer unsigned").getSqlType(), Types.INTEGER);
        Assert.assertEquals(LakeDataType.getByTypeName("bigint unsigned").getSqlType(), Types.BIGINT);

        // Float types
        Assert.assertEquals(LakeDataType.getByTypeName("float").getSqlType(), Types.FLOAT);
        Assert.assertEquals(LakeDataType.getByTypeName("double").getSqlType(), Types.DOUBLE);

        // Other types
        Assert.assertEquals(LakeDataType.getByTypeName("varchar").getSqlType(), Types.VARCHAR);
        Assert.assertEquals(LakeDataType.getByTypeName("bool").getSqlType(), Types.BOOLEAN);
    }

    /**
     * Verify SQL type codes are still correct for old internal type names
     * (backward compatibility with v1.2.723).
     */
    @Test(groups = {"Unit"})
    public void testSqlTypeCodesForOldTypeNames() {
        Assert.assertEquals(LakeDataType.getByTypeName("int8").getSqlType(), Types.TINYINT);
        Assert.assertEquals(LakeDataType.getByTypeName("int16").getSqlType(), Types.SMALLINT);
        Assert.assertEquals(LakeDataType.getByTypeName("int32").getSqlType(), Types.INTEGER);
        Assert.assertEquals(LakeDataType.getByTypeName("int64").getSqlType(), Types.BIGINT);
        Assert.assertEquals(LakeDataType.getByTypeName("uint8").getSqlType(), Types.TINYINT);
        Assert.assertEquals(LakeDataType.getByTypeName("uint16").getSqlType(), Types.SMALLINT);
        Assert.assertEquals(LakeDataType.getByTypeName("uint32").getSqlType(), Types.INTEGER);
        Assert.assertEquals(LakeDataType.getByTypeName("uint64").getSqlType(), Types.BIGINT);
        Assert.assertEquals(LakeDataType.getByTypeName("float32").getSqlType(), Types.FLOAT);
        Assert.assertEquals(LakeDataType.getByTypeName("float64").getSqlType(), Types.DOUBLE);
        Assert.assertEquals(LakeDataType.getByTypeName("boolean").getSqlType(), Types.BOOLEAN);
        Assert.assertEquals(LakeDataType.getByTypeName("string").getSqlType(), Types.VARCHAR);
    }

    /**
     * Verify that new type names do NOT resolve to NULL (the original bug).
     */
    @Test(groups = {"Unit"})
    public void testNewTypeNamesDoNotResolveToNull() {
        String[] newTypeNames = {
                "tinyint", "tinyint unsigned",
                "smallint", "smallint unsigned",
                "integer", "integer unsigned",
                "bigint", "bigint unsigned",
                "float", "double",
                "varchar", "bool"
        };
        for (String typeName : newTypeNames) {
            Assert.assertNotEquals(LakeDataType.getByTypeName(typeName), LakeDataType.NULL,
                    "Type name '" + typeName + "' should not resolve to NULL");
        }
    }

    /**
     * Verify case-insensitive matching for new SQL-standard type names.
     */
    @Test(groups = {"Unit"})
    public void testCaseInsensitiveNewTypeNames() {
        Assert.assertEquals(LakeDataType.getByTypeName("BIGINT"), LakeDataType.INT_64);
        Assert.assertEquals(LakeDataType.getByTypeName("Bigint"), LakeDataType.INT_64);
        Assert.assertEquals(LakeDataType.getByTypeName("bigint"), LakeDataType.INT_64);
        Assert.assertEquals(LakeDataType.getByTypeName("TINYINT"), LakeDataType.INT_8);
        Assert.assertEquals(LakeDataType.getByTypeName("SMALLINT"), LakeDataType.INT_16);
        Assert.assertEquals(LakeDataType.getByTypeName("INTEGER"), LakeDataType.INT_32);
        Assert.assertEquals(LakeDataType.getByTypeName("VARCHAR"), LakeDataType.STRING);
        Assert.assertEquals(LakeDataType.getByTypeName("DOUBLE"), LakeDataType.DOUBLE);
        Assert.assertEquals(LakeDataType.getByTypeName("FLOAT"), LakeDataType.FLOAT);
        Assert.assertEquals(LakeDataType.getByTypeName("BOOL"), LakeDataType.BOOLEAN);
        Assert.assertEquals(LakeDataType.getByTypeName("BIGINT UNSIGNED"), LakeDataType.UNSIGNED_INT_64);
    }

    /**
     * Verify LakeRawType correctly handles Nullable wrapper with new type names.
     * Simulates: Lake v1.2.889 returning "Nullable(bigint)" instead of "Nullable(Int64)".
     */
    @Test(groups = {"Unit"})
    public void testNullableWithNewTypeNames() {
        // Nullable signed integers
        LakeRawType nullBigint = new LakeRawType("Nullable(bigint)");
        Assert.assertTrue(nullBigint.isNullable());
        Assert.assertEquals(nullBigint.getDataType(), LakeDataType.INT_64);
        Assert.assertEquals(nullBigint.getDataType().getSqlType(), Types.BIGINT);

        LakeRawType nullTinyint = new LakeRawType("Nullable(tinyint)");
        Assert.assertTrue(nullTinyint.isNullable());
        Assert.assertEquals(nullTinyint.getDataType(), LakeDataType.INT_8);
        Assert.assertEquals(nullTinyint.getDataType().getSqlType(), Types.TINYINT);

        LakeRawType nullSmallint = new LakeRawType("Nullable(smallint)");
        Assert.assertTrue(nullSmallint.isNullable());
        Assert.assertEquals(nullSmallint.getDataType(), LakeDataType.INT_16);
        Assert.assertEquals(nullSmallint.getDataType().getSqlType(), Types.SMALLINT);

        LakeRawType nullInteger = new LakeRawType("Nullable(integer)");
        Assert.assertTrue(nullInteger.isNullable());
        Assert.assertEquals(nullInteger.getDataType(), LakeDataType.INT_32);
        Assert.assertEquals(nullInteger.getDataType().getSqlType(), Types.INTEGER);

        // Nullable float types
        LakeRawType nullFloat = new LakeRawType("Nullable(float)");
        Assert.assertTrue(nullFloat.isNullable());
        Assert.assertEquals(nullFloat.getDataType(), LakeDataType.FLOAT);
        Assert.assertEquals(nullFloat.getDataType().getSqlType(), Types.FLOAT);

        LakeRawType nullDouble = new LakeRawType("Nullable(double)");
        Assert.assertTrue(nullDouble.isNullable());
        Assert.assertEquals(nullDouble.getDataType(), LakeDataType.DOUBLE);
        Assert.assertEquals(nullDouble.getDataType().getSqlType(), Types.DOUBLE);

        // Nullable string / boolean
        LakeRawType nullVarchar = new LakeRawType("Nullable(varchar)");
        Assert.assertTrue(nullVarchar.isNullable());
        Assert.assertEquals(nullVarchar.getDataType(), LakeDataType.STRING);

        LakeRawType nullBool = new LakeRawType("Nullable(bool)");
        Assert.assertTrue(nullBool.isNullable());
        Assert.assertEquals(nullBool.getDataType(), LakeDataType.BOOLEAN);
    }

    /**
     * Verify non-nullable new type names work correctly.
     * Simulates: Lake v1.2.889 returning "bigint" instead of "Int64".
     */
    @Test(groups = {"Unit"})
    public void testNonNullableNewTypeNames() {
        LakeRawType bigint = new LakeRawType("bigint");
        Assert.assertFalse(bigint.isNullable());
        Assert.assertEquals(bigint.getDataType(), LakeDataType.INT_64);
        Assert.assertEquals(bigint.getDataType().getSqlType(), Types.BIGINT);

        LakeRawType tinyint = new LakeRawType("tinyint");
        Assert.assertFalse(tinyint.isNullable());
        Assert.assertEquals(tinyint.getDataType(), LakeDataType.INT_8);

        LakeRawType varchar = new LakeRawType("varchar");
        Assert.assertFalse(varchar.isNullable());
        Assert.assertEquals(varchar.getDataType(), LakeDataType.STRING);
    }

    /**
     * Verify signed/unsigned properties are correct for new type names.
     */
    @Test(groups = {"Unit"})
    public void testSignedPropertyForNewTypeNames() {
        Assert.assertTrue(LakeDataType.getByTypeName("bigint").isSigned());
        Assert.assertFalse(LakeDataType.getByTypeName("bigint unsigned").isSigned());
        Assert.assertTrue(LakeDataType.getByTypeName("tinyint").isSigned());
        Assert.assertFalse(LakeDataType.getByTypeName("tinyint unsigned").isSigned());
        Assert.assertTrue(LakeDataType.getByTypeName("smallint").isSigned());
        Assert.assertFalse(LakeDataType.getByTypeName("smallint unsigned").isSigned());
        Assert.assertTrue(LakeDataType.getByTypeName("integer").isSigned());
        Assert.assertFalse(LakeDataType.getByTypeName("integer unsigned").isSigned());
    }

    /**
     * Simulate the exact scenario from the bug report:
     * DatabaseMetaData.getColumns() returns TYPE_NAME=bigint, and we expect
     * DATA_TYPE=Types.BIGINT (-5), not 0.
     */
    @Test(groups = {"Unit"})
    public void testBugScenario_BigintSqlTypeCode() {
        // Before fix: getByTypeName("bigint") returned NULL, getSqlType() = Types.NULL = 0
        // After fix: getByTypeName("bigint") returns INT_64, getSqlType() = Types.BIGINT = -5
        LakeDataType dataType = LakeDataType.getByTypeName("bigint");
        Assert.assertEquals(dataType, LakeDataType.INT_64, "bigint should map to INT_64");
        Assert.assertEquals(dataType.getSqlType(), Types.BIGINT, "bigint SQL type should be Types.BIGINT (-5)");
        Assert.assertNotEquals(dataType.getSqlType(), 0, "bigint SQL type must not be 0 (Types.NULL)");
    }

    /**
     * Verify that old-style Nullable type names still work (backward compat with v1.2.723).
     */
    @Test(groups = {"Unit"})
    public void testOldNullableTypeNamesStillWork() {
        LakeRawType nullInt64 = new LakeRawType("Nullable(Int64)");
        Assert.assertTrue(nullInt64.isNullable());
        Assert.assertEquals(nullInt64.getDataType(), LakeDataType.INT_64);
        Assert.assertEquals(nullInt64.getDataType().getSqlType(), Types.BIGINT);

        LakeRawType nullInt32 = new LakeRawType("Nullable(Int32)");
        Assert.assertTrue(nullInt32.isNullable());
        Assert.assertEquals(nullInt32.getDataType(), LakeDataType.INT_32);

        LakeRawType nullUint64 = new LakeRawType("Nullable(Uint64)");
        Assert.assertTrue(nullUint64.isNullable());
        Assert.assertEquals(nullUint64.getDataType(), LakeDataType.UNSIGNED_INT_64);

        LakeRawType nullFloat64 = new LakeRawType("Nullable(Float64)");
        Assert.assertTrue(nullFloat64.isNullable());
        Assert.assertEquals(nullFloat64.getDataType(), LakeDataType.DOUBLE);
    }
}
