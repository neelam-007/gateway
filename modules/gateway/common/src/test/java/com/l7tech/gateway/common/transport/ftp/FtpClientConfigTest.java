package com.l7tech.gateway.common.transport.ftp;

import org.junit.Test;
import org.junit.Assert;

import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;

import com.l7tech.util.HexUtils;

/**
 * Tests for FTP configuration
 */
public class FtpClientConfigTest {

    private static final String B64_SERIALIZED_V1_CONFIG =
            "rO0ABXNyADtjb20ubDd0ZWNoLmdhdGV3YXkuY29tbW9uLnRyYW5zcG9ydC5mdHAuRnRwQ2xpZW50\n" +
            "Q29uZmlnSW1wbHnjjn2MX5WlAgAOSgAUY2xpZW50Q2VydEtleXN0b3JlSWRaABJpc1ZlcmlmeVNl\n" +
            "cnZlckNlcnRJAARwb3J0SQAHdGltZW91dFoADXVzZUNsaWVudENlcnRMABJjbGllbnRDZXJ0S2V5\n" +
            "QWxpYXN0ABJMamF2YS9sYW5nL1N0cmluZztMABFjcmVkZW50aWFsc1NvdXJjZXQAPkxjb20vbDd0\n" +
            "ZWNoL2dhdGV3YXkvY29tbW9uL3RyYW5zcG9ydC9mdHAvRnRwQ3JlZGVudGlhbHNTb3VyY2U7TAAJ\n" +
            "ZGlyZWN0b3J5cQB+AAFMAA5maWxlTmFtZVNvdXJjZXQAO0xjb20vbDd0ZWNoL2dhdGV3YXkvY29t\n" +
            "bW9uL3RyYW5zcG9ydC9mdHAvRnRwRmlsZU5hbWVTb3VyY2U7TAAEaG9zdHEAfgABTAAEcGFzc3EA\n" +
            "fgABTAAHcGF0dGVybnEAfgABTAAIc2VjdXJpdHl0ADVMY29tL2w3dGVjaC9nYXRld2F5L2NvbW1v\n" +
            "bi90cmFuc3BvcnQvZnRwL0Z0cFNlY3VyaXR5O0wABHVzZXJxAH4AAXhw//////////8AAAAAFQAA\n" +
            "JxAAcHNyADxjb20ubDd0ZWNoLmdhdGV3YXkuY29tbW9uLnRyYW5zcG9ydC5mdHAuRnRwQ3JlZGVu\n" +
            "dGlhbHNTb3VyY2V/hFllr5p4LQIAAkwACl9wcmludE5hbWVxAH4AAUwACF93c3BOYW1lcQB+AAF4\n" +
            "cHQACXNwZWNpZmllZHEAfgAIdAAFL3Rlc3RwdAAPZmlzaC5sN3RlY2guY29tdAAIcGFzc3dvcmRw\n" +
            "c3IAM2NvbS5sN3RlY2guZ2F0ZXdheS5jb21tb24udHJhbnNwb3J0LmZ0cC5GdHBTZWN1cml0ebEo\n" +
            "XJNDGxUcAgACTAAKX3ByaW50TmFtZXEAfgABTAAIX3dzcE5hbWVxAH4AAXhwdAANdW5zZWN1cmVk\n" +
            "IEZUUHQAA2Z0cHQAA2Z0cA==";

    private static final String B64_SERIALIZED_V2_CONFIG = 
            "rO0ABXNyADtjb20ubDd0ZWNoLmdhdGV3YXkuY29tbW9uLnRyYW5zcG9ydC5mdHAuRnRwQ2xpZW50\n"+
            "Q29uZmlnSW1wbHnjjn2MX5WlAgAPSgAUY2xpZW50Q2VydEtleXN0b3JlSWRaAAdlbmFibGVkWgAS\n"+
            "aXNWZXJpZnlTZXJ2ZXJDZXJ0SQAEcG9ydEkAB3RpbWVvdXRaAA11c2VDbGllbnRDZXJ0TAASY2xp\n"+
            "ZW50Q2VydEtleUFsaWFzdAASTGphdmEvbGFuZy9TdHJpbmc7TAARY3JlZGVudGlhbHNTb3VyY2V0\n"+
            "AD5MY29tL2w3dGVjaC9nYXRld2F5L2NvbW1vbi90cmFuc3BvcnQvZnRwL0Z0cENyZWRlbnRpYWxz\n"+
            "U291cmNlO0wACWRpcmVjdG9yeXEAfgABTAAOZmlsZU5hbWVTb3VyY2V0ADtMY29tL2w3dGVjaC9n\n"+
            "YXRld2F5L2NvbW1vbi90cmFuc3BvcnQvZnRwL0Z0cEZpbGVOYW1lU291cmNlO0wABGhvc3RxAH4A\n"+
            "AUwABHBhc3NxAH4AAUwAB3BhdHRlcm5xAH4AAUwACHNlY3VyaXR5dAA1TGNvbS9sN3RlY2gvZ2F0\n"+
            "ZXdheS9jb21tb24vdHJhbnNwb3J0L2Z0cC9GdHBTZWN1cml0eTtMAAR1c2VycQB+AAF4cP//////\n"+
            "////AAAAAAAVAAAnEABwc3IAPGNvbS5sN3RlY2guZ2F0ZXdheS5jb21tb24udHJhbnNwb3J0LmZ0\n"+
            "cC5GdHBDcmVkZW50aWFsc1NvdXJjZX+EWWWvmngtAgACTAAKX3ByaW50TmFtZXEAfgABTAAIX3dz\n"+
            "cE5hbWVxAH4AAXhwdAAJc3BlY2lmaWVkcQB+AAh0AAUvdGVzdHB0AA9maXNoLmw3dGVjaC5jb210\n"+
            "AAhwYXNzd29yZHBzcgAzY29tLmw3dGVjaC5nYXRld2F5LmNvbW1vbi50cmFuc3BvcnQuZnRwLkZ0\n"+
            "cFNlY3VyaXR5sShck0MbFRwCAAJMAApfcHJpbnROYW1lcQB+AAFMAAhfd3NwTmFtZXEAfgABeHB0\n"+
            "AA11bnNlY3VyZWQgRlRQdAADZnRwdAADZnRw";

    /**
     * Test read of "V1" serialized format.
     */
    @Test
    public void testReadV1Configuration() throws Exception {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(HexUtils.decodeBase64(B64_SERIALIZED_V1_CONFIG)));
        FtpClientConfig ftpConfig = (FtpClientConfig) in.readObject();

        Assert.assertEquals( "Config dir", "/test", ftpConfig.getDirectory() );
        Assert.assertEquals( "Config host", "fish.l7tech.com", ftpConfig.getHost() );
        Assert.assertEquals( "Config pass", "password", ftpConfig.getPass() );
        Assert.assertEquals( "Config port", 21, ftpConfig.getPort() );
        Assert.assertEquals( "Config sec", FtpSecurity.FTP_UNSECURED, ftpConfig.getSecurity() );
        Assert.assertEquals( "Config timeout", 10000, ftpConfig.getTimeout() );
        Assert.assertEquals( "Config user", "ftp", ftpConfig.getUser() );

        Assert.assertTrue( "Should be enabled by default.", ftpConfig.isEnabled() ); // enabled was not part of serialized v1 data
    }

    /**
     * Test read of "V2" serialized format (added enabled flag) 
     */
    @Test
    public void testReadV2Configuration() throws Exception {
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(HexUtils.decodeBase64(B64_SERIALIZED_V2_CONFIG)));
        FtpClientConfig ftpConfig = (FtpClientConfig) in.readObject();

        Assert.assertEquals( "Config dir", "/test", ftpConfig.getDirectory() );
        Assert.assertEquals( "Config host", "fish.l7tech.com", ftpConfig.getHost() );
        Assert.assertEquals( "Config pass", "password", ftpConfig.getPass() );
        Assert.assertEquals( "Config port", 21, ftpConfig.getPort() );
        Assert.assertEquals( "Config sec", FtpSecurity.FTP_UNSECURED, ftpConfig.getSecurity() );
        Assert.assertEquals( "Config timeout", 10000, ftpConfig.getTimeout() );
        Assert.assertEquals( "Config user", "ftp", ftpConfig.getUser() );
        Assert.assertEquals( "Config enabled.", false, ftpConfig.isEnabled() );
    }
}
