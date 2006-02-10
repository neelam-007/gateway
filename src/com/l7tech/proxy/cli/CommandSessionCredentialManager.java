/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.proxy.datamodel.CredentialManager;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.SslPeer;
import com.l7tech.common.util.TextUtils;

import java.io.*;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

/**
 * Credential manager for use in the interactive Bridge command line configurator.
 */
class CommandSessionCredentialManager extends CredentialManager {
    private final CommandSession session;
    private final InputStream in;
    private final OutputStream out;

    /**
     * Creates a credential manager that will use command line prompts for usernames and passwords.  When prompting
     * for a password, the specified output stream will be spammed with backspaces followed by asterisks to
     * attempt to mask the password.  (Sadly, that appears to be the best that can be done with the current JRE.)
     *
     * @param session  the command session we are attached to.  Must not be null.
     * @param in     the inputstream to read passwords from.  Must not be null.
     * @param out    the outputstream where the keystrokes from passwords being typed might be echoed.  Must not be null.
     */
    public CommandSessionCredentialManager(CommandSession session, InputStream in, OutputStream out) {
        super();
        this.session = session;
        this.in = in;
        this.out = out;
    }

    public PasswordAuthentication getCredentials(Ssg ssg) throws OperationCanceledException {
        return ssg.getRuntime().getCredentials();
    }

    public PasswordAuthentication getCredentialsWithReasonHint(Ssg ssg, ReasonHint hint, boolean disregardExisting, boolean reportBadPassword) throws OperationCanceledException {
        if (disregardExisting || reportBadPassword) {

            return getNewCredentials(ssg, reportBadPassword);
        } else {
            return getCredentials(ssg);
        }
    }

    public PasswordAuthentication getNewCredentials(Ssg ssg, boolean displayBadPasswordMessage) throws OperationCanceledException {
        PrintStream err = session.getErr();
        if (displayBadPasswordMessage) err.println("Bad username or password.");
        err.print("Credential required for Gateway " + ssg + ".");
        err.print("Please enter a username (or press enter to cancel): ");
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        try {
            String username = bin.readLine();
            if (username == null) throw new OperationCanceledException("Input has ended");
            if (username.length() < 1) throw new OperationCanceledException("User canceled login prompt.");

            char[] password = TextUtils.getPassword(in, out, "Please enter a password: ");

            ssg.setUsername(username);
            ssg.getRuntime().setCachedPassword(password);
            ssg.getRuntime().onCredentialsUpdated();
            ssg.getRuntime().promptForUsernameAndPassword(true);

            return ssg.getRuntime().getCredentials();
        } catch (IOException e) {
            throw new OperationCanceledException("Unable to read from input", e);
        }
    }

    public void notifyLengthyOperationStarting(Ssg ssg, String message) {

    }

    public void notifyLengthyOperationFinished(Ssg ssg) {

    }

    public void notifyKeyStoreCorrupt(Ssg ssg) throws OperationCanceledException {

    }

    public void notifyCertificateAlreadyIssued(Ssg ssg) {

    }

    public void notifySslHostnameMismatch(String server, String whatWeWanted, String whatWeGotInstead) {

    }

    public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException {

    }

    public void saveSsgChanges(Ssg ssg) {
        // Take no action -- changes are saved at the end of the session, unless the admin declines to do so
    }
}
