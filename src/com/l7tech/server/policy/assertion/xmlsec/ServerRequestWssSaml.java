package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.security.token.SamlSecurityToken;
import com.l7tech.common.security.token.SecurityToken;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.SoapFaultDetailImpl;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.AssertionMessages;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Class <code>ServerRequestWssSaml</code> represents the server
 * side saml Assertion that validates the SAML requestWssSaml.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ServerRequestWssSaml implements ServerAssertion {
    private RequestWssSaml requestWssSaml;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private ApplicationContext applicationContext;
    private SamlAssertionValidate assertionValidate;

    /**
     * Create the server side saml security policy element
     *
     * @param sa the saml
     */
    public ServerRequestWssSaml(RequestWssSaml sa, ApplicationContext context) {
        if (sa == null) {
            throw new IllegalArgumentException();
        }
        this.applicationContext = context;
        if (applicationContext == null) {
            throw new IllegalArgumentException("The Application Context is required");
        }

        requestWssSaml = sa;
        assertionValidate = new SamlAssertionValidate(requestWssSaml, context);

    }

    /**
     * SSG Server-side processing of the given request.
     *
     * @param context
     * @return AssertionStatus.NONE if this Assertion did its business successfully; otherwise, some error code
     * @throws com.l7tech.policy.assertion.PolicyAssertionException
     *          something is wrong in the policy dont throw this if there is an issue with the request or the response
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context)
      throws IOException, PolicyAssertionException {

        Auditor auditor = new Auditor(context.getAuditContext(), logger);
        try {
            final XmlKnob xmlKnob = context.getRequest().getXmlKnob();
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_REQUEST_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }

            ProcessorResult wssResults = xmlKnob.getProcessorResult();
            if (wssResults == null) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED);
                context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }

            SecurityToken[] tokens = wssResults.getSecurityTokens();
            if (tokens == null) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_TOKENS_PROCESSED);
                context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
            SamlSecurityToken samlAssertion = null;
            for (int i = 0; i < tokens.length; i++) {
                SecurityToken tok = tokens[i];
                if (tok instanceof SamlSecurityToken) {
                    SamlSecurityToken samlToken = (SamlSecurityToken)tok;
                    if (samlToken.isPossessionProved() || samlToken.isBearerToken()) {
                        if (samlAssertion != null) {
                            auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_MULTIPLE_SAML_ASSERTIONS_UNSUPPORTED);
                            return AssertionStatus.BAD_REQUEST;
                        }
                        samlAssertion = samlToken;
                    }
                }
            }
            if (samlAssertion == null) {
                auditor.logAndAudit(AssertionMessages.SAML_AUTHN_STMT_NO_ACCEPTABLE_SAML_ASSERTION);
                context.setAuthenticationMissing();
                return AssertionStatus.AUTH_REQUIRED;
            }
            Collection validateResults = new ArrayList();
            assertionValidate.validate(xmlKnob.getDocumentReadOnly(), wssResults, validateResults);
            if (validateResults.size() > 0) {
                StringBuffer sb = new StringBuffer();
                boolean firstPass = true;
                for (Iterator iterator = validateResults.iterator(); iterator.hasNext();) {
                    if (!firstPass) sb.append("\n");
                    SamlAssertionValidate.Error error = (SamlAssertionValidate.Error)iterator.next();
                    sb.append(error.getReason());
                    firstPass = false;
                }
                SoapFaultDetail sfd = new SoapFaultDetailImpl(SoapFaultUtils.FC_CLIENT, sb.toString(), null);
                context.setFaultDetail(sfd);
            }
            context.setCredentials(new LoginCredentials(samlAssertion.getNameIdentifierValue(),
                                                        null,
                                                        CredentialFormat.SAML,
                                                        SamlAuthenticationStatement.class,
                                                        null,
                                                        samlAssertion));
            return AssertionStatus.NONE;
        } catch (SAXException e) {
            throw (IOException)new IOException().initCause(e);
        }
    }
}
