/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.util.Enumeration;

/**
 * @author alex
 * @version $Revision$
 */
public class CopyKeystore {
    private CopyKeystore(String[] args) {
        if (args.length < 5) {
            System.err.println("Usage: java " + getClass().getName() + " oldKeystoreFilename oldKeystoreType newKeystoreFilename newKeystoreType password" );
            System.exit(1);
        }

        oldKeystoreFilename = args[0];
        oldKeystoreType = args[1];
        newKeystoreFilename = args[2];
        newKeystoreType = args[3];
        keystorePass = args[4].toCharArray();
    }

    private void doIt() throws Exception {
        File oldKeystoreFile = new File(oldKeystoreFilename);
        if ( !(oldKeystoreFile.exists() && oldKeystoreFile.canRead()) || oldKeystoreFile.isDirectory() )
            throw new IllegalArgumentException("Old keystore file '" + oldKeystoreFilename + "' is not a readable file");

        File newKeystoreFile = new File(newKeystoreFilename);
        if ( newKeystoreFile.exists() || newKeystoreFile.isDirectory() )
            throw new IllegalArgumentException("New keystore file '" + newKeystoreFilename + "' exists or is not a writable file");

        KeyStore oldKeystore = KeyStore.getInstance(oldKeystoreType);
        KeyStore newKeystore = KeyStore.getInstance(newKeystoreType);
        newKeystore.load(null, null);

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(oldKeystoreFile);
            oldKeystore.load(fis, keystorePass);
        } finally {
            if (fis != null) fis.close();
        }

        for (Enumeration e = oldKeystore.aliases(); e.hasMoreElements();) {
            String alias = (String)e.nextElement();
            if (oldKeystore.isKeyEntry(alias)) {
                newKeystore.setKeyEntry(alias, oldKeystore.getKey(alias, keystorePass), keystorePass, oldKeystore.getCertificateChain(alias));
            } else if (oldKeystore.isCertificateEntry(alias)) {
                newKeystore.setCertificateEntry(alias, oldKeystore.getCertificate(alias));
            }
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(newKeystoreFile);
            newKeystore.store(fos, keystorePass);
        } finally {
            if (fos != null) fos.close();
        }
    }

    public static void main(String[] args) throws Exception {
        CopyKeystore me = new CopyKeystore(args);
        me.doIt();
    }

    private final String oldKeystoreFilename;
    private final String oldKeystoreType;
    private final String newKeystoreFilename;
    private final String newKeystoreType;
    private final char[] keystorePass;
}
