package com.l7tech.server.identity.cert;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class TrustedCertCacheJavaTest {

    @Mock
    private TrustedCertManager trustedCertManager;

    private TrustedCert trustedCert;
    private List<TrustedCert> trustedCertList;
    private TrustedCertCache trustedCertCache;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        trustedCert = new TrustedCert();
        trustedCertList = Arrays.asList(trustedCert, new TrustedCert());
        trustedCertCache = new TrustedCertCacheImpl(trustedCertManager);
    }

    /**
     * Expect the TrustedCert returned by findByPrimaryKey() to be immutable.
     */
    @Test
    public void testFindByPrimaryKey_ReturnedTrustedCertIsReadOnly() throws FindException {
        when(trustedCertManager.findByPrimaryKey(any(Goid.class))).thenReturn(trustedCert);

        TrustedCert trustedCert = trustedCertCache.findByPrimaryKey(Goid.DEFAULT_GOID);

        try {
            trustedCert.setCertBase64(null);
            fail("Expected IllegalStateException on mutation attempt.");
        } catch (IllegalStateException e) {
            assertEquals("This instance is read-only.", e.getMessage());
        }
    }

    /**
     * Expect every TrustedCert in the Collection returned by findByName() to be immutable.
     */
    @Test
    public void testFindByName_AllTrustedCertsInReturnedCollectionAreReadOnly() throws FindException {
        when(trustedCertManager.findByName(any(String.class))).thenReturn(trustedCertList);

        validateAllTrustedCertsImmutable(trustedCertCache.findByName(null));
    }

    /**
     * Expect every TrustedCert in the Collection returned by findByTrustFlag() to be immutable.
     */
    @Test
    public void testFindByTrustFlag_AllTrustedCertsInReturnedCollectionAreReadOnly() throws FindException {
        when(trustedCertManager.findByTrustFlag(any(TrustedCert.TrustedFor.class))).thenReturn(trustedCertList);

        validateAllTrustedCertsImmutable(trustedCertCache.findByTrustFlag(null));
    }

    /**
     * Expect every TrustedCert in the Collection returned by findBySubjectDn() to be immutable.
     */
    @Test
    public void testFindBySubjectDn_AllTrustedCertsInReturnedCollectionAreReadOnly() throws FindException {
        when(trustedCertManager.findBySubjectDn(any(String.class))).thenReturn(trustedCertList);

        validateAllTrustedCertsImmutable(trustedCertCache.findBySubjectDn(null));
    }

    private void validateAllTrustedCertsImmutable(Collection<TrustedCert> trustedCerts) {
        assertEquals(2, trustedCerts.size());

        for (TrustedCert immutableCert : trustedCerts) {
            try {
                immutableCert.setCertBase64(null);
                fail("Expected IllegalStateException on mutation attempt.");
            } catch (IllegalStateException e) {
                assertEquals("This instance is read-only.", e.getMessage());
            }
        }
    }
}
