package com.l7tech.server.security.signer;

import com.l7tech.common.io.CertUtils;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * Code Signing Certificates related tests
 */
public class CodeSigningCertTest {

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static final String SSG_KEYSTORE = "etc/ssgKeyStore";
    private static final String SSG_KEYSTORE_TYPE = "JKS";
    private static final char[] SSG_KEYSTORE_PASS = "password".toCharArray();

    private static final String CODE_SIGNER_ALIAS_SYS_PROP = "code.signer.cert.alias";
    private static final String CODE_SIGNER_EXPIRY_DAYS_SYS_PROP = "code.signer.cert.expiry.reminder.days";

    /**
     * Test if current code signing certificate, specified with {@link #CODE_SIGNER_ALIAS_SYS_PROP} system property,
     * is valid at least {@link #CODE_SIGNER_EXPIRY_DAYS_SYS_PROP} days.
     * <p/>
     * Note that this test fails outside of TeamCity as it relies on the above system properties being set.
     */
    @Test
    @ConditionalIgnore(condition = IgnoreOnDaily.class)
    public void testExpiry() throws Exception {
        final File ssgKeyStore = new File(SSG_KEYSTORE);
        Assert.assertTrue("ssgKeyStore '" + ssgKeyStore.getCanonicalPath() + "' doesn't exists", ssgKeyStore.exists());
        Assert.assertTrue("ssgKeyStore '" + ssgKeyStore.getCanonicalPath() + "' is not a file", ssgKeyStore.isFile());

        // get CODE_SIGNER_ALIAS_SYS_PROP
        final String codeSigningCertAlias = System.getProperty(CODE_SIGNER_ALIAS_SYS_PROP);
        Assert.assertThat("System property '" + CODE_SIGNER_ALIAS_SYS_PROP + "' is not set or empty", codeSigningCertAlias, Matchers.not(Matchers.isEmptyOrNullString()));

        // get CODE_SIGNER_EXPIRY_DAYS_SYS_PROP
        final String expiry = System.getProperty(CODE_SIGNER_EXPIRY_DAYS_SYS_PROP);
        Assert.assertThat("System property '" + CODE_SIGNER_EXPIRY_DAYS_SYS_PROP + "' is not set or empty", expiry, Matchers.not(Matchers.isEmptyOrNullString()));
        final int codeSigningCertExpiry;
        try {
            codeSigningCertExpiry = Integer.parseInt(expiry);
        } catch (NumberFormatException e){
            Assert.fail("Not an integer value for property '" + CODE_SIGNER_EXPIRY_DAYS_SYS_PROP + "': " + expiry);
            // (shouldn't be reachable) throwing so that I won't have to initialize codeSigningCertExpiry
            throw new Exception("Not an integer value for property '" + CODE_SIGNER_EXPIRY_DAYS_SYS_PROP + "': " + expiry);
        }

        X509Certificate codeSigningCert = null;
        final KeyStore keyStore = KeyStore.getInstance(SSG_KEYSTORE_TYPE);
        try (final InputStream is = new BufferedInputStream(new FileInputStream(ssgKeyStore))) {
            keyStore.load(is, SSG_KEYSTORE_PASS);
            final Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                final String alias = aliases.nextElement();
                if (codeSigningCertAlias.equals(alias) && keyStore.isKeyEntry(alias)) {
                    final java.security.cert.Certificate cert = keyStore.getCertificate(alias);
                    if (cert instanceof X509Certificate) {
                        codeSigningCert = (X509Certificate) cert;
                        break;
                    }
                }
            }
        }

        Assert.assertNotNull("Failed to find current code signing cert: " + codeSigningCertAlias, codeSigningCert);

        // test for expiry
        final int daysToExpire = CertUtils.getCertificateExpiry(codeSigningCert).getDays();
        Assert.assertThat(
                "Current code signing cert '" + codeSigningCertAlias + "' " + (daysToExpire > 0 ? "will expire in " + daysToExpire + " days." : "expired " + -daysToExpire + " days ago."),
                daysToExpire,
                Matchers.greaterThan(codeSigningCertExpiry)
        );
    }
}
