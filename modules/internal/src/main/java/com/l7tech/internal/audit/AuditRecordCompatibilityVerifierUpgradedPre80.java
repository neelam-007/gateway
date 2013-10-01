package com.l7tech.internal.audit;

import com.l7tech.gateway.common.audit.AuditRecordVerifier;
import com.l7tech.gateway.common.audit.MessagesUtil;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.KeyUsageException;

import java.security.*;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.text.FieldPosition;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utility class for verifying exported audit signatures upgraded from pre 8.0 systems.
 */
public class AuditRecordCompatibilityVerifierUpgradedPre80 {
    private static Logger logger = Logger.getLogger(AuditRecordCompatibilityVerifierUpgradedPre80.class.getName());

    private final X509Certificate signerCert;

    private static final String SEPARATOR_PATTERN = ":";


    public AuditRecordCompatibilityVerifierUpgradedPre80(X509Certificate signerCert) {
        this.signerCert = signerCert;
    }

    static String getOid(String goidStr) {
        try {
            Goid goid = Goid.parseGoid(goidStr);
            return Long.toString(goid.getLow());
        } catch (IllegalArgumentException e) {
            return goidStr;
        }
    }

    /**
     * @param signature signature to use for verification.  If null, this method will extract its own copy of it from the raw exported record.
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
    public boolean verifyAuditRecordSignature(String signature, String input, String type) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, KeyUsageException, CertificateParsingException {
        if (input == null) return false;
        input = input.trim();
        // removed escaped \n characters (replace '\\\n' with '\n')
        input = input.replace("\\\n", "\n");

        ArrayList<Integer> separatorPositions = new ArrayList<Integer>();
        int pos = 0;
        int tmp = nextUnescapedSeparator(input, pos);
        while (tmp >= 0) {
            separatorPositions.add(tmp);
            pos = tmp + 1;
            tmp = nextUnescapedSeparator(input, pos);
        }

        if (separatorPositions.size() < 30) {
            logger.info("Not enough fields in exported audit record (saw " + separatorPositions.size() + "); treating signature as invalid");
            return false;
        }

        StringBuffer parsedTmp = new StringBuffer();

        parsedTmp.append(input.substring(separatorPositions.get(0) + 1, separatorPositions.get(7)));

        // user id field
        parsedTmp.append(SEPARATOR_PATTERN);
        String userId = input.substring(separatorPositions.get(7) + 1, separatorPositions.get(8));
        parsedTmp.append(getOid(userId));
        parsedTmp.append(SEPARATOR_PATTERN);

        // provider id field
        String providerId = input.substring(separatorPositions.get(8) + 1, separatorPositions.get(9));
        parsedTmp.append(getOid(providerId));

        // append either the AdminAuditRecord, MessageSummaryAuditRecord or the SystemAuditRecord
        int tmpstart = separatorPositions.get(10);
        int tmpend = separatorPositions.get(11);
        if ((tmpend - tmpstart) > 1) {
            // admin audit record
            parsedTmp.append(input.substring(separatorPositions.get(11), separatorPositions.get(12)));
            parsedTmp.append(SEPARATOR_PATTERN);
            parsedTmp.append(getOid(input.substring(separatorPositions.get(12) + 1, separatorPositions.get(13))));
            parsedTmp.append(input.substring(separatorPositions.get(13), separatorPositions.get(14)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(14);
        tmpend = separatorPositions.get(15);
        if ((tmpend - tmpstart) > 1) {
            // message summary audit record
            parsedTmp.append(input.substring(separatorPositions.get(15), separatorPositions.get(17)));
            parsedTmp.append(SEPARATOR_PATTERN);
            parsedTmp.append(getOid(input.substring(separatorPositions.get(17) + 1, separatorPositions.get(18))));
            parsedTmp.append(input.substring(separatorPositions.get(18), separatorPositions.get(27)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(27);
        tmpend = separatorPositions.get(28);
        if ((tmpend - tmpstart) > 1) {
            // system audit record
            parsedTmp.append(input.substring(separatorPositions.get(28), separatorPositions.get(30)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }

        String parsingResult = parsedTmp.toString();
        parsingResult = Pattern.compile("\\\\([^\\040-\\0176]|\\\\|\\:)").matcher(parsingResult).replaceAll("$1");

        // Append the audit details if any
        tmpstart = input.indexOf("[", separatorPositions.get(30));

        if (tmpstart > 0) {
            String details = input.substring(tmpstart + 1, input.length() - 2);
            String parsedDetail = updateDetailMessages(details);
            parsingResult = parsingResult + parsedDetail;
        }


        return verify(signature, parsingResult);

    }

    boolean verify(String signature, String signable) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, KeyUsageException, CertificateParsingException {

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("should not happen", e);
        }
        byte[] digestvalue = digest.digest(signable.getBytes());

        boolean result = new AuditRecordVerifier(signerCert).verifySignatureOfDigest(signature, digestvalue);
        if (!result)
            System.out.println(signable);
        return result;
    }

    static String unEscape(String input){
        if(input==null)return null;
        return Pattern.compile("\\\\([^\\040-\\0176]|\\\\|\\:)").matcher(input).replaceAll("$1");
    }

    String updateDetailMessages( String detailsStr) {
        StringBuffer digest = new StringBuffer();
        if (detailsStr != null) {
            digest.append("[");
            String[] detailPairs = detailsStr.split("\\:,");
            boolean firstDetail = true;
            for (String detailPair : detailPairs) {
                if (!firstDetail) {
                    digest.append(",");
                }
                String idStr;
                int numIndex = detailPair.indexOf("\\:");
                if (numIndex > 0) {
                    idStr = detailPair.substring(0,numIndex);
                    int paramStart = nextUnescapedSeparator(detailPair, numIndex+1);
                    String fullText = paramStart > 0 ? detailPair.substring(numIndex, paramStart) :detailPair.substring(numIndex);
                    fullText = unEscape(fullText);
                    String params = paramStart > 0 ? (detailPair.length() > paramStart ? (detailPair.substring(paramStart + 1)+":") : null) : null;
                    String[] detailParams = params == null ? null: params.split("\\\\:");
                    String detailMessageString = getMessageStr(idStr,detailParams);
                    digest.append(idStr + "\\:" + detailMessageString);

                } else {
                    // This probably won't work, but we'll try to guess as close as we can
                    digest.append(detailPair);
                }
                firstDetail = false;
            }
            digest.append("]");
        }
        return digest.toString();
    }

    private String getMessageStr(String idStr, String[] detailParams) {
        try {
            int msgId = Integer.parseInt(idStr);
            String msgStr = MessagesUtil.getAuditDetailMessageByIdPre80(msgId);
            if (msgStr == null) return "";
            if (detailParams == null) return msgStr;

            StringBuffer tmp = new StringBuffer();
            MessageFormat mf = new MessageFormat(msgStr);
            mf.format(detailParams, tmp, new FieldPosition(0));
            msgStr = tmp.toString();
            return msgStr;

        } catch (NumberFormatException e) {
            return "";
        }
    }

    private static int nextUnescapedSeparator(String input, int startPos) {
        int res = input.indexOf(SEPARATOR_PATTERN, startPos);
        if (res < 1) return res;
        if (input.charAt(res - 1) == '\\') {
            return nextUnescapedSeparator(input, res + 1);
        } else return res;
    }
}

