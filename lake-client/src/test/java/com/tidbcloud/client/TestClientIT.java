/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tidbcloud.client;

import okhttp3.OkHttpClient;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.tidbcloud.client.ClientSettings.*;

@Test(timeOut = 120000)
public class TestClientIT {
    static String host = System.getenv("LAKE_TEST_HOST") != null ? System.getenv("LAKE_TEST_HOST").trim() : "127.0.0.1";
    static String port = System.getenv("LAKE_TEST_CONN_PORT") != null ? System.getenv("LAKE_TEST_CONN_PORT").trim() : "8000";
    static String username = System.getenv("LAKE_TEST_USER") != null ? System.getenv("LAKE_TEST_USER").trim() : "databend";
    static String password = System.getenv("LAKE_TEST_PASSWORD") != null ? System.getenv("LAKE_TEST_PASSWORD").trim() : "databend";
    static String warehouse = System.getenv("LAKE_TEST_WAREHOUSE") != null ? System.getenv("LAKE_TEST_WAREHOUSE").trim() : null;
    static boolean ssl = System.getenv("LAKE_TEST_SSL") != null ? Boolean.parseBoolean(System.getenv("LAKE_TEST_SSL").trim()) : false;
    private static final String LAKE_HOST = (ssl ? "https" : "http") + "://" + username + ":" + password + "@" + host + ":" + port;

    private static Map<String, String> defaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        if (warehouse != null) {
            headers.put(LakeWarehouseHeader, warehouse);
        }
        return headers;
    }
    private static final String DATABASE = "default";

    @Test(groups = {"it"})
    public void testBasicQueryPagination() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(OkHttpUtils.basicAuthInterceptor(username, password)).build();

        ClientSettings settings = new ClientSettings(LAKE_HOST, LakeSession.createDefault(), DEFAULT_QUERY_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT, PaginationOptions.defaultPaginationOptions(), defaultHeaders(), null, DEFAULT_RETRY_ATTEMPTS);
        AtomicReference<String> lastNodeID = new AtomicReference<>();
        LakeClient cli = new LakeClientV1(client, "select 1", settings, null, lastNodeID);
        Assert.assertEquals(cli.getQuery(), "select 1");
        Assert.assertEquals(cli.getSession().getDatabase(), DATABASE);
        Assert.assertNotNull(cli.getResults());
        Assert.assertEquals(cli.getResults().getSchema().size(), 1);
        for (List<Object> data : cli.getResults().getData()) {
            Assert.assertEquals(data.size(), 1);
            Assert.assertEquals((Short) data.get(0), Short.valueOf("1"));
        }
        cli.close();
    }

    @Test(groups = {"it"})
    public void testConnectionRefused() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(OkHttpUtils.basicAuthInterceptor(username, password)).build();
        ClientSettings settings = new ClientSettings("http://localhost:13191");

        AtomicReference<String> lastNodeID = new AtomicReference<>();

        try {
            LakeClient cli = new LakeClientV1(client, "select 1", settings, null, lastNodeID);
            cli.getResults(); // This should trigger the connection attempt
            Assert.fail("Expected exception was not thrown");
        } catch (Exception e) {
            Assert.assertTrue(
                    e.getCause().getCause() instanceof ConnectException, "Exception should be ConnectionException or contain ConnectionException as cause");

        }
    }

    @Test(groups = {"it"})
    public void testBasicQueryIDHeader() {
        OkHttpClient client = new OkHttpClient.Builder().addInterceptor(OkHttpUtils.basicAuthInterceptor(username, password)).build();
        String expectedUUID = UUID.randomUUID().toString().replace("-","");
        AtomicReference<String> lastNodeID = new AtomicReference<>();

        Map<String, String> additionalHeaders = new HashMap<>(defaultHeaders());
        additionalHeaders.put(X_Lake_Query_ID, expectedUUID);
        ClientSettings settings = new ClientSettings(LAKE_HOST, LakeSession.createDefault(), DEFAULT_QUERY_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT, PaginationOptions.defaultPaginationOptions(), additionalHeaders, null, DEFAULT_RETRY_ATTEMPTS);
        LakeClient cli = new LakeClientV1(client, "select 1", settings, null, lastNodeID);
        Assert.assertEquals(cli.getAdditionalHeaders().get(X_Lake_Query_ID), expectedUUID);

        String expectedUUID1 = UUID.randomUUID().toString().replace("-", "");
        Map<String, String> additionalHeaders1 = new HashMap<>(defaultHeaders());
        additionalHeaders1.put(X_Lake_Query_ID, expectedUUID1);
        ClientSettings settings1 = new ClientSettings(LAKE_HOST, LakeSession.createDefault(), DEFAULT_QUERY_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT, DEFAULT_SOCKET_TIMEOUT, PaginationOptions.defaultPaginationOptions(), additionalHeaders1, null, DEFAULT_RETRY_ATTEMPTS);
        Assert.assertEquals(cli.getAdditionalHeaders().get(X_Lake_Query_ID), expectedUUID);
        // check X_Lake_Query_ID won't change after calling next()
        LakeClient cli1 = new LakeClientV1(client, "SELECT number from numbers(200000) order by number", settings1, null, lastNodeID);
        for (int i = 1; i < 1000; i++) {
            cli.advance();
            Assert.assertEquals(cli1.getAdditionalHeaders().get(X_Lake_Query_ID), expectedUUID1);
        }
        Assert.assertEquals(cli1.getAdditionalHeaders().get(X_Lake_Query_ID), expectedUUID1);
    }
}
