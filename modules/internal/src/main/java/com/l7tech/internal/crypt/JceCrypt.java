package com.l7tech.internal.crypt;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.Provider;
import java.security.Security;

/**
 * Command line utility to encrypt or decrypt using JCE.
 */
public class JceCrypt {

    private static String cipherName = "AES/GCM/NoPadding";
    private static final String keyAlg = "AES";
    private static final byte[] secretKeyBytes =
            new byte[]{0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
                    0x18, 0x19, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25};
    private static final byte[] ivBytes =
            new byte[]{0x01, 0x02, 0x03, 0x04, 0x05, 0x06,
                    0x09, 0x10, 0x11, 0x12, 0x13, 0x14};
    private static final int ivLen = 12;

    public static void main(String[] args) throws Exception {
        if (Boolean.getBoolean("addprov"))
            Security.addProvider((Provider)Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider").newInstance());

        if (args[0].equals("encrypt")) {
            doEncrypt();
        } else {
            InputStream in = System.in;
            if (args.length > 1)
                in = new ByteArrayInputStream(args[1].getBytes());
            if (in == System.in)
                System.err.println("Reading Base64 encoded ciphertext from STDIN");

            doDecrypt(in);
        }
    }

    private static void doEncrypt() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Cipher cipher = Cipher.getInstance(JceCrypt.cipherName);
        Key key = new SecretKeySpec(secretKeyBytes, keyAlg);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);
        assertTrue(iv.getIV().length == ivLen);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        out.write(cipher.getIV());

        byte[] cipherBlock = cipher.update("test stuff woohoo".getBytes());
        if (cipherBlock != null) out.write(cipherBlock);

        cipherBlock = cipher.doFinal();
        if (cipherBlock != null) out.write(cipherBlock);

        System.err.println("Reference output: AQIDBAUGCRAREhMUysjZuvNsiE4JkvIqta+g3oNYpglp5C3TkPQAO0R6KlGK\nActual output:");
        System.err.flush();
        System.out.println(new BASE64Encoder().encodeBuffer(out.toByteArray()));
    }

    private static void doDecrypt(InputStream in) throws Exception {
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        BASE64Decoder decoder = new BASE64Decoder();
        decoder.decodeBuffer(in, input);
        in = new ByteArrayInputStream(input.toByteArray());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Key key = new SecretKeySpec(secretKeyBytes, keyAlg);
        byte[] iv = new byte[ivLen];
        assertEquals(ivLen, in.read(iv));

        Cipher cipher = Cipher.getInstance(JceCrypt.cipherName);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] block = new byte[cipher.getBlockSize()];
        int got;
        while ((got = in.read(block)) > 0) {
            byte[] output = cipher.update(block, 0, got);
            if (output != null) out.write(output);
        }

        byte[] output = cipher.doFinal();
        if (output != null) out.write(output);

        System.out.println("Decrypted plaintext: " + out.toString());
    }

    private static void assertTrue(Boolean b) {
        if (!b) throw new AssertionError("Assertion failed");
    }

    private static void assertEquals(Object left, Object right) {
        if (!left.equals(right)) throw new AssertionError("Assertion failed: expected " + left + ", got " + right);
    }
}
