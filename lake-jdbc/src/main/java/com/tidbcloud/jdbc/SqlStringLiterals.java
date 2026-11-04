package com.tidbcloud.jdbc;

/**
 * Renders values as single-quoted SQL string literals for the lake server.
 *
 * <p>The server treats the backslash as the escape character inside a single-quoted literal (the
 * driver reports {@code \} from {@code DatabaseMetaData#getSearchStringEscape()}), so both the
 * backslash and the quote must be escaped - the backslash first. Escaping only the quote lets a
 * caller-supplied backslash consume the escaping backslash and close the literal, e.g.
 * {@code x\' or 1=1 -- } used to be rendered as {@code 'x\\' or 1=1 -- '} and the remainder was
 * parsed as SQL.
 *
 * <p>Both the prepared-statement setters and the metadata filter builders go through this class:
 * they splice caller-supplied values into SQL text, and having a single implementation keeps the
 * two paths from drifting apart.
 */
final class SqlStringLiterals {
    private SqlStringLiterals() {
    }

    static String escape(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    static String quote(String value) {
        return String.format("'%s'", escape(value));
    }
}
