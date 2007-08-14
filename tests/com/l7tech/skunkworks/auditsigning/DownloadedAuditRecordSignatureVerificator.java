package com.l7tech.skunkworks.auditsigning;

import javax.security.cert.X509Certificate;
import java.util.ArrayList;

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
    private boolean isSigned;
    private String signature;
    private String recordInExportedFormat;
    private String parsedRecordInSignableFormat;
    private String type;

    public static DownloadedAuditRecordSignatureVerificator parse(String input) {
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
            pos = tmp+1;
            tmp = nextUnescapedSeparator(input, pos);
        }

        if (separatorPositions.size() < 30) {
            throw new RuntimeException("This does not appear to be a valid audit record (" + separatorPositions.size() + ")");
        }

        StringBuffer parsedTmp = new StringBuffer();

        // extract signature, remove initial ID and signature the signature starts after the 10th ':' and has a length of 173
        out.signature = input.substring(separatorPositions.get(9) + 1, separatorPositions.get(10));
        if (out.signature == null || out.signature.length() < 1) {
            // we're dealing with a record which does not contain a signature
            out.isSigned = false;
        } else if (out.signature.length() != 172) {
            throw new IllegalArgumentException("Unexpected signature length " + out.signature.length() + ". " + out.signature);
        } else {
            out.isSigned = true;
        }
        parsedTmp.append(input.substring(separatorPositions.get(0) +1, separatorPositions.get(9)));

        // append either the AdminAuditRecord, MessageSummaryAuditRecord or the SystemAuditRecord
        int tmpstart = separatorPositions.get(10);
        int tmpend = separatorPositions.get(11);
        if ((tmpend - tmpstart) > 1) {
            if (out.type != null) {
                throw new RuntimeException("record cannot be admin AND " + out.type);
            }
            out.type = "Admin";
            parsedTmp.append(input.substring(separatorPositions.get(11), separatorPositions.get(14)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(14);
        tmpend = separatorPositions.get(15);
        if ((tmpend - tmpstart) > 1) {
            if (out.type != null) {
                throw new RuntimeException("record cannot be summary AND " + out.type);
            }
            out.type = "Msg Summary";
            parsedTmp.append(input.substring(separatorPositions.get(15), separatorPositions.get(27)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }
        tmpstart = separatorPositions.get(27);
        tmpend = separatorPositions.get(28);
        if ((tmpend - tmpstart) > 1) {
            if (out.type != null) {
                throw new RuntimeException("record cannot be system AND " + out.type);
            }
            out.type = "System";
            parsedTmp.append(input.substring(separatorPositions.get(28), separatorPositions.get(30)));
            parsedTmp.append(SEPARATOR_PATTERN);
        }

        // Unescape : separators until now
        String parsingResult = parsedTmp.toString();
        parsingResult = parsingResult.replace("\\:", ":");

        // Append the audit details if any
        tmpstart = input.indexOf("[", separatorPositions.get(30));
        if (tmpstart > 0) {
            parsingResult = parsingResult + input.substring(tmpstart);
        }

        out.parsedRecordInSignableFormat = parsingResult;
        return out;
    }

    public boolean verifySignature(X509Certificate cert) {
        // todo
        return true;
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
        if (input.charAt(res-1) == '\\') {
            return nextUnescapedSeparator(input, res+1);
        } else return res;
    }

    public String toString() {
        return "Parsed audit record: " + parsedRecordInSignableFormat + "\n" +
               "Signed: " + isSigned + "\n" +
               "Record Type: " + type;
    }

    private static final String SEPARATOR_PATTERN = ":";
}
