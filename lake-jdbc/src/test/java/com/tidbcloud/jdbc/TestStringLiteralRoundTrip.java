package com.tidbcloud.jdbc;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Integration coverage for the escaping rules documented on {@link SqlStringLiterals}.
 *
 * <p>The driver renders bound values as SQL literals, and {@link LakePreparedStatement} escapes the
 * backslash and the quote on the assumption that the server treats {@code \} as the escape
 * character inside a single-quoted literal. This test pins that assumption down by round-tripping
 * values through a live server instead of asserting on the rendered text alone.
 */
@Test(timeOut = 60000)
public class TestStringLiteralRoundTrip {

    @Test(groups = {"IT"})
    public void testSetStringRoundTripsSpecialCharacters() throws SQLException {
        String[] values = {
                "C:\\temp",
                "back\\slash",
                "x\\' or 1=1 -- ",
                "O'Reilly",
                "50%_off",
                "mixed \\\\' quotes'' and \\ backslashes",
                "trailing backslash\\",
                "",
        };
        try (Connection connection = Utils.createConnection()) {
            for (String value : values) {
                try (PreparedStatement statement = connection.prepareStatement("select ? as v")) {
                    statement.setString(1, value);
                    try (ResultSet resultSet = statement.executeQuery()) {
                        Assert.assertTrue(resultSet.next(), "no row for " + value);
                        Assert.assertEquals(resultSet.getString(1), value);
                    }
                }
            }
        }
    }
}
