package com.l7tech.external.assertions.samlpassertion.server.v1;

import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlp1MessageGenerator;
import com.l7tech.external.assertions.samlpassertion.server.JaxbUtil;
import com.l7tech.external.assertions.samlpassertion.server.SamlpAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.util.ExceptionUtils;
import saml.v1.assertion.*;
import saml.v1.protocol.AuthorizationDecisionQueryType;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author: vchan
 */
public final class AuthorizationDecisionQueryGenerator extends AbstractSamlp1MessageGenerator<AuthorizationDecisionQueryType> {
    private static final Logger logger = Logger.getLogger(AuthorizationDecisionQueryGenerator.class.getName());

    public AuthorizationDecisionQueryGenerator(final Map<String, Object> variablesMap, final Auditor auditor)
        throws SamlpAssertionException
    {
        super(variablesMap, auditor);
    }

    protected AuthorizationDecisionQueryType createMessageInstance() {
        return samlpFactory.createAuthorizationDecisionQueryType();
    }

    protected void buildSpecificMessageParts() {

        // build subject - common for all SAMLP requests
        samlpMessage.setSubject( buildSubject() );

        // specific payload
        if (assertion.getAuthorizationStatement() != null) {
            samlpMessage.setResource( assertion.getAuthorizationStatement().getResource() );
            samlpMessage.getAction().addAll( buildActionList() );
        }
        samlpMessage.setEvidence( buildEvidence() );
    }

    private List<ActionType> buildActionList() {
        List<ActionType> actions = new ArrayList<ActionType>();

        String ns = null;
        for (String action : assertion.getAuthorizationStatement().getActions()) {
            ActionType at = samlFactory.createActionType();
            at.setValue(action.substring(0, action.indexOf("||")));

            ns = action.substring(action.indexOf("||")+2);
            if (ns != null && ns.trim().length() > 0)
                at.setNamespace(ns.trim());
            actions.add(at);
        }

        return actions;
    }


    private EvidenceType buildEvidence() {

        EvidenceType evidence = samlFactory.createEvidenceType();
        if (assertion.getEvidence() == SAMLP_AUTHZ_EVIDENCE_FROM_VAR) {
            if (evidenceBlockResolver != null) {
                try {
                    Unmarshaller um = JaxbUtil.getUnmarshallerV1(evidenceBlockResolver.getKey());
                    JAXBElement<EvidenceType> evid =
                            um.unmarshal(evidenceBlockResolver.getValue(), EvidenceType.class);

                    if (!evid.isNil() && evid.getValue() != null)
                        evidence = evid.getValue();

                } catch (JAXBException jaxEx) {
                    logger.log(Level.WARNING, "Bad Authorization evidence from context variable, could not be unmarshalled: {0}", ExceptionUtils.getMessage(jaxEx));
                } finally {
                    JaxbUtil.releaseJaxbResources(evidenceBlockResolver.getKey());
                }
            }
        } else {
            // create standard evidence
            evidence.getAssertionIDReferenceOrAssertion().add(samlFactory.createAssertion(buildDefaultEvidenceAssertion()));
        }
        return evidence;
    }


    private AssertionType buildDefaultEvidenceAssertion() {

        AssertionType assertion = samlFactory.createAssertionType();
        assertion.setAssertionID(generateAssertionId());
        assertion.setIssueInstant(getIssueInstant());
        assertion.setMajorVersion(MAJOR_VERSION);
        assertion.setMinorVersion(MINOR_VERSION);
        // Do need to set conditions?
        // assertion.setConditions();
        assertion.getStatementOrSubjectStatementOrAuthenticationStatement().add(buildDefaultAuthnStatement());
        if (issuerNameResolver != null)
            assertion.setIssuer(issuerNameResolver.getNameValue());

        return assertion;
    }

    private AuthenticationStatementType buildDefaultAuthnStatement() {

        AuthenticationStatementType stmt = samlFactory.createAuthenticationStatementType();
        stmt.setAuthenticationInstant(getIssueInstant()); // TODO: Fix for authn instant
        stmt.setSubject(samlpMessage.getSubject());

        if (authnMethodResolver != null)
            stmt.setAuthenticationMethod(authnMethodResolver.getValue());

        if (addressResolver != null) {
            SubjectLocalityType subjLocality = samlFactory.createSubjectLocalityType();
            subjLocality.setIPAddress(addressResolver.getAddress().getHostAddress());
            subjLocality.setDNSAddress(addressResolver.getAddress().getCanonicalHostName());
            stmt.setSubjectLocality(subjLocality);
        }

        return stmt;
    }
}