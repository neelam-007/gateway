package com.l7tech.external.assertions.samlpassertion.server.v1;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlpResponseEvaluator;
import com.l7tech.external.assertions.samlpassertion.server.ResponseAttributeData;
import com.l7tech.external.assertions.samlpassertion.server.SamlpAssertionException;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import saml.v1.assertion.*;
import saml.v1.protocol.ResponseType;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
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
            QName code = samlpMessage.getStatus().getStatusCode().getValue();
            StringBuffer sb = new StringBuffer(code.getNamespaceURI()).append(":").append(code.getLocalPart());
            result.getStatusCodes().add(sb.toString());

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

        AuthorizationDecisionStatementType authzStmt = (AuthorizationDecisionStatementType)
                getSingleStatement(AuthorizationDecisionStatementType.class);
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
            for (AttributeType attr : ast.getAttribute()) {
                data = new ResponseAttributeData(attr.getAttributeName(), attr.getAttributeNamespace());

                /*
                 * in SAML v1.1, the AttributeValue is an "Any" type, we will just attempt to parse
                 * the text value out
                 */
                Object valToSet;
                for (Object val : attr.getAttributeValue()) {
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

        for (AssertionType as : samlpMessage.getAssertion()) {
            for (StatementAbstractType stmt : as.getStatementOrSubjectStatementOrAuthenticationStatement()) {
                if (stmt.getClass().isAssignableFrom(which)) {
                    result.add(stmt);
                    if (stopOnFirstHit) break;
                }
            }
            if (stopOnFirstHit && result.size() > 0) break;
        }

        if (result.size() > 0) {
            return result;
        }
        throw new SamlpAssertionException("Could not find the expected statement in the response: " + expectedType.toString());
    }


}
