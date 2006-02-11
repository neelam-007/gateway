/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.TextUtils;
import com.l7tech.common.util.CertUtils;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

/**
 * Configuration noun referring to an Ssg instance.
 */
class SsgNoun extends Noun {
    private final Ssg ssg;
    private final Words properties;

    public SsgNoun(Ssg inssg) {
        super(inssg.getLocalEndpoint(), "Gateway Account for " + inssg.getUsername() + "@" + inssg.getSsgAddress());
        this.ssg = inssg;
        properties = new Words(Arrays.asList(new NounProperty[] {
                new NounProperty(ssg, "hostname", "SsgAddress", "Hostname or IP address of SecreSpan Gateway"),

                new NounProperty(ssg, "username", "Username", "Username of account on this Gateway"),

                new NounProperty(ssg.getRuntime(), "password", "CachedPassword", "Password of account on this Gateway") {
                    public void printValue(PrintStream out, boolean singleLine) {
                        char[] pass = (char[])getValue();
                        if (pass == null) {
                            out.print("<not yet set>");
                        } else {
                            if (pass.length < 1)
                                out.print("<empty password>");
                            else
                                out.print("<present but not shown>");
                        }
                        if (!singleLine) out.println();
                    }

                    protected Object getValue() {
                        PasswordAuthentication creds = ssg.getRuntime().getCredentials();
                        return creds == null ? null : creds.getPassword();
                    }
                },

                new NounProperty(ssg, "chainCredentials", "ChainCredentialsFromClient", "Chain credentials from client (HTTP Basic)"),

                new NounProperty(ssg, "savePassword", "SavePasswordToDisk", "Save the password in the configuration file"),

                new NounProperty(ssg, "serverCert", "ServerCertificate", "Gateway X.509 SSL certificate") {
                    public void printValue(PrintStream out, boolean singleLine) {
                        X509Certificate cert = (X509Certificate)getValue();

                        if (cert == null) {
                            out.print("<not yet discovered>");
                            if (!singleLine) out.println();
                            return;
                        }

                        if (singleLine) {
                            out.print(cert.getSubjectDN().getName());
                            return;
                        }

                        try {
                            out.println(CertUtils.toString(cert));
                        } catch (CertificateEncodingException e) {
                            out.println("<certificate damaged>"); // can't happen
                        }
                    }
                },

                new NounProperty(ssg, "clientCert", "ClientCertificate", "Bridge X.509 client ceritifcate") {
                    public void printValue(PrintStream out, boolean singleLine) {
                        final boolean m = !singleLine;
                        X509Certificate clientCert = (X509Certificate)getValue();
                        if (clientCert == null) {
                            out.print("<none>");
                            if (m) out.println();
                            return;
                        } else {
                            if (singleLine) {
                                out.print(clientCert.getSubjectDN().getName());
                            } else {
                                try {
                                    out.println(CertUtils.toString(clientCert));
                                } catch (CertificateEncodingException e) {
                                    out.println("<certificate damaged>"); // can't happen
                                }
                            }
                            try {
                                boolean haveKey = ssg.getRuntime().getSsgKeyStoreManager().isClientCertUnlocked();
                                if (haveKey) {
                                    out.print("<private key ready>");
                                } else {
                                    out.print("<private key locked>");
                                }
                            } catch (KeyStoreCorruptException e) {
                                out.print("<private key unavailable>");
                            }
                            if (m) out.println();
                        }
                    }
                },
        }));
    }

    /** @return the Ssg this configuration noun refers to.  Never null. */
    public Ssg getSsg() {
        return ssg;
    }

    public static String getOverviewHelpText() {
        return OVERVIEW_HELP_TEXT;
    }

    public String getHelpText() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(baos);

        out.println(getOverviewHelpText());
        out.println("\nAvailable properties:\n");

        List props = properties.getAll();
        for (Iterator i = props.iterator(); i.hasNext();) {
            NounProperty prop = (NounProperty)i.next();
            out.print(TextUtils.pad(prop.getName(), 20));
            out.print(' ');
            out.println(prop.getDesc());
        }

        out.println();
        out.println(EXAMPLE_COMMANDS);

        out.close();
        return baos.toString();
    }

    // Display all relevant info about this noun to the specified output stream.
    public void show(PrintStream out, String[] args) {
        super.show(out, args);

        if (args != null && args.length > 0) {
            String arg = args[0];
            //noinspection UnusedAssignment
            args = ArrayUtils.shift(args);
            NounProperty prop = (NounProperty)properties.getByPrefix(arg);
            if (prop == null) throw new IllegalArgumentException("Unknown property '" + arg + "'.  Use 'help " + getName() + "' for a list.");
            out.println("  " + prop.getName() + " - " + prop.getDesc() + "\n");
            prop.printValue(out, false);
            return;
        }

        out.println("Property            Value");
        out.println("=================== ====================================================");

        List props = this.properties.getAll();
        for (Iterator i = props.iterator(); i.hasNext();) {
            NounProperty prop = (NounProperty)i.next();
            out.print(TextUtils.pad(prop.getName(), 19));
            out.print(' ');
            prop.printValue(out, true);
            out.println();
        }
    }

    private static final String OVERVIEW_HELP_TEXT =
            "The Gateway Account is at the heart of the SecureSpan Bridge.  Each \n" +
                    "Gateway Account holds a set of Bridge credentials for a target Gateway.";

    private static final String EXAMPLE_COMMANDS = "Example Commands: show gateways\n" +
            "                  show gateway37\n" +
            "                  create gateway ssg.example.com testuser secret\n" +
            "                  set gateway1 password\n" +
            "                  delete g3";
}
