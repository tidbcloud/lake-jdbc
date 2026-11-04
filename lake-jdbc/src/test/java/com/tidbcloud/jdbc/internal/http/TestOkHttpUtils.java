package com.tidbcloud.jdbc.internal.http;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;
import okhttp3.tls.HeldCertificate;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class TestOkHttpUtils {
    @Test(groups = {"UNIT"}, expectedExceptions = CertificateException.class)
    public void testCachedSslRejectsUntrustedCertificates() throws CertificateException {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        OkHttpUtils.setupSsl(builder);
        OkHttpClient client = builder.build();
        HeldCertificate untrusted = new HeldCertificate.Builder().commonName("localhost").build();

        client.x509TrustManager().checkServerTrusted(
                new X509Certificate[] {untrusted.certificate()}, "ECDHE_ECDSA");
    }

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
