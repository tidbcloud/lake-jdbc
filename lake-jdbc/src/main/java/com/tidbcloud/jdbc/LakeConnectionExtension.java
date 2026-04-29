package com.tidbcloud.jdbc;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * The SnowflakeConnection interface contains Snowflake-specific methods.
 * providing specialized methods for interacting with Lake stages and loading data efficiently.
 * <p>
 * This interface extends standard JDBC connection capabilities to support streaming
 * operations for uploading/downloading files to/from internal stages, as well as direct
 * streaming data loading into target tables. Ideal for handling large files or continuous
 * data streams in Lake.
 * </p>
 */
public interface LakeConnectionExtension {

    /**
     * Enumeration of available loading strategies for streaming data into tables.
     */
    enum LoadMethod {
        /**
         * Load strategy that first uploads the stream to a Lake internal stage,
         * then loads the data from the stage into the target table.
         * Suitable for large files or scenarios requiring temporary storage.
         */
        STAGE,

        /**
         * Direct streaming strategy that loads data directly into the target table
         * without intermediate stage storage.
         * Optimized for real-time or small-to-medium size data streams.
         */
        STREAMING
    }


    void uploadStream(String stageName, String destPrefix, InputStream inputStream, String destFileName, long fileSize, boolean compressData) throws SQLException;

    /**
     * Download a file from the databend internal stage, the data would be downloaded as one file with no split.
     *
     * @param stageName the stage which contains the file
     * @param sourceFileName the file name in the stage
     * @return the input stream of the file
     * @throws SQLException failed to download input stream
     */
    InputStream downloadStream(String stageName, String sourceFileName) throws SQLException;

    /**
     * Loads data from an input stream directly into a target Lake table using the specified SQL command.
     * Supports two loading strategies via {@link LoadMethod}.
     *
     * @param sql         SQL command with Lake's load syntax:
     *                    {@code INSERT INTO <table> [(<columns>)] FROM @_databend_load [file_format=(...)]}
     * @param inputStream Input stream containing the data to load into the table
     * @param fileSize    Size of the data (in bytes) to be loaded
     * @param loadMethod  Loading strategy ({@link LoadMethod#STAGE} or {@link LoadMethod#STREAMING})
     * @return Number of rows successfully loaded into the target table
     * @throws SQLException If the load operation fails (e.g., invalid SQL, stream errors, or data format issues)
     */
    int loadStreamToTable(String sql, InputStream inputStream, long fileSize, LoadMethod loadMethod) throws SQLException;
}
