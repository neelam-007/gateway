/*
* Copyright (C) 2003-2004 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.gateway.common.security;

import com.l7tech.util.HexUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;

/**
 * Exports a JKS keystore to a PKCS#8 keypair and certificate in separate files, both PEM-encoded
 *
 * @author alex
 * @version $Revision$
 */
public class ExportKeys {
    public static void main( String[] args ) throws Exception {
        if (args.length < 5) {
            System.err.println("Usage: java " + ExportKeys.class.getName() +
                               " jksKeystoreFile keystorePassword keyPassword exportKeyFile exportCertFile [keystoreType]" );
            System.exit(1);
        }

        String keystoreFilename = args[0];
        String keystorePassword = args[1];
        String keyPassword = args[2];
        String exportKeyFilename = args[3];
        String certFilename = args[4];
        String keystoreType = "JKS";
        if (args.length > 5) {
            keystoreType = args[5];
        }

        FileInputStream fis = null;
        if (keystoreFilename.length() > 0 && !"NONE".equals(keystoreFilename)) {
            File keystoreFile = new File(keystoreFilename);
            if (!(keystoreFile.exists() && keystoreFile.isFile() && keystoreFile.canRead())) {
                System.err.println("Keystore File '" + keystoreFilename + "' does not exist or is not a readable file");
                System.exit(2);
            }
            fis = new FileInputStream(keystoreFile);
        }

        KeyStore ks = KeyStore.getInstance(keystoreType);
        ks.load(fis, keystorePassword.toCharArray()) ;
        Enumeration e = ks.aliases();
        int count = 0;
        while (e.hasMoreElements()) {
            String alias = (String) e.nextElement();
            System.err.println("alias="+alias);
            count++;

            if ( ks.isKeyEntry(alias) ){
                Key privkey = ks.getKey( alias, keyPassword.equals("NULL") ? null : keyPassword.toCharArray() );

                System.err.println("private key format=" + privkey.getFormat());
                FileOutputStream fos = null;
                try {
                    byte encoded[] = privkey.getEncoded();
                    fos = new FileOutputStream(exportKeyFilename + "-" + alias);
                    fos.write("-----BEGIN PRIVATE KEY-----\n".getBytes());
                    fos.write(HexUtils.encodeBase64(encoded).getBytes());
                    fos.write("\n-----END PRIVATE KEY-----\n".getBytes());
                    System.err.println( "Wrote " + exportKeyFilename);
                } finally {
                    if ( fos != null ) fos.close();
                }

                Certificate cert = ks.getCertificate(alias);
                try {
                    byte encoded[] = cert.getEncoded();
                    fos = new FileOutputStream(certFilename + "-" + alias);
                    fos.write("-----BEGIN TRUSTED CERTIFICATE-----\n".getBytes());
                    fos.write(HexUtils.encodeBase64(encoded).getBytes("UTF-8"));
                    fos.write("\n-----END TRUSTED CERTIFICATE-----\n".getBytes());
                    System.err.println( "Wrote " + certFilename);
                } finally {
                    if ( fos != null ) fos.close();
                }
            }
        }
        System.err.println("Found " + count + " aliases");
    }
}
