/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.proxy.cli;

import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.proxy.datamodel.exceptions.KeyStoreCorruptException;

import java.io.PrintStream;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;

/**
 * Property for an SSG client cert.
 */
class ClientCertProperty extends NounProperty {
    private SsgNoun ssgNoun;

    public ClientCertProperty(SsgNoun ssgNoun) {
        super(ssgNoun.ssg, "clientCert", "ClientCertificate", "Bridge X.509 client ceritifcate");
        this.ssgNoun = ssgNoun;
    }

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
                boolean haveKey = ssgNoun.ssg.getRuntime().getSsgKeyStoreManager().isClientCertUnlocked();
                out.print(' ');
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

    public void set(String[] args) throws CommandException {
        throw new CommandException("Unable to set client cert.  Please use 'request', 'import', or 'delete'.");
    }

    public void delete() throws CommandException {
        try {
            ssgNoun.ssg.getRuntime().getSsgKeyStoreManager().deleteClientCert();
        } catch (Exception e) {
            throw new CommandException("Unable to delete client certificate: " + ExceptionUtils.getMessage(e), e);
        }
    }

}
