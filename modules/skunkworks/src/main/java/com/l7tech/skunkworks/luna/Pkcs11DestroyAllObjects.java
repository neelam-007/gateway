package com.l7tech.skunkworks.luna;

import sun.security.pkcs11.wrapper.CK_ATTRIBUTE;
import sun.security.pkcs11.wrapper.PKCS11;
import sun.security.pkcs11.wrapper.PKCS11Constants;
import sun.security.pkcs11.wrapper.PKCS11Exception;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 */
public class Pkcs11DestroyAllObjects {
    /** PKCS#11 wrapper */
    public final PKCS11 p;

    /** Our session handle */
    public final long s;

    public Pkcs11DestroyAllObjects(String libPath, char[] tokenPin) throws IOException, PKCS11Exception {
        // Log into the token
        p = PKCS11.getInstance(libPath, "C_GetFunctionList", null, false);
        s = p.C_OpenSession(1, PKCS11Constants.CKF_SERIAL_SESSION, new Object(), null);
        p.C_Login(s, PKCS11Constants.CKU_USER, tokenPin);
    }

    public Pkcs11DestroyAllObjects() throws IOException, PKCS11Exception {
        this(getLibPath(), getTokenPin());
    }

    public static void main(String[] args) throws Exception {
        boolean confirm = true;
        if (args.length > 0) {
            if (args[0].contains("h") || args[0].contains("?")) {
                usage();
            } else if (args[0].equalsIgnoreCase("--noconfirm")) {
                confirm = false;
            }
        }

        Pkcs11DestroyAllObjects p = new Pkcs11DestroyAllObjects();

        if (confirm) {
            p.doInfo();
            p.doList();
            System.out.print("\nAre you sure you wish to destroy all the above objects?\nIf so, type 'proceed' to continue\n> ");
            System.out.flush();
            String line = new BufferedReader(new InputStreamReader(System.in)).readLine();
            System.out.println();
            if (!("proceed".equals(line))) {
                System.out.println("Cancelled.");
                System.exit(3);
            }
        }

        p.doDeleteAllObjectsWithoutConfirmation();
    }

    /**
     * Show token info.
     *
     * @throws PKCS11Exception
     */
    private void doInfo() throws PKCS11Exception {
        System.out.println("PKCS#11 info: " + p.C_GetInfo());
    }


    /**
     * List all objects in the token.
     *
     * @throws PKCS11Exception
     */
    private void doList() throws PKCS11Exception {
        // List all objects
        CK_ATTRIBUTE[] template = new CK_ATTRIBUTE[] {
        };
        p.C_FindObjectsInit(s, template);

        long[] found = p.C_FindObjects(s, 100000);

        System.out.printf("Token contains %d objects\n", found.length);

        for (long handle : found) {
            template = new CK_ATTRIBUTE[] {
                    new CK_ATTRIBUTE(PKCS11Constants.CKA_ID),
                    new CK_ATTRIBUTE(PKCS11Constants.CKA_LABEL)
            };
            p.C_GetAttributeValue(s, handle, template);
            System.out.printf("handle %d %s / %s\n", handle, template[0].toString(), template[1].toString());
        }
    }

    /**
     * Delete all objects in the token without further confirmation.
     *
     * @throws PKCS11Exception
     */
    private void doDeleteAllObjectsWithoutConfirmation() throws PKCS11Exception {
        // List all objects
        CK_ATTRIBUTE[] template = new CK_ATTRIBUTE[] {
        };
        p.C_FindObjectsInit(s, template);

        long[] found = p.C_FindObjects(s, 100000);

        for (long handle : found) {
            p.C_DestroyObject(s, handle);
        }
        System.out.println("Destroyed " + found.length + " objects.");
    }

    private static void usage() {
        System.out.println(
                "Usage: java -Dcom.l7tech.lunaLibraryPath=/path/to/cryptoki.dll -Dcom.l7tech.lunaPin=tH3S-HpW3-sCFK-p7E9 Pkcs11DestroyAllObjects [--noconfirm]\n\n");
        System.exit(1);
    }

    public static String getLibPath() {
        String libPath = System.getProperty("com.l7tech.lunaLibraryPath");
        if (libPath == null)
            throw new IllegalStateException("Please set the system property com.l7tech.lunaLibraryPath to point to the appropriate cryptoki shared library.");
        return libPath;
    }

    public static char[] getTokenPin() {
        String pin = System.getProperty("com.l7tech.lunaPin");
        if (pin == null)
            throw new IllegalStateException("Please set the system property com.l7tech.lunaPin");
        return pin.toCharArray();
    }
}
