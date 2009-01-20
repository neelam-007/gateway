package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.external.assertions.samlpassertion.SamlpAuthorizationStatement;
import com.l7tech.external.assertions.samlpassertion.SamlProtocolAssertion;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: vchan
 */
public abstract class AbstractSamlpResponseEvaluator<RSP_TYPE> {


    public static enum SamlpResponseType {
        AUTHENTICATION,
        AUTHORIZATION,
        ATTRIBUTE
    }

    protected final Integer samlVersion;
    protected final SamlpResponseType expectedType;
    protected SamlAuthenticationStatement stmtAuthn;
    protected SamlpAuthorizationStatement stmtAuthz;
    protected SamlAttributeStatement stmtAttrib;
    protected RSP_TYPE samlpMessage;

    /**
     * Constructor.
     *
     * @param assertion The Samlp assertion
     */
    protected AbstractSamlpResponseEvaluator(final SamlProtocolAssertion assertion) {

        this.samlVersion = assertion.getSamlVersion();

        if (assertion.getAuthenticationStatement() != null) {
            this.expectedType = SamlpResponseType.AUTHENTICATION;
            this.stmtAuthn = assertion.getAuthenticationStatement();

        } else if (assertion.getAuthorizationStatement() != null) {
            this.expectedType = SamlpResponseType.AUTHORIZATION;
            this.stmtAuthz = assertion.getAuthorizationStatement();

        } else {
            this.expectedType = SamlpResponseType.ATTRIBUTE;
            this.stmtAttrib = assertion.getAttributeStatement();
        }
    }

    public abstract ResponseBean parseMessage(Node response) throws SamlpAssertionException;

    protected abstract Class<RSP_TYPE> getResponseClass();

    protected JAXBElement<RSP_TYPE> unmarshal(Node response) throws SamlpAssertionException {

        JAXBElement<RSP_TYPE> result = null;
        try {
            JAXBContext jxbContext;
            Unmarshaller um;

            // --- move to common utility ---
            if (samlVersion == 2) {
                jxbContext = JAXBContext.newInstance("saml.v2.protocol:saml.v2.assertion:saml.support.ds", AbstractSamlpResponseEvaluator.class.getClassLoader());
            } else {
                jxbContext = JAXBContext.newInstance("saml.v1.protocol:saml.v1.assertion:saml.support.ds", AbstractSamlpResponseEvaluator.class.getClassLoader());
            }
            // --- move to common utility ---

            um = jxbContext.createUnmarshaller();
            result = um.unmarshal(response, getResponseClass());

            if (result == null) {
                throw new SamlpAssertionException("Unable to unmarshal SAMLP response");
            }

        } catch (JAXBException jxbEx) {
            // TODO: log+audit?

            throw new SamlpAssertionException("Unable to unmarshal SAMLP response", jxbEx);
        }

        return result;
    }


    /**
     * Response bean that holds the results of parsing the SAMLP response message
     *
     *
     */
    public class ResponseBean {

        private List<String> statusCodes = new ArrayList<String>();
        private String authzDecision;
//        private List<ResponseAttributeData> attributes = new ArrayList();
        private Map<String, ResponseAttributeData> attributesMap = new HashMap<String, ResponseAttributeData>();

        public List<String> getStatusCodes() {
            return statusCodes;
        }

        public void setStatusCodes(List<String> statusCodes) {
            this.statusCodes = statusCodes;
        }

        public String getAuthzDecision() {
            return authzDecision;
        }

        public void setAuthzDecision(String authzDecision) {
            this.authzDecision = authzDecision;
        }

        public List<ResponseAttributeData> getAttributesList() {
            return new ArrayList<ResponseAttributeData>(attributesMap.values());
        }

        public Map<String, ResponseAttributeData> getAttributes() {
            return attributesMap;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("SAMLP response bean values:[");
            sb.append("statusCode(s)=");
            for (String s : statusCodes) {
                sb.append(s).append(",");
            }
            sb.append("\n");

            if (authzDecision != null) {
                sb.append("authzDecision=").append(authzDecision).append("\n");
            }

            if (!getAttributesList().isEmpty()) {
                sb.append("attributes found=").append(getAttributesList().size()).append(":\n");

                for (ResponseAttributeData data : getAttributesList()) {
                    sb.append(data.toString()).append("\n");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

}
