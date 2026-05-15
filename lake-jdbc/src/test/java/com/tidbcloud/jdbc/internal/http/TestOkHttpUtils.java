package com.tidbcloud.jdbc.internal.http;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class TestOkHttpUtils {
    @Test(groups = {"UNIT"})
    public void testSetupSslUsesModernTlsSettings() {
        OkHttpClient defaultClient = new OkHttpClient();
        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        OkHttpUtils.setupSsl(builder);

        OkHttpClient client = builder.build();
        List<ConnectionSpec> connectionSpecs = client.connectionSpecs();
        Assert.assertFalse(connectionSpecs.isEmpty());
        Assert.assertTrue(connectionSpecs.get(0).isTls());
        Assert.assertEquals(connectionSpecs.get(0).tlsVersions(), Arrays.asList(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2));
        Assert.assertEquals(client.hostnameVerifier().getClass(), defaultClient.hostnameVerifier().getClass());
    }
}
