package com.l7tech.policy.assertion;

import com.l7tech.message.JmsKnob;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class AddHeaderAssertionTest {
    private static final String CURRENT_POLICY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" " +
                    "xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <L7p:AddHeader>\n" +
            "        <L7p:EvaluateNameAsExpression booleanValue=\"true\"/>\n" +
            "        <L7p:EvaluateValueExpression booleanValue=\"true\"/>\n" +
            "        <L7p:HeaderName stringValue=\"name\"/>\n" +
            "        <L7p:HeaderValue stringValue=\"value\"/>\n" +
            "        <L7p:MetadataType stringValue=\"JMS Property\"/>\n" +
            "        <L7p:Operation operation=\"REMOVE\"/>\n" +
            "        <L7p:Target target=\"RESPONSE\"/>\n" +
            "    </L7p:AddHeader>\n" +
            "</wsp:Policy>\n";

    private AddHeaderAssertion assertion;
    private AssertionNodeNameFactory<AddHeaderAssertion> assertionNameFactory;

    @Before
    public void setup() {
        assertion = new AddHeaderAssertion();
        assertionNameFactory = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
    }

    @Test
    public void testSerialization() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(AddHeaderAssertion.class);

        WspReader wspReader = new WspReader(registry);

        // test deserialization
        AddHeaderAssertion assertion =
                (AddHeaderAssertion) wspReader.parseStrictly(CURRENT_POLICY, WspReader.INCLUDE_DISABLED);

        assertEquals("JMS Property", assertion.getMetadataType());
        assertEquals(AddHeaderAssertion.Operation.REMOVE, assertion.getOperation());
        assertEquals(TargetMessageType.RESPONSE, assertion.getTarget());
        assertEquals("name", assertion.getHeaderName());
        assertEquals("value", assertion.getHeaderValue());
        assertTrue(assertion.isEvaluateNameAsExpression());
        assertTrue(assertion.isEvaluateValueExpression());

        // test serialization
        assertEquals(CURRENT_POLICY, WspWriter.getPolicyXml(assertion));
    }

    @Test
    public void getAssertionNameNoDecorate() {
        assertion.setHeaderName("foo");
        assertEquals("Manage Transport Properties/Headers", assertionNameFactory.getAssertionName(assertion, false));
    }

    @Test
    public void getAssertionNameAddHttpHeader() {
        assertion.setHeaderName("foo");
        assertion.setHeaderValue("bar");
        assertEquals("Request: Add HTTP Header foo:bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameAddJmsProperty() {
        assertion.setMetadataType(JmsKnob.HEADER_TYPE_JMS_PROPERTY);
        assertion.setHeaderName("foo");
        assertion.setHeaderValue("bar");
        assertEquals("Request: Add JMS Property foo:bar", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameReplace() {
        assertion.setHeaderName("foo");
        assertion.setHeaderValue("bar");
        assertion.setRemoveExisting(true);
        assertEquals("Request: Add HTTP Header foo:bar (replace existing)", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemove() {
        assertion.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertion.setHeaderName("foo");
        // remove existing should be ignored
        assertion.setRemoveExisting(true);
        assertEquals("Request: Remove HTTP Header(s) foo", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemoveJmsPropertyResponse() {
        assertion.setMetadataType(JmsKnob.HEADER_TYPE_JMS_PROPERTY);
        assertion.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertion.setHeaderName("foo");
        assertion.setTarget(TargetMessageType.RESPONSE);
        assertion.setRemoveExisting(true);
        assertEquals("Response: Remove JMS Property(s) foo", assertionNameFactory.getAssertionName(assertion, true));
    }

    @Test
    public void getAssertionNameRemoveMatchValue() {
        assertion.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertion.setHeaderName("foo");
        assertion.setHeaderValue("bar");
        // remove existing should be ignored
        assertion.setRemoveExisting(true);
        assertEquals("Request: Remove HTTP Header(s) foo:bar", assertionNameFactory.getAssertionName(assertion, true));
    }
}
