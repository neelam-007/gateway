package com.l7tech.common.security;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.OutputStream;
import java.io.IOException;
import java.security.*;

/**
 *  List the features of the security providers registered in the VM (see jre/lib/security/java.secuirty file)
 *
 *  <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class JceProviderMatrix {
    public static void main(String[] args) throws IOException {

        String[] keystoreTypes = {
            "JKS",
            "PKCS12",
            "BCPKCS12",
        };

        String[] cryptoAlgos = {
            "RSA",
            "AES",
            "DES",
            "DESede",
            "RC2",
            "RC4",
        };

        String[] digestAlgos = {
            "MD5",
            "SHA-1",
        };

        OutputStream out = System.out;
        pad(out, "", 20);
        for (int j = 0; j < keystoreTypes.length; j++) {
            String keystoreType = keystoreTypes[j];
            out.write(keystoreType.getBytes());
            out.write(' ');
        }
        for (int j = 0; j < digestAlgos.length; j++) {
            String digestAlgo = digestAlgos[j];
            out.write(digestAlgo.getBytes());
            out.write(' ');
        }
        for (int j = 0; j < cryptoAlgos.length; j++) {
            String cryptoAlgo = cryptoAlgos[j];
            out.write(cryptoAlgo.getBytes());
            out.write(' ');
        }
        out.write('\n');

        Provider[] providers = Security.getProviders();

        String[] providerNames = new String[providers.length];

        for (int i = 0; i < providers.length; i++ ) {
            providerNames[i] = providers[i].getName();
        }

        for (int i = 0; i < providerNames.length; i++) {
            final String providerName = providerNames[i];
            pad(out, providerName, 20);

            char got;

            // Check keystores
            for (int j = 0; j < keystoreTypes.length; j++) {
                String keystoreType = keystoreTypes[j];
                got = ' ';
                try {
                    if (KeyStore.getInstance(keystoreType, providerName) != null) got = 'X';
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                    got = '?';
                } catch (KeyStoreException e) {
                }
                pad(out, String.valueOf(got), keystoreTypes[j].length()+1);
            }

            // Check digests
            for (int j = 0; j < digestAlgos.length; j++) {
                String digestAlgo = digestAlgos[j];
                got = ' ';
                try {
                    if (MessageDigest.getInstance(digestAlgo, providerName) != null) got = 'X';
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                    got = '?';
                } catch (NoSuchAlgorithmException e) {
                }
                pad(out, String.valueOf(got), digestAlgos[j].length()+1);

            }

            // Check algos
            for (int j = 0; j < cryptoAlgos.length; j++) {
                String cryptoAlgo = cryptoAlgos[j];
                got = ' ';
                try {
                    if (Cipher.getInstance(cryptoAlgo, providerName) != null) got = 'X';
                } catch (NoSuchProviderException e) {
                    e.printStackTrace();
                    got = '?';
                } catch (NoSuchAlgorithmException e) {
                } catch (NoSuchPaddingException e) {
                    e.printStackTrace();
                    got = '?';
                }
                pad(out, String.valueOf(got), cryptoAlgos[j].length()+1);

            }
            out.write('\n');
        }
    }

    static void pad(OutputStream out, String s, int len) throws IOException {
        out.write(s.getBytes());
        for (int i = 0; i < len - s.length(); ++i) {
            out.write(' ');
        }
    }
}
