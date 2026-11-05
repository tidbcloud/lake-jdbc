package com.tidbcloud.jdbc.internal.binding;

import java.sql.SQLException;

/** SQL rendering for driver-generated control statements. */
public final class SqlText {
    private SqlText() {
    }

    public static String escapeString(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static String quoteString(String value) {
        return "'" + escapeString(value) + "'";
    }

    public static String quoteIdentifier(String value) throws SQLException {
        if (value == null || value.isEmpty() || value.indexOf('\0') >= 0) {
            throw new SQLException("SQL identifier must be non-empty and contain no NUL");
        }
        // The Lake lexer treats backslashes as escapes in double-quoted tokens.
        // Backtick tokens keep them literal, including before an embedded quote.
        String delimiter = value.indexOf('\\') >= 0 ? "`" : "\"";
        return delimiter + value.replace(delimiter, delimiter + delimiter) + delimiter;
    }

    public static void requireSingleStatement(String sql) throws SQLException {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SQLException("Control SQL must not be empty");
        }
        boolean terminated = false;
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (Character.isWhitespace(ch)) {
                continue;
            }
            if (sql.startsWith("--", i)) {
                while (i < sql.length() && sql.charAt(i) != '\n' && sql.charAt(i) != '\f') {
                    i++;
                }
                continue;
            }
            if (sql.startsWith("/*", i)) {
                int end = sql.indexOf("*/", i + 2);
                if (end < 0) {
                    throw new SQLException("Unterminated SQL comment");
                }
                i = end + 1;
                continue;
            }
            if (ch == ';') {
                terminated = true;
                continue;
            }
            if (terminated) {
                throw new SQLException("Control SQL must contain only one statement");
            }
            if (sql.startsWith("$$", i)) {
                int end = sql.indexOf("$$", i + 2);
                if (end < 0) {
                    throw new SQLException("Unterminated SQL string");
                }
                i = end + 1;
            } else if (ch == '\'' || ch == '"' || ch == '`') {
                i = quotedEnd(sql, i, ch);
            }
        }
    }

    private static int quotedEnd(String sql, int start, char quote) throws SQLException {
        for (int i = start + 1; i < sql.length(); i++) {
            char ch = sql.charAt(i);
            if (ch == '\\' && quote != '`') {
                i++;
            } else if (ch == quote) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
                    i++;
                } else {
                    return i;
                }
            }
        }
        throw new SQLException("Unterminated SQL quoted token");
    }
}
