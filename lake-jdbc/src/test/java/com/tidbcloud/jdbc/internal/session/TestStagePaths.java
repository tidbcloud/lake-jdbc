package com.tidbcloud.jdbc.internal.session;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;

@Test(timeOut = 10000)
public class TestStagePaths {
    @Test(groups = {"UNIT"})
    public void testNormalizationAndTraversalRejection() throws Exception {
        Assert.assertEquals(StagePaths.stage(null), "~");
        Assert.assertEquals(StagePaths.stage("stage/"), "stage");
        Assert.assertEquals(StagePaths.path("/a//./b/", false), "a/b");
        Assert.assertEquals(StagePaths.path("", true), "");
        Assert.assertEquals(StagePaths.path("a/space ' ; -- %20.csv", false), "a/space ' ; -- %20.csv");
        for (String value : new String[] {"../secret", "a/../../secret", "a/..", "a\\..\\b",
                "a/%2e%2e/b", "a/%252e%252e/b", "a%2f..%2fb", "bad\nname", "bad\0name"}) {
            Assert.expectThrows(SQLException.class, () -> StagePaths.path(value, false));
        }
        for (String value : new String[] {"a/b", "..", "", "bad\rname"}) {
            Assert.expectThrows(SQLException.class, () -> StagePaths.stage(value));
        }
    }

    @Test(groups = {"UNIT"})
    public void testUploadAndDownloadRejectBeforeNetworkOrInputRead() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
            requests.incrementAndGet();
            throw new AssertionError("Unsafe paths must not send HTTP requests");
        }).build();
        for (String presign : new String[] {"on", "off"}) {
            LakeSessionHandle handle = new LakeSessionHandle(client,
                    SessionHandleConfig.builder().setBaseUri(URI.create("http://127.0.0.1:1"))
                            .setInitialSession(SessionState.createDefault()).build(), null);
            handle.initializePresign(presign, false);
            ByteArrayInputStream stream = new ByteArrayInputStream(new byte[] {1, 2, 3});
            Assert.expectThrows(SQLException.class, () -> handle.uploadStream("~", "../escape", stream, "f", 3, true));
            Assert.expectThrows(SQLException.class, () -> handle.uploadStream("~", "safe", stream, "../f", 3, false));
            Assert.expectThrows(SQLException.class, () -> handle.downloadStream("~", "../escape"));
            Assert.expectThrows(SQLException.class, () -> handle.uploadStream("bad/stage", "safe", stream, "f", 3, false));
            Assert.assertEquals(stream.available(), 3);
        }
        Assert.assertEquals(requests.get(), 0);
    }
}
