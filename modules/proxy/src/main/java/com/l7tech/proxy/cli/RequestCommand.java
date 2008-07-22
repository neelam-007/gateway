/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.datamodel.exceptions.CertificateAlreadyIssuedException;
import com.l7tech.util.ExceptionUtils;

import java.io.PrintStream;
import java.net.PasswordAuthentication;
import java.security.cert.X509Certificate;

/**
 * Special command to send a certificate signing request to the Gateway.
 */
class RequestCommand extends Command {
    private SsgNoun ssgNoun;

    public RequestCommand(SsgNoun ssgNoun) {
        super("request", "Request client certificate");
        this.ssgNoun = ssgNoun;
        setHelpText("Use this command to generate a new Certificate Signing Request and send it\n" +
                    "to the Gateway.  This requires that a username and password be set.  If\n" +
                    "the server certificate has not yet been set, server certificate discovery\n" +
                    "will be attempted first.\n\n" +
                    "    Usage: <gateway> request clientCert\n\n" +
                    "  Example: " + ssgNoun.getName() + " request clientCert");
    }

    public void execute(CommandSession session, PrintStream out, String[] args) throws CommandException {
        if (args == null || args.length < 1 || args[0].length() < 1)
            throw new CommandException("Usage: " + ssgNoun.getName() + " request clientCert\n\n" +
                                       "Requires that a username and password be configured.");

        PasswordAuthentication creds = ssgNoun.ssg.getRuntime().getCredentials();
        if (creds == null)
            throw new CommandException("To apply for a client certificate, first set a username and password:\n" +
                                       "    " + ssgNoun.getName() + " set username alice\n" +
                                       "    " + ssgNoun.getName() + " set password s3cr3t");

        final X509Certificate serverCertificate = ssgNoun.getServerCert();
        if (serverCertificate == null) {
            session.getOut().println("Attempting automatic server certificate discovery...");
            new DiscoverCommand(ssgNoun).execute(session, out, new String[] { "serverCert" });
        }

        try {

            ssgNoun.ssg.getRuntime().getSsgKeyStoreManager().obtainClientCertificate(creds);
            session.onChangesMade();
            out.println("Now using client certificate with DN: " + ssgNoun.ssg.getClientCertificate().getSubjectDN().getName());
        } catch (BadCredentialsException e) {
            throw new CommandException("Gateway indicates invalid current credentials (bad password? missing cert?)", e);
        } catch (CertificateAlreadyIssuedException e) {
            throw new CommandException("The Gateway has already issued a certificate to this account.  Please use\n" +
                                       "a different account, or have the Gateway administrator revoke the old cert");
        } catch (Exception e) {
            throw new CommandException("Unable to obtain client certificate: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
