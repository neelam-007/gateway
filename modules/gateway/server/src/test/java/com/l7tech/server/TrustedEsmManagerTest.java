package com.l7tech.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;

public class TrustedEsmManagerTest extends EntityManagerTest {

    private static final String ESM_NAME = "TrustedEsmName";

    private TrustedEsmManager trustedEsmManager;

    @Before
    public void setUp() throws Exception {
        trustedEsmManager = applicationContext.getBean("trustedEsmManager", TrustedEsmManager.class);
        TrustedEsm trustedEsm = new TrustedEsm();
        trustedEsm.setName(ESM_NAME);

        TrustedCertManager trustedCertManager = applicationContext.getBean("trustedCertManager", TrustedCertManager.class);
        TrustedCert trustedCert = new TrustedCert();
        trustedCert.setName("test");
        trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.SPECIFIED);
        trustedCert.setCertificate(TestDocuments.getWssInteropAliceCert());
        trustedCert.setGoid(trustedCertManager.save(trustedCert));

        trustedEsm.setTrustedCert(trustedCert);

        trustedEsm.setGoid(trustedEsmManager.save(trustedEsm));
        session.flush();
    }

    @Test
    public void testFindEsmById() throws Exception {
        assertNotNull(trustedEsmManager.findEsmById(ESM_NAME));
    }
}
