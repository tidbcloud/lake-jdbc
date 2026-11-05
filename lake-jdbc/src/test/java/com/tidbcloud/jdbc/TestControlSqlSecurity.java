package com.tidbcloud.jdbc;

import com.tidbcloud.jdbc.internal.binding.SqlText;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

@Test(timeOut = 60000)
public class TestControlSqlSecurity {
    @Test(groups = {"UNIT"})
    public void testSingleControlStatementGuard() throws Exception {
        for (String sql : new String[] {
                "USE \"a; b\"; -- trailing comment", "USE `a; b`; /* trailing */",
                "SELECT 'it\\\'s; fine'", "SELECT $$a; b$$;", "SELECT ';', 1;;",
                "/* leading */ SELECT 1 /* ; */;\n-- tail",
        }) {
            SqlText.requireSingleStatement(sql);
        }
        for (String sql : new String[] {
                "USE db; DROP TABLE sentinel", "USE db; /* hidden */ SELECT 1",
                "SELECT 'it\\\'s'; DROP TABLE sentinel", "SELECT $$;$$; SELECT 2",
                "SELECT 1; -- comment\nDROP TABLE sentinel", "USE \"unterminated",
                "SELECT 1 /* unterminated", "", "   ",
        }) {
            Assert.expectThrows(SQLException.class, () -> SqlText.requireSingleStatement(sql));
        }
    }

    @Test(groups = {"UNIT"})
    public void testIdentifiersAndQuotedLoadTargets() throws Exception {
        Assert.assertEquals(SqlText.quoteIdentifier("a\"b"), "\"a\"\"b\"");
        Assert.assertEquals(SqlText.quoteIdentifier("a\\`\"b"), "`a\\``\"b`");
        Assert.expectThrows(SQLException.class, () -> SqlText.quoteIdentifier(null));
        Assert.expectThrows(SQLException.class, () -> SqlText.quoteIdentifier("bad\0name"));
    }

    @Test(groups = {"IT"})
    public void testSchemaRoundTripAndStreamingLoad() throws Exception {
        String prefix = "sec_" + UUID.randomUUID().toString().replace("-", "");
        String schema = prefix + " a\\`\"; --";
        String quotedSchema = SqlText.quoteIdentifier(schema);
        try (Connection connection = Utils.createConnection("default");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + quotedSchema);
            try {
                connection.setSchema(schema);
                Assert.assertEquals(connection.getSchema(), schema);
                for (LakeConnectionExtension.LoadMethod method : LakeConnectionExtension.LoadMethod.values()) {
                    String table = "target.with space_" + method.name();
                    String quotedTable = SqlText.quoteIdentifier(table);
                    statement.execute("CREATE TABLE " + quotedSchema + "." + quotedTable + " (id INT)");
                    byte[] csv = "11\n22\n".getBytes(StandardCharsets.UTF_8);
                    int rows = connection.unwrap(LakeConnectionExtension.class).loadStreamToTable(
                            "INSERT INTO " + quotedTable + " FROM @_databend_load FILE_FORMAT=(TYPE=CSV)",
                            new ByteArrayInputStream(csv), csv.length, method);
                    Assert.assertEquals(rows, 2);
                    try (ResultSet result = statement.executeQuery("SELECT SUM(id) FROM " + quotedTable)) {
                        Assert.assertTrue(result.next());
                        Assert.assertEquals(result.getInt(1), 33);
                    }
                }
                connection.setSchema("default");
                Assert.expectThrows(SQLException.class, () -> connection.setSchema("default; SELECT 123 --"));
                Assert.assertEquals(connection.getSchema(), "default");
            } finally {
                statement.execute("DROP DATABASE IF EXISTS " + quotedSchema);
            }
        }
    }

    @Test(groups = {"IT"})
    public void testLoadRejectsAdditionalStatementBeforeReadingStream() throws Exception {
        try (Connection connection = Utils.createConnection("default")) {
            LakeConnectionExtension extension = connection.unwrap(LakeConnectionExtension.class);
            for (LakeConnectionExtension.LoadMethod method : LakeConnectionExtension.LoadMethod.values()) {
                ByteArrayInputStream stream = new ByteArrayInputStream(new byte[] {1, 2, 3});
                Assert.expectThrows(SQLException.class, () -> extension.loadStreamToTable(
                        "INSERT INTO t FROM @_databend_load; DROP TABLE sentinel", stream, 3, method));
                Assert.assertEquals(stream.available(), 3);
            }
        }
    }

    @Test(groups = {"IT"})
    public void testSpecialObjectNamesRoundTrip() throws Exception {
        byte[] content = "security test bytes".getBytes(StandardCharsets.UTF_8);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String prefix = "jdbc_security/" + suffix + "/dir with space";
        String file = "f_" + suffix + "'; SELECT 123 -- %20.csv";
        String stage = "stage_" + suffix;
        try (Connection connection = Utils.createConnection("default");
             Statement statement = connection.createStatement()) {
            LakeConnectionExtension extension = connection.unwrap(LakeConnectionExtension.class);
            statement.execute("CREATE STAGE " + stage);
            try {
                for (String stageName : new String[] {null, stage}) {
                    for (String directory : new String[] {"", prefix}) {
                        extension.uploadStream(stageName, directory, new ByteArrayInputStream(content), file, content.length, false);
                        String path = directory.isEmpty() ? file : directory + "/" + file;
                        try (java.io.InputStream input = extension.downloadStream(stageName, path)) {
                            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int size;
                            while ((size = input.read(buffer)) != -1) {
                                output.write(buffer, 0, size);
                            }
                            Assert.assertEquals(output.toByteArray(), content);
                        }
                    }
                }
            } finally {
                statement.execute("DROP STAGE " + stage);
            }
        }
    }
}
