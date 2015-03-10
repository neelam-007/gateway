package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.external.assertions.saml2attributequery.SamlToLdapMap;
import com.l7tech.security.saml.SamlConstants;
import org.junit.Test;
import saml.v2.assertion.*;
import saml.v2.protocol.ResponseType;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 2-Feb-2009
 * Time: 6:43:41 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQueryResponseGeneratorTest {
    private static final String BASIC_NAME_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:basic";
    private static final String UNSPECIFIED_FORMAT = "urn:oasis:names:tc:SAML:2.0:nameid-format:unspecified";

    private static final String ENCODED_ATTR_MAP = "rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAA" +
            "AADdwQAAAAKc3IARmNvbS5sN3RlY2guZXh0ZXJuYWwuYXNzZXJ0aW9ucy5zYW1sMmF0dHJpYnV0ZXF1ZXJ5LlNhbWxUb0xkYXBNYXAkR" +
            "W50cnlkj4/+K1qF0wIAA0wACGxkYXBOYW1ldAASTGphdmEvbGFuZy9TdHJpbmc7TAAKbmFtZUZvcm1hdHEAfgADTAAIc2FtbE5hbWVxA" +
            "H4AA3hwdAACY250ADF1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXR0cm5hbWUtZm9ybWF0OmJhc2ljdAAKdXJuOm9pZDpjbnNxA" +
            "H4AAnQACWdpdmVuTmFtZXEAfgAGdAARdXJuOm9pZDpnaXZlbk5hbWVzcQB+AAJ0AAJzbnEAfgAGdAAPdXJuOm9pZDpzdXJuYW1leA==";

    @Test
    public void testCreateConditions() throws Exception {
        Saml2AttributeQueryResponseGenerator generator = new Saml2AttributeQueryResponseGenerator();
        Saml2AttributeQueryResponseGenerator.Conditions cond = new Saml2AttributeQueryResponseGenerator.Conditions();
        cond.audience = "http://www.layer7tech.com/";
        ConditionsType conditions = generator.createConditions(cond, new GregorianCalendar());

        assertNotNull(conditions);
        assertEquals(1, conditions.getConditionOrAudienceRestrictionOrOneTimeUse().size());
        assertTrue(conditions.getConditionOrAudienceRestrictionOrOneTimeUse().get(0) instanceof AudienceRestrictionType);

        AudienceRestrictionType audienceRestriction = (AudienceRestrictionType)conditions.getConditionOrAudienceRestrictionOrOneTimeUse().get(0);
        assertEquals(1, audienceRestriction.getAudience().size());
        assertEquals("http://www.layer7tech.com/", audienceRestriction.getAudience().get(0));
    }

    @Test
    public void testBuildSubject() throws Exception {
        Saml2AttributeQuery query = createQuery();
        Saml2AttributeQueryResponseGenerator generator = new Saml2AttributeQueryResponseGenerator();
        SubjectType subject = generator.buildSubject(query);

        assertNotNull(subject);
        assertEquals(1, subject.getContent().size());

        JAXBElement nameID = subject.getContent().get(0);
        assertTrue(nameID.getValue() instanceof NameIDType);
        NameIDType nameIdType = (NameIDType)nameID.getValue();

        assertEquals("testuser", nameIdType.getValue());
        assertEquals(UNSPECIFIED_FORMAT, nameIdType.getFormat());
        assertEquals("http://my.id.provider.com/", nameIdType.getNameQualifier());
    }

    @Test
    public void testCreateAttribute() throws Exception {
        Saml2AttributeQueryResponseGenerator generator = new Saml2AttributeQueryResponseGenerator();
        HashMap<String, Object> values = createValues();
        AttributeType attribute = generator.createAttribute(BASIC_NAME_FORMAT, "urn:oid:givenName", values.get("givenName"));

        assertNotNull(attribute);
        assertEquals(BASIC_NAME_FORMAT, attribute.getNameFormat());
        assertEquals("urn:oid:givenName", attribute.getName());

        assertEquals(2, attribute.getAttributeValue().size());
        assertEquals("first given name", attribute.getAttributeValue().get(0));
        assertEquals("second given name", attribute.getAttributeValue().get(1));
    }

    @Test
    public void testBuildCommonMessageParts() throws Exception {
        Saml2AttributeQuery query = createQuery();
        Saml2AttributeQueryResponseGenerator generator = new Saml2AttributeQueryResponseGenerator();

        ResponseType response = generator.getSamlpFactory().createResponseType();
        generator.buildCommonMessageParts(response, query.getID(), new GregorianCalendar());

        assertNotNull(response.getID());
        assertEquals("2.0", response.getVersion());
        assertNotNull(response.getIssueInstant());
        assertEquals(query.getID(), response.getInResponseTo());
    }

    @Test
    public void testBuildSpecificParts() throws Exception {
        Saml2AttributeQuery query = createQuery();
        HashMap<String, Object> values = createValues();
        Saml2AttributeQueryResponseGenerator generator = new Saml2AttributeQueryResponseGenerator();
        SamlToLdapMap map = new SamlToLdapMap(ENCODED_ATTR_MAP);

        ResponseType response = generator.getSamlpFactory().createResponseType();
        Saml2AttributeQueryResponseGenerator.Conditions cond = new Saml2AttributeQueryResponseGenerator.Conditions();
        cond.audience = "http://www.layer7tech.com/";
        generator.buildSpecificMessageParts(response, "http://my.issuer.com/", query, map, values, cond, new GregorianCalendar());

        assertEquals(1, response.getAssertionOrEncryptedAssertion().size());
        assertTrue(response.getAssertionOrEncryptedAssertion().get(0) instanceof AssertionType);

        AssertionType assertion = (AssertionType)response.getAssertionOrEncryptedAssertion().get(0);

        assertEquals("2.0", assertion.getVersion());
        assertNotNull(assertion.getIssueInstant());
        assertNotNull(assertion.getID());

        assertNotNull(assertion.getIssuer());
        assertEquals("http://my.issuer.com/", assertion.getIssuer().getValue());

        assertNotNull(assertion.getSubject());
        assertEquals(1, assertion.getSubject().getContent().size());
        assertTrue(assertion.getSubject().getContent().get(0).getValue() instanceof NameIDType);
        NameIDType subject = (NameIDType)assertion.getSubject().getContent().get(0).getValue();
        assertEquals("testuser", subject.getValue());
        assertEquals(UNSPECIFIED_FORMAT, subject.getFormat());
        assertEquals("http://my.id.provider.com/", subject.getNameQualifier());

        assertNotNull(assertion.getConditions());
        assertEquals(1, assertion.getConditions().getConditionOrAudienceRestrictionOrOneTimeUse().size());
        assertTrue(assertion.getConditions().getConditionOrAudienceRestrictionOrOneTimeUse().get(0) instanceof AudienceRestrictionType);
        AudienceRestrictionType audienceRestriction = (AudienceRestrictionType)assertion.getConditions().getConditionOrAudienceRestrictionOrOneTimeUse().get(0);
        assertEquals(1, audienceRestriction.getAudience().size());
        assertEquals("http://www.layer7tech.com/", audienceRestriction.getAudience().get(0));

        assertEquals(1, assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().size());
        assertTrue(assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().get(0) instanceof AttributeStatementType);
        AttributeStatementType statement = (AttributeStatementType)assertion.getStatementOrAuthnStatementOrAuthzDecisionStatement().get(0);

        assertEquals(2, statement.getAttributeOrEncryptedAttribute().size());
        assertTrue(statement.getAttributeOrEncryptedAttribute().get(0) instanceof AttributeType);
        assertTrue(statement.getAttributeOrEncryptedAttribute().get(1) instanceof AttributeType);
        //assert attributes
        for(Object attr : statement.getAttributeOrEncryptedAttribute()) {
            AttributeType attribute = (AttributeType) attr;
            assertEquals(BASIC_NAME_FORMAT, attribute.getNameFormat());
            if("urn:oid:cn".equals(attribute.getName())) {
                assertEquals(1, attribute.getAttributeValue().size());
                assertEquals("Common name", attribute.getAttributeValue().get(0));
            }
            else if("urn:oid:givenName".equals(attribute.getName())) {
                attribute = (AttributeType) attr;
                assertEquals(2, attribute.getAttributeValue().size());
                assertEquals("first given name", attribute.getAttributeValue().get(0));
                assertEquals("second given name", attribute.getAttributeValue().get(1));
            }
            else {
                fail("Unexpected attribute found: " + attribute.getName());
            }
        }
    }

    @Test
    public void testCreateErrorMessage() throws Exception {
        Saml2AttributeQuery query = createQuery();
        List<Saml2AttributeQuery.QueriedAttribute> queriedAttributes = new ArrayList<Saml2AttributeQuery.QueriedAttribute>();
        queriedAttributes.add(new Saml2AttributeQuery.QueriedAttribute("urn:oid:cn", SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC));
        queriedAttributes.add(new Saml2AttributeQuery.QueriedAttribute("urn:oid:givenName", SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC));

        Saml2AttributeQueryResponseGenerator generator = new Saml2AttributeQueryResponseGenerator();
        Object responseObject = generator.createErrorMessage("urn:oasis:names:tc:SAML:2.0:status:Requester",
                "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue",
                "Test error message",
                query.getID(),
                queriedAttributes);

        assertTrue(responseObject instanceof JAXBElement);
        assertTrue(((JAXBElement)responseObject).getValue() instanceof ResponseType);

        ResponseType response = (ResponseType)((JAXBElement)responseObject).getValue();
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Requester", response.getStatus().getStatusCode().getValue());
        assertNotNull(response.getStatus().getStatusCode().getStatusCode());
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue", response.getStatus().getStatusCode().getStatusCode().getValue());
        assertNull(response.getStatus().getStatusCode().getStatusCode().getStatusCode());

        assertEquals("Test error message", response.getStatus().getStatusMessage());

        assertNotNull(response.getStatus().getStatusDetail());
        assertEquals(2, response.getStatus().getStatusDetail().getAny().size());
        assertTrue(response.getStatus().getStatusDetail().getAny().get(0) instanceof JAXBElement);
        assertTrue(response.getStatus().getStatusDetail().getAny().get(1) instanceof JAXBElement);

        JAXBElement element = (JAXBElement)response.getStatus().getStatusDetail().getAny().get(0);
        assertTrue(element.getValue() instanceof AttributeType);
        AttributeType attribute = (AttributeType)element.getValue();
        assertEquals(BASIC_NAME_FORMAT, attribute.getNameFormat());
        assertEquals("urn:oid:cn", attribute.getName());

        element = (JAXBElement)response.getStatus().getStatusDetail().getAny().get(1);
        assertTrue(element.getValue() instanceof AttributeType);
        attribute = (AttributeType)element.getValue();
        assertEquals(BASIC_NAME_FORMAT, attribute.getNameFormat());
        assertEquals("urn:oid:givenName", attribute.getName());
    }

    @Test
    public void testCreateSuccessMessage() throws Exception {
        Saml2AttributeQuery query = createQuery();
        Saml2AttributeQueryResponseGenerator generator = new Saml2AttributeQueryResponseGenerator();
        HashMap<String, Object> values = createValues();
        SamlToLdapMap map = new SamlToLdapMap(ENCODED_ATTR_MAP);
        Saml2AttributeQueryResponseGenerator.Conditions cond = new Saml2AttributeQueryResponseGenerator.Conditions();
        cond.audience = "http://my.audience.com/";
        Object responseObject =  generator.createSuccessMesage(query, "http://my.issuer.com/", map, values, cond);

        assertTrue(responseObject instanceof JAXBElement);
        assertTrue(((JAXBElement)responseObject).getValue() instanceof ResponseType);

        ResponseType response = (ResponseType)((JAXBElement)responseObject).getValue();

        assertNotNull(response.getIssuer());
        assertEquals("http://my.issuer.com/", response.getIssuer().getValue());

        assertNotNull(response.getStatus().getStatusCode());
        assertEquals("urn:oasis:names:tc:SAML:2.0:status:Success", response.getStatus().getStatusCode().getValue());
        assertNull(response.getStatus().getStatusCode().getStatusCode());

        assertEquals(1, response.getAssertionOrEncryptedAssertion().size());
    }

    private Saml2AttributeQuery createQuery() {
        List<Saml2AttributeQuery.AttributeFilter> filters = new ArrayList<Saml2AttributeQuery.AttributeFilter>();
        filters.add(new Saml2AttributeQuery.AttributeFilter("urn:oid:cn", "cn", BASIC_NAME_FORMAT));
        filters.add(new Saml2AttributeQuery.AttributeFilter("urn:oid:givenName", "givenName", BASIC_NAME_FORMAT));

        return new Saml2AttributeQuery("test-1", "testuser", UNSPECIFIED_FORMAT, "http://my.id.provider.com/", filters);
    }

    private HashMap<String, Object> createValues() {
        HashMap<String, Object> values = new HashMap<String, Object>();
        values.put("cn", "Common name");

        List<String> list = new ArrayList<String>();
        list.add("first given name");
        list.add("second given name");
        values.put("givenName", list);

        return values;
    }
}
