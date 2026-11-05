package com.tidbcloud.jdbc;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.sql.SQLException;
import java.util.Properties;

public class TestSecurityDefaults {
    @Test(groups={"UNIT"})
    public void defaultConnectionsUseTlsAndFiniteTimeouts() throws SQLException {
        LakeDriverUri uri=LakeDriverUri.create("jdbc:lake://warehouse.example.com/default",null);
        Assert.assertEquals(uri.getUri().getScheme(),"https");
        Assert.assertEquals(uri.getUri().getPort(),443);
        Assert.assertFalse(uri.copyPurge());
        Assert.assertTrue(uri.toSessionHandleConfig().getConnectionTimeoutSecs()>0);
        Assert.assertTrue(uri.toSessionHandleConfig().getQueryTimeoutSecs()>0);
        Assert.assertTrue(uri.toSessionHandleConfig().getSocketTimeoutSecs()>0);
        Assert.assertFalse(uri.toSessionHandleConfig().isPresignAllowPrivate());
    }
    @Test(groups={"UNIT"})
    public void plaintextRequiresAnExplicitSelector() throws SQLException {
        for(String selector:new String[]{"ssl=false","sslmode=disable"}) {
            Assert.assertEquals(LakeDriverUri.create("jdbc:lake://localhost:8000/default?"+selector,null).getUri().getScheme(),"http");
        }
        Properties props=new Properties();props.setProperty("ssl","false");
        Assert.assertEquals(LakeDriverUri.create("jdbc:lake://localhost",props).getUri().getScheme(),"http");
        Assert.expectThrows(SQLException.class,()->LakeDriverUri.create("jdbc:lake://host?sslmode=typo",null));
    }
    @Test(groups={"UNIT"})
    public void privateStorageOptInReachesTheSession() throws SQLException {
        Assert.assertTrue(LakeDriverUri.create("jdbc:lake://host?presign_allow_private=true",null).toSessionHandleConfig().isPresignAllowPrivate());
    }
}
