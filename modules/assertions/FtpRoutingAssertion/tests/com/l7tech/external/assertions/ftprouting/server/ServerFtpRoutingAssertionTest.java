package com.l7tech.external.assertions.ftprouting.server;

import javax.net.ssl.X509TrustManager;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import com.l7tech.server.security.keystore.SsgKeyStoreManager;

/**
 * Test for FTP routing assertion
 *
 * @author Steve Jones
 */
public class ServerFtpRoutingAssertionTest  extends TestCase {

    public ServerFtpRoutingAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(ServerFtpRoutingAssertionTest.class);
        return suite;
    }

    /**
     * Test method invoked from admin layer via reflection is still available. 
     */
    public void testReflectedMethod() throws Exception {
        Class serverFtpRoutingAssertionClass = ServerFtpRoutingAssertion.class;
        //
        // NOTE: If this needs changing then you also need to update the 
        //       "UneasyRooster/src/com/l7tech/server/transport/ftp/FtpAdminImpl.java"
        //       class
        //
        serverFtpRoutingAssertionClass.getMethod("testConnection",
                                                 Boolean.TYPE,              // isFtps
                                                 Boolean.TYPE,              // isExplicit
                                                 Boolean.TYPE,              // isVerifyServerCert
                                                 String.class,              // hostName
                                                 Integer.TYPE,              // port
                                                 String.class,              // userName
                                                 String.class,              // password
                                                 Boolean.TYPE,              // useClientCert
                                                 Long.TYPE,                 // clientCertKeystoreId
                                                 String.class,              // clientCertKeyAlias
                                                 String.class,              // directory
                                                 Integer.TYPE,              // timeout
                                                 X509TrustManager.class,    // x509TrustManager
                                                 SsgKeyStoreManager.class); // ssgKeyStoreManager        
    }
    
}
