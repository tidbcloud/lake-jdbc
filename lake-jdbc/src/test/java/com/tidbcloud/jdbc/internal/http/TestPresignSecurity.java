package com.tidbcloud.jdbc.internal.http;

import com.sun.net.httpserver.HttpServer;
import okhttp3.Headers;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

public class TestPresignSecurity {
    @Test(groups={"UNIT"})
    public void unsafeUrlsFailBeforeAnyRequestAndDoNotExposeSecrets() {
        PresignClient client=new PresignClient();
        for (String url:new String[]{"http://example.com/path?token=secret", "https://127.0.0.1/path", "https://169.254.169.254/latest/meta-data", "https://[::ffff:127.0.0.1]/path", "https://user:secret@example.com/path", "https://example.com/path#fragment"}) {
            Exception error=Assert.expectThrows(java.io.IOException.class,
                    ()->client.presignDownloadStream(new Headers.Builder().build(),url));
            Assert.assertFalse(error.getMessage().contains("secret"));
        }
    }
    @Test(groups={"UNIT"})
    public void reservedAddressesAreRejected() throws Exception {
        for(String ip:new String[]{"0.1.2.3","127.0.0.1","10.0.0.1","100.100.100.200","169.254.169.254","192.0.2.1","198.18.0.1","203.0.113.1","240.0.0.1","::1","fd00::1","fe80::1","2001:db8::1","2002:7f00:1::"}) {
            Assert.assertFalse(PresignClient.publicAddress(InetAddress.getByName(ip)),ip);
        }
        Assert.assertTrue(PresignClient.publicAddress(InetAddress.getByName("8.8.8.8")));
        Assert.assertTrue(PresignClient.publicAddress(InetAddress.getByName("2606:4700:4700::1111")));
    }
    @Test(groups={"UNIT"})
    public void redirectsCannotReachSecondEndpoint() throws Exception {
        AtomicInteger targetCalls=new AtomicInteger();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/target",exchange->{targetCalls.incrementAndGet();exchange.sendResponseHeaders(200,-1);exchange.close();});
        server.createContext("/redirect",exchange->{exchange.getResponseHeaders().set("Location","/target");exchange.sendResponseHeaders(302,-1);exchange.close();});
        server.start();
        try {
            PresignClient client=new PresignClient(true);
            Assert.expectThrows(java.io.IOException.class,()->client.presignDownloadStream(new Headers.Builder().build(),"http://127.0.0.1:"+server.getAddress().getPort()+"/redirect"));
            Assert.assertEquals(targetCalls.get(),0);
            Assert.assertFalse(client.clientForTesting().followRedirects());
        } finally {server.stop(0);}
    }
}
