package com.l7tech.server.identity.cert;

import com.l7tech.server.ApplicationContexts;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

/**
 *
 */
public class TrustedCertServicesTest {

    static ApplicationContext applicationContext;
    TrustedCertServices trustedCertServices;
    TestTrustedCertManager trustedCertManager;

    @BeforeClass
    public void setupClass() throws Exception {
         applicationContext = ApplicationContexts.getTestApplicationContext();
    }

    @Before
    public void setupTest() throws Exception {
        trustedCertServices = (TrustedCertServices) applicationContext.getBean("trustedCertServices");
        trustedCertManager = (TestTrustedCertManager) applicationContext.getBean("trustedCertManager");
    }

    @Test
    public void testFilterByDn() throws Exception {



    }
}
