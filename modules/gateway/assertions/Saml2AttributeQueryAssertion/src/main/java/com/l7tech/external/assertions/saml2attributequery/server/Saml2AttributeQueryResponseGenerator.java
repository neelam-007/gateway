package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.util.HexUtils;
import com.l7tech.external.assertions.saml2attributequery.SamlToLdapMap;
import saml.v2.assertion.*;
import saml.v2.assertion.ObjectFactory;
import saml.v2.protocol.*;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

/**
 * Abstract base class for SAMLP v2 MessageGenerator implementations.  This class contains
 * common helper methods that are re-used all subclasses.
 *
 * @author: vchan
 */
public class Saml2AttributeQueryResponseGenerator
{
    public static class Conditions {
        public int notBeforeTime = -1;
        public int notOnOrAfterTime = -1;
        public String audience;
    }

    private static final String SAML_VERSION_2_0 = "2.0";
    private static final String SAMLP_V2_REQUEST_ID_PREFIX = "samlp2-";
    private static final String SAMLP_V2_REQUEST_ASSN_ID_PREFIX = "samlp2Assertion-";

    private static final Logger logger = Logger.getLogger(Saml2AttributeQueryResponseGenerator.class.getName());

    protected ObjectFactory samlFactory;
    protected saml.v2.protocol.ObjectFactory samlpFactory;
    protected saml.support.ds.ObjectFactory digsigFactory;

    private DatatypeFactory xmltypeFactory;
    private Random rand = new SecureRandom();

    public Saml2AttributeQueryResponseGenerator()
        throws SamlpAssertionException
    {
        this.samlFactory = new ObjectFactory();
        this.samlpFactory = new saml.v2.protocol.ObjectFactory();
        this.digsigFactory = new saml.support.ds.ObjectFactory();

        try {
            this.xmltypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dcex) {
            throw new SamlpAssertionException("Error while initializing Samlp message generator", dcex);
        }
    }

    protected ObjectFactory getSamlFactory() {
        return samlFactory;
    }
    
    protected saml.v2.protocol.ObjectFactory getSamlpFactory() {
        return samlpFactory;
    }

    public Object createSuccessMesage(final Saml2AttributeQuery query, String issuer, SamlToLdapMap map, HashMap<String, Object> values, Conditions conditions) {
        ResponseType samlpMessage = samlpFactory.createResponseType();

        GregorianCalendar now = new GregorianCalendar();

        buildCommonMessageParts(samlpMessage, query.getID(), now);

        NameIDType issuerType = null;
        issuerType = samlFactory.createNameIDType();
        issuerType.setValue(issuer);
        samlpMessage.setIssuer(issuerType);

        StatusType status = samlpFactory.createStatusType();
        StatusCodeType statusCode = samlpFactory.createStatusCodeType();
        statusCode.setValue("urn:oasis:names:tc:SAML:2.0:status:Success");
        status.setStatusCode(statusCode);
        samlpMessage.setStatus(status);

        buildSpecificMessageParts(samlpMessage, issuer, query, map, values, conditions, now);

        // finally convert the samlpMessage into XML tree
        return samlpFactory.createResponse(samlpMessage);
    }

    public Object createErrorMessage(String code,
                                     String subcode,
                                     String message,
                                     String queryId,
                                     List<Saml2AttributeQuery.QueriedAttribute> queriedAttributes)
    {
        ResponseType samlpMessage = samlpFactory.createResponseType();

        buildCommonMessageParts(samlpMessage, queryId, new GregorianCalendar());

        StatusType status = samlpFactory.createStatusType();
        StatusCodeType statusCode = samlpFactory.createStatusCodeType();
        statusCode.setValue(code);
        StatusCodeType statusSubCode = samlpFactory.createStatusCodeType();
        statusSubCode.setValue(subcode);
        statusCode.setStatusCode(statusSubCode);
        status.setStatusCode(statusCode);

        status.setStatusMessage(message);

        StatusDetailType statusDetail = samlpFactory.createStatusDetailType();
        for(Saml2AttributeQuery.QueriedAttribute queriedAttribute : queriedAttributes) {
            AttributeType attr = samlFactory.createAttributeType();
            attr.setNameFormat(queriedAttribute.nameFormat);
            attr.setName(queriedAttribute.samlName);
            statusDetail.getAny().add(samlFactory.createAttribute(attr));
        }
        status.setStatusDetail(statusDetail);

        samlpMessage.setStatus(status);
        
        return samlpFactory.createResponse(samlpMessage);
    }

    /**
     * Instantiate an instance of the samlpMessage type.
     *
     * @return
     */
    protected ResponseType createMessageInstance() {
        return samlpFactory.createResponseType();
    }

    protected void buildSpecificMessageParts(ResponseType samlpMessage,
                                             String issuer,
                                             Saml2AttributeQuery query,
                                             SamlToLdapMap map,
                                             HashMap<String, Object> values,
                                             Conditions conditions,
                                             GregorianCalendar now)
    {
        AssertionType assertion = samlFactory.createAssertionType();
        assertion.setVersion(SAML_VERSION_2_0);
        assertion.setIssueInstant(xmltypeFactory.newXMLGregorianCalendar(now));
        assertion.setID(generateAssertionId());

        NameIDType issuerType = null;
        issuerType = samlFactory.createNameIDType();
        issuerType.setValue(issuer);
        assertion.setIssuer(issuerType);
        
        samlpMessage.getAssertionOrEncryptedAssertion().add(assertion);

        assertion.setSubject(buildSubject(query));

        assertion.setConditions(createConditions(conditions, now));

		assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().add(createStatement(query, map, values));
    }

    protected ConditionsType createConditions(Conditions conditions, GregorianCalendar now) {
        boolean haveConditions = false;
        if(conditions != null) {
            if(conditions.audience != null && conditions.audience.length() > 0) {
                haveConditions = true;
            }
            if(conditions.notBeforeTime > -1 || conditions.notOnOrAfterTime > -1) {
                haveConditions = true;
            }
        }

        if(haveConditions) {
            ConditionsType con = samlFactory.createConditionsType();
            AudienceRestrictionType audienceRestriction = samlFactory.createAudienceRestrictionType();
            if(conditions.audience != null && conditions.audience.length() > 0) {
                audienceRestriction.getAudience().add(conditions.audience);
                con.getConditionOrAudienceRestrictionOrOneTimeUse().add(audienceRestriction);
            }
            if(conditions.notBeforeTime > -1) {
                GregorianCalendar gc = (GregorianCalendar)now.clone();
                gc.add(Calendar.SECOND, -1 * conditions.notBeforeTime);
                XMLGregorianCalendar cal = xmltypeFactory.newXMLGregorianCalendar(gc);
                con.setNotBefore(cal);
            }
            if(conditions.notOnOrAfterTime > -1) {
                GregorianCalendar gc = (GregorianCalendar)now.clone();
                gc.add(Calendar.SECOND, conditions.notOnOrAfterTime);
                XMLGregorianCalendar cal = xmltypeFactory.newXMLGregorianCalendar(gc);
                con.setNotOnOrAfter(cal);
            }
            return con;
        } else {
            return null;
        }
    }

    /**
     * Sets all common elements for SAMLP messages (i.e. RequestId, issueInstant, etc).
     */
    protected void buildCommonMessageParts(ResponseType samlpMessage, String queryId, GregorianCalendar now) {

        // mandatory attributes
        samlpMessage.setID( getResponseId() );
        samlpMessage.setVersion( SAML_VERSION_2_0 );
        samlpMessage.setIssueInstant( xmltypeFactory.newXMLGregorianCalendar(now) );

        // optional attributes
        samlpMessage.setInResponseTo(queryId);
    }

    protected SubjectType buildSubject(Saml2AttributeQuery query) {
        SubjectType subj = samlFactory.createSubjectType();

        NameIDType subjectName = samlFactory.createNameIDType();
        subjectName.setFormat(query.getSubjectNameFormat());
        if(query.getSubjectNameQualifier() != null) {
            subjectName.setNameQualifier(query.getSubjectNameQualifier());
        }
        subjectName.setValue(query.getSubject());
        subj.getContent().add(samlFactory.createNameID(subjectName));

        return subj;
    }

    protected String getResponseId() {
        StringBuffer sb = new StringBuffer(SAMLP_V2_REQUEST_ID_PREFIX);
        return sb.append(generateHexBytesForId()).toString();
    }

    protected String generateAssertionId() {
        StringBuffer sb = new StringBuffer(SAMLP_V2_REQUEST_ASSN_ID_PREFIX);
        return sb.append(generateHexBytesForId()).toString();
    }

    protected String generateHexBytesForId() {
        byte[] disambig = new byte[16];
        rand.nextBytes(disambig);
        return HexUtils.hexDump(disambig);
    }

    protected AttributeStatementType createStatement(Saml2AttributeQuery query, SamlToLdapMap map, HashMap<String, Object> values) {
        AttributeStatementType statement = samlFactory.createAttributeStatementType();

        for(Map.Entry<String, Object> entry : values.entrySet()) {
            statement.getAttributeOrEncryptedAttribute().add(createAttribute(map.getNameFormatFromLdapName(entry.getKey()),
                    map.getSamlName(entry.getKey()),
                    entry.getValue()));
        }
        /*for(Saml2AttributeQuery.AttributeFilter attribute : query.getAttributeFilters()) {
            if(!values.containsKey(attribute.getLdapAttributeName())) {
                continue;
            }

            statement.getAttributeOrEncryptedAttribute().add(createAttribute(attribute.getNameFormat(),
                    attribute.getSaml2AttributeName(),
                    values.get(attribute.getLdapAttributeName()))
            );
        }*/

        return statement;
    }

    protected AttributeType createAttribute(String nameFormat, String samlAttributeName, Object value)
    {
        AttributeType attributeType = samlFactory.createAttributeType();
		attributeType.setNameFormat(nameFormat);
		attributeType.setName(samlAttributeName);

        if(value instanceof String) {
            attributeType.getAttributeValue().add(value);
        } else if(value instanceof List) {
            List<String> values = (List<String>)value;
            for(String v : values) {
                attributeType.getAttributeValue().add(v);
            }
        }

        return attributeType;
    }
}