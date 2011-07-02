package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.external.assertions.saml2attributequery.EncryptSamlAssertionAssertion;
import com.l7tech.external.assertions.saml2attributequery.EncryptNameIDAssertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.saml.SamlConstants;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.util.Collection;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 23-Jan-2009
 * Time: 6:53:56 PM
 */
public class ServerEncryptNameIDAssertion extends ServerEncryptElementAssertionBase {
    private static final String PREFERRED_PREFIX = SamlConstants.NS_SAML2_PREFIX;
    private static final String ENCRYPTED_ID_TAG_NAME = "EncryptedID";

    protected static final String SYMMETRIC_KEY_ALGORITHM = "AES";
    protected static final String SYMMETRIC_KEY_ENCRYPTION_ALGORITHM = "AES/CBC/PKCS5Padding";

    //- PUBLIC

    @SuppressWarnings({"UnusedDeclaration"})
    public ServerEncryptNameIDAssertion( final EncryptNameIDAssertion assertion,
                                    final ApplicationContext context )
        throws PolicyAssertionException
    {
        super(assertion, context);
    }

    @Override
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        X509Certificate cert;
        String subjectDN = null;
        try {
            Object obj = context.getVariable(EncryptSamlAssertionAssertion.VARIABLE_NAME);
            if(!(obj instanceof String)) {
                logAndAudit( AssertionMessages.SAML2_AQ_RESPONSE_ENCRYPT_SAML_ASSERTION_VAR_UNUSABLE );
                return AssertionStatus.FAILED;
            }

            subjectDN = (String)obj;
            Collection<TrustedCert> certs = getTrustedCertManager().findBySubjectDn(subjectDN);
            if(certs == null || certs.size() != 1) {
                logAndAudit( AssertionMessages.SAML2_AQ_RESPONSE_ENCRYPT_SAML_ASSERTION_CERT_NOT_FOUND, subjectDN );
                return AssertionStatus.FAILED;
            }
            cert = certs.iterator().next().getCertificate();
        } catch(NoSuchVariableException nsve) {
            logAndAudit( AssertionMessages.SAML2_AQ_RESPONSE_ENCRYPT_SAML_ASSERTION_VAR_UNUSABLE );
            return AssertionStatus.FAILED;
        } catch(FindException fe) {
            logAndAudit( AssertionMessages.SAML2_AQ_RESPONSE_ENCRYPT_SAML_ASSERTION_CERT_NOT_FOUND, subjectDN );
            return AssertionStatus.FAILED;
        }

        Document doc;
        try {
            doc = context.getRequest().getXmlKnob().getDocumentWritable();
        } catch(SAXException se) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, se.toString() );
            return AssertionStatus.FAILED;
        }

        encryptElement(doc, cert, "NameID", SamlConstants.NS_SAML2, PREFERRED_PREFIX, ENCRYPTED_ID_TAG_NAME);

        return AssertionStatus.NONE;
    }
}
