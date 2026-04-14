package com.tidbcloud.jdbc.cloud;

import com.tidbcloud.client.data.LakeDataType;
import com.tidbcloud.client.data.LakeRawType;
import com.tidbcloud.jdbc.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;


public class TestSpecifiedAPI {
    public static Connection createConnection()
            throws SQLException {
        return Utils.createConnection();
    }

    @Test(groups = {"IT"})
    public void testUnwrap()
            throws SQLException {
        try (Connection conn = createConnection()) {
            LakeConnection connection = conn.unwrap(LakeConnection.class);


            // LakeStatement: no specified public method
            LakeStatement statement = connection.createStatement().unwrap(LakeStatement.class);
            statement.execute("create or replace table test_unwrap(c1 int32)");
            //statement.execute("insert into test_unwrap values (1)");
            statement.execute("create or replace stage test_unwrap");

            // LakeConnection: local file APIs
            String testData = "1234";
            LakeConnection.LoadMethod m = LakeConnection.LoadMethod.STAGE;
            InputStream inputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
            connection.uploadStream(inputStream, "test_unwrap", "dir1", "f1", 4, false);

            // LakePreparedStatement: no specified public method
            LakePreparedStatement ps = connection.prepareStatement("select 1").unwrap(LakePreparedStatement.class);

            try(LakeResultSet rs =  statement.executeQuery("select * from test_unwrap").unwrap(LakeResultSet.class)) {
                Assert.assertEquals(rs.columnIndex("c1"), 1);
            }

            LakeDatabaseMetaData dbMeta = connection.getMetaData().unwrap(LakeDatabaseMetaData.class);
            try (ResultSet rs = dbMeta.getColumns(null, "default", "test_unwrap", new String[]{"c1"})) {
                Assert.assertTrue(rs.next());
                Assert.assertEquals(rs.getString("COLUMN_NAME"), "c1");
                Assert.assertFalse(rs.next());
            }
        }
    }
    @Test(groups = {"UNIT"})
    public void testUtils() {
        ConnectionProperties.allProperties();
        ConnectionProperties.getDefaults();

        LakeRawType rawType = new LakeRawType("Nullable(int32)");
        LakeDataType t = rawType.getDataType();
        Assert.assertEquals(t, LakeDataType.INT_32);
        LakeColumnInfo.Builder builder = LakeColumnInfo.newBuilder("c1", rawType);
        LakeColumnInfo ci = builder.build();
        Assert.assertEquals(ci.getColumnName(), "c1");
        Assert.assertEquals(new JdbcTypeMapping().toSqlType(ci), Types.INTEGER);
    }
}
