package com.l7tech.internal.audit;

import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for verifying old exported audit signatures, from 5.2 and 5.3 (pre-5.3.1).
 */
public class AuditRecordCompatibilityVerifier52 {
    private static Logger logger = Logger.getLogger(AuditRecordCompatibilityVerifier52.class.getName());

    private static final byte COLON_BYTE = (byte)':';
    private static final byte[] COLON_BYTES = ":".getBytes(Charsets.UTF8);
    private static final byte BACKSLASH_BYTE = (byte)'\\';
    private static final byte[] BACKSLASH_BYTES = "\\".getBytes(Charsets.UTF8);
    private static final byte OPENBRACKET_BYTE = (byte)'[';
    private static final byte[] OPENBRACKET_BYTES = "[".getBytes(Charsets.UTF8);
    private static final byte CLOSEBRACKET_BYTE = (byte)']';
    private static final byte[] CLOSEBRACKET_BYTES = "]".getBytes(Charsets.UTF8);
    private static final byte COMMA_BYTE = (byte)',';
    private static final byte[] COMMA_BYTES = ",".getBytes(Charsets.UTF8);


    private final X509Certificate signerCert;
    private final boolean ecc;

    public AuditRecordCompatibilityVerifier52(X509Certificate signerCert) {
        this.signerCert = signerCert;
        this.ecc = "EC".equals(signerCert.getPublicKey().getAlgorithm()) || signerCert.getPublicKey() instanceof ECPublicKey;
    }

    static List<String> splitFields(String line) {
        List<String> ret = new ArrayList<String>();
        StringBuilder sb = new StringBuilder();

        char[] chars = line.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            switch (c) {
                case '\\':
                    if (i + 1 < chars.length)
                        sb.append(chars[++i]);
                    break;
                case ':':
                    ret.add(sb.toString());
                    sb = new StringBuilder();
                    break;
                default:
                    sb.append(c);
            }
        }
        ret.add(sb.toString());
        return ret;
    }

    /**
     *
     * @param signature  signature to use for verification.  If null, this method will extract its own copy of it from the raw exported record.
     * @param rawRecordInExportedFormat  the raw record to use for verification.  Required.  This method expects newline characters to have been
     *                                   unescaped, but no further processing to have been done on the record as read from audit.dat.
     * @return true if the signature verifies with the specified certificate.  False if the signature is invalid.
     * @throws NoSuchAlgorithmException  if a signature algorithm is unavailable
     *                                   (perhaps because an EC cert is specified but there is no implementation of Signature.SHA512withECDSA in the current JVM environment)
     * @throws InvalidKeyException if the signing cert key cannot be used to verify this signature
     * @throws SignatureException if some other error occurs during signature verification
     */
    public boolean verifyAuditRecordSignature(String signature, String rawRecordInExportedFormat) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature sig = Signature.getInstance(ecc ? "SHA512withECDSA" : "SHA512withRSA");
        sig.initVerify(signerCert.getPublicKey());

        int signatureFieldNum = 10;

        int adminOidFieldNum = 11;
        int adminActionFieldNum = 14; // needs special handling since it was written as a single byte rather than as a block. it's the only field like this, luckily
        boolean[] includeFieldAdmin = {
                // objectid:nodeid:time:audit_level:name:message:ip_address:
                   false,   true,  true,true,       true,true,   true,

                //   user_name:user_id:provider_oid:signature:objectid:entity_class:
                     true,     true,   true,        false,    false,   true,

                //   entity_id:action:objectid:status:request_id:service_oid:
                     true,     true,  false,    false, false,     false,

                //   operation_name:authenticated:authenticationtype:request_length:
                     false,         false,        false,             false,

                //   response_length:request_zipxml:response_zipxml:response_status:
                     false,          false,         false,          false,

                //   routing_latency:objectid:component_id:action:audit_associated_logs
                     false,          false,   false,       false
        };

        int systemOidFieldNum = 28;
        boolean[] includeFieldSystem = {
                // objectid:nodeid:time:audit_level:name:message:ip_address:
                   false,   true,  true,true,       true,true,   true,

                //   user_name:user_id:provider_oid:signature:objectid:entity_class:
                     true,     true,   true,        false,    false,   false,

                //   entity_id:action:objectid:status:request_id:service_oid:
                     false,    false, false,    false, false,     false,

                //   operation_name:authenticated:authenticationtype:request_length:
                     false,         false,        false,             false,

                //   response_length:request_zipxml:response_zipxml:response_status:
                     false,          false,         false,          false,

                //   routing_latency:objectid:component_id:action:audit_associated_logs
                     false,          false,   true,        true
        };

        int messageOidFieldNum = 15;
        boolean[] includeFieldMessage = {
                // objectid:nodeid:time:audit_level:name:message:ip_address:
                   false,   true,  true,true,       true,true,   true,

                //   user_name:user_id:provider_oid:signature:objectid:entity_class:
                     true,     true,   true,        false,    false,   false,

                //   entity_id:action:objectid:status:request_id:service_oid:
                     false,    false, false,   true,  true,      true,

                //   operation_name:authenticated:authenticationtype:request_length:
                     true,          true,         true,              true,

                //   response_length:request_zipxml:response_zipxml:response_status:
                     true,           true,          true,           true,

                //   routing_latency:objectid:component_id:action:audit_associated_logs
                     true,           false,   false,       false
        };


        List<String> allFields = splitFields(rawRecordInExportedFormat);
        if (allFields.size() < 30) {
            logger.info("Not enough fields in exported audit record (saw " + allFields.size() + "); treating signature as invalid");
            return false;
        }

        if (signature == null) {
            signature = allFields.get(signatureFieldNum);
            if (signature == null || signature.trim().length() < 1) {
                logger.info("No signature in record; treating signature as invalid");
                return false;
            }
        }

        boolean[] includeField;
        if (allFields.get(adminOidFieldNum).length() > 0) {
            includeField = includeFieldAdmin;
        } else if (allFields.get(systemOidFieldNum).length() > 0) {
            includeField = includeFieldSystem;
        } else if (allFields.get(messageOidFieldNum).length() > 0) {
            includeField = includeFieldMessage;
        } else {
            logger.info("Unable to identify record type; treating signature as invalid");
            return false;
        }

        LinkedList<String> fields = new LinkedList<String>(allFields);
        if (fields.isEmpty()) {
            // can't happen
            logger.info("Record contains no fields");
            return false;
        }
        String details = fields.removeLast();

        // Emulate double-write bug present in rel5_2 and rel5_3
        boolean first = true;
        int fieldNum = 0;
        for (String s : fields) {
            if (fieldNum < includeField.length && includeField[fieldNum]) {
                if (!first) {
                    updateSingleByte(sig, COLON_BYTE);
                    updateByteArray(sig, COLON_BYTES);
                }
                first = false;
                if (s.length() > 0) {
                    if (fieldNum == adminActionFieldNum && s.length() == 1) {
                        // Action got written as a single byte, so no dupe
                        updateSingleByte(sig, s.getBytes(Charsets.UTF8)[0]);
                    } else {
                        updateStringAsBytesThenBlock(sig, s);
                    }
                }
            }
            fieldNum++;
        }

        Matcher detailMatcher = Pattern.compile("^\\[(.*)\\]$", Pattern.DOTALL).matcher(details);
        if (detailMatcher.matches()) {
            updateDetailMessages(sig, detailMatcher.group(1));
        } else {
            updateStringAsBytesThenBlock(sig, ":");
        }

        return sig.verify(HexUtils.decodeBase64(signature.trim(), true));
    }

    void updateDetailMessages(Signature sig, String detailsStr) throws SignatureException {
        if (detailsStr != null) {
            updateSingleByte(sig, COLON_BYTE);
            updateByteArray(sig, COLON_BYTES);
            updateSingleByte(sig, OPENBRACKET_BYTE);
            updateByteArray(sig, OPENBRACKET_BYTES);
            String[] detailPairs = detailsStr.split("(?<!\\\\),(?=-?\\d+\\\\:)");
            boolean firstDetail = true;
            for (String detailPair : detailPairs) {
                if (!firstDetail) {
                    updateSingleByte(sig, COMMA_BYTE);
                    updateByteArray(sig, COMMA_BYTES);
                }
                String[] numAndMsg = detailPair.split("\\\\:", 2);
                if (numAndMsg.length == 2) {
                    updateStringAsBytesThenBlock(sig, numAndMsg[0]);
                    updateSingleByte(sig, BACKSLASH_BYTE);
                    updateByteArray(sig, BACKSLASH_BYTES);
                    updateSingleByte(sig, COLON_BYTE);
                    updateByteArray(sig, COLON_BYTES);
                    updateStringAsBytesThenBlock(sig, numAndMsg[1]);
                } else {
                    // This probably won't work, but we'll try to guess as close as we can
                    logger.info("Malformed detail number and message pair: " + detailPair);
                    updateStringAsBytesThenBlock(sig, detailPair);
                }
                firstDetail = false;
            }
            updateSingleByte(sig, CLOSEBRACKET_BYTE);
            updateByteArray(sig, CLOSEBRACKET_BYTES);
        }
    }

    private static void updateStringAsBytesThenBlock(Signature sig, String s) throws SignatureException {
        byte[] fieldBytes = s.getBytes(Charsets.UTF8);
        for (byte b : fieldBytes) {
            updateSingleByte(sig, b);
        }
        updateByteArray(sig, fieldBytes);
    }

    private static void updateByteArray(Signature sig, byte[] bytes) throws SignatureException {
        if (logger.isLoggable(Level.FINEST)) logger.finest("update byte array : " + HexUtils.hexDump(bytes) + " (" + new String(bytes) + ")");
        sig.update(bytes);
    }

    private static void updateSingleByte(Signature sig, byte b) throws SignatureException {
        if (logger.isLoggable(Level.FINEST)) logger.finest("update single byte: " + HexUtils.hexDump(new byte[] { b }) + " (" + new String(new byte[] { b }) + ")");
        sig.update(b);
    }
}

