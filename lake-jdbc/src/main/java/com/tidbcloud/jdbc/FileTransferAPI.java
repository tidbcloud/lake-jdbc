package com.tidbcloud.jdbc;

import com.tidbcloud.jdbc.cloud.LakeCopyParams;

import java.io.InputStream;
import java.sql.SQLException;

/**
 * <strong>Deprecated.</strong> This interface has been replaced by {@link LakeConnection}.
 * <p>
 * Deprecated since version 4.0.1. Scheduled for removal in a future release.
 * Please migrate to {@link LakeConnection} for equivalent functionality.
 */
@Deprecated
public interface FileTransferAPI {
    /**
     *  <strong>Deprecated.</strong> Use {@link LakeConnection#uploadStream(String, String, InputStream, String, long, boolean)} instead.
     * Upload inputStream to the lake internal stage, the data would be uploaded as one file with no split.
     * Caller should close the input stream after the upload is done.
     *
     * @param stageName the stage which receive uploaded file
     * @param destPrefix the prefix of the file name in the stage
     * @param inputStream the input stream of the file
     * @param destFileName the destination file name in the stage
     * @param fileSize the file size in the stage
     * @param compressData whether to compress the data
     * @throws SQLException failed to upload input stream
     *
     * @deprecated Replaced by LakeConnection.uploadStream() since version 4.0.1
     */
    void uploadStream(String stageName, String destPrefix, InputStream inputStream, String destFileName, long fileSize, boolean compressData) throws SQLException;

    /**
     *  <strong>Deprecated.</strong> Use {@link LakeConnection#downloadStream(String, String)} instead.
     * Download a file from the lake internal stage, the data would be downloaded as one file with no split.
     *
     * @param stageName the stage which contains the file
     * @param sourceFileName the file name in the stage
     * @param decompress whether to decompress the data
     * @return the input stream of the file
     * @throws SQLException failed to download input stream
     *
     * @deprecated Replaced by LakeConnection.downloadStream() since version 4.0.1
     */
    InputStream downloadStream(String stageName, String sourceFileName, boolean decompress) throws SQLException;

    /**
     * Copy into the target table from files on the internal stage
     * Documentation: <a href="https://docs.tidbcloud.com/sql-commands/dml/dml-copy-into-table">...</a>
     *
     * @param database the target table's database
     * @param tableName the target table name
     * @param params copy options and file options
     * @throws SQLException fail to copy into table
     * @deprecated execute the Copy SQL directly
     */
    void copyIntoTable(String database, String tableName, LakeCopyParams params) throws SQLException;
}
