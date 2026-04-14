package com.tidbcloud.jdbc;

import com.tidbcloud.client.QueryRowField;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * We create a new ResultSet class constructed directly from fixed values or pre-query combinations of data.
 * This class is created to do some specific processing on some implementations of the DatabaseMetaData interface.
 * <p>
 * Since some of the queries returned from the server will be some different from the jdbc standard,
 * so that we need to make some modifications to the types, values, etc. after the returned results.
 * The actual returned datas from the interface is a new ResultSet.
 */
class LakeUnboundQueryResultSet extends AbstractLakeResultSet {

    private boolean closed = false;

    LakeUnboundQueryResultSet(Optional<Statement> statement, List<QueryRowField> schema, Iterator<List<Object>> results) {
        super(statement, schema, results, null, "NotQueryResultSet");
    }

    @Override
    public void close() throws SQLException {
        Statement statement = getStatement();
        if (statement != null) {
            statement.close();
        }
        this.closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }
}
