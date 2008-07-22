/*
* Copyright (C) 2005 Layer 7 Technologies Inc.
*
*/

package com.l7tech.security.prov.luna;

import com.chrysalisits.crypto.LunaJCAProvider;
import com.chrysalisits.cryptox.LunaJCEProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Provider;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class BouncyCastleToLunaProvider extends Provider {
    private static final Logger logger = Logger.getLogger(BouncyCastleToLunaProvider.class.getName());

    private static final Provider lunajce = new LunaJCEProvider();
    private static final Provider lunajca = new LunaJCAProvider();

    public BouncyCastleToLunaProvider() {
        super("BC2L", 1.0, "Maps algorithm OIDs as used by some Bouncy Castle code to Luna algorithm implementations.");

        init();
    }

    private void init() {
        // Fill in mappings for OID aliases that BC's X.509 classes expect but which Luna's JCE doesn't provide

        /* BEGIN GENERATED CODE -- Do not change here, change in makeTheCode() instead, then rerun */
        put("Cipher.RSA//NOPADDING", "com.chrysalisits.cryptox.LunaCipherRSAX509");
        put("Signature.1.3.14.3.2.26with1.2.840.10040.4.3", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("Signature.1.3.14.3.2.26with1.2.840.10040.4.1", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("Signature.SHA-1/RSA", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("Signature.1.2.840.10040.4.3", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("Signature.1.3.14.3.2.26with1.2.840.113549.1.1.5", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("Signature.1.3.14.3.2.26with1.2.840.113549.1.1.1", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("Signature.SHA/DSA", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("KeyGenerator.1.2.840.113549.3.4", "com.chrysalisits.cryptox.LunaKeyGeneratorRc4");
        put("Cipher.RSA//RAW", "com.chrysalisits.cryptox.LunaCipherRSAX509");
        put("Signature.SHA1WithDSA", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("KeyGenerator.RC5-32", "com.chrysalisits.cryptox.LunaKeyGeneratorRc5");
        put("Signature.SHA1WITHRSA", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("Signature.DSAwithSHA1", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("Signature.DSAWITHSHA1", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("Signature.1.2.840.113549.1.1.5", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("Cipher.RSA/ECB/NOPADDING", "com.chrysalisits.cryptox.LunaCipherRSAX509");
        put("Signature.SHA1withDSA", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("KeyGenerator.1.3.14.3.2.7", "com.chrysalisits.cryptox.LunaKeyGeneratorDes");
        put("Signature.DSAWithSHA1", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("KeyPairGenerator.1.2.840.113549.1.1.1", "com.chrysalisits.crypto.LunaKeyPairGeneratorRsa");
        put("MessageDigest.SHA", "com.chrysalisits.crypto.SHA");
        put("MessageDigest.1.3.14.3.2.26", "com.chrysalisits.crypto.SHA");
        put("Signature.SHA1withRSAEncryption", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("KeyGenerator.ARC4", "com.chrysalisits.cryptox.LunaKeyGeneratorRc4");
        put("Cipher.RSA/NONE/NOPADDING", "com.chrysalisits.cryptox.LunaCipherRSAX509");
        put("Signature.SHA1WITHDSA", "com.chrysalisits.crypto.LunaSignatureDSA");
        put("Signature.1.2.840.113549.1.1.1", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("Signature.SHA1/RSA", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("MessageDigest.SHA1", "com.chrysalisits.crypto.SHA");
        put("Signature.SHA1WITHRSAENCRYPTION", "com.chrysalisits.crypto.LunaSignatureSHA1withRSA");
        put("Cipher.1.2.840.113549.3.4", "com.chrysalisits.cryptox.LunaCipherRC4");
        /* END GENERATED CODE */

        // Now copy in the Luna mappings
        putAll(lunajca);
        putAll(lunajce);
    }

    public synchronized Service getService(String type, String algorithm) {
        logger.warning("Request for security servcie type=" + type + "  alg=" + algorithm);
        return super.getService(type, algorithm);
    }

    private static void makeTheCode() {
        BouncyCastleProvider bc = new BouncyCastleProvider();

        String[] aliases = ALIASES.split("\n");
        for (int i = 0; i < aliases.length; i++) {
            String alias = aliases[i];
            String[] keyval = alias.split("=");
            String key = keyval[0].trim();
            String val = keyval[1].trim();
            //System.err.println(key + ":" + val);
            String[] keyparts = key.split("\\.", 4);
            String category = keyparts[2];
            String aliasName = keyparts[3];
            String realname = category + "." + val;

            String lunaClass = findLuna(category, aliasName, val);

            if (lunaClass == null) {
                System.err.println("LunaJCE does not support: " + realname);
            } else {
                final String newKey = category + "." + aliasName;
                System.out.println("put(\"" + newKey + "\", \"" + lunaClass + "\");");
            }

        }
    }

    private static String findLuna(String category, String aliasName, String val) {
        // Check for real match first
        String found = findAlg(lunajce, category, aliasName);
        if (found == null) findAlg(lunajca, category, aliasName);
        if (found != null) {
            System.err.println("Luna already recognizes " + category + "." + aliasName);
            return null;
        }

        found = findAlg(lunajce, category, val);
        if (found == null) found = findAlg(lunajca, category, val);
        return found;
    }

    private static String findAlg(Provider prov, String category, String val) {
        String found = (String)prov.get(category + "." + val);
        if (found != null) return found;
        val = (String)prov.get("Alg.Alias." + category + "." + val);
        if (val != null) return findAlg(prov, category, val);
        return null;
    }

    public static void main(String[] args) {
        makeTheCode();
        new BouncyCastleToLunaProvider();
    }

    public static final String ALIASES =
            "    Alg.Alias.Cipher.RSA//NOPADDING=RSA\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHHMACRIPEMD160=PBE/PKCS12\n" +
            //"    Alg.Alias.Signature.RMD160withRSA=RIPEMD160WithRSAEncryption\n" +
            //"    Alg.Alias.Mac.HMAC/Tiger=HMACTiger\n" +
            //"    Alg.Alias.SecretKeyFactory.1.3.14.3.2.26=PBE/PKCS12\n" +
            //"    Alg.Alias.Cipher.1.2.840.113549.1.12.1.6=PBEWITHSHAAND40BITRC2-CBC\n" +
            //"    Alg.Alias.Cipher.1.2.840.113549.1.12.1.5=PBEWITHSHAAND128BITRC2-CBC\n" +
            //"    Alg.Alias.Cipher.1.2.840.113549.1.12.1.4=PBEWITHSHAAND2-KEYTRIPLEDES-CBC\n" +
            //"    Alg.Alias.Mac.HMAC-SHA384=HMACSHA384\n" +
            //"    Alg.Alias.Cipher.1.2.840.113549.1.12.1.3=PBEWITHSHAAND3-KEYTRIPLEDES-CBC\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHA1ANDRC2=PBE/PKCS5\n" +
            //"    Alg.Alias.Cipher.1.2.840.113549.1.12.1.2=PBEWITHSHAAND40BITRC4\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHAAND128BITRC4=PBE/PKCS12\n" +
            //"    Alg.Alias.Cipher.1.2.840.113549.1.12.1.1=PBEWITHSHAAND128BITRC4\n" +
            //"    Alg.Alias.Signature.SHA1WithRSA/ISO9796-2=SHA1withRSA/ISO9796-2\n" +
            //"    Alg.Alias.Signature.RIPEMD160WITHRSAENCRYPTION=RIPEMD160WithRSAEncryption\n" +
            "    Alg.Alias.Signature.1.3.14.3.2.26with1.2.840.10040.4.3=DSA\n" +
            "    Alg.Alias.Signature.1.3.14.3.2.26with1.2.840.10040.4.1=DSA\n" +
            //"    Alg.Alias.Signature.1.3.36.3.3.1.2=RIPEMD160WithRSAEncryption\n" +
            "    Alg.Alias.Signature.SHA-1/RSA=SHA1withRSA\n" +
            //"    Alg.Alias.Mac.SKIPJACK/CFB8=SKIPJACKMAC/CFB8\n" +
            "    Alg.Alias.Signature.1.2.840.10040.4.3=DSA\n" +
            "    Alg.Alias.Signature.1.3.14.3.2.26with1.2.840.113549.1.1.5=SHA1withRSA\n" +
            "    Alg.Alias.Signature.1.3.14.3.2.26with1.2.840.113549.1.1.1=SHA1withRSA\n" +
            //"    Alg.Alias.Signature.MD5WITHRSAENCRYPTION=MD5WithRSAEncryption\n" +
            //"    Alg.Alias.Signature.ECDSAWITHSHA1=ECDSA\n" +
            //"    Alg.Alias.Signature.SHA512WithRSAEncryption=SHA512withRSA/PSS\n" +
            "    Alg.Alias.Signature.SHA/DSA=DSA\n" +
            //"    Alg.Alias.Signature.SHA1WITHECDSA=ECDSA\n" +
            //"    Alg.Alias.AlgorithmParameters.2.16.840.1.101.3.4.42=AES\n" +
            //"    Alg.Alias.Cipher.RSA/ECB/PKCS1PADDING=RSA/PKCS1\n" +
            //"    Alg.Alias.Signature.RIPEMD-160/RSA=RIPEMD160WithRSAEncryption\n" +
            //"    Alg.Alias.Cipher.RSA/1/PCKS1PADDING=RSA/1\n" +
            //"    Alg.Alias.Cipher.RSA//OAEPPADDING=RSA/OAEP\n" +
            //"    Alg.Alias.AlgorithmParameterGenerator.2.16.840.1.101.3.4.2=AES\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHAANDTWOFISH-CBC=PBE/PKCS12\n" +
            //"    Alg.Alias.Mac.RC2/CFB8=RC2MAC/CFB8\n" +
            //"    Alg.Alias.Cipher.RSA/ECB/OAEPPADDING=RSA/OAEP\n" +
            //"    Alg.Alias.Signature.RMD160/RSA=RIPEMD160WithRSAEncryption\n" +
            "    Alg.Alias.KeyGenerator.1.2.840.113549.3.4=RC4\n" +
            //"    Alg.Alias.Cipher.2.16.840.1.101.3.4.2=AES\n" +
            "    Alg.Alias.Cipher.RSA//RAW=RSA\n" +
            //"    Alg.Alias.Signature.MD5WithRSA/ISO9796-2=MD5withRSA/ISO9796-2\n" +
            //"    Alg.Alias.Cipher.RSA/NONE/PKCS1PADDING=RSA/PKCS1\n" +
            "    Alg.Alias.Signature.SHA1WithDSA=DSA\n" +
            //"    Alg.Alias.AlgorithmParameters.2.16.840.1.101.3.4.22=AES\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHAANDIDEA-CBC=PBE/PKCS12\n" +
            //"    Alg.Alias.Signature.SHA256withRSAEncryption=SHA256withRSA/PSS\n" +
            //"    Alg.Alias.Signature.MD5withRSA=MD5WithRSAEncryption\n" +
            "    Alg.Alias.KeyGenerator.RC5-32=RC5\n" +
            //"    Alg.Alias.Mac.DESEDE/CFB8=DESEDEMAC/CFB8\n" +
            //"    Alg.Alias.Cipher.RSA/NONE/ISO9796-1PADDING=RSA/ISO9796-1\n" +
            //"    Alg.Alias.AlgorithmParameters.PBEWITHSHAANDRC4=PKCS12PBE\n" +
            //"    Alg.Alias.AlgorithmParameters.PBEWITHSHAANDRC2=PKCS12PBE\n" +
            //"    Alg.Alias.SecretKeyFactory.PBE=PBE/PKCS5\n" +
            //"    Alg.Alias.Mac.HMAC-RIPEMD160=HMACRIPEMD160\n" +
            //"    Alg.Alias.Signature.MD5WithRSA=MD5WithRSAEncryption\n" +
            //"    Alg.Alias.Mac.HMAC-Tiger=HMACTiger\n" +
            //"    Alg.Alias.Signature.SHA384withRSAEncryption=SHA384withRSA/PSS\n" +
            //"    Alg.Alias.Mac.IDEA/CFB8=IDEAMAC/CFB8\n" +
            //"    Alg.Alias.Mac.HMAC/SHA512=HMACSHA512\n" +
            "    Alg.Alias.Signature.SHA1WITHRSA=SHA1withRSA\n" +
            "    Alg.Alias.Signature.DSAwithSHA1=DSA\n" +
            //"    Alg.Alias.SecretKeyFactory.BROKENPBEWITHSHAAND3-KEYTRIPLEDES-CBC=PBE/PKCS12\n" +
            "    Alg.Alias.Signature.DSAWITHSHA1=DSA\n" +
            //"    Alg.Alias.Mac.HMAC/SHA1=HMACSHA1\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHAAND3-KEYTRIPLEDES-CBC=PBE/PKCS12\n" +
            "    Alg.Alias.Signature.1.2.840.113549.1.1.5=SHA1withRSA\n" +
            //"    Alg.Alias.Signature.1.2.840.113549.1.1.5=SHA1withRSA\n" +
            //"    Alg.Alias.SecretKeyFactory.BROKENPBEWITHSHAAND2-KEYTRIPLEDES-CBC=PBE/PKCS12\n" +
            //"    Alg.Alias.Signature.1.2.840.113549.1.1.4=MD5WithRSAEncryption\n" +
            //"    Alg.Alias.Signature.SHA256WITHRSAENCRYPTION=SHA256withRSA/PSS\n" +
            //"    Alg.Alias.Signature.1.2.840.113549.1.1.2=MD2WithRSAEncryption\n" +
            //"    Alg.Alias.Signature.ECDSAwithSHA1=ECDSA\n" +
            //"    Alg.Alias.Mac.HMAC/RIPEMD160=HMACRIPEMD160\n" +
            //"    Alg.Alias.AlgorithmParameters.PBEWITHSHA1ANDRC2=PKCS12PBE\n" +
            //"    Alg.Alias.Mac.HMAC-SHA256=HMACSHA256\n" +
            //"    Alg.Alias.Signature.RIPEMD160withRSA=RIPEMD160WithRSAEncryption\n" +
            //"    Alg.Alias.Mac.HMAC-RIPEMD128=HMACRIPEMD128\n" +
            //"    Alg.Alias.Signature.RMD160WITHRSA=RIPEMD160WithRSAEncryption\n" +
            //"    Alg.Alias.Signature.1.2.840.113549.2.5with1.2.840.113549.1.1.1=MD5WithRSAEncryption\n" +
            //"    Alg.Alias.Mac.HMAC-MD5=HMACMD5\n" +
            //"    Alg.Alias.Mac.HMAC-MD4=HMACMD4\n" +
            //"    Alg.Alias.Mac.HMAC-MD2=HMACMD2\n" +
            "    Alg.Alias.Cipher.RSA/ECB/NOPADDING=RSA\n" +
            //"    Alg.Alias.Signature.MD2withRSA=MD2WithRSAEncryption\n" +
            "    Alg.Alias.Signature.SHA1withDSA=DSA\n" +
            //"    Alg.Alias.Signature.RIPEMD160WithRSA=RIPEMD160WithRSAEncryption\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHA1ANDDES=PBE/PKCS5\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHAAND40BITRC4=PBE/PKCS12\n" +
            //"    Alg.Alias.Cipher.RSA/NONE/OAEPPADDING=RSA/OAEP\n" +
            //"    Alg.Alias.Signature.SHA384WITHRSAENCRYPTION=SHA384withRSA/PSS\n" +
            //"    Alg.Alias.Mac.RC5=RC5MAC\n" +
            //"    Alg.Alias.Mac.RC2=RC2MAC\n" +
            //"    Alg.Alias.Signature.MD2WithRSA=MD2WithRSAEncryption\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHMD5ANDRC2=PBE/PKCS5\n" +
            //"    Alg.Alias.AlgorithmParameters.2.16.840.1.101.3.4.2=AES\n" +
            //"    Alg.Alias.Mac.HMAC-SHA1=HMACSHA1\n" +
            "    Alg.Alias.KeyGenerator.1.3.14.3.2.7=DES\n" +
            //"    Alg.Alias.Mac.HMAC/SHA384=HMACSHA384\n" +
            //"    Alg.Alias.Signature.MD2WITHRSAENCRYPTION=MD2WithRSAEncryption\n" +
            //"    Alg.Alias.KeyFactory.1.2.840.10040.4.1=DSA\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHAAND40BITRC2-CBC=PBE/PKCS12\n" +
            //"    Alg.Alias.Mac.HMAC/RIPEMD128=HMACRIPEMD128\n" +
            "    Alg.Alias.Signature.DSAWithSHA1=DSA\n" +
            //"    Alg.Alias.Cipher.RSA//ISO9796-1PADDING=RSA/ISO9796-1\n" +
            //"    Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.6=PKCS12PBE\n" +
            //"    Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.5=PKCS12PBE\n" +
            //"    Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.4=PKCS12PBE\n" +
            //"    Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.3=PKCS12PBE\n" +
            //"    Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.2=PKCS12PBE\n" +
            //"    Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.1=PKCS12PBE\n" +
            //"    Alg.Alias.Signature.SHA1withECDSA=ECDSA\n" +
            //"    Alg.Alias.CertificateFactory.X509=X.509\n" +
            //"    Alg.Alias.Signature.SHA1WithECDSA=ECDSA\n" +
            //"    Alg.Alias.Signature.ECDSAWithSHA1=ECDSA\n" +
            //"    Alg.Alias.SecretKeyFactory.OLDPBEWITHSHAANDTWOFISH-CBC=PBE/PKCS12\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHHMACSHA=PBE/PKCS12\n" +
            "    Alg.Alias.KeyPairGenerator.1.2.840.113549.1.1.1=RSA\n" +
            //"    Alg.Alias.AlgorithmParameters.PBEWITHSHAAND3-KEYTRIPLEDES=PKCS12PBE\n" +
            //"    Alg.Alias.Signature.SHA512withRSAEncryption=SHA512withRSA/PSS\n" +
            "    Alg.Alias.MessageDigest.SHA=SHA-1\n" +
            //"    Alg.Alias.Mac.IDEA=IDEAMAC\n" +
            //"    Alg.Alias.SecretKeyFactory.BROKENPBEWITHSHA1ANDDES=PBE/PKCS5\n" +
            //"    Alg.Alias.Signature.SHA1WithRSA=SHA1withRSA\n" +
            //"    Alg.Alias.Signature.MD2withRSAEncryption=MD2WithRSAEncryption\n" +
            //"    Alg.Alias.Signature.SHA256WithRSAEncryption=SHA256withRSA/PSS\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHAAND2-KEYTRIPLEDES-CBC=PBE/PKCS12\n" +
            //"    Alg.Alias.Mac.RC5/CFB8=RC5MAC/CFB8\n" +
            //"    Alg.Alias.AlgorithmParameters.1.3.14.3.2.7=DES\n" +
            //"    Alg.Alias.Signature.1.2.840.10045.4.1=ECDSA\n" +
            //"    Alg.Alias.Mac.HMAC/MD5=HMACMD5\n" +
            //"    Alg.Alias.Mac.HMAC/MD4=HMACMD4\n" +
            //"    Alg.Alias.Mac.HMAC/MD2=HMACMD2\n" +
            "    Alg.Alias.MessageDigest.1.3.14.3.2.26=SHA-1\n" +
            //"    Alg.Alias.Signature.SHA384WithRSAEncryption=SHA384withRSA/PSS\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHSHAAND128BITRC2-CBC=PBE/PKCS12\n" +
            //"    Alg.Alias.AlgorithmParameterGenerator.2.16.840.1.101.3.4.42=AES\n" +
            //"    Alg.Alias.SecretKeyFactory.BROKENPBEWITHMD5ANDDES=PBE/PKCS5\n" +
            //"    Alg.Alias.KeyStore.bouncycastle=BouncyCastle\n" +
            //"    Alg.Alias.Signature.MD5/RSA=MD5WithRSAEncryption\n" +
            //"    Alg.Alias.Mac.DES/CFB8=DESMAC/CFB8\n" +
            //"    Alg.Alias.Cipher.RSA//PKCS1PADDING=RSA/PKCS1\n" +
            "    Alg.Alias.Signature.SHA1withRSAEncryption=SHA1withRSA\n" +
            //"    Alg.Alias.Mac.1.3.14.3.2.26=PBEWITHHMACSHA\n" +
            //"    Alg.Alias.Signature.SHA512WITHRSAENCRYPTION=SHA512withRSA/PSS\n" +
            //"    Alg.Alias.Cipher.2.16.840.1.101.3.4.42=AES\n" +
            "    Alg.Alias.KeyGenerator.ARC4=RC4\n" +
            //"    Alg.Alias.AlgorithmParameters.PBEWITHSHAANDIDEA=PKCS12PBE\n" +
            //"    Alg.Alias.KeyStore.UBER=BouncyCastle\n" +
            //"    Alg.Alias.Mac.HMAC-SHA512=HMACSHA512\n" +
            //"    Alg.Alias.Signature.RIPEMD160WITHRSA=RIPEMD160WithRSAEncryption\n" +
            //"    Alg.Alias.SecretKeyFactory.OLDPBEWITHSHAAND3-KEYTRIPLEDES-CBC=PBE/PKCS12\n" +
            "    Alg.Alias.KeyFactory.1.2.840.113549.1.1.1=RSA\n" +
            //"    Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.6=PBE/PKCS12\n" +
            //"    Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.5=PBE/PKCS12\n" +
            //"    Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.4=PBE/PKCS12\n" +
            //"    Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.3=PBE/PKCS12\n" +
            //"    Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.2=PBE/PKCS12\n" +
            //"    Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.1=PBE/PKCS12\n" +
            //"    Alg.Alias.Cipher.RC5-32=RC5\n" +
            //"    Alg.Alias.Signature.RIPEMD160WithRSA/ISO9796-2=RIPEMD160withRSA/ISO9796-2\n" +
            "    Alg.Alias.Cipher.RSA/NONE/NOPADDING=RSA\n" +
            //"    Alg.Alias.Cipher.RSA/ECB/ISO9796-1PADDING=RSA/ISO9796-1\n" +
            //"    Alg.Alias.Cipher.RSA/2/PCKS1PADDING=RSA/2\n" +
            //"    Alg.Alias.AlgorithmParameterGenerator.2.16.840.1.101.3.4.22=AES\n" +
            "    Alg.Alias.Signature.SHA1WITHDSA=DSA\n" +
            "    Alg.Alias.Signature.1.2.840.113549.1.1.1=SHA1withRSA\n" +
            //"    Alg.Alias.KeyStore.BOUNCYCASTLE=BouncyCastle\n" +
            "    Alg.Alias.Signature.SHA1/RSA=SHA1withRSA\n" +
            //"    Alg.Alias.Cipher.2.16.840.1.101.3.4.22=AES\n" +
            //"    Alg.Alias.Signature.RIPEMD160withRSAEncryption=RIPEMD160WithRSAEncryption\n" +
            "    Alg.Alias.MessageDigest.SHA1=SHA-1\n" +
            //"    Alg.Alias.Mac.HMAC/SHA256=HMACSHA256\n" +
            //"    Alg.Alias.Mac.DES=DESMAC\n" +
            //"    Alg.Alias.SecretKeyFactory.PBEWITHMD5ANDDES=PBE/PKCS5\n" +
            //"    Alg.Alias.AlgorithmParameters.PBEWITHSHAAND2-KEYTRIPLEDES=PKCS12PBE\n" +
            //"    Alg.Alias.Signature.MD5withRSAEncryption=MD5WithRSAEncryption\n" +
            //"    Alg.Alias.Mac.DESEDE=DESEDEMAC\n" +
            //"    Alg.Alias.Signature.MD5WITHRSA=MD5WithRSAEncryption\n" +
            //"    Alg.Alias.Mac.SKIPJACK=SKIPJACKMAC\n" +
            "    Alg.Alias.Signature.SHA1WITHRSAENCRYPTION=SHA1withRSA\n" +
            "    Alg.Alias.Cipher.1.2.840.113549.3.4=RC4\n" +
            //"    Alg.Alias.AlgorithmParameters.PBEWITHSHAANDTWOFISH=PKCS12PBE\n" +
            //"    Alg.Alias.Signature.MD2/RSA=MD2WithRSAEncryption" +
            "";
}
