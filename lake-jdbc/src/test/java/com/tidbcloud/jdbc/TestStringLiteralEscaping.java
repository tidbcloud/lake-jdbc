package com.tidbcloud.jdbc;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Regression tests for SQL string-literal escaping in {@link LakePreparedStatement}.
 *
 * <p>The driver has no server-side prepared-statement protocol: bound values are rendered as SQL
 * literals and spliced into the statement text, so the escaping performed by the setters is the
 * only thing standing between a parameter value and SQL injection.
 */
@Test(timeOut = 10000)
public class TestStringLiteralEscaping {

    @Test(groups = {"UNIT"})
    public void testPlainValueIsOnlyWrapped() {
        Assert.assertEquals(LakePreparedStatement.escapeStringLiteral("abc"), "abc");
        Assert.assertEquals(LakePreparedStatement.escapeStringLiteral(""), "");
    }

    @Test(groups = {"UNIT"})
    public void testQuoteIsEscaped() {
        Assert.assertEquals(LakePreparedStatement.escapeStringLiteral("O'Reilly"), "O\\'Reilly");
    }

    @Test(groups = {"UNIT"})
    public void testBackslashIsEscaped() {
        Assert.assertEquals(LakePreparedStatement.escapeStringLiteral("a\\b"), "a\\\\b");
        Assert.assertEquals(LakePreparedStatement.escapeStringLiteral("\\"), "\\\\");
    }

    @Test(groups = {"UNIT"})
    public void testBackslashBeforeQuoteCannotCloseTheLiteral() {
        // x\' or 1=1 --  used to be rendered as 'x\\' or 1=1 -- ': the \\ is an escaped
        // backslash, the quote closes the literal and the rest is parsed as SQL. The fixed
        // escaper must escape the caller-supplied backslash first so the quote stays escaped.
        String escaped = LakePreparedStatement.escapeStringLiteral("x\\' or 1=1 -- ");
        Assert.assertEquals(escaped, "x\\\\\\' or 1=1 -- ");
        Assert.assertEquals(renderLiteral(escaped), "'x\\\\\\' or 1=1 -- '");
        // every quote is preceded by an odd number of backslashes, i.e. it is escaped
        Assert.assertTrue(isFullyEscaped(escaped));
    }

    @Test(groups = {"UNIT"})
    public void testClassicQuoteInjectionIsEscaped() {
        String escaped = LakePreparedStatement.escapeStringLiteral("x' OR '1'='1");
        Assert.assertEquals(escaped, "x\\' OR \\'1\\'=\\'1");
        Assert.assertTrue(isFullyEscaped(escaped));
    }

    @Test(groups = {"UNIT"})
    public void testEvenBackslashesBeforeQuoteStillEscapeTheQuote() {
        // two caller-supplied backslashes followed by a quote
        String escaped = LakePreparedStatement.escapeStringLiteral("x\\\\' OR 1=1 -- ");
        Assert.assertTrue(isFullyEscaped(escaped));
    }

    private static String renderLiteral(String escaped) {
        return String.format("'%s'", escaped);
    }

    /**
     * Returns true when every {@code '} in the escaped value is preceded by an odd number of
     * backslashes, which is what keeps it inside the literal under backslash-escape grammar.
     */
    private static boolean isFullyEscaped(String escaped) {
        for (int i = 0; i < escaped.length(); i++) {
            if (escaped.charAt(i) != '\'') {
                continue;
            }
            int backslashes = 0;
            for (int j = i - 1; j >= 0 && escaped.charAt(j) == '\\'; j--) {
                backslashes++;
            }
            if (backslashes % 2 == 0) {
                return false;
            }
        }
        return true;
    }
}
