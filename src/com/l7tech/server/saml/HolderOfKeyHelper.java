/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.saml;

import com.l7tech.common.security.saml.Constants;
import com.l7tech.common.security.xml.SignerInfo;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.AssertionType;
import x0Assertion.oasisNamesTcSAML1.AuthenticationStatementType;
import x0Assertion.oasisNamesTcSAML1.SubjectConfirmationType;

import java.io.IOException;
import java.net.InetAddress;
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class HolderOfKeyHelper extends SamlAssertionHelper {
    public static String addressToString(InetAddress address) {
        StringBuffer sb = new StringBuffer();
        byte[] bytes = address.getAddress();
        for ( int i = 0; i < bytes.length; i++ ) {
            byte b = bytes[i];
            sb.append(b & 0xff);
            if ( i < bytes.length-1 ) sb.append(".");
        }
        return sb.toString();
    }

    static final Logger log = Logger.getLogger(HolderOfKeyHelper.class.getName());

    /**
     * Instantiate the sender voucher helper
     *
     * @param soapDom               the soap message as a dom.w3c.org document
     * @param signer                the signer
     */
    public HolderOfKeyHelper(Document soapDom, SamlAssertionGenerator.Options options, LoginCredentials creds, SignerInfo signer) {
        super(soapDom, options, creds, signer);
    }

    /**
     * create saml sender vouches assertion
     *
     * @return the saml assertion as a dom.w3c.org document
     * @throws IOException
     * @throws SAXException
     */
    public Document createAssertion() throws IOException, SAXException, CertificateException {
        Calendar now = Calendar.getInstance(utcTimeZone);

        AssertionType assertion = getGenericAssertion( now );
        AuthenticationStatementType at = attachAuthenticationStatement(assertion, now);

        SubjectConfirmationType sc = at.getSubject().getSubjectConfirmation();
        sc.addConfirmationMethod(Constants.CONFIRMATION_HOLDER_OF_KEY);

        return assertionToDocument( assertion );
    }
}
