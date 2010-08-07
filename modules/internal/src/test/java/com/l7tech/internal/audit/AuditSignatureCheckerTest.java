package com.l7tech.internal.audit;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Try to verify some exported audit signature files to make sure the verifier works.
 */
public class AuditSignatureCheckerTest {
    private static Logger logger = Logger.getLogger(AuditSignatureCheckerTest.class.getName());

    static final ClassLoader classLoader = AuditSignatureCheckerTest.class.getClassLoader();
    static final String DIR = "com/l7tech/internal/audit";
    static final String DIR_53_RSA = DIR + "/signed_53_rsa";
    static final String DIR_53_EC = DIR + "/signed_53_ec";
    static final String DIR_531_RSA = DIR + "/signed_531_rsa";
    static final String DIR_531_EC = DIR + "/signed_531_ec";

    @BeforeClass
    public static void initJceProvider() {
        // Init jceprovider to ensure a Singature.SHA512withECDSA implementation is available
        JceProvider.init();
    }

    @Test
    public void testVerifySignedRsa() throws Exception {
        verifyExportFile(DIR_531_RSA);
    }

    @Test
    public void testVerifySignedEc() throws Exception {
        verifyExportFile(DIR_531_EC);
    }

    @Test
    public void testVerify53FormatRsa() throws Exception {
        verifyExportFile(DIR_53_RSA);
    }

    @Test
    public void testVerify53FormatEc() throws Exception {
        verifyExportFile(DIR_53_EC);
    }

    static void verifyExportFile(String dir) throws IOException, CertificateException, DownloadedAuditRecordSignatureVerificator.InvalidAuditRecordException {
        String datFile = dir + "/audit.dat";
        String certFile = dir + "/cert.pem";

        InputStream certStream = null;
        InputStream datStream = null;
        try {
            certStream = classLoader.getResourceAsStream(certFile);
            assertNotNull("Couldn't find resource: " + certFile, certStream);
            X509Certificate signingCert = CertUtils.decodeFromPEM(new String(IOUtils.slurpStream(certStream), Charsets.UTF8), false);
            datStream = classLoader.getResourceAsStream(datFile);
            assertNotNull("Couldn't find resource: " + datFile, datStream);
            BufferedReader in = new BufferedReader(new InputStreamReader(datStream, Charsets.UTF8));

            int numSigned = 0, numValid = 0, numInvalid = 0;
            String record;
            for (int i = 0; (record = AuditSignatureChecker.readRecord(in)) != null; i++) {
                // Skip header
                if (i < 2)
                    continue;
                DownloadedAuditRecordSignatureVerificator verificator = DownloadedAuditRecordSignatureVerificator.parse(record);
                if (verificator.isSigned()) {
                    numSigned++;
                    boolean validity = verificator.verifySignature(signingCert);
                    if (logger.isLoggable(Level.FINEST))
                        logger.finest((validity ? "  ok " : "FAIL ") + verificator.getParsedRecordInSignableFormat());
                    if (validity)
                        numValid++;
                    else
                        numInvalid++;
                }
            }

            assertTrue("must have seen at least one signed record", numSigned > 0);
            assertTrue("all signed records must verify successfully (numValid=" + numValid + ", numInvalid=" + numInvalid + ")", numInvalid == 0);

        } finally {
            ResourceUtils.closeQuietly(datStream);
            ResourceUtils.closeQuietly(certStream);
        }
    }
}
