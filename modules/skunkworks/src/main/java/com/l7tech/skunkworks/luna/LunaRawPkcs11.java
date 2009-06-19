package com.l7tech.skunkworks.luna;

import sun.security.pkcs11.wrapper.*;

import java.io.IOException;
import java.text.MessageFormat;

/**
 * Accesses the Luna via raw PKCS#11 (well, through the Sun wrapper class) to do various things.
 */
public class LunaRawPkcs11 {

    /** PKCS#11 wrapper */
    public final PKCS11 p;

    /** Our session handle */
    public final long s;

    public LunaRawPkcs11(String libPath, char[] tokenPin) throws IOException, PKCS11Exception {
        // Log into the token
        p = PKCS11.getInstance(libPath, "C_GetFunctionList", null, false);
        CK_NOTIFY notify = new CK_NOTIFY() {
            public void CK_NOTIFY(long a, long b, Object o) throws PKCS11Exception {
                System.out.println(MessageFormat.format("CK_NOTIFY: {0} {1} {2}", a, b, o));
            }
        };
        s = p.C_OpenSession(1, PKCS11Constants.CKF_SERIAL_SESSION, new Object(), notify);
        p.C_Login(s, PKCS11Constants.CKU_USER, tokenPin);
    }

    public LunaRawPkcs11() throws IOException, PKCS11Exception {
        this(getLibPath(), getTokenPin());
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) usage();

        new LunaRawPkcs11().executeCommand(args);
    }

    private void executeCommand(String[] args) throws PKCS11Exception {
        String command = args[0];
        if ("info".equalsIgnoreCase(command))
            doInfo();
        else if ("list".equalsIgnoreCase(command))
            doList();
        else if ("deleteAllObjectsWithoutConfirmation".equalsIgnoreCase(command))
            doDeleteAllObjectsWithoutConfirmation();
        else
            usage();
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
                "Usage: LunaRawPkcs11 <command>\n\n" +
                "Available commands: \n  info\n  list\n  deleteAllObjectsWithoutConfirmation\n\n");
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
