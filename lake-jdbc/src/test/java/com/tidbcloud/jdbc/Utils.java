package com.tidbcloud.jdbc;

import java.sql.*;
import java.util.Properties;

public class Utils {

    static String host = System.getenv("LAKE_TEST_HOST") != null ? System.getenv("LAKE_TEST_HOST").trim() : "localhost";
    static String port = System.getenv("LAKE_TEST_CONN_PORT") != null ? System.getenv("LAKE_TEST_CONN_PORT").trim() : "8000";
    static String username = System.getenv("LAKE_TEST_USER") != null ? System.getenv("LAKE_TEST_USER").trim() : "databend";
    static String password = System.getenv("LAKE_TEST_PASSWORD") != null ? System.getenv("LAKE_TEST_PASSWORD").trim() : "databend";
    static String warehouse = System.getenv("LAKE_TEST_WAREHOUSE") != null ? System.getenv("LAKE_TEST_WAREHOUSE").trim() : null;
    static boolean ssl = System.getenv("LAKE_TEST_SSL") != null ? Boolean.parseBoolean(System.getenv("LAKE_TEST_SSL").trim()) : false;

    public static String baseURL() {
        String url = "jdbc:lake://" + host + ":" + port;
        String separator = "?";
        if (ssl) {
            url += separator + "ssl=true";
            separator = "&";
        }
        if (warehouse != null) {
            url += separator + "warehouse=" + warehouse;
        }
        return url;
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
        String base = baseURL();
        String url;
        if (base.contains("?")) {
            // Insert database before query params: jdbc:lake://host:port/database?params
            int qIdx = base.indexOf("?");
            url = base.substring(0, qIdx) + "/" + database + base.substring(qIdx);
        } else {
            url = base + "/" + database;
        }
        return DriverManager.getConnection(url, username, password);
    }

    public static Connection createConnection(String database, Properties p) throws SQLException {
        String base = baseURL();
        String url;
        if (base.contains("?")) {
            int qIdx = base.indexOf("?");
            url = base.substring(0, qIdx) + "/" + database + base.substring(qIdx);
        } else {
            url = base + "/" + database;
        }
        return DriverManager.getConnection(url, p);
    }


    public static Connection createConnectionWithPresignedUrlDisable() throws SQLException {
        String base = baseURL();
        String url = base + (base.contains("?") ? "&" : "?") + "presigned_url_disabled=true";
        return DriverManager.getConnection(url, username, password);
    }

    public static int countTable(Statement statement, String table) throws SQLException {
        ResultSet r = statement.executeQuery(String.format("select count(*) from %s", table));
        r.next();
        return r.getInt(1);
    }
}

