package com.tidbcloud.jdbc.internal.session;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Validate stage paths before either SQL generation or stage-upload headers. */
final class StagePaths {
    private StagePaths() {
    }

    static String stage(String value) throws SQLException {
        String stage = value == null ? "~" : value.replaceAll("/+$", "");
        rejectControls(stage);
        if (stage.isEmpty() || stage.indexOf('/') >= 0 || stage.indexOf('\\') >= 0
                || stage.equals(".") || stage.equals("..")) {
            throw new SQLException("Invalid stage name");
        }
        return stage;
    }

    static String path(String value, boolean allowEmpty) throws SQLException {
        if (value == null) {
            throw new SQLException("Stage path must not be null");
        }
        rejectControls(value);
        rejectTraversal(value);
        // Check encoded traversal too, without changing literal object-key bytes.
        String inspected = value;
        for (int i = 0; i < 16; i++) {
            String decoded = decodePercents(inspected);
            if (decoded.equals(inspected)) {
                break;
            }
            rejectControls(decoded);
            rejectTraversal(decoded);
            inspected = decoded;
            if (i == 15) {
                throw new SQLException("Excessively encoded stage path");
            }
        }
        List<String> parts = new ArrayList<>();
        for (String part : value.split("/")) {
            if (!part.isEmpty() && !part.equals(".")) {
                parts.add(part);
            }
        }
        String path = String.join("/", parts);
        if (!allowEmpty && path.isEmpty()) {
            throw new SQLException("Stage file path must not be empty");
        }
        return path;
    }

    private static void rejectTraversal(String value) throws SQLException {
        if (value.indexOf('\\') >= 0) {
            throw new SQLException("Backslash is not a stage path separator");
        }
        for (String part : value.split("/")) {
            if (part.equals("..")) {
                throw new SQLException("Parent traversal is not allowed in stage paths");
            }
        }
    }

    private static void rejectControls(String value) throws SQLException {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < 32 || value.charAt(i) == 127) {
                throw new SQLException("Stage names and paths must contain no control characters");
            }
        }
    }

    private static String decodePercents(String value) {
        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == '%' && i + 2 < value.length()) {
                int high = Character.digit(value.charAt(i + 1), 16);
                int low = Character.digit(value.charAt(i + 2), 16);
                if (high >= 0 && low >= 0) {
                    decoded.append((char) (high * 16 + low));
                    i += 2;
                    continue;
                }
            }
            decoded.append(value.charAt(i));
        }
        return decoded.toString();
    }
}
