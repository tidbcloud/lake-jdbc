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
        // An odd backslash run can hide a double-quote delimiter from the
        // lexer. Prefer ANSI quoting otherwise: older servers cannot parse
        // doubled backticks even though current servers accept them.
        int backslashes = 0;
        boolean ansiSafe = true;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '\\') {
                backslashes++;
            } else {
                if (ch == '"' && backslashes % 2 != 0) {
                    ansiSafe = false;
                }
                backslashes = 0;
            }
        }
        ansiSafe &= backslashes % 2 == 0;
        if (!ansiSafe && value.indexOf('`') >= 0) {
            throw new SQLException("Identifier cannot be quoted safely across supported Lake server versions");
        }
        String delimiter = ansiSafe ? "\"" : "`";
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
