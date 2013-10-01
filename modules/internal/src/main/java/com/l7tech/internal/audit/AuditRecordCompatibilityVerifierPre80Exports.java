package com.l7tech.internal.audit;

import com.l7tech.gateway.common.audit.AuditRecordVerifier;
import com.l7tech.security.cert.KeyUsageException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: wlui
 * Date: 30/09/13
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class AuditRecordCompatibilityVerifierPre80Exports {

    private static Logger logger = Logger.getLogger(AuditRecordCompatibilityVerifierUpgradedPre80.class.getName());

    private final X509Certificate signerCert;

    private static final String SEPARATOR_PATTERN = ":";


    public AuditRecordCompatibilityVerifierPre80Exports(X509Certificate signerCert) {
        this.signerCert = signerCert;
    }

    /**
     * @param input     the parsed record in signable format to use for verification.
     * @return true if the signature verifies with the specified certificate.  False if the signature is invalid.
     * @throws java.security.NoSuchAlgorithmException
     *          if a signature algorithm is unavailable
     *          (perhaps because an EC cert is specified but there is no implementation of Signature.SHA512withECDSA in the current JVM environment)
     * @throws java.security.InvalidKeyException
     *          if the signing cert key cannot be used to verify this signature
     * @throws java.security.SignatureException
     *          if some other error occurs during signature verification
     */
    public boolean verifyAuditRecordSignature(String input) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, KeyUsageException, CertificateParsingException, IOException {
        if (input == null) return false;
        input = input.trim();
        // removed escaped \n characters (replace '\\\n' with '\n')
        input = input.replace("\\\n", "\n");

        DownloadedAuditRecordSignatureVerificator out = new DownloadedAuditRecordSignatureVerificator();
        ArrayList<Integer> separatorPositions = new ArrayList<Integer>();
        int pos = 0;
        int tmp = nextUnescapedSeparator(input, pos);
        while (tmp >= 0) {
            separatorPositions.add(tmp);
            pos = tmp+1;
            tmp = nextUnescapedSeparator(input, pos);
        }

        if (separatorPositions.size() < 30) {
            throw new IllegalArgumentException("This does not appear to be a valid audit record (" +
                    separatorPositions.size() + ")");
        }

        StringBuffer parsedTmp = new StringBuffer();

        // extract signature, remove initial ID and signature the signature starts after the 10th ':' and has a length of 173
        String signature = input.substring(separatorPositions.get(9) + 1, separatorPositions.get(10));
        if (signature == null || signature.length() < 1) {
            // we're dealing with a record which does not contain a signature
            signature = null;
        } else if (signature.length() < 64) {
            throw new SignatureException("Unexpected signature length " + signature.length() +
                    ". " + signature);
        }
        parsedTmp.append(input.substring(separatorPositions.get(0) +1, separatorPositions.get(9)));
        // append either the AdminAuditRecord, MessageSummaryAuditRecord or the SystemAuditRecord
        int tmpstart = separatorPositions.get(10);
        int tmpend = separatorPositions.get(11);
        if ((tmpend - tmpstart) > 1) {
            parsedTmp.append(input.substring(separatorPositions.get(11), separatorPositions.get(14)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(14);
        tmpend = separatorPositions.get(15);
        if ((tmpend - tmpstart) > 1) {
            parsedTmp.append(input.substring(separatorPositions.get(15), separatorPositions.get(27)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(27);
        tmpend = separatorPositions.get(28);
        if ((tmpend - tmpstart) > 1) {
            parsedTmp.append(input.substring(separatorPositions.get(28), separatorPositions.get(30)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }

        String parsingResult = parsedTmp.toString();

        // Append the audit details if any
        tmpstart = input.indexOf("[", separatorPositions.get(30));
        if (tmpstart > 0) {
            parsingResult = parsingResult + input.substring(tmpstart);
        }

        parsingResult = Pattern.compile("\\\\([^\\040-\\0176]|\\\\|\\:)").matcher(parsingResult).replaceAll("$1");

        return verifySignature(parsingResult, signature);
    }

    private static int nextUnescapedSeparator(String input, int startPos) {
        int res = input.indexOf(SEPARATOR_PATTERN, startPos);
        if (res < 1) return res;
        if (input.charAt(res-1) == '\\') {
            return nextUnescapedSeparator(input, res+1);
        } else return res;
    }

    public boolean verifySignature(String recordInExportedFormat, String signature) throws IOException {
        if (signature==null) {
            logger.info("Verify signature fails because the record is not signed.");
            return false;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("should not happen", e);
        }
        byte[] digestvalue = digest.digest(recordInExportedFormat.getBytes());

        try {
            boolean result = new AuditRecordVerifier(signerCert).verifySignatureOfDigest(signature, digestvalue);
            return result;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
