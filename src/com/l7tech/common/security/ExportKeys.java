/*
* Copyright (C) 2003-2004 Layer 7 Technologies Inc.
*
* $Id$
*/

package com.l7tech.common.security;

import com.l7tech.common.util.HexUtils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;

/**
 * @author alex
 * @version $Revision$
 */
public class ExportKeys {
    public static void main( String[] args ) throws Exception {
        String keystorefile = args[0];
        String storepassword = args[1];
        String keypassword = args[2];
        String privout = args[3];
        String pubout = args[4];

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load( new FileInputStream( keystorefile ), storepassword.toCharArray()) ;
        Enumeration enum = ks.aliases();
        while (enum.hasMoreElements()) {
            String alias = (String) enum.nextElement();
            System.err.println("alias="+alias);

            if ( ks.isKeyEntry(alias) ){
                Key privkey = ks.getKey( alias, keypassword.toCharArray() );

                System.err.println("private key format=" + privkey.getFormat());
                FileOutputStream fos = null;
                try {
                    byte encoded[] = privkey.getEncoded();
                    fos = new FileOutputStream(privout);
                    fos.write("-----BEGIN PRIVATE KEY-----\n".getBytes());
                    fos.write(HexUtils.encodeBase64(encoded).getBytes());
                    fos.write("\n-----END PRIVATE KEY-----\n".getBytes());
                    System.err.println( "Wrote " + privout);
                } finally {
                    if ( fos != null ) fos.close();
                }

                Certificate cert = ks.getCertificate(alias);
                try {
                    byte encoded[] = cert.getEncoded();
                    fos = new FileOutputStream(pubout);
                    fos.write("-----BEGIN TRUSTED CERTIFICATE-----\n".getBytes());
                    fos.write(HexUtils.encodeBase64(encoded).getBytes("UTF-8"));
                    fos.write("\n-----END TRUSTED CERTIFICATE-----\n".getBytes());
                    System.err.println( "Wrote " + pubout);
                } finally {
                    if ( fos != null ) fos.close();
                }
            }
        }
    }
}
