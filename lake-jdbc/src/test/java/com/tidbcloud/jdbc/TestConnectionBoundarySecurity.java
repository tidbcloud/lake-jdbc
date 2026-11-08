package com.tidbcloud.jdbc;

import com.sun.net.httpserver.HttpServer;
import com.tidbcloud.jdbc.internal.session.LakeSessionCookieJar;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.MediaType;
import okhttp3.Response;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Test(groups = {"UNIT"}, timeOut = 10000)
public class TestConnectionBoundarySecurity {
    @Test
    public void encodedValuesCannotIntroduceConnectionProperties() throws Exception {
        for (String prefix : Arrays.asList("jdbc:lake://", "jdbc:lake://https://user:password@")) {
            String value = "x&ssl=false&sslmode=disable&presign_allow_private=true";
            LakeDriverUri uri = LakeDriverUri.create(prefix + "example.com/default?warehouse="
                    + "x%26ssl%3Dfalse%26sslmode%3Ddisable%26presign_allow_private%3Dtrue", null);
            Assert.assertEquals(uri.getUri().getScheme(), "https");
            Assert.assertEquals(uri.getProperties().getProperty("warehouse"), value);
            Assert.assertFalse(uri.toSessionHandleConfig().isPresignAllowPrivate());
            Assert.assertTrue(uri.getUri().getRawQuery().contains("%26ssl%3Dfalse"));
        }
        LakeDriverUri once = LakeDriverUri.create("jdbc:lake://host?warehouse=a%2526b%2Bc", null);
        Assert.assertEquals(once.getProperties().getProperty("warehouse"), "a%26b+c");
        LakeDriverUri credentials = LakeDriverUri.create("jdbc:lake://host?pass%77ord=p%26ssl%3Dfalse", null);
        Assert.assertEquals(credentials.getProperties().getProperty("password"), "p&ssl=false");
        Assert.assertNull(credentials.getUri().getRawQuery());
        Assert.expectThrows(SQLException.class, () -> LakeDriverUri.create("jdbc:lake://host?ssl=true&%73sl=false", null));
        Assert.expectThrows(SQLException.class, () -> LakeDriverUri.create("jdbc:lake://host?ssl=true&SSL=false", null));
    }

    @Test
    public void propertyInfoOmitsSecretsButKeepsOtherValues() throws Exception {
        try (NonRegisteringLakeDriver driver = new NonRegisteringLakeDriver()) {
            Properties supplied = new Properties();
            supplied.setProperty("PASSWORD", "property-password");
            supplied.setProperty("ACCESSTOKEN", "property-token");

            Map<String, DriverPropertyInfo> fromProperties = propertyInfo(
                    driver, "jdbc:lake://localhost/default?warehouse=example", supplied);
            Assert.assertNull(fromProperties.get("password").value);
            Assert.assertNull(fromProperties.get("accesstoken").value);
            Assert.assertEquals(fromProperties.get("warehouse").value, "example");
            Assert.assertEquals(supplied.getProperty("PASSWORD"), "property-password");

            Map<String, DriverPropertyInfo> fromUrl = propertyInfo(
                    driver, "jdbc:lake://test-user:uri-password@localhost/default?accesstoken=uri-token", null);
            Assert.assertNull(fromUrl.get("password").value);
            Assert.assertNull(fromUrl.get("accesstoken").value);
            Assert.assertEquals(fromUrl.get("user").value, "test-user");
        }
    }

    @Test
    public void invalidPropertyInfoUrlFailsWithoutEchoingCredentials() throws Exception {
        try (NonRegisteringLakeDriver driver = new NonRegisteringLakeDriver()) {
            SQLException failure = Assert.expectThrows(SQLException.class, () -> driver.getPropertyInfo(
                    "jdbc:lake://localhost/default?accesstoken=dummy-token&malformed", null));
            Assert.assertEquals(failure.getMessage(), "Invalid Lake JDBC URL or connection properties");
            Assert.assertNull(failure.getCause());
        }
    }

    private static Map<String, DriverPropertyInfo> propertyInfo(
            NonRegisteringLakeDriver driver, String url, Properties supplied) throws SQLException {
        return Arrays.stream(driver.getPropertyInfo(url, supplied))
                .collect(Collectors.toMap(property -> property.name, Function.identity()));
    }

    @Test
    public void cookiesRespectDomainPathSecureExpiryAndDistinctPaths() {
        LakeSessionCookieJar jar = new LakeSessionCookieJar();
        jar.add(new Cookie.Builder().name("sid").value("root").hostOnlyDomain("lake.example").path("/").build());
        jar.add(new Cookie.Builder().name("sid").value("query").hostOnlyDomain("lake.example").path("/v1/query").build());
        jar.add(new Cookie.Builder().name("secure").value("value").hostOnlyDomain("lake.example").path("/").secure().build());
        jar.add(new Cookie.Builder().name("expired").value("value").hostOnlyDomain("lake.example").path("/").expiresAt(1).build());
        Assert.assertEquals(jar.loadForRequest(HttpUrl.get("https://lake.example/v1/query")).size(), 3);
        Assert.assertEquals(jar.loadForRequest(HttpUrl.get("http://lake.example/elsewhere")).size(), 1);
        Assert.assertTrue(jar.loadForRequest(HttpUrl.get("https://other.example/v1/query")).isEmpty());
        Assert.assertTrue(jar.loadForRequest(HttpUrl.get("https://sub.lake.example/v1/query")).isEmpty());
    }

    @Test
    public void redirectsKeepSameOriginAndDoNotDeliverToOtherOrigins() throws Exception {
        HttpServer origin = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        HttpServer other = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger deliveries = new AtomicInteger();
        AtomicReference<String> cookie = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        other.createContext("/capture", exchange -> { deliveries.incrementAndGet(); exchange.sendResponseHeaders(200, -1); exchange.close(); });
        origin.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Set-Cookie", "sid=local-session; Path=/");
            exchange.getResponseHeaders().add("Location", "/same");
            exchange.sendResponseHeaders(307, -1); exchange.close();
        });
        origin.createContext("/same", exchange -> {
            cookie.set(exchange.getRequestHeaders().getFirst("Cookie"));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(200, -1); exchange.close();
        });
        origin.createContext("/foreign", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://127.0.0.1:" + other.getAddress().getPort() + "/capture");
            exchange.sendResponseHeaders(307, -1); exchange.close();
        });
        origin.start(); other.start();
        OkHttpClient client = null;
        try {
            String base = "http://127.0.0.1:" + origin.getAddress().getPort();
            LakeDriverUri uri = LakeDriverUri.create("jdbc:lake://" + base + "/default?user=test&password=test&ssl=false", null);
            OkHttpClient.Builder builder = new OkHttpClient.Builder(); uri.setupClient(builder); client = builder.build();
            try (Response response = client.newCall(new Request.Builder().url(base + "/start").build()).execute()) {
                Assert.assertEquals(response.code(), 200);
            }
            Assert.assertTrue(cookie.get().contains("sid=local-session"));
            Assert.assertTrue(cookie.get().contains("cookie_enabled=true"));
            Assert.assertTrue(authorization.get().startsWith("Basic "));
            OkHttpClient configured = client;
            Assert.expectThrows(IOException.class, () -> {
                try (Response ignored = configured.newCall(new Request.Builder().url(base + "/foreign")
                        .post(RequestBody.create("select 1", MediaType.get("text/plain"))).build()).execute()) { }
            });
            Assert.assertEquals(deliveries.get(), 0);
        } finally {
            origin.stop(0); other.stop(0);
            if (client != null) { client.connectionPool().evictAll(); client.dispatcher().executorService().shutdownNow(); }
        }
    }
}
