/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;

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
            out.println(new String(CertUtils.encodeAsPEM(cert)));
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
