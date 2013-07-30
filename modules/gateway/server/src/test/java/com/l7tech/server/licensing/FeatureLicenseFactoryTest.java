package com.l7tech.server.licensing;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.licensing.FeatureLicense;
import com.l7tech.gateway.common.licensing.LicenseDocument;
import com.l7tech.util.IOUtils;
import org.junit.Test;

import java.security.cert.X509Certificate;

import static org.junit.Assert.*;

/**
 * Tests for {@link FeatureLicenseFactory}.
 *
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public class FeatureLicenseFactoryTest {

    /* ---- Resources ---- */
    private static final String VALID_LICENSE_DOCUMENT_WITHOUT_SIGNATURE_XML =
            getResourceContents("ValidLicenseDocumentWithoutSignature.xml");

    private static final String VALID_LICENSE_DOCUMENT_WITH_VALID_SIGNATURE_XML =
            getResourceContents("ValidLicenseDocumentWithValidSignature.xml");

    /* ---- Issuer Certificate(s) ---- */
    public static final X509Certificate[] TRUSTED_ISSUERS_X509_CERTIFICATES = getTrustedIssuers();

    protected final FeatureLicenseFactory factory = createFactory();

    @Test
    public void newInstance_GivenValidSignedLicenseDocument_ReturnsValidFeatureLicenseWithoutIssuer() throws Exception {
        FeatureLicense featureLicense =
                factory.newInstance(new LicenseDocument(VALID_LICENSE_DOCUMENT_WITH_VALID_SIGNATURE_XML));

        assertEquals(VALID_LICENSE_DOCUMENT_WITH_VALID_SIGNATURE_XML, featureLicense.getLicenseDocument().getContents());
        assertNull(featureLicense.getTrustedIssuer());
    }

    @Test
    public void newInstance_GivenValidSignedLicenseDocumentAndValidCertificates_ReturnsValidFeatureLicenseIncludingIssuer() throws Exception {
        FeatureLicense featureLicense =
                factory.newInstance(new LicenseDocument(VALID_LICENSE_DOCUMENT_WITH_VALID_SIGNATURE_XML), TRUSTED_ISSUERS_X509_CERTIFICATES);

        assertEquals(VALID_LICENSE_DOCUMENT_WITH_VALID_SIGNATURE_XML, featureLicense.getLicenseDocument().getContents());
        assertNotNull(featureLicense.getTrustedIssuer());
    }

    @Test
    public void newInstance_GivenValidUnsignedLicenseDocumentAndValidCertificates_ReturnsValidFeatureLicenseWithoutIssuer() throws Exception {
        FeatureLicense featureLicense =
                factory.newInstance(new LicenseDocument(VALID_LICENSE_DOCUMENT_WITHOUT_SIGNATURE_XML), TRUSTED_ISSUERS_X509_CERTIFICATES);

        assertEquals(VALID_LICENSE_DOCUMENT_WITHOUT_SIGNATURE_XML, featureLicense.getLicenseDocument().getContents());
        assertNull(featureLicense.getTrustedIssuer());
    }

    public FeatureLicenseFactory createFactory() {
        return new FeatureLicenseFactory();
    }

    private static String getResourceContents(String resource) {
        try {
            return new String(IOUtils.slurpStream(FeatureLicenseFactoryTest.class.getResourceAsStream(resource)));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not load resource '" + resource + "'. " + e.getMessage(), e);
        }
    }

    private static X509Certificate[] getTrustedIssuers() {
        try {
            return new X509Certificate[] {TestDocuments.getFrancoCertificate()};
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not get X.509 certificate - this should never happen!", e);
        }
    }
}