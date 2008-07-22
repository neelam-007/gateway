/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;

import java.io.PrintStream;
import java.io.File;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;

/**
 * Special command for importing a server or client cert into a Gateway Account.
 */
class ImportCommand extends Command {
    private SsgNoun ssgNoun;

    protected ImportCommand(SsgNoun ssgNoun) {
        super("import", "Import a client or server certificate");
        this.ssgNoun = ssgNoun;
        setHelpText("Use this command to manually import an X.509 certificate to use as the client\n" +
                    "or server certificate. Server certs can be in either PEM or DER format.\n" +
                    "Client certs must be in PKCS#12 format, and you must supply the pass phrase\n" +
                    "(and alias, if the file contains multiple entries).\n" +
                    "\n" +
                    "   Usage: " + ssgNoun.getName() + " import serverCert <PEM or DER file>\n" +
                    "          " + ssgNoun.getName() + " import clientCert <PKCS#12 file> <passphrase> [<alias>]\n" +
                    "\n" +
                    "Examples: gateway3 import serverCert /tmp/cert.pem\n" +
                    "          g7 import clientCert alice.p12 fooSecret alice\n");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 2 || args[0] == null || args[1] == null)
            throw new CommandException(usage());

        final String what = args[0];
        args = ArrayUtils.shift(args);
        final String path = args[0];
        args = ArrayUtils.shift(args);
        final File file = new File(path);
        if (!file.exists())
            throw new CommandException("File not found: " + path);
        if (file.isDirectory())
            throw new CommandException("File is actually a directory: " + path);
        if (!file.isFile())
            throw new CommandException("File is not a normal file: " + path);
        Word prop = ssgNoun.properties.getByPrefix(what);
        try {
        if (prop instanceof ServerCertProperty) {
            // Import server cert
            importServerCert(file);
        } else if (prop instanceof ClientCertProperty) {
            // Import client cert
            importClientCert(file, args);
        } else
            throw new CommandException(usage());
        } finally {
            session.onChangesMade();
        }
    }

    private void importServerCert(File file) throws CommandException {
        try {
            ssgNoun.ssg.getRuntime().getSsgKeyStoreManager().importServerCertificate(file);
        } catch (IOException e) {
            throw new CommandException("Unable to read file: " + ExceptionUtils.getMessage(e), e);
        } catch (CertificateException e) {
            throw new CommandException("Unable to parse certificate: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreCorruptException e) {
            throw new CommandException("Certs file is corrupt for " +
                                       getName() + ": " + ExceptionUtils.getMessage(e) + "\n\n" +
                                       "Use '" + getName() + " delete' to delete its key stores.", e);
        } catch (KeyStoreException e) {
            throw new CommandException("Unable to save certificate: " + ExceptionUtils.getMessage(e), e); // shouldn't happen
        }
    }

    private void importClientCert(File file, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new CommandException(usage());
        String phrase = args[0];
        final String alias = args.length > 1 ? args[1] : null;

        PasswordAuthentication creds = ssgNoun.ssg.getRuntime().getCredentials();
        char[] ssgpass = creds == null ? null : creds.getPassword();
        if (ssgpass == null || ssgpass.length < 1) {
            throw new CommandException("To import a client certificate, first set a username and password\n" +
                                       "for this Gateway:\n" +
                                       "    " + ssgNoun.getName() + " set username alice\n" +
                                       "    " + ssgNoun.getName() + " set password s3cr3t\n\n" +
                                       "The newly-created keys file will be encrypted with this password.");
        }

        SsgKeyStoreManager.AliasPicker picker = new SsgKeyStoreManager.AliasPicker() {
            public String selectAlias(String[] options) throws SsgKeyStoreManager.AliasNotFoundException {
                for (int i = 0; i < options.length; i++) {
                    String option = options[i];
                    if (option != null && option.equalsIgnoreCase(alias))
                        return option;
                }

                // Report failure appropriately
                StringBuffer sb = new StringBuffer();
                if (alias == null || alias.length() < 1)
                    sb.append("Please specify the alias to import.\n\n");
                else
                    sb.append("The specified alias was not found.\n\n");
                sb.append("The file contains ").append(options.length).append(" aliases:\n");
                for (int i = 0; i < options.length; i++) {
                    String option = options[i];
                    sb.append("  ").append(option).append("\n");
                }
                throw new SsgKeyStoreManager.AliasNotFoundException(sb.toString());
            }
        };

        try {
            ssgNoun.ssg.getRuntime().getSsgKeyStoreManager().importClientCertificate(file,
                                                                                     phrase.toCharArray(),
                                                                                     picker,
                                                                                     ssgpass);
        } catch (IOException e) {
            throw new CommandException("Unable to import client certificate: " + ExceptionUtils.getMessage(e), e);
        } catch (GeneralSecurityException e) {
            throw new CommandException("Unable to import client certificate: " + ExceptionUtils.getMessage(e), e);
        } catch (KeyStoreCorruptException e) {
            throw new CommandException("Unable to import client certificate: " + ExceptionUtils.getMessage(e), e);
        } catch (SsgKeyStoreManager.AliasNotFoundException e) {
            throw new CommandException("Unable to import client certificate: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private String usage() {
        return "Usage: " + ssgNoun.getName() + " import serverCert <path of PEM or DER file>\n" +
                                   "       " + ssgNoun.getName() + " import clientCert <path of PKCS#12 file> <pass phrase> [<alias>]";
    }
}
