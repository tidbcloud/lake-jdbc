package com.tidbcloud.jdbc;

import java.sql.*;
import java.util.Properties;

public class Utils {
    private static final String TEST_EXTRA_QUERY = "LAKE_JDBC_TEST_EXTRA_QUERY";
    private static final String TEST_QUERY_RESULT_FORMAT = "LAKE_JDBC_TEST_QUERY_RESULT_FORMAT";
    private static final String TEST_JDBC_URL = "LAKE_TEST_JDBC_URL";
    private static final String TEST_USERNAME = "LAKE_TEST_USERNAME";
    private static final String TEST_PASSWORD = "LAKE_TEST_PASSWORD";

    static String port = System.getenv("LAKE_TEST_CONN_PORT") != null ? System.getenv("LAKE_TEST_CONN_PORT").trim() : "8000";
    static String username = envOrDefault(TEST_USERNAME, "tidbcloud");
    static String password = envOrDefault(TEST_PASSWORD, "tidbcloud");

    public static String baseURL() {
        return buildURL(null, null);
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }

    public static boolean isPresignedUrlDisabled() {
        String query = testExtraQuery();
        return query != null && query.contains("presigned_url_disabled=true");
    }

    public static boolean isRemoteTestEnvironment() {
        return trimToNull(System.getenv(TEST_JDBC_URL)) != null;
    }


    public static Connection createConnection()
            throws SQLException {
        return DriverManager.getConnection(baseURL(), username, password);
    }

    public static Connection createConnection(String database) throws SQLException {
        String url = buildURL(database, null);
        return DriverManager.getConnection(url, username, password);
    }

    public static Connection createConnection(String database, Properties p) throws SQLException {
        String url = buildURL(database, null);
        Properties props = new Properties();
        props.putAll(p);
        props.putIfAbsent("user", username);
        props.putIfAbsent("password", password);
        return DriverManager.getConnection(url, props);
    }


    public static Connection createConnectionWithPresignedUrlDisable() throws SQLException {
        String url = buildURL(null, "presigned_url_disabled=true");
        return DriverManager.getConnection(url, username, password);
    }

    private static String buildURL(String database, String extraQuery) {
        String configuredUrl = trimToNull(System.getenv(TEST_JDBC_URL));
        StringBuilder url = configuredUrl != null
                ? new StringBuilder(replaceDatabase(configuredUrl, database))
                : new StringBuilder(defaultLocalUrl(database));
        appendQueryParameter(url, testQueryResultFormat());
        appendQueryParameter(url, testExtraQuery());
        appendQueryParameter(url, extraQuery);
        return url.toString();
    }

    private static String defaultLocalUrl(String database) {
        StringBuilder url = new StringBuilder("jdbc:lake://localhost:").append(port);
        if (database != null && !database.isEmpty()) {
            url.append("/").append(database);
        }
        return url.toString();
    }

    private static String replaceDatabase(String url, String database) {
        if (database == null || database.isEmpty()) {
            return url;
        }
        int queryStart = url.indexOf('?');
        String base = queryStart >= 0 ? url.substring(0, queryStart) : url;
        String query = queryStart >= 0 ? url.substring(queryStart) : "";
        int schemeEnd = base.indexOf("://");
        if (schemeEnd < 0) {
            return base + "/" + database + query;
        }
        int pathStart = base.indexOf('/', schemeEnd + 3);
        if (pathStart < 0) {
            return base + "/" + database + query;
        }
        return base.substring(0, pathStart + 1) + database + query;
    }

    private static String testExtraQuery() {
        String query = System.getenv(TEST_EXTRA_QUERY);
        if (query == null || query.trim().isEmpty()) {
            return null;
        }
        return query.trim();
    }

    private static String testQueryResultFormat() {
        String format = System.getenv(TEST_QUERY_RESULT_FORMAT);
        if (format == null || format.trim().isEmpty()) {
            return null;
        }
        return "query_result_format=" + format.trim().toLowerCase();
    }

    private static void appendQueryParameter(StringBuilder url, String query) {
        if (query == null || query.isEmpty()) {
            return;
        }
        String key = query;
        int equals = query.indexOf('=');
        if (equals >= 0) {
            key = query.substring(0, equals);
        }
        if (url.indexOf(key + "=") >= 0) {
            return;
        }
        url.append(url.indexOf("?") >= 0 ? "&" : "?").append(query);
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = trimToNull(System.getenv(name));
        return value != null ? value : defaultValue;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static int countTable(Statement statement, String table) throws SQLException {
        ResultSet r = statement.executeQuery(String.format("select count(*) from %s", table));
        r.next();
        return r.getInt(1);
    }
}
