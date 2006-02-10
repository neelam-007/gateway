/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;

import java.io.PrintStream;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

/**
 * Configuration noun referring to an Ssg instance.
 */
class SsgNoun extends Noun {
    private final Ssg ssg;

    public SsgNoun(Ssg ssg) {
        super(ssg.getLocalEndpoint(), "Gateway Account for " + ssg.getUsername() + "@" + ssg.getSsgAddress());
        this.ssg = ssg;
        setHelpText(getOverviewHelpText());
    }

    /** @return the Ssg this configuration noun refers to.  Never null. */
    public Ssg getSsg() {
        return ssg;
    }

    public static String getOverviewHelpText() {
        return OVERVIEW_HELP_TEXT;
    }

    // Display all relevant info about this noun to the specified output stream.
    public void show(PrintStream out) {
        super.show(out);

        out.print("Server certificate: ");
        X509Certificate serverCert = ssg.getServerCertificate();
        if (serverCert == null) {
            out.println("<not yet discovered>");
        } else {
            out.println(serverCert.getSubjectDN().getName());
        }

        out.print("Client certificate: ");
        X509Certificate clientCert = ssg.getClientCertificate();
        if (clientCert == null) {
            out.println("<none>");
        } else {
            out.print(clientCert.getSubjectDN().getName());
            try {
                boolean haveKey = ssg.getRuntime().getSsgKeyStoreManager().isClientCertUnlocked();
                if (haveKey)
                    out.println("<private key is ready>");
                else
                    out.println("<private key locked>");
            } catch (KeyStoreCorruptException e) {
                out.println("<private key unavailable>");
            }
        }

        out.println("Property            Value");
        out.println("=================== ====================================================");
        out.println("hostname            " + ssg.getSsgAddress());
        out.println("username            " + ssg.getUsername());
        out.print(  "password            ");
        PasswordAuthentication creds = ssg.getRuntime().getCredentials();
        if (creds == null || creds.getPassword() == null) {
            out.println("<not yet set>");
        } else {
            if (creds.getPassword().length < 1)
                out.println("<empty password>");
            else
                out.println("<present but not shown>");
        }
    }

    private static final String OVERVIEW_HELP_TEXT =
            "The Gateway Account is at the heart of the SecureSpan Bridge.  Each \n" +
                    "Gateway Account holds a set of Bridge credentials for a target Gateway.\n" +
                    "\n" +
                    "Available properties:\n" +
                    "\n" +
                    "  hostname\n" +
                    "  username\n" +
                    "  password\n" +
                    "\n" +
                    "Example Commands: show gateways\n" +
                    "                  show gateway37\n" +
                    "                  create gateway ssg.example.com testuser secret\n" +
                    "                  set gateway1 \n" +
                    "                  delete gateway3\n";
}
