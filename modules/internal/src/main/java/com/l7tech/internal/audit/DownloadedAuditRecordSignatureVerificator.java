package com.l7tech.internal.audit;

import com.l7tech.gateway.common.audit.AuditRecordVerifier;
import com.l7tech.util.SyspropUtil;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Stuff used to parse downloaded audit records and verify it's signature.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 14, 2007<br/>
 */
public class DownloadedAuditRecordSignatureVerificator {
    private Logger logger = Logger.getLogger(DownloadedAuditRecordSignatureVerificator.class.getName());

    private static final boolean ENABLE_COMPAT_52 = SyspropUtil.getBoolean("audit.signature.verifier.compat52.enable", true);
    private static final boolean ENABLE_COMPAT_PRE_80 = SyspropUtil.getBoolean("audit.signature.verifier.compatPre80.enable", true);
    private static final boolean ENABLE_COMPAT_PRE_80_EXPORTS = SyspropUtil.getBoolean("audit.signature.verifier.compatPre80Exports.enable", true);

    private boolean isSigned;
    private String signature;
    private String recordInExportedFormat;
    private String parsedRecordInSignableFormat;
    private String type;
    private String auditID;

    public static class InvalidAuditRecordException extends Exception {
        public InvalidAuditRecordException(String msg) {
            super(msg);
        }
    }

    public String getAuditID() {
        return auditID;
    }

    public static DownloadedAuditRecordSignatureVerificator parse(String input) throws InvalidAuditRecordException {
        if (input == null) return null;
        input = input.trim();
        // removed escaped \n characters (replace '\\\n' with '\n')
        input = input.replace("\\\n", "\n");

        DownloadedAuditRecordSignatureVerificator out = new DownloadedAuditRecordSignatureVerificator();
        out.recordInExportedFormat = input;
        ArrayList<Integer> separatorPositions = new ArrayList<Integer>();
        int pos = 0;
        int tmp = nextUnescapedSeparator(input, pos);
        while (tmp >= 0) {
            separatorPositions.add(tmp);
            pos = tmp + 1;
            tmp = nextUnescapedSeparator(input, pos);
        }

        if (separatorPositions.size() < 30) {
            throw new InvalidAuditRecordException("This does not appear to be a valid audit record (" +
                    separatorPositions.size() + ")");
        }

        StringBuffer parsedTmp = new StringBuffer();

        // extract signature, remove initial ID and signature the signature starts after the 10th ':' and has a length of 173
        out.signature = input.substring(separatorPositions.get(9) + 1, separatorPositions.get(10));
        if (out.signature == null || out.signature.length() < 1) {
            // we're dealing with a record which does not contain a signature
            out.isSigned = false;
        } else if (out.signature.length() < 64) {
            throw new InvalidAuditRecordException("Unexpected signature length " + out.signature.length() +
                    ". " + out.signature);
        } else {
            out.isSigned = true;
        }
        out.auditID = input.substring(0, separatorPositions.get(0));
        parsedTmp.append(input.substring(separatorPositions.get(0) + 1, separatorPositions.get(9)));

        // append either the AdminAuditRecord, MessageSummaryAuditRecord or the SystemAuditRecord
        int tmpstart = separatorPositions.get(10);
        int tmpend = separatorPositions.get(11);
        if ((tmpend - tmpstart) > 1) {
            if (out.type != null) {
                throw new InvalidAuditRecordException("record cannot be admin AND " + out.type);
            }
            out.type = "Admin";
            parsedTmp.append(input.substring(separatorPositions.get(11), separatorPositions.get(14)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(14);
        tmpend = separatorPositions.get(15);
        if ((tmpend - tmpstart) > 1) {
            if (out.type != null) {
                throw new InvalidAuditRecordException("record cannot be summary AND " + out.type);
            }
            out.type = "Msg Summary";
            parsedTmp.append(input.substring(separatorPositions.get(15), separatorPositions.get(27)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(27);
        tmpend = separatorPositions.get(28);
        if ((tmpend - tmpstart) > 1) {
            if (out.type != null) {
                throw new InvalidAuditRecordException("record cannot be system AND " + out.type);
            }
            out.type = "System";
            parsedTmp.append(input.substring(separatorPositions.get(28), separatorPositions.get(30)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }

        String parsingResult = parsedTmp.toString();
        parsingResult = Pattern.compile("\\\\([^\\040-\\0176]|\\\\|\\:)").matcher(parsingResult).replaceAll("$1");

        // Append the audit details if any
        tmpstart = input.indexOf("[", separatorPositions.get(30));

        if (tmpstart > 0) {
            String details = input.substring(tmpstart + 1, input.length() - 2);
            String parsedDetail = parseDetailMessages( details);
            parsingResult = parsingResult + parsedDetail;
        }



        out.parsedRecordInSignableFormat = parsingResult;
        return out;
    }

    static String unEscape(String input){
        if(input==null)return null;
        return Pattern.compile("\\\\([^\\040-\\0176]|\\\\|\\:)").matcher(input).replaceAll("$1");
    }

    static String parseDetailMessages( String detailsStr) {
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
                    params = unEscape(params);
                    digest.append(idStr + "\\:" + (params == null ? "" : params));

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

    public boolean verifySignature(X509Certificate cert) throws IOException {
        if (!isSigned()) {
            logger.info("Verify signature fails because the record is not signed.");
            return false;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("should not happen", e);
        }
        byte[] digestvalue = digest.digest(parsedRecordInSignableFormat.getBytes());

        try {
            boolean result = new AuditRecordVerifier(cert).verifySignatureOfDigest(signature, digestvalue);

            if (!result && ENABLE_COMPAT_PRE_80) {
                // Try again in compatibility mode with records signed using the format used for pre 8.0.
                try {
                    result = new AuditRecordCompatibilityVerifierUpgradedPre80(cert).verifyAuditRecordSignature(signature, recordInExportedFormat, type);
                } catch (Exception e) {/* intentionally ignore compatibility check errors, overwrites original error*/ }
                ;
            }

            if (!result && ENABLE_COMPAT_PRE_80_EXPORTS) {
                // Try again in compatibility mode with records signed using the format used for pre 8.0.
                try {
                    result = new AuditRecordCompatibilityVerifierPre80Exports(cert).verifyAuditRecordSignature(recordInExportedFormat);
                } catch (Exception e) {/* intentionally ignore compatibility check errors, overwrites original error*/ }
                ;
            }



            if (!result && ENABLE_COMPAT_52) {
                // Try again in compatibility mode with records signed using the format used for 5.2 and 5.3.
                // Note that this needs to go all the way back to the raw record.
                try {
                    result = new AuditRecordCompatibilityVerifier52(cert).verifyAuditRecordSignature(signature, recordInExportedFormat);
                } catch (Exception e) {/* intentionally ignore compatibility check errors, overwrites original error*/ }
                ;
            }

            return result;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public boolean isSigned() {
        return isSigned;
    }

    public String getSignature() {
        return signature;
    }

    public String getRecordInExportedFormat() {
        return recordInExportedFormat;
    }

    public String getParsedRecordInSignableFormat() {
        return parsedRecordInSignableFormat;
    }

    private static int nextUnescapedSeparator(String input, int startPos) {
        int res = input.indexOf(SEPARATOR_PATTERN, startPos);
        if (res < 1) return res;
        if (input.charAt(res - 1) == '\\') {
            return nextUnescapedSeparator(input, res + 1);
        } else return res;
    }

    public String toString() {
        return "Parsed audit record: " + parsedRecordInSignableFormat + "\n" +
                "Signed: " + isSigned + "\n" +
                "Record Type: " + type;
    }

    private static final String SEPARATOR_PATTERN = ":";
}
