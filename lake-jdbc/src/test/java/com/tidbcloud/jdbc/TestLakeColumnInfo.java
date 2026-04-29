package com.tidbcloud.jdbc;

import com.tidbcloud.jdbc.internal.data.LakeDataType;
import com.tidbcloud.jdbc.internal.data.LakeRawType;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Types;

/**
 * Unit tests for LakeColumnInfo verifying that SQL-standard type names
 * returned by Lake >= v1.2.889 produce correct column metadata.
 *
 * Before v1.2.889, information_schema.columns returned internal names like
 * "Nullable(Int64)". After v1.2.889, it returns SQL-standard names like "bigint".
 */
public class TestLakeColumnInfo {

    // =========================================================================
    // End-to-end: new type name -> LakeColumnInfo -> correct metadata
    // =========================================================================

    @Test(groups = {"UNIT"})
    public void testBigintColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("bigint"));
        Assert.assertEquals(info.getColumnType(), Types.BIGINT);
        Assert.assertTrue(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 19);
        Assert.assertEquals(info.getColumnDisplaySize(), 20);
        Assert.assertEquals(info.getScale(), 0);
    }

    @Test(groups = {"UNIT"})
    public void testNullableBigintColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("Nullable(bigint)"));
        Assert.assertEquals(info.getColumnType(), Types.BIGINT);
        Assert.assertEquals(info.getNullable(), LakeColumnInfo.Nullable.NULLABLE);
        Assert.assertTrue(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 19);
    }

    @Test(groups = {"UNIT"})
    public void testBigintUnsignedColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("bigint unsigned"));
        Assert.assertEquals(info.getColumnType(), Types.BIGINT);
        Assert.assertFalse(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 19);
        Assert.assertEquals(info.getColumnDisplaySize(), 20);
    }

    @Test(groups = {"UNIT"})
    public void testTinyintColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("tinyint"));
        Assert.assertEquals(info.getColumnType(), Types.TINYINT);
        Assert.assertTrue(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 4);
        Assert.assertEquals(info.getColumnDisplaySize(), 5);
        Assert.assertEquals(info.getScale(), 0);
    }

    @Test(groups = {"UNIT"})
    public void testTinyintUnsignedColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("tinyint unsigned"));
        Assert.assertEquals(info.getColumnType(), Types.TINYINT);
        Assert.assertFalse(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 3);
        Assert.assertEquals(info.getColumnDisplaySize(), 4);
    }

    @Test(groups = {"UNIT"})
    public void testSmallintColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("smallint"));
        Assert.assertEquals(info.getColumnType(), Types.SMALLINT);
        Assert.assertTrue(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 5);
        Assert.assertEquals(info.getColumnDisplaySize(), 6);
    }

    @Test(groups = {"UNIT"})
    public void testSmallintUnsignedColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("smallint unsigned"));
        Assert.assertEquals(info.getColumnType(), Types.SMALLINT);
        Assert.assertFalse(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 5);
        Assert.assertEquals(info.getColumnDisplaySize(), 6);
    }

    @Test(groups = {"UNIT"})
    public void testIntegerColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("integer"));
        Assert.assertEquals(info.getColumnType(), Types.INTEGER);
        Assert.assertTrue(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 10);
        Assert.assertEquals(info.getColumnDisplaySize(), 11);
    }

    @Test(groups = {"UNIT"})
    public void testIntegerUnsignedColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("integer unsigned"));
        Assert.assertEquals(info.getColumnType(), Types.INTEGER);
        Assert.assertFalse(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 10);
        Assert.assertEquals(info.getColumnDisplaySize(), 11);
    }

    @Test(groups = {"UNIT"})
    public void testFloatColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("float"));
        Assert.assertEquals(info.getColumnType(), Types.FLOAT);
        Assert.assertTrue(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 9);
        Assert.assertEquals(info.getColumnDisplaySize(), 16);
    }

    @Test(groups = {"UNIT"})
    public void testDoubleColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("double"));
        Assert.assertEquals(info.getColumnType(), Types.DOUBLE);
        Assert.assertTrue(info.isSigned());
        Assert.assertEquals(info.getPrecision(), 17);
        Assert.assertEquals(info.getColumnDisplaySize(), 24);
    }

    @Test(groups = {"UNIT"})
    public void testVarcharColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("varchar"));
        Assert.assertEquals(info.getColumnType(), Types.VARCHAR);
        Assert.assertFalse(info.isSigned());
    }

    @Test(groups = {"UNIT"})
    public void testBoolColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("bool"));
        Assert.assertEquals(info.getColumnType(), Types.BOOLEAN);
        Assert.assertEquals(info.getColumnDisplaySize(), 5);
    }

    @Test(groups = {"UNIT"})
    public void testTupleColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("Tuple(x Int64, y Int64 NULL)"));
        Assert.assertEquals(info.getColumnType(), Types.STRUCT);
        Assert.assertEquals(info.getColumnTypeName(), "tuple");
    }

    @Test(groups = {"UNIT"})
    public void testBitmapColumnInfo() {
        LakeColumnInfo info = LakeColumnInfo.of("col", new LakeRawType("Bitmap"));
        Assert.assertEquals(info.getColumnType(), Types.OTHER);
        Assert.assertEquals(info.getColumnTypeName(), "bitmap");
    }

    // =========================================================================
    // Verify new type names produce identical metadata to old type names
    // =========================================================================

    @Test(groups = {"UNIT"})
    public void testNewTypeNamesMatchOldTypeNamesMetadata() {
        // bigint vs Int64
        assertSameMetadata("bigint", "Int64");
        // tinyint vs Int8
        assertSameMetadata("tinyint", "Int8");
        // smallint vs Int16
        assertSameMetadata("smallint", "Int16");
        // integer vs Int32
        assertSameMetadata("integer", "Int32");
        // bigint unsigned vs UInt64
        assertSameMetadata("bigint unsigned", "UInt64");
        // tinyint unsigned vs UInt8
        assertSameMetadata("tinyint unsigned", "UInt8");
        // smallint unsigned vs UInt16
        assertSameMetadata("smallint unsigned", "UInt16");
        // integer unsigned vs UInt32
        assertSameMetadata("integer unsigned", "UInt32");
        // float vs Float32
        assertSameMetadata("float", "Float32");
        // double vs Float64
        assertSameMetadata("double", "Float64");
        // varchar vs String
        assertSameMetadata("varchar", "String");
        // bool vs Boolean
        assertSameMetadata("bool", "Boolean");
    }

    @Test(groups = {"UNIT"})
    public void testNullableNewTypeNamesMatchOldTypeNamesMetadata() {
        assertSameMetadata("Nullable(bigint)", "Nullable(Int64)");
        assertSameMetadata("Nullable(tinyint)", "Nullable(Int8)");
        assertSameMetadata("Nullable(smallint)", "Nullable(Int16)");
        assertSameMetadata("Nullable(integer)", "Nullable(Int32)");
        assertSameMetadata("Nullable(float)", "Nullable(Float32)");
        assertSameMetadata("Nullable(double)", "Nullable(Float64)");
        assertSameMetadata("Nullable(varchar)", "Nullable(String)");
        assertSameMetadata("Nullable(bool)", "Nullable(Boolean)");
    }

    // =========================================================================
    // JdbcTypeMapping end-to-end with new type names
    // =========================================================================

    @Test(groups = {"UNIT"})
    public void testJdbcTypeMappingWithNewTypeNames() {
        JdbcTypeMapping mapping = new JdbcTypeMapping();

        LakeColumnInfo bigintCol = LakeColumnInfo.of("col", new LakeRawType("bigint"));
        Assert.assertEquals(mapping.toSqlType(bigintCol), Types.BIGINT);

        LakeColumnInfo tinyintCol = LakeColumnInfo.of("col", new LakeRawType("tinyint"));
        Assert.assertEquals(mapping.toSqlType(tinyintCol), Types.TINYINT);

        LakeColumnInfo smallintCol = LakeColumnInfo.of("col", new LakeRawType("smallint"));
        Assert.assertEquals(mapping.toSqlType(smallintCol), Types.SMALLINT);

        LakeColumnInfo integerCol = LakeColumnInfo.of("col", new LakeRawType("integer"));
        Assert.assertEquals(mapping.toSqlType(integerCol), Types.INTEGER);

        LakeColumnInfo floatCol = LakeColumnInfo.of("col", new LakeRawType("float"));
        Assert.assertEquals(mapping.toSqlType(floatCol), Types.FLOAT);

        LakeColumnInfo doubleCol = LakeColumnInfo.of("col", new LakeRawType("double"));
        Assert.assertEquals(mapping.toSqlType(doubleCol), Types.DOUBLE);

        LakeColumnInfo varcharCol = LakeColumnInfo.of("col", new LakeRawType("varchar"));
        Assert.assertEquals(mapping.toSqlType(varcharCol), Types.VARCHAR);

        LakeColumnInfo boolCol = LakeColumnInfo.of("col", new LakeRawType("bool"));
        Assert.assertEquals(mapping.toSqlType(boolCol), Types.BOOLEAN);
    }

    private void assertSameMetadata(String newTypeName, String oldTypeName) {
        LakeColumnInfo newInfo = LakeColumnInfo.of("col", new LakeRawType(newTypeName));
        LakeColumnInfo oldInfo = LakeColumnInfo.of("col", new LakeRawType(oldTypeName));

        Assert.assertEquals(newInfo.getColumnType(), oldInfo.getColumnType(),
                newTypeName + " vs " + oldTypeName + ": columnType mismatch");
        Assert.assertEquals(newInfo.isSigned(), oldInfo.isSigned(),
                newTypeName + " vs " + oldTypeName + ": signed mismatch");
        Assert.assertEquals(newInfo.getPrecision(), oldInfo.getPrecision(),
                newTypeName + " vs " + oldTypeName + ": precision mismatch");
        Assert.assertEquals(newInfo.getScale(), oldInfo.getScale(),
                newTypeName + " vs " + oldTypeName + ": scale mismatch");
        Assert.assertEquals(newInfo.getColumnDisplaySize(), oldInfo.getColumnDisplaySize(),
                newTypeName + " vs " + oldTypeName + ": columnDisplaySize mismatch");
    }
}
