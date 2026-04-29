package com.tidbcloud.jdbc.examples;

import com.tidbcloud.jdbc.LakeConnection;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class LoadToTableFromStream {
    public static void main(String[] args) throws SQLException {
        uploadAndCopy();
    }

    static void uploadAndCopy() throws SQLException {
        // assuming the file and stage1 and table1 already exist
        String filePath = "data.csv";

        String stageName = "stage1";
        String prefix = "data_set1";
        String fileName = "data1";
        String path = "@stage1/data_set1/data1";

        String url = "jdbc:lake://localhost:8000";
        try(Connection conn = DriverManager.getConnection(url, "tidbcloud", "tidbcloud");
            Statement stmt = conn.createStatement())  {

            // upload
            InputStream inputStream = Files.newInputStream(Paths.get(filePath));
            long fileSize = Files.size(Paths.get("data.csv"));
            LakeConnection lakeConnection = conn.unwrap(LakeConnection.class);
            lakeConnection.uploadStream(stageName, prefix, inputStream, fileName, fileSize, false);

            // https://docs.tidbcloud.com/tidb-cloud/lakehouse-list-stage/
            String sql = String.format("list %s", path);
            try(ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    rs.getString(1);
                }
            }

            // copy into table
            // https://docs.tidbcloud.com/tidb-cloud/lakehouse-copy-into-table/
            sql = String.format("copy into table1 from %s file_format =(type=csv) purge=true", path);
            try(ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    rs.getString("File");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
