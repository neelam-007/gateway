package com.l7tech.external.assertions.samlpassertion.server.v2;

import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlp2MessageGenerator;
import com.l7tech.external.assertions.samlpassertion.server.JaxbUtil;
import com.l7tech.external.assertions.samlpassertion.server.SamlpAssertionException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.util.ExceptionUtils;
import saml.v2.assertion.*;
import saml.v2.protocol.AuthzDecisionQueryType;

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
public final class AuthzDecisionQueryGenerator extends AbstractSamlp2MessageGenerator<AuthzDecisionQueryType> {
    private static final Logger logger = Logger.getLogger(AuthzDecisionQueryGenerator.class.getName());

    public AuthzDecisionQueryGenerator(final Map<String, Object> variablesMap, final Auditor auditor)
        throws SamlpAssertionException
    {
        super(variablesMap, auditor);
    }

    public JAXBElement<AuthzDecisionQueryType> createJAXBElement(AuthzDecisionQueryType samlpMsg) {
        return samlpFactory.createAuthzDecisionQuery(samlpMsg);
    }

    protected AuthzDecisionQueryType createMessageInstance() {
        return samlpFactory.createAuthzDecisionQueryType();
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
                    Unmarshaller um = JaxbUtil.getUnmarshallerV2(evidenceBlockResolver.getKey());
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
            evidence.getAssertionIDRefOrAssertionURIRefOrAssertion().add(samlFactory.createAssertion(buildDefaultEvidenceAssertion()));
        }
        return evidence;
    }


    private AssertionType buildDefaultEvidenceAssertion() {

        AssertionType assertion = samlFactory.createAssertionType();
        assertion.setID(generateAssertionId());
        assertion.setIssueInstant(samlpMessage.getIssueInstant());
        assertion.setIssuer(samlpMessage.getIssuer());
        assertion.setVersion(getSamlVersion());
        assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().add(buildDefaultAuthnStatement());
        return assertion;
    }


    private AuthnStatementType buildDefaultAuthnStatement() {

        AuthnStatementType stmt = samlFactory.createAuthnStatementType();
        stmt.setAuthnInstant(getIssueInstant()); // TODO: Need to fix authn instant
        stmt.setAuthnContext(buildAuthnContext());

        // create SubjectLocality
        if (addressResolver != null) {
            SubjectLocalityType subjLocality = samlFactory.createSubjectLocalityType();
            subjLocality.setAddress(addressResolver.getAddress().getHostAddress());
            subjLocality.setDNSName(addressResolver.getAddress().getCanonicalHostName());
            stmt.setSubjectLocality(subjLocality);
        }
        return stmt;
    }


    private AuthnContextType buildAuthnContext() {

        AuthnContextType authCtx = samlFactory.createAuthnContextType();
        if (authnMethodResolver != null)
            authCtx.getContent().add(samlFactory.createAuthnContextClassRef(authnMethodResolver.getValue()));
        // depending on the authMethod
//        authCtx.getContent().add(samlFactory.createAuthnContextDecl("something"));

        return authCtx;
    }
    
}
