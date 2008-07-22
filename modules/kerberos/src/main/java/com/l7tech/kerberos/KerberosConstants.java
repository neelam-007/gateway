package com.l7tech.kerberos;

/**
 * Kerberos constants.
 *
 * <p>You would not usually create instances of this class.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public final class KerberosConstants {

    public static final int ETYPE_DES_CRC = 1;
    public static final int ETYPE_DES_MD4 = 2;
    public static final int ETYPE_DES_MD5 = 3;
    public static final int ETYPE_DES3_MD5 = 5;
    public static final int ETYPE_DES3_SHA1 = 7;
    public static final int ETYPE_DES_EDE = 15;
    public static final int ETYPE_DES3_SHA1_KD = 16;
    public static final int ETYPE_AES128_SHA1 = 17;
    public static final int ETYPE_AES256_SHA1 = 18;
    public static final int ETYPE_RC4_HMAC = 23;

    /**
     * Descriptions by etype (index).
     *
     * <p>Contains nulls for reserved/unknown types.</p> 
     */
    public static final String[] ETYPE_NAMES = {
        null,
        "des-cbc-crc",
        "des-cbc-md4",
        "des-cbc-md5",
        null,
        "des3-cbc-md5",
        null,
        "des3-cbc-sha1",
        null,
        "dsaWithSHA1-CmsOID",
        "md5WithRSAEncryption-CmsOID",
        "sha1WithRSAEncryption-CmsOID",
        "rc2CBC-EnvOID",
        "rsaEncryption-EnvOID",
        "rsaES-OAEP-ENV-OID",
        "des-ede3-cbc-Env-OID",
        "des3-cbc-sha1-kd",
        "aes128-cts-hmac-sha1-96",
        "aes256-cts-hmac-sha1-96",
        null,
        null,
        null,
        null,
        "rc4-hmac",
    };
}
