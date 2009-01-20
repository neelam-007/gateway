package com.l7tech.external.assertions.samlpassertion.server.v2;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlpResponseEvaluator;
import com.l7tech.external.assertions.samlpassertion.server.ResponseAttributeData;
import com.l7tech.external.assertions.samlpassertion.server.SamlpAssertionException;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import saml.v2.assertion.*;
import saml.v2.protocol.ResponseType;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

/**
 * User: vchan
 */
public class ResponseEvaluator extends AbstractSamlpResponseEvaluator<ResponseType> {

    public ResponseEvaluator(SamlProtocolAssertion assertion) {
        super(assertion);
    }

    public ResponseBean parseMessage(Node response) throws SamlpAssertionException {

        JAXBElement<ResponseType> jxbResponse = unmarshal(response);
        samlpMessage = jxbResponse.getValue();

        /*
         *
         *
         *
         *
         */
        if (samlpMessage != null && samlpMessage.getStatus() != null && samlpMessage.getStatus().getStatusCode() != null ) {
            ResponseBean result = new ResponseBean();
            result.getStatusCodes().add(samlpMessage.getStatus().getStatusCode().getValue());

            switch(this.expectedType) {

                case AUTHENTICATION:
                    // do nothing else, really...
                    break;
                case AUTHORIZATION:
                    parseAuthorizationDecision(result);
                    break;
                case ATTRIBUTE:
                    parseAttributes(result);
                    break;
            }

            return result;
        }
        throw new SamlpAssertionException("Unable to parse SAMLP Response: bad message");
    }

    protected Class<ResponseType> getResponseClass() {
        return ResponseType.class;
    }

    private void parseAuthorizationDecision(ResponseBean result) throws SamlpAssertionException {

        AuthzDecisionStatementType authzStmt = (AuthzDecisionStatementType)
                getSingleStatement(AuthzDecisionStatementType.class);
        if (authzStmt.getDecision() != null)
            result.setAuthzDecision(authzStmt.getDecision().value());
        else
            // set to empty - means either decision was not set or did not match valid values
            result.setAuthzDecision("");
    }

    private void parseAttributes(ResponseBean result) throws SamlpAssertionException {

        List<StatementAbstractType> attribStatements = getAllStatements(AttributeStatementType.class);

        AttributeStatementType ast;
        ResponseAttributeData data;
        for (StatementAbstractType stmt : attribStatements) {
            ast = (AttributeStatementType) stmt;
            for (Object attr : ast.getAttributeOrEncryptedAttribute()) {

                if (attr instanceof AttributeType) {
                    AttributeType at = (AttributeType) attr;
                    data = new ResponseAttributeData(at.getName(), at.getNameFormat(), at.getFriendlyName());

                    // using similar behaviour as Saml v1.1 attribute parsing because AttributeValue is "anyType"
                    Object valToSet;
                    for (Object val : at.getAttributeValue()) {
                        if (val instanceof Element) {
                            valToSet = XmlUtil.getTextValue((Element) val);
                            if (valToSet == null || valToSet.toString().isEmpty())
                                valToSet = val;
                        } else {
                            valToSet = val;
                        }
                        data.getAttributeValues().add(valToSet);
                    }

                    // add to results
                    result.getAttributes().put(data.getName(), data);
                }

                // TODO: what todo w/ encrypted attrib ??
            }
        }
    }

    private StatementAbstractType getSingleStatement(Class<? extends StatementAbstractType> which)
        throws SamlpAssertionException
    {
        return getAllStatements(which, true).get(0);
    }


    private List<StatementAbstractType> getAllStatements(Class<? extends StatementAbstractType> which)
        throws SamlpAssertionException
    {
        return getAllStatements(which, false);
    }


    private List<StatementAbstractType> getAllStatements(Class<? extends StatementAbstractType> which, boolean stopOnFirstHit)
        throws SamlpAssertionException
    {
        List<StatementAbstractType> result = new ArrayList<StatementAbstractType>();

        for (Object as : samlpMessage.getAssertionOrEncryptedAssertion()) {

            if (as instanceof AssertionType) {
                AssertionType ast = (AssertionType) as;
                for (StatementAbstractType stmt : ast.getStatementOrAuthnStatementOrAuthzDecisionStatement()) {
                    if (stmt.getClass().isAssignableFrom(which)) {
                        result.add(stmt);
                        if (stopOnFirstHit) break;
                    }
                }
                if (stopOnFirstHit && result.size() > 0) break;
            }
            
            // ignore EncryptedAssertions for now
        }

        if (result.size() > 0) {
            return result;
        }
        throw new SamlpAssertionException("Could not find the expected statement in the response: " + expectedType.toString());
    }


}