package com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken;

/**
 * User: rseminoff
 * Date: 18/12/12
 */
public class JwtUtilities {

    public static byte[] encode(byte[] incoming) {
        if (incoming.length != 0) {
            return org.apache.commons.codec.binary.Base64.encodeBase64URLSafe(incoming);
        }
        return null;
    }

    public static byte[] decode(byte[] incoming) {
        if (incoming.length != 0) {
            return org.apache.commons.codec.binary.Base64.decodeBase64(incoming);
        }
        return null;
    }

    // Bitfield for selected signature selection support
    public static final int SELECTED_SIGNATURE_NONE = 0;
    public static final int SELECTED_SIGNATURE_LIST = 0x01;
    public static final int SELECTED_SIGNATURE_VARIABLE = 0x02;  // Signature Variables are not encoded.  Always plaintext.

    // Bitfield for selected secret selection support
    public static final int SELECTED_SECRET_NONE = 0;
    public static final int SELECTED_SECRET_KEY = 0x01;
    public static final int SELECTED_SECRET_PASSWORD = 0x02;
    public static final int SELECTED_SECRET_VARIABLE = 0x04;  // Plaintext Secret Variable Contents
    public static final int SELECTED_SECRET_VARIABLE_BASE64 = 0x0C;  // Secret Variable contents are Base64 Encoded. (0x04 + 0x08)

    // Bitfield for available secret selections
    public static final int AVAILABLE_SECRET_NONE = 0x00;
    public static final int AVAILABLE_SECRET_KEY = 0x01;
    public static final int AVAILABLE_SECRET_PASSWORD = 0x02;
    public static final int AVAILABLE_SECRET_VARIABLE = 0x04; // Plaintext Secret Variable with Base64 Flag. The flag is not optional.
    public static final int AVAILABLE_SECRET_VARIABLE_BASE64 = 0x08;    // Base64 Encoded Variable allowed.

    // For supplied JWT Headers.
    public static final int NO_SUPPLIED_HEADER_CLAIMS = 0x00;
    public static final int SUPPLIED_FULL_JWT_HEADER = 0x01;
    public static final int SUPPLIED_PARTIAL_CLAIMS = 0x02;

}
