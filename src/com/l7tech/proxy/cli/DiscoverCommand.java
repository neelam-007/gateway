/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.ArrayUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.CertUtils;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;

import java.io.PrintStream;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Iterator;
import java.net.PasswordAuthentication;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.KeyStoreException;
import java.security.GeneralSecurityException;

/**
 * Special command for discovering server certificate.
 */
class DiscoverCommand extends Command {
    private SsgNoun ssgNoun;

    public DiscoverCommand(SsgNoun ssgNoun) {
        super("discover", "Discover Gateway SSL certificate");
        this.ssgNoun = ssgNoun;
    }

    public void execute(CommandSession session, PrintStream realOut, String[] args) throws CommandException {
        NounProperty prop = null;
        if (args != null && args.length > 0 && args[0].length() >= 1) {
            final String propName = args[0];
            args = ArrayUtils.shift(args);
            prop = (NounProperty)ssgNoun.properties.getByPrefix(propName);
        }

        if (prop == null || !prop.getName().startsWith("serverCert"))
            throw new CommandException("Usage: " + ssgNoun.getName() + " discover serverCert [<thumbprint>]\n" +
              "Include the thumbprint to force a cert to be trusted even if automatic verification fails.");

        final CommandSessionCredentialManager credentialManager = session.getCredentialManager();
        final List failedCerts;
        Exception caught = null;
        try {
            if (args.length > 0 && args[0].trim().length() > 0)
                credentialManager.addTrustedServerCertThumbprint(args[0].trim());
            credentialManager.clearLastFailedServerCerts();
            PasswordAuthentication creds = ssgNoun.ssg.getRuntime().getCredentialManager().getCredentials(ssgNoun.ssg);
            ssgNoun.ssg.getRuntime().getSsgKeyStoreManager().installSsgServerCertificate(ssgNoun.ssg, creds);
            realOut.println(ssgNoun.getName() + " serverCertificate changed to cert with DN: " +
                    ssgNoun.ssg.getServerCertificateAlways().getSubjectDN().getName());
            session.onChangesMade();
            return;

        } catch (OperationCanceledException e) {
            caught = e;
        } catch (IOException e) {
            caught = e;
        } catch (CertificateException e) {
            caught = e;
        } catch (KeyStoreCorruptException e) {
            caught = e;
        } catch (BadCredentialsException e) {
            caught = e;
        } catch (KeyStoreException e) {
            caught = e;
        } catch (Exception e) {
            // Unexpected -- trigger full stack trace
            throw new RuntimeException("Server certificate discovery failed: " + ExceptionUtils.getMessage(e), e);
        }

        failedCerts = credentialManager.getLastFailedServerCerts();
        if (failedCerts == null || failedCerts.isEmpty())
            throw new CommandException("Server certificate discovery failed: " + ExceptionUtils.getMessage(caught), caught);

        // We got a cert, but we just don't trust it enough
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(bout);
        for (Iterator i = failedCerts.iterator(); i.hasNext();) {
            X509Certificate cert = (X509Certificate)i.next();
            out.println("Unable to automatically trust this server certificate:\n");
            try {
                out.println(CertUtils.toString(cert));
                final String thumb = CertUtils.getCertificateFingerprint(cert,
                                                                         CertUtils.ALG_SHA1,
                                                                         CertUtils.FINGERPRINT_RAW_HEX).toLowerCase();
                out.println("To manually trust the above certificate, use: \n    " +
                        ssgNoun.getName() + " discover serverCert " +
                        thumb.substring(0, 16));
            } catch (GeneralSecurityException e1) {
                // can't happen -- either checked earlier, or can only happen if vm misconfigured
                throw new CommandException("Unable to show certificate: " + ExceptionUtils.getMessage(e1), e1);
            }
        }

        PasswordAuthentication creds = credentialManager.getCredentials(ssgNoun.ssg);
        if (creds == null || creds.getUserName() == null ||
                creds.getUserName().length() < 1 || creds.getPassword() == null)
        {
            out.println("\nTo attempt automatic validation, first set both a username and password:");
            out.println("    " + ssgNoun.getName() + " set username alice");
            out.print("    " + ssgNoun.getName() + " set password s3cr3t");
        }

        out.close();
        throw new CommandException(bout.toString(), caught);
    }
}
