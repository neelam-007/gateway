package com.l7tech.server;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class TrustedEsmUserManagerTest extends EntityManagerTest {

    private static final String ESM_NAME = "TrustedEsmName";
    private static final String ESM_USER_ID = "TrustedEsmName";
    private TrustedEsmManager trustedEsmManager;
    private TrustedEsmUserManager trustedEsmUserManager;
    private IdentityProviderConfigManager identityProviderConfigManager;
    private static final String SSG_USER_ID = "SSG_USER_ID";
    private static final String IP_NAME = "IP_NAME";

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

        trustedEsmUserManager = applicationContext.getBean("trustedEsmUserManager", TrustedEsmUserManager.class);
        TrustedEsmUser trustedEsmUser = new TrustedEsmUser();
        trustedEsmUser.setEsmUserId(ESM_USER_ID);
        trustedEsmUser.setTrustedEsm(trustedEsm);
        trustedEsmUser.setSsgUserId(SSG_USER_ID);

        identityProviderConfigManager = applicationContext.getBean("identityProviderConfigManager", IdentityProviderConfigManager.class);
        IdentityProviderConfig identityProvider = new IdentityProviderConfig();
        identityProvider.setName(IP_NAME);
        identityProvider.setGoid(identityProviderConfigManager.save(identityProvider));

        trustedEsmUser.setProviderGoid(identityProvider.getGoid());

        trustedEsmUserManager.save(trustedEsmUser);

        session.flush();
    }

    @Test
    public void testDeleteMappingsForUser() throws Exception {
        IdentityProviderConfig identityProviderConfig = identityProviderConfigManager.findByUniqueName(IP_NAME);
        User user = new UserBean(identityProviderConfig.getGoid(),SSG_USER_ID);
        trustedEsmUserManager.deleteMappingsForUser(user);
        session.flush();
        TrustedEsm trustedEsm = trustedEsmManager.findEsmById(ESM_NAME);
        assertTrue(trustedEsmUserManager.findByEsmId(trustedEsm.getGoid()).isEmpty());

    }

    @Test
    public void testDeleteMappingsForIdentityProvider() throws Exception {
        IdentityProviderConfig identityProviderConfig = identityProviderConfigManager.findByUniqueName(IP_NAME);
        trustedEsmUserManager.deleteMappingsForIdentityProvider(identityProviderConfig.getGoid());
        session.flush();
        TrustedEsm trustedEsm = trustedEsmManager.findEsmById(ESM_NAME);
        assertTrue(trustedEsmUserManager.findByEsmId(trustedEsm.getGoid()).isEmpty());
    }

    @Test
    public void testFindByEsmId() throws Exception {
        TrustedEsm trustedEsm = trustedEsmManager.findEsmById(ESM_NAME);
        assertFalse(trustedEsmUserManager.findByEsmId(trustedEsm.getGoid()).isEmpty());
    }

    @Test
    public void testFindByEsmIdAndUserUUID() throws Exception {
        TrustedEsm trustedEsm = trustedEsmManager.findEsmById(ESM_NAME);
        assertNotNull(trustedEsmUserManager.findByEsmIdAndUserUUID(trustedEsm.getGoid(), ESM_USER_ID));

    }
}
