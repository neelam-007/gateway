package com.l7tech.common.xml.saml;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.security.cert.X509Certificate;

/**
 * This is an assertion as in, saml assertion (not to be confused with a policy assertion).
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 25, 2004<br/>
 */
public class SamlSenderVouchesAssertion extends SamlAssertion {
    /**
     * constructor
     * @param xmlassertion the xml element containing the assertion
     */
    protected SamlSenderVouchesAssertion(Element xmlassertion) throws SAXException {
        super(xmlassertion);
    }
    public X509Certificate getSigningCertificate() {
        return issuerCertificate;
    }
}
