package com.tidbcloud.jdbc;

import com.tidbcloud.client.QueryResults;
import com.tidbcloud.client.errors.QueryErrors;
import com.tidbcloud.jdbc.exception.LakeSQLException;
import org.testng.annotations.Test;

import java.sql.SQLException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class TestLakeSQLException {
    @Test
    public void testLakeWithQueryIdSqlExceptionIncludesQueryIdInMessage() {
        LakeSQLException exception = new LakeSQLException("boom", "abc123");

        assertEquals(exception.getMessage(), "boom [query_id=abc123]");
        assertEquals(exception.getSQLState(), "abc123");
    }

    @Test
    public void testResultsExceptionIncludesQueryIdInMessage() {
        QueryResults results = new QueryResults(
                "abc123",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                QueryErrors.builder().setCode(1001).setMessage("syntax error").build(),
                null,
                null,
                0L,
                null,
                null,
                null,
                null);

        SQLException exception = AbstractLakeResultSet.resultsException(results, "select 1");

        assertTrue(exception.getMessage().contains("query_id=abc123"));
        assertEquals(exception.getSQLState(), "1001");
    }

    @Test
    public void testLakeSQLExceptionPreservesOriginalSQLExceptionInformation() {
        IllegalStateException cause = new IllegalStateException("root cause");
        SQLException original = new SQLException("boom", "42000", 1234, cause);
        SQLException next = new SQLException("next");
        IllegalArgumentException suppressed = new IllegalArgumentException("suppressed");
        original.setNextException(next);
        original.addSuppressed(suppressed);

        LakeSQLException wrapped = new LakeSQLException(original, "abc123");

        assertEquals(wrapped.getMessage(), "boom [query_id=abc123]");
        assertEquals(wrapped.getSQLState(), "42000");
        assertEquals(wrapped.getErrorCode(), 1234);
        assertEquals(wrapped.getCause(), cause);
        assertEquals(wrapped.getNextException(), next);
        assertEquals(wrapped.getSuppressed().length, 1);
        assertEquals(wrapped.getSuppressed()[0], suppressed);
    }
}
