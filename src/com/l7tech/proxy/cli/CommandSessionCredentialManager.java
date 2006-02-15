/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.proxy.datamodel.CredentialManagerImpl;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.ssl.SslPeer;

import java.io.*;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

/**
 * Credential manager for use in the interactive Bridge command line configurator.
 */
class CommandSessionCredentialManager extends CredentialManagerImpl {
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

    public PasswordAuthentication DISABLED_getCredentials(Ssg ssg) throws OperationCanceledException {
        PasswordAuthentication pw = ssg.getRuntime().getCredentials();
        if (pw == null || pw.getUserName() == null) return getNewCredentials(ssg, false);
        return pw;
    }

    /**
     * Get credentials using text-mode password prompt hack.  Experimental.
     *
     * @param ssg the Ssg whose credentials you want to update
     * @param displayBadPasswordMessage if true, user will be told that current credentials are no good.
     * @return the new credentials for this Ssg.  Never null.
     * @throws OperationCanceledException if we prompted the user, but he clicked cancel
     */
    public PasswordAuthentication DISABLED_getNewCredentials(Ssg ssg, boolean displayBadPasswordMessage) throws OperationCanceledException {
        PrintStream err = session.getErr();
        if (displayBadPasswordMessage)
            err.println("Gateway rejected existing username or password for gateway" + ssg.getId());
        err.println("Credentials required for gateway" + ssg.getId() + " (" + ssg + ").");
        err.print("Please enter a username (or press enter to cancel): ");
        BufferedReader bin = new BufferedReader(new InputStreamReader(in));
        try {
            String username = bin.readLine();
            if (username == null) throw new OperationCanceledException("Input has ended");
            if (username.length() < 1) throw new OperationCanceledException("User canceled login prompt.");

            err.print("Password: ");
            String passstr = bin.readLine();
            if (passstr == null) throw new OperationCanceledException("Input has ended");
            char[] password = passstr.toCharArray();
            //char[] password = TextUtils.getPassword(in, out, "Please enter a password: ");

            ssg.setUsername(username);
            ssg.getRuntime().setCachedPassword(password);
            ssg.getRuntime().onCredentialsUpdated();
            ssg.getRuntime().promptForUsernameAndPassword(true);

            return ssg.getRuntime().getCredentials();
        } catch (IOException e) {
            throw new OperationCanceledException("Unable to read from input", e);
        }
    }

    public void notifySslCertificateUntrusted(SslPeer sslPeer, String serverDesc, X509Certificate untrustedCertificate) throws OperationCanceledException {
        super.notifySslCertificateUntrusted(sslPeer, serverDesc, untrustedCertificate);
    }

    public void saveSsgChanges(Ssg ssg) {
        session.onChangesMade(); // mark the session as dirty so any changed state will get saved
    }
}
