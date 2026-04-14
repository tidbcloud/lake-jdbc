package com.tidbcloud.jdbc;

import com.tidbcloud.jdbc.cloud.LakeCopyParams;
import com.tidbcloud.jdbc.cloud.LakeStage;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class TestCopyInto {
    @Test(groups = {"UNIT"})
    public void TestGenSQL() {
        LakeStage s = LakeStage.builder().stageName("~").path("a/b/c").build();
        List<String> files = new ArrayList<>();
        files.add("file.csv");
        String sql = LakeConnection.getCopyIntoSql("db1", LakeCopyParams.builder().setFiles(files).setLakeStage(s).setDatabaseTableName("tb1").build());
        assertEquals(sql.trim(), "COPY INTO db1.tb1 FROM @~/a/b/c FILES = ('file.csv') FILE_FORMAT = ( type = 'CSV' )");
        sql = LakeConnection.getCopyIntoSql(null, LakeCopyParams.builder().setLakeStage(s).setDatabaseTableName("tb1").build());
        assertEquals(sql.trim(), "COPY INTO tb1 FROM @~/a/b/c FILE_FORMAT = ( type = 'CSV' )");
    }
}
