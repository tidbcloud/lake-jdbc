package com.tidbcloud.jdbc;

import com.tidbcloud.jdbc.internal.session.PaginationOptions;
import com.tidbcloud.jdbc.internal.session.SessionHandleConfig;
import okhttp3.OkHttpClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@Test(timeOut = 10000)
public class TestLakeDriverUri {
    private static LakeDriverUri createDriverUri(String url)
            throws SQLException {
        Properties properties = new Properties();

        return LakeDriverUri.create(url, properties);
    }

    private static void assertInvalid(String url, String prefix) {
        SQLException exception = Assert.expectThrows(SQLException.class, () -> createDriverUri(url));
        String msg = exception.getMessage();
        Assert.assertTrue(
                msg.startsWith(prefix),
                "error message not start with " + prefix + ":" + msg
        );
    }

    private static void assertPaginationConfig(SessionHandleConfig config, int waitTimeSecs, int maxRowsInBuffer,
                                               int maxRowsPerPage) {
        Assert.assertEquals(config.getWaitTimeSecs().intValue(), waitTimeSecs);
        Assert.assertEquals(config.getMaxRowsInBuffer().intValue(), maxRowsInBuffer);
        Assert.assertEquals(config.getMaxRowsPerPage().intValue(), maxRowsPerPage);
    }

    @Test(groups = {"UNIT"})
    public void testInvalidUri() {
        // missing jdbc: prefix
        assertInvalid("test", "Invalid JDBC URL: test");

        // empty jdbc: url
        assertInvalid("jdbc:", "Invalid JDBC URL: jdbc:");
        assertInvalid("jdbc:lake://localhost:8080/default?SSL=", "Connection property 'ssl' value is empty");
        assertInvalid("jdbc:lake://localhost:8080?SSL=0", "Connection property 'ssl' value is invalid: 0");

    }

    @Test(groups = {"UNIT"})
    public void testBasic() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://http://localhost", null);
        Assert.assertEquals(uri.getUri().getScheme(), "http");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 8000);
    }

    @Test(groups = {"UNIT"})
    public void testMultiHostNotSupported() {
        assertInvalid("jdbc:lake://localhost:8000,localhost:8001/default", "Multiple hosts in JDBC URL are not supported");
    }

    @Test(groups = {"UNIT"})
    public void testDefaultSSL() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://localhost", null);

        Assert.assertEquals(uri.getUri().getScheme(), "http");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 8000);
    }

    @Test(groups = {"UNIT"})
    public void testSSLSetFalse() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://localhost?SSL=false", null);

        Assert.assertEquals(uri.getUri().getScheme(), "http");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 8000);
    }

    @Test(groups = {"UNIT"})
    public void testSSLSetTrue() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://localhost?ssl=true", null);

        Assert.assertEquals(uri.getUri().getScheme(), "https");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 443);
    }

    @Test(groups = {"UNIT"})
    public void testTlsConfigurationOnlyAppliesToSecureConnections() throws SQLException {
        OkHttpClient defaultClient = new OkHttpClient();

        OkHttpClient.Builder httpBuilder = defaultClient.newBuilder();
        createDriverUri("jdbc:lake://localhost").setupClient(httpBuilder);
        OkHttpClient httpClient = httpBuilder.build();

        OkHttpClient.Builder firstHttpsBuilder = defaultClient.newBuilder();
        createDriverUri("jdbc:lake://localhost?ssl=true").setupClient(firstHttpsBuilder);
        OkHttpClient firstHttpsClient = firstHttpsBuilder.build();
        OkHttpClient.Builder secondHttpsBuilder = defaultClient.newBuilder();
        createDriverUri("jdbc:lake://localhost?ssl=true").setupClient(secondHttpsBuilder);
        OkHttpClient secondHttpsClient = secondHttpsBuilder.build();
        OkHttpClient.Builder sslModeBuilder = defaultClient.newBuilder();
        createDriverUri("jdbc:lake://http://localhost?sslmode=enable").setupClient(sslModeBuilder);
        OkHttpClient sslModeClient = sslModeBuilder.build();

        Assert.assertSame(httpClient.hostnameVerifier(), defaultClient.hostnameVerifier());
        Assert.assertSame(httpClient.sslSocketFactory(), defaultClient.sslSocketFactory());
        Assert.assertSame(firstHttpsClient.hostnameVerifier(), secondHttpsClient.hostnameVerifier());
        Assert.assertSame(firstHttpsClient.sslSocketFactory(), secondHttpsClient.sslSocketFactory());
        Assert.assertSame(firstHttpsClient.hostnameVerifier(), sslModeClient.hostnameVerifier());
        Assert.assertSame(firstHttpsClient.sslSocketFactory(), sslModeClient.sslSocketFactory());
        Assert.assertSame(firstHttpsClient.hostnameVerifier(), defaultClient.hostnameVerifier());
        Assert.assertNotSame(firstHttpsClient.sslSocketFactory(), defaultClient.sslSocketFactory());
    }

    @Test(groups = {"UNIT"})
    public void testSSLCustomPort() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://localhost:33101", null);

        Assert.assertEquals(uri.getUri().getScheme(), "http");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 33101);
    }

    @Test(groups = {"UNIT"})
    public void testSSLCustomPort2() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://http://localhost:33101", null);

        Assert.assertEquals(uri.getUri().getScheme(), "http");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 33101);

    }

    @Test(groups = {"UNIT"})
    public void testUser() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://u1@localhost:33101?password=p1", null);

        Assert.assertEquals(uri.getProperties().get("user"), "u1");
        Assert.assertEquals(uri.getProperties().get("password"), "p1");
        Assert.assertEquals(uri.getDatabase(), "default");
    }

    @Test(groups = {"UNIT"})
    public void testUserDatabase() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://u1@localhost:33101/db1?password=p1", null);

        Assert.assertEquals(uri.getProperties().get("user"), "u1");
        Assert.assertEquals(uri.getProperties().get("password"), "p1");
        Assert.assertEquals(uri.getDatabase(), "db1");
    }

    @Test(groups = {"UNIT"})
    public void testUserDatabasePath() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://u1@localhost:33101/db1?password=p1&database=db2", null);

        Assert.assertEquals(uri.getProperties().get("user"), "u1");
        Assert.assertEquals(uri.getProperties().get("password"), "p1");
        Assert.assertEquals(uri.getDatabase(), "db2");
    }

    @Test(groups = {"UNIT"})
    public void testUserDatabaseProp() throws SQLException {
        Properties props = new Properties();
        props.setProperty("database", "db3");
        props.setProperty("SSL", "true");
        props.setProperty("binary_format", "base64");
        props.setProperty("sslmode", "enable");
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://u1@localhost:33101/db1?password=p1&database=db2&query_timeout=120&connection_timeout=15&socket_timeout=15", props);

        Assert.assertEquals(uri.getProperties().get("user"), "u1");
        Assert.assertEquals(uri.getProperties().get("password"), "p1");
        Assert.assertEquals(uri.getDatabase(), "db3");
        Assert.assertEquals(uri.getUri().getScheme(), "https");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 33101);
        Assert.assertEquals(uri.getConnectionTimeout().intValue(), 15);
        Assert.assertEquals(uri.getQueryTimeout().intValue(), 120);
        Assert.assertEquals(uri.getSocketTimeout().intValue(), 20);
        Assert.assertEquals(uri.getWaitTimeSecs().intValue(), PaginationOptions.getDefaultWaitTimeSec());
        Assert.assertEquals(uri.getMaxRowsInBuffer().intValue(), PaginationOptions.getDefaultMaxRowsInBuffer());
        Assert.assertEquals(uri.getMaxRowsPerPage().intValue(), PaginationOptions.getDefaultMaxRowsPerPage());
        Assert.assertFalse(uri.presignedUrlDisabled().booleanValue());
        Assert.assertTrue(uri.copyPurge().booleanValue());
        Assert.assertEquals("\\N", uri.nullDisplay());
        Assert.assertEquals("base64", uri.binaryFormat());
        Assert.assertEquals("enable", uri.getSslmode());
        assertPaginationConfig(uri.toSessionHandleConfig(), PaginationOptions.getDefaultWaitTimeSec(),
                PaginationOptions.getDefaultMaxRowsInBuffer(), PaginationOptions.getDefaultMaxRowsPerPage());
    }

    @Test(groups = {"UNIT"})
    public void testUserDatabasePropFull() throws SQLException {
        Properties props = new Properties();
        props.setProperty("database", "db3");
        props.setProperty("SSL", "true");
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://u1@localhost:33101/db1?password=p1&database=db2&query_timeout=120&connection_timeout=15&socket_timeout=15&presigned_url_disabled=true&wait_time_secs=1&max_rows_in_buffer=10&max_rows_per_page=5", props);

        Assert.assertEquals(uri.getProperties().get("user"), "u1");
        Assert.assertEquals(uri.getProperties().get("password"), "p1");
        Assert.assertEquals(uri.getDatabase(), "db3");
        Assert.assertEquals(uri.getUri().getScheme(), "https");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 33101);
        Assert.assertEquals(uri.getQueryTimeout().intValue(), 120);
        Assert.assertEquals(uri.getConnectionTimeout().intValue(), 15);
        Assert.assertEquals(uri.getSocketTimeout().intValue(), 15);
        Assert.assertEquals(uri.getWaitTimeSecs().intValue(), 1);
        Assert.assertEquals(uri.getMaxRowsInBuffer().intValue(), 10);
        Assert.assertEquals(uri.getMaxRowsPerPage().intValue(), 5);
        Assert.assertTrue(uri.presignedUrlDisabled().booleanValue());
        Assert.assertTrue(uri.copyPurge().booleanValue());
        Assert.assertEquals("", uri.binaryFormat().toString());
        assertPaginationConfig(uri.toSessionHandleConfig(), 1, 10, 5);
    }

    @Test(groups = {"UNIT"})
    public void testFull() throws SQLException {
        Properties props = new Properties();
        props.setProperty("database", "db3");
        props.setProperty("SSL", "true");
        props.setProperty("presigned_url_disabled", "false");
        props.setProperty("wait_time_secs", "9");
        props.setProperty("max_rows_in_buffer", "11");
        props.setProperty("max_rows_per_page", "7");
        props.setProperty("connection_time", "15");
        props.setProperty("warehouse", "test");
        props.setProperty("tenant", "tenant1");
        props.setProperty("strnullasnull", String.valueOf(false));
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://u1@localhost:33101/db1?password=p1&database=db2&tenant=tenant1&warehouse=test&null_display=null&connection_timeout=15&socket_timeout=15&presigned_url_disabled=true&wait_time_secs=1&max_rows_in_buffer=10&max_rows_per_page=5", props);

        Assert.assertEquals(uri.getProperties().get("user"), "u1");
        Assert.assertEquals(uri.getProperties().get("password"), "p1");
        Assert.assertEquals(uri.getProperties().get("warehouse"), "test");
        Assert.assertEquals(uri.getProperties().get("tenant"), "tenant1");
        Assert.assertEquals(uri.getDatabase(), "db3");
        Assert.assertEquals(uri.getUri().getScheme(), "https");
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getUri().getPort(), 33101);
        Assert.assertEquals(uri.getConnectionTimeout().intValue(), 15);
        // wait_time_secs + 10
        Assert.assertEquals(uri.getSocketTimeout().intValue(), 19);
        Assert.assertEquals(uri.getWaitTimeSecs().intValue(), 9);
        Assert.assertEquals(uri.getMaxRowsInBuffer().intValue(), 11);
        Assert.assertEquals(uri.getMaxRowsPerPage().intValue(), 7);
        Assert.assertFalse(uri.presignedUrlDisabled().booleanValue());
        Assert.assertEquals("null", uri.nullDisplay().toString());
        Assert.assertFalse(uri.getStrNullAsNull());
    }

    @Test(groups = {"UNIT"})
    public void testQueryResultFormat() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://localhost:8000/default?query_result_format=arrow", null);
        Assert.assertEquals(uri.getQueryResultFormat(), "arrow");
    }

    @Test(groups = {"UNIT"})
    public void testQueryResultFormatNormalizesCase() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://localhost:8000/default?query_result_format=ARROW", null);
        Assert.assertEquals(uri.getQueryResultFormat(), "arrow");
    }

    @Test(groups = {"UNIT"})
    public void testInvalidQueryResultFormat() {
        assertInvalid("jdbc:lake://localhost:8000/default?query_result_format=csv", "Connection property 'query_result_format' value is invalid: csv");
    }

    @Test(groups = {"UNIT"})
    public void testSessionSettingsParsingIgnoresMalformedEntries() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://localhost:8000/default?session_settings=max_threads=4,timezone=UTC,malformed,enable_planner_v2=1",
                null);

        Map<String, String> expected = new HashMap<>();
        expected.put("max_threads", "4");
        expected.put("timezone", "UTC");
        expected.put("enable_planner_v2", "1");
        Assert.assertEquals(uri.getSessionSettings(), expected);
    }

    @Test(groups = {"UNIT"})
    public void testUrlUserPasswordKeepsReservedCharacters() throws SQLException {
        // The password is used verbatim as the HTTP basic-auth credential, so it must not be
        // re-encoded: p@ss:wo@rd has to reach the server as p@ss:wo@rd.
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://user:p@ss:wo@rd@localhost:8000/default", null);
        Assert.assertEquals(uri.getProperties().getProperty("user"), "user");
        Assert.assertEquals(uri.getProperties().getProperty("password"), "p@ss:wo@rd");
    }

    @Test(groups = {"UNIT"})
    public void testUrlUserPasswordDecodesPercentEscapes() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://us%65r:p%2Fss%40word@localhost:8000/default", null);
        Assert.assertEquals(uri.getProperties().getProperty("user"), "user");
        Assert.assertEquals(uri.getProperties().getProperty("password"), "p/ss@word");
    }

    @Test(groups = {"UNIT"})
    public void testUrlUserPasswordKeepsPlusLiteral() throws SQLException {
        // form-urlencoded decoding would turn '+' into a space, userinfo decoding must not.
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://user:a+b@localhost:8000/default", null);
        Assert.assertEquals(uri.getProperties().getProperty("password"), "a+b");
    }

    @Test(groups = {"UNIT"})
    public void testUrlUserPasswordDecodesUtf8PercentEscapes() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://user:p%C3%A4ss@localhost:8000/default", null);
        Assert.assertEquals(uri.getProperties().getProperty("password"), "päss");
    }

    @Test(groups = {"UNIT"})
    public void testSchemePrefixedUserInfoKeepsPortAndCredentials() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://https://user:password@host:8443/default", null);

        Assert.assertEquals(uri.getUri().getScheme(), "https");
        Assert.assertEquals(uri.getUri().getHost(), "host");
        Assert.assertEquals(uri.getUri().getPort(), 8443);
        Assert.assertNull(uri.getUri().getUserInfo());
        Assert.assertEquals(uri.getProperties().getProperty("user"), "user");
        Assert.assertEquals(uri.getProperties().getProperty("password"), "password");
    }

    @Test(groups = {"UNIT"})
    public void testSchemePrefixedUserInfoKeepsPlainHttpPort() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://http://user:pass@localhost:8000/default", null);

        Assert.assertEquals(uri.getUri().getScheme(), "http");
        Assert.assertEquals(uri.getUri().getPort(), 8000);
        Assert.assertEquals(uri.getProperties().getProperty("user"), "user");
        Assert.assertEquals(uri.getProperties().getProperty("password"), "pass");
    }

    @Test(groups = {"UNIT"})
    public void testAtSignInPathIsKeptAsPartOfTheDatabaseName() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://localhost:8000/db@pfx", null);
        Assert.assertEquals(uri.getUri().getHost(), "localhost");
        Assert.assertEquals(uri.getProperties().getProperty("database"), "db@pfx");
    }

    @Test(groups = {"UNIT"})
    public void testIpv6HostWithUserInfo() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://user:pw@[::1]:8000/default", null);
        Assert.assertEquals(uri.getUri().getHost(), "[::1]");
        Assert.assertEquals(uri.getUri().getPort(), 8000);
        Assert.assertEquals(uri.getProperties().getProperty("user"), "user");
        Assert.assertEquals(uri.getProperties().getProperty("password"), "pw");
    }

    @Test(groups = {"UNIT"})
    public void testCredentialQueryParameterCaseVariantsAreNotRetained() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://localhost:8000/default?Password=secret&ACCESS_TOKEN=tok&warehouse=w1", null);

        Assert.assertEquals(uri.getProperties().getProperty("password"), "secret");
        Assert.assertEquals(uri.getProperties().getProperty("access_token"), "tok");
        Assert.assertEquals(uri.getUri().getQuery(), "warehouse=w1");
    }

    @Test(groups = {"UNIT"})
    public void testUriWithoutRemainingQueryParametersHasNoQuery() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://localhost:8000/default?password=secret&accesstoken=tok", null);

        Assert.assertNull(uri.getUri().getQuery());
    }

    @Test(groups = "IT")
    public void TestSetSchema() throws SQLException {
        try (Connection connection = Utils.createConnection()) {
            connection.createStatement().execute("create or replace database test2");
            connection.createStatement().execute("create or replace table test2.test2(id int)");
            connection.setSchema("test2");
            Assert.assertEquals(connection.getSchema(), "test2");
            connection.createStatement().execute("insert into test2 values (1)");
        }
    }

    @Test(groups = "IT")
    public void TestSetSchemaFailureDoesNotUpdateLocalState() throws SQLException {
        try (Connection connection = Utils.createConnection("default")) {
            SQLException exception = Assert.expectThrows(SQLException.class,
                    () -> connection.setSchema("test_schema_missing"));
            Assert.assertNotNull(exception);
            Assert.assertEquals(connection.getSchema(), "default");
        }
    }

    @Test(groups = "IT")
    public void TestSetSessionSettings() throws SQLException {
        Properties props = new Properties();
        // set session settings
        props.setProperty("session_settings", "max_threads=1,query_tag=tag1");
        props.setProperty("user", Utils.getUsername());
        props.setProperty("password", Utils.getPassword());
        try (Connection connection = Utils.createConnection("default", props)) {
            Statement statement = connection.createStatement();
            statement.execute("show settings");
            ResultSet r = statement.getResultSet();
            Map<String, String> settings = new HashMap<>();
            while (r.next()) {
                settings.put(r.getString("name"), r.getString("value"));
            }
            Assert.assertEquals(settings.get("max_threads"), "1");
            Assert.assertEquals(settings.get("query_tag"), "tag1");
        }
    }

    @Test(groups = {"UNIT"})
    public void testAtSignInQueryValueDoesNotRedefineHost() throws SQLException {
        // An '@' in a query-string value must not be treated as the userinfo delimiter: that would
        // make the driver treat the query value as the host and ship credentials to it.
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://user:pw@real.host:443/db?warehouse=x@evil.example.com", null);

        Assert.assertEquals(uri.getUri().getHost(), "real.host");
        Assert.assertEquals(uri.getUri().getPort(), 443);
        Assert.assertEquals(uri.getProperties().getProperty("user"), "user");
        Assert.assertEquals(uri.getProperties().getProperty("password"), "pw");
        Assert.assertEquals(uri.getProperties().getProperty("warehouse"), "x@evil.example.com");
    }

    @Test(groups = {"UNIT"})
    public void testAtSignInQueryValueWithoutUserInfoKeepsHost() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://real.host:8000/db?null_display=@null@", null);

        Assert.assertEquals(uri.getUri().getHost(), "real.host");
        Assert.assertNull(uri.getProperties().getProperty("user"));
        Assert.assertEquals(uri.getProperties().getProperty("null_display"), "@null@");
    }

    @Test(groups = {"UNIT"})
    public void testCredentialQueryParametersAreNotRetainedInBaseUri() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create(
                "jdbc:lake://localhost:8000/default?password=secret&accesstoken=tok123&warehouse=w1", null);

        // credentials are still available as connection properties
        Assert.assertEquals(uri.getProperties().getProperty("password"), "secret");
        Assert.assertEquals(uri.getProperties().getProperty("accesstoken"), "tok123");
        Assert.assertEquals(uri.getProperties().getProperty("warehouse"), "w1");

        // but they must not be replayed on every request, nor surface through getURL()
        String query = uri.getUri().getQuery();
        Assert.assertNotNull(query);
        Assert.assertFalse(query.contains("secret"), "base URI still carries the password: " + query);
        Assert.assertFalse(query.contains("tok123"), "base URI still carries the access token: " + query);
        Assert.assertEquals(query, "warehouse=w1");
    }

    @Test(groups = {"UNIT"})
    public void testUserInfoIsNotRetainedInBaseUri() throws SQLException {
        LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://user:pw@localhost:8000/default", null);

        Assert.assertEquals(uri.getProperties().getProperty("user"), "user");
        Assert.assertEquals(uri.getProperties().getProperty("password"), "pw");
        Assert.assertNull(uri.getUri().getUserInfo());
        Assert.assertFalse(uri.getUri().toString().contains("pw"));
    }
}
