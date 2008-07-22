/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.io.CertUtils;
import com.l7tech.util.ExceptionUtils;

import java.io.IOException;
import java.io.PrintStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Property for an SSG server cert.
 */
class ServerCertProperty extends NounProperty {
    private SsgNoun ssgNoun;

    public ServerCertProperty(SsgNoun ssgNoun) {
        super(ssgNoun.ssg, "serverCert", "ServerCertificate", "Gateway X.509 SSL certificate");
        this.ssgNoun = ssgNoun;
    }

    public void printValue(PrintStream out, boolean singleLine)
    {
        final boolean m = !singleLine;
        final X509Certificate cert;
        try {
            cert = (X509Certificate)getValue();
        } catch (RuntimeException e) {
            // The only expected way to fail to get the server cert is if the certsNNN.p12 file is corrupt
            if (ExceptionUtils.causedBy(e, CommandSessionCredentialManager.BadKeystoreException.class)) {
                out.print("<certs file damaged>");
                if (m) out.println();
                return;
            }

            // Nope.. who knows what happened.  Throw up to trigger internal error report.
            throw e;
        }

        if (cert == null) {
            out.print("<not yet discovered>");
            if (m) out.println();
            return;
        }

        if (singleLine) {
            out.print(cert.getSubjectDN().getName());
            return;
        }

        try {
            out.println(CertUtils.toString(cert));
            out.println(new String( CertUtils.encodeAsPEM(cert)));
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        } catch (CertificateEncodingException e) {
            out.println("<certificate damaged>"); // can't happen
        }
    }

    public void set(String[] args) throws CommandException {
        throw new CommandException("Unable to set server cert.  Please use 'discover', 'import', or 'delete'.");
    }

    public void delete() throws CommandException {
        try {
            if (ssgNoun.ssg.getClientCertificate() != null)
                throw new CommandException("Unable to delete serverCert because a clientCert is present.\nPlease use '" + ssgNoun.getName() + " delete clientCert' first.");
            ssgNoun.ssg.getRuntime().getSsgKeyStoreManager().deleteStores();
        } catch (Exception e) {
            throw new CommandException("Unable to delete client certificate: " + ExceptionUtils.getMessage(e), e);
        }
    }
}
