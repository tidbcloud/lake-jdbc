package com.tidbcloud.jdbc;

import okhttp3.OkHttpClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.Properties;

@Test(timeOut = 10000)
public class TestTlsMode {
    @Test(groups = {"UNIT"})
    public void testExplicitTlsModesCannotDowngradeToHttp() throws SQLException {
        OkHttpClient defaultClient = new OkHttpClient();
        for (String mode : new String[] {"enable", "require", "verify-ca", "verify-full", "REQUIRE"}) {
            LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://host/default?sslmode=" + mode, null);
            Assert.assertEquals(uri.getUri().getScheme(), "https", mode);
            Assert.assertEquals(uri.getUri().getPort(), "enable".equals(mode) ? 8000 : 443, mode);
            OkHttpClient.Builder builder = defaultClient.newBuilder();
            uri.setupClient(builder);
            OkHttpClient client = builder.build();
            Assert.assertSame(client.hostnameVerifier(), defaultClient.hostnameVerifier());
            Assert.assertNotSame(client.sslSocketFactory(), defaultClient.sslSocketFactory());
        }
    }

    @Test(groups = {"UNIT"})
    public void testSecureDefaultAndExplicitPortArePreserved() throws SQLException {
        Assert.assertEquals(LakeDriverUri.create("jdbc:lake://host", null).getUri().getScheme(), "https");
        Assert.assertEquals(LakeDriverUri.create("jdbc:lake://host", null).getUri().getPort(), 443);
        Assert.assertEquals(LakeDriverUri.create("jdbc:lake://host:8443?sslmode=require", null).getUri().getPort(), 8443);
        Properties properties = new Properties();
        properties.setProperty("sslmode", "require");
        properties.setProperty("ssl", "false");
        Assert.assertEquals(LakeDriverUri.create("jdbc:lake://host", properties).getUri().getScheme(), "https");
    }
}
