package com.tidbcloud.jdbc;

import com.tidbcloud.client.PaginationOptions;
import com.tidbcloud.jdbc.cloud.LakePresignClient;
import com.tidbcloud.jdbc.cloud.LakePresignClientV1;
import okhttp3.OkHttpClient;
import org.testng.annotations.Test;

import java.io.*;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;

public class TestLakePresignClient {

    private String generateRandomCSV(int lines) throws IOException {
        if (lines <= 0) {
            return "";
        }
        String tmpDir = System.getProperty("java.io.tmpdir");
        String csvPath = tmpDir + "/test.csv";
        try (FileWriter writer = new FileWriter(csvPath)) {
            for (int i = 0; i < lines; i++) {
                int num = (int) (Math.random() * 1000);
                writer.write("a,b,c," + num + "\n");
            }
        }
        return csvPath;
    }

    @Test(groups = {"LOCAL"})
    public void uploadFileAPI() throws IOException, SQLException {
        String filePath = null;
        try (Connection connection = Utils.createConnection()) {
            URI uri = (URI)  Compatibility.invokeMethodNoArg(connection, "getURI");
            OkHttpClient client  = (OkHttpClient)  Compatibility.invokeMethodNoArg(connection, "getHttpClient");
            LakePresignClient presignClient = new LakePresignClientV1(client,uri.toString());
            filePath = generateRandomCSV(10);
            File file = new File(filePath);
            InputStream inputStream = new FileInputStream(file);
            presignClient.presignUpload(null, inputStream, "~", "api/upload/", "1.csv", file.length(), true);
        } finally {
            //remove temp file
            if (filePath != null) {
                File file = new File(filePath);
                file.delete();
            }
        }
    }
}
