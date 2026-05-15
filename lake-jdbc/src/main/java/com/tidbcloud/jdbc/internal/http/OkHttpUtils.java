package com.tidbcloud.jdbc.internal.http;

import com.google.common.base.CharMatcher;
import okhttp3.ConnectionSpec;
import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static java.util.Objects.requireNonNull;

public final class OkHttpUtils {
    private OkHttpUtils() {
    }

    public static Interceptor userAgentInterceptor(String userAgent) {
        return chain -> chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build());
    }

    public static Interceptor basicAuthInterceptor(String username, String password) {
        return chain -> chain.proceed(chain.request().newBuilder().header("Authorization", Credentials.basic(username, password)).build());
    }

    public static Interceptor tokenAuth(String accessToken) {
        requireNonNull(accessToken, "accessToken is null");
        checkArgument(CharMatcher.inRange((char) 33, (char) 126).matchesAllOf(accessToken));
        return chain -> chain.proceed(chain.request().newBuilder()
                .addHeader(AUTHORIZATION, "Bearer " + accessToken)
                .build());
    }

    public static void setupTimeouts(OkHttpClient.Builder clientBuilder, int timeout, TimeUnit unit) {
        clientBuilder
                .connectTimeout(timeout, unit)
                .readTimeout(timeout, unit)
                .writeTimeout(timeout, unit);
    }

    public static void setupSsl(OkHttpClient.Builder clientBuilder) {
        try {
            X509TrustManager trustManager = defaultTrustManager();
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] {trustManager}, new SecureRandom());
            clientBuilder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            clientBuilder.connectionSpecs(Arrays.asList(
                    new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                            .tlsVersions(TlsVersion.TLS_1_3, TlsVersion.TLS_1_2)
                            .build(),
                    ConnectionSpec.COMPATIBLE_TLS));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Error setting up SSL: " + e.getMessage(), e);
        }
    }

    private static X509TrustManager defaultTrustManager() throws GeneralSecurityException {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init((KeyStore) null);

        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
            throw new GeneralSecurityException("Unexpected default trust managers: " + Arrays.toString(trustManagers));
        }
        return (X509TrustManager) trustManagers[0];
    }
}
