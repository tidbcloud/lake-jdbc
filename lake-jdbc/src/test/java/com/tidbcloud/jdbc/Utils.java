package com.tidbcloud.jdbc;

import java.sql.*;
import java.util.Properties;

public class Utils {
    private static final String TEST_EXTRA_QUERY = "LAKE_JDBC_TEST_EXTRA_QUERY";
    private static final String TEST_QUERY_RESULT_FORMAT = "LAKE_JDBC_TEST_QUERY_RESULT_FORMAT";

    static String port = System.getenv("LAKE_TEST_CONN_PORT") != null ? System.getenv("LAKE_TEST_CONN_PORT").trim() : "8000";
    static String username = "tidbcloud";
    static String password = "tidbcloud";

    public static String baseURL() {
        return buildURL(null, null);
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
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
        return DriverManager.getConnection(url, p);
    }


    public static Connection createConnectionWithPresignedUrlDisable() throws SQLException {
        String url = buildURL(null, "presigned_url_disabled=true");
        return DriverManager.getConnection(url, username, password);
    }

    private static String buildURL(String database, String extraQuery) {
        StringBuilder url = new StringBuilder("jdbc:lake://localhost:").append(port);
        if (database != null && !database.isEmpty()) {
            url.append("/").append(database);
        }
        appendQueryParameter(url, testQueryResultFormat());
        appendQueryParameter(url, testExtraQuery());
        appendQueryParameter(url, extraQuery);
        return url.toString();
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
        url.append(url.indexOf("?") >= 0 ? "&" : "?").append(query);
    }

    public static int countTable(Statement statement, String table) throws SQLException {
        ResultSet r = statement.executeQuery(String.format("select count(*) from %s", table));
        r.next();
        return r.getInt(1);
    }
}
