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
import java.security.cert.CertificateException;
import java.util.Calendar;
import java.util.logging.Logger;

/**
 * Class <code>SenderVouchesHelper</code> is the package private class
 * that provisions the sender voucher saml scenario.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class SenderVouchesHelper extends SamlAssertionHelper {
    static final Logger log = Logger.getLogger(SenderVouchesHelper.class.getName());

    /**
     * Instantiate the sender vouches helper
     * 
     * @param soapDom               the soap message as a dom.w3c.org document
     * @param options               the options for this operation
     * @param creds
     * @param signer                the signer
     */
    SenderVouchesHelper(Document soapDom, SamlAssertionGenerator.Options options, LoginCredentials creds, SignerInfo signer) {
        super(soapDom, options, creds, signer);
    }

    /**
     * create saml sender vouches assertion
     * 
     * @return the saml assertion as a dom.w3c.org document
     * @param assertionId what to use as value of AssertionID in created element.  If null, one will be made up.
     * @throws IOException
     * @throws SAXException 
     */
    Document createAssertion(String assertionId) throws IOException, SAXException, CertificateException {
        Calendar now = Calendar.getInstance(utcTimeZone);
        AssertionType assertion = getGenericAssertion( now, assertionId );
        AuthenticationStatementType at = attachAuthenticationStatement(assertion, now);

        SubjectConfirmationType st = at.getSubject().addNewSubjectConfirmation();
        st.addConfirmationMethod(Constants.CONFIRMATION_SENDER_VOUCHES);
        return assertionToDocument( assertion );
    }
}
