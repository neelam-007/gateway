/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 3, 2009
 * Time: 12:32:58 PM
 */
package com.l7tech.external.assertions.xacmlpdp;

import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.AssertionResourceInfo;
import org.junit.Test;
import org.junit.Assert;

import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * Tests the PDP and PEP Xacml assertions
 */
public class XacmlAssertionsTest {
    private static final String ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id";
    private static final String STRING_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#string";
    private static final String SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";
    private static final String SUBJECT_DATA_TYPE = "urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name";
    private static final String RESOURCE_ID = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";
    private static final String ANY_URI = "http://www.w3.org/2001/XMLSchema#anyURI";

    @Test
    public void testPepFreeze() throws IOException {

        XacmlRequestBuilderAssertion assertion = new XacmlRequestBuilderAssertion();

        //Create the Action
        XacmlRequestBuilderAssertion.Action action = new XacmlRequestBuilderAssertion.Action();
        setAttributeHolderTag(action, ACTION_ID, STRING_DATA_TYPE, "", "read");
        assertion.setAction(action);

        //Create the Subjects with 2 attributes
        XacmlRequestBuilderAssertion.Subject subject = new XacmlRequestBuilderAssertion.Subject();
        setAttributeHolderTag(subject, SUBJECT_ID, SUBJECT_DATA_TYPE, "", "darmstrong@layer7tech.com");
        setAttributeHolderTag(subject, "group", STRING_DATA_TYPE, "admin@layer7tech.com", "developers");

        List<XacmlRequestBuilderAssertion.Subject> subjectList = new ArrayList<XacmlRequestBuilderAssertion.Subject>();
        subjectList.add(subject);

        assertion.setSubjects(subjectList);

        //Resource
        XacmlRequestBuilderAssertion.Resource resource = new XacmlRequestBuilderAssertion.Resource();
        setAttributeHolderTag(resource, RESOURCE_ID, ANY_URI, "", "http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_XACML");

        List<XacmlRequestBuilderAssertion.Resource> resourceList = new ArrayList<XacmlRequestBuilderAssertion.Resource>();
        resourceList.add(resource);
        assertion.setResources(resourceList);

        assertion.setSoapEncapsulation(XacmlAssertionEnums.SoapVersion.v1_1);

        XacmlRequestBuilderAssertion.Environment environment = new XacmlRequestBuilderAssertion.Environment();
        setAttributeHolderTag(environment, "ENV",STRING_DATA_TYPE, "", "EnvValue");

        assertion.setEnvironment(environment);

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(XacmlRequestBuilderAssertion.class);

        WspWriter writer = new WspWriter();
        writer.setPolicy(assertion);
        String actualXml = fixLines(writer.getPolicyXmlAsString());
        System.out.println(actualXml);


        Assert.assertEquals("WspWriter generated policy xml should match canned xml", fixLines(XACML_REQUEST_XML), actualXml.trim());
    }

    /**
     * Test Subject thaw and validate object state
     * @throws Exception
     */
    @Test
    public void testThawXacmlAssertion_Subject() throws Exception {
        XacmlRequestBuilderAssertion assertion = getPepXacmlAssertion();

        //Validate Subjects
        List<XacmlRequestBuilderAssertion.Subject> subjects = assertion.getSubjects();
        Assert.assertNotNull("subjects should not be null", subjects);
        Assert.assertEquals("Only 1 subject was expected", 1, subjects.size());

        XacmlRequestBuilderAssertion.Subject subject = subjects.get(0);

        List<XacmlRequestBuilderAssertion.AttributeTreeNodeTag> attributeTreeNodes = subject.getAttributes();
        Assert.assertNotNull("attributes should not be null", attributeTreeNodes);

        Assert.assertEquals("There should be 2 attributes", 2, attributeTreeNodes.size());

        //Test the subjects first attribute and it's value
        XacmlRequestBuilderAssertion.Attribute attribute = (XacmlRequestBuilderAssertion.Attribute) attributeTreeNodes.get(0);
        Assert.assertEquals("Invalid ID found", SUBJECT_ID, attribute.getId());
        Assert.assertEquals("Invalid data type found", SUBJECT_DATA_TYPE, attribute.getDataType());
        Assert.assertEquals("Empty issuer expected", "", attribute.getIssuer());

        List<XacmlRequestBuilderAssertion.AttributeValue> attributeValues = attribute.getValues();
        Assert.assertNotNull("values should not be null", attributeValues);
        Assert.assertEquals("values should contain 1 value", 1, attributeValues.size());

        XacmlRequestBuilderAssertion.AttributeValue attributeValue = attributeValues.get(0);
        Assert.assertNotNull("value should not be null", attributeValue);

        String content = attributeValue.getContent();
        Assert.assertNotNull("content should not be null", content);
        Assert.assertEquals("Incorrect content found", "darmstrong@layer7tech.com", content);

        //Test the subjects second attribute and it's value
        attribute = (XacmlRequestBuilderAssertion.Attribute) attributeTreeNodes.get(1);
        Assert.assertEquals("Invalid ID found", "group", attribute.getId());
        Assert.assertEquals("Invalid data type found", STRING_DATA_TYPE, attribute.getDataType());
        Assert.assertEquals("Empty issuer expected", "admin@layer7tech.com", attribute.getIssuer());

        attributeValues = attribute.getValues();
        Assert.assertNotNull("values should not be null", attributeValues);
        Assert.assertEquals("values should contain 1 value", 1, attributeValues.size());

        attributeValue = attributeValues.get(0);
        Assert.assertNotNull("value should not be null", attributeValue);

        content = attributeValue.getContent();
        Assert.assertNotNull("content should not be null", content);
        Assert.assertEquals("Incorrect content found", "developers", content);        

    }

    /**
     * Test Resource thaw and validate object state
     * @throws Exception
     */
    @Test
    public void testThawXacmlAssertion_Resources() throws Exception {
        XacmlRequestBuilderAssertion assertion = getPepXacmlAssertion();

        //Validate Subjects
        List<XacmlRequestBuilderAssertion.Resource> resources = assertion.getResources();
        Assert.assertNotNull("resources should not be null", resources);
        Assert.assertEquals("Only 1 resource was expected", 1, resources.size());

        XacmlRequestBuilderAssertion.Resource resource = resources.get(0);

        List<XacmlRequestBuilderAssertion.AttributeTreeNodeTag> attributeTreeNodes = resource.getAttributes();
        Assert.assertNotNull("attributes should not be null", attributeTreeNodes);

        Assert.assertEquals("There should be 1 attribute", 1, attributeTreeNodes.size());

        //Test the resources attribute and it's value
        XacmlRequestBuilderAssertion.Attribute attribute = (XacmlRequestBuilderAssertion.Attribute) attributeTreeNodes.get(0);
        Assert.assertEquals("Invalid ID found", RESOURCE_ID, attribute.getId());
        Assert.assertEquals("Invalid data type found", ANY_URI, attribute.getDataType());
        Assert.assertEquals("Empty issuer expected", "", attribute.getIssuer());

        List<XacmlRequestBuilderAssertion.AttributeValue> attributeValues = attribute.getValues();
        Assert.assertNotNull("values should not be null", attributeValues);
        Assert.assertEquals("values should contain 1 value", 1, attributeValues.size());

        XacmlRequestBuilderAssertion.AttributeValue attributeValue = attributeValues.get(0);
        Assert.assertNotNull("value should not be null", attributeValue);

        String content = attributeValue.getContent();
        Assert.assertNotNull("content should not be null", content);
        Assert.assertEquals("Incorrect content found", "http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_XACML", content);
    }

    /**
     * Test Action thaw and validate object state
     * @throws Exception
     */
    @Test
    public void testThawXacmlAssertion_Action() throws Exception {
        XacmlRequestBuilderAssertion assertion = getPepXacmlAssertion();

        //Validate Subjects
        XacmlRequestBuilderAssertion.Action action = assertion.getAction();
        Assert.assertNotNull("action should not be null", action);

        List<XacmlRequestBuilderAssertion.AttributeTreeNodeTag> attributeTreeNodes = action.getAttributes();
        Assert.assertNotNull("attributes should not be null", attributeTreeNodes);

        Assert.assertEquals("There should be 1 attribute", 1, attributeTreeNodes.size());

        //Test the resources attribute and it's value
        XacmlRequestBuilderAssertion.Attribute attribute = (XacmlRequestBuilderAssertion.Attribute) attributeTreeNodes.get(0);
        Assert.assertEquals("Invalid ID found", ACTION_ID, attribute.getId());
        Assert.assertEquals("Invalid data type found", STRING_DATA_TYPE, attribute.getDataType());
        Assert.assertEquals("Empty issuer expected", "", attribute.getIssuer());

        List<XacmlRequestBuilderAssertion.AttributeValue> attributeValues = attribute.getValues();
        Assert.assertNotNull("values should not be null", attributeValues);
        Assert.assertEquals("values should contain 1 value", 1, attributeValues.size());

        XacmlRequestBuilderAssertion.AttributeValue attributeValue = attributeValues.get(0);
        Assert.assertNotNull("value should not be null", attributeValue);

        String content = attributeValue.getContent();
        Assert.assertNotNull("content should not be null", content);
        Assert.assertEquals("Incorrect content found", "read", content);
    }

    /**
     * Test Action thaw and validate object state
     * @throws Exception
     */
    @Test
    public void testThawXacmlAssertion_Environment() throws Exception {
        XacmlRequestBuilderAssertion assertion = getPepXacmlAssertion();

        //Validate Subjects
        XacmlRequestBuilderAssertion.Environment environment = assertion.getEnvironment();
        Assert.assertNotNull("environment should not be null", environment);

        List<XacmlRequestBuilderAssertion.AttributeTreeNodeTag> attributeTreeNodes = environment.getAttributes();
        Assert.assertNotNull("attributes should not be null", attributeTreeNodes);

        Assert.assertEquals("There should be 1 attribute", 1, attributeTreeNodes.size());

        //Test the resources attribute and it's value
        XacmlRequestBuilderAssertion.Attribute attribute = (XacmlRequestBuilderAssertion.Attribute) attributeTreeNodes.get(0);
        Assert.assertEquals("Invalid ID found", "ENV", attribute.getId());
        Assert.assertEquals("Invalid data type found", STRING_DATA_TYPE, attribute.getDataType());
        Assert.assertEquals("Empty issuer expected", "", attribute.getIssuer());

        List<XacmlRequestBuilderAssertion.AttributeValue> attributeValues = attribute.getValues();
        Assert.assertNotNull("values should not be null", attributeValues);
        Assert.assertEquals("values should contain 1 value", 1, attributeValues.size());

        XacmlRequestBuilderAssertion.AttributeValue attributeValue = attributeValues.get(0);
        Assert.assertNotNull("value should not be null", attributeValue);

        String content = attributeValue.getContent();
        Assert.assertNotNull("content should not be null", content);
        Assert.assertEquals("Incorrect content found", "EnvValue", content);
    }

    private XacmlRequestBuilderAssertion getPepXacmlAssertion() throws IOException {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(XacmlRequestBuilderAssertion.class);
        WspReader wspr = new WspReader(registry);

        XacmlRequestBuilderAssertion assertion = (XacmlRequestBuilderAssertion) wspr.parseStrictly(XACML_REQUEST_XML);

        Assert.assertNotNull(assertion);
        return assertion;
    }

    private XacmlPdpAssertion getPdpXacmlAssertion() throws IOException {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(XacmlPdpAssertion.class);
        WspReader wspr = new WspReader(registry);

        XacmlPdpAssertion assertion = (XacmlPdpAssertion) wspr.parseStrictly(XACML_PDP_XML);

        Assert.assertNotNull(assertion);
        return assertion;
    }

    @Test
    public void testPdpFreeze() throws IOException {
        XacmlPdpAssertion assertion = new XacmlPdpAssertion();
        assertion.setOutputMessageVariableName("decision");

        StaticResourceInfo sri = new StaticResourceInfo(PDP_POLICY_XML);
        assertion.setResourceInfo(sri);

        assertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST);

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(XacmlPdpAssertion.class);

        WspWriter writer = new WspWriter();
        writer.setPolicy(assertion);
        String actualXml = fixLines(writer.getPolicyXmlAsString());
        System.out.println(actualXml);


        Assert.assertEquals("WspWriter generated policy xml should match canned xml", fixLines(XACML_PDP_XML), actualXml.trim());

    }

    @Test
    public void testPdpThaw() throws IOException {
        XacmlPdpAssertion assertion = getPdpXacmlAssertion();

        String messageVariable = assertion.getOutputMessageVariableName();
        Assert.assertNotNull("message variable should not be null", messageVariable);
        Assert.assertEquals("incorrect message variable name", "decision", messageVariable);

        AssertionResourceInfo ari = assertion.getResourceInfo();
        Assert.assertNotNull("Resource should not be null", ari);
        Assert.assertTrue("Resource should be instance of StaticResourceInfo", ari instanceof StaticResourceInfo);
        StaticResourceInfo sri = (StaticResourceInfo) ari;
        String doc = sri.getDocument();
        Assert.assertNotNull("doc should not be null", doc);
        Assert.assertEquals("Resource document should match canned xml policy", PDP_POLICY_XML, doc);

        XacmlPdpAssertion.SoapEncapsulationType soapEnc = assertion.getSoapEncapsulation();
        Assert.assertNotNull("Soap encoding should not be null", soapEnc);
        Assert.assertEquals("Incorrect soap encoding", XacmlPdpAssertion.SoapEncapsulationType.REQUEST, soapEnc);
    }

    @Test
    public void testRequestBuilderAssertionClone() {
        XacmlRequestBuilderAssertion assertion = new XacmlRequestBuilderAssertion();
        XacmlRequestBuilderAssertion.Attribute sourceAttribute = new XacmlRequestBuilderAssertion.Attribute();
        XacmlRequestBuilderAssertion.MultipleAttributeConfig sourceMAC = new XacmlRequestBuilderAssertion.MultipleAttributeConfig();
        assertion.getAction().getAttributes().add( sourceAttribute );
        assertion.getAction().getAttributes().add( sourceMAC );
        assertion.getEnvironment().getAttributes().add( sourceAttribute );
        assertion.getResources().get( 0 ).getAttributes().add( sourceAttribute );
        assertion.getSubjects().get( 0 ).getAttributes().add( sourceAttribute );
        assertion.setSoapEncapsulation( XacmlAssertionEnums.SoapVersion.v1_2 );

        XacmlRequestBuilderAssertion clonedAssertion = (XacmlRequestBuilderAssertion) assertion.getCopy();
        Assert.assertNotSame( "Clone should be copy", assertion, clonedAssertion );
        Assert.assertNotSame( "Clone should be copy (Action)", assertion.getAction(), clonedAssertion.getAction() );
        Assert.assertNotSame( "Clone should be copy (Environment)", assertion.getEnvironment(), clonedAssertion.getEnvironment() );
        Assert.assertNotSame( "Clone should be copy (Resources)", assertion.getResources(), clonedAssertion.getResources() );
        Assert.assertNotSame( "Clone should be copy (Subject)", assertion.getSubjects(), clonedAssertion.getSubjects() );
        Assert.assertNotSame( "Clone should be copy (Resources content)", assertion.getResources().get( 0 ), clonedAssertion.getResources().get( 0 ) );
        Assert.assertNotSame( "Clone should be copy (Subject content)", assertion.getSubjects().get( 0 ), clonedAssertion.getSubjects().get( 0 ) );
        Assert.assertNotSame( "Clone should be copy (Action, attributes)", assertion.getAction().getAttributes(), clonedAssertion.getAction().getAttributes() );
        Assert.assertNotSame( "Clone should be copy (Environment, attributes)", assertion.getEnvironment().getAttributes(), clonedAssertion.getEnvironment().getAttributes() );
        Assert.assertNotSame( "Clone should be copy (Action, attribute content 0)", assertion.getAction().getAttributes().get( 0 ), clonedAssertion.getAction().getAttributes().get( 0 ) );
        Assert.assertNotSame( "Clone should be copy (Action, attribute content 1)", assertion.getAction().getAttributes().get( 1 ), clonedAssertion.getAction().getAttributes().get( 1 ) );
    }

    @Test
    public void testPdpAssertionClone() {
        XacmlPdpAssertion assertion = new XacmlPdpAssertion();
        assertion.setFailIfNotPermit( true );
        assertion.setSoapEncapsulation( XacmlPdpAssertion.SoapEncapsulationType.REQUEST_AND_RESPONSE );
        assertion.setResourceInfo( new StaticResourceInfo("<Policy/>") );

        XacmlPdpAssertion clonedAssertion = (XacmlPdpAssertion) assertion.getCopy();
        Assert.assertNotSame( "Clone should be copy", assertion, clonedAssertion );
    }

    /**
     * Subject, Resource, Action & Environment are all AttributeHolderTag s. Utility method to add a single value
     * to the set of attributes already in the AttributeHolderTag holder
     * @param holder
     * @param ids
     * @param dataType
     * @param issuer
     * @param attributeValue
     */
    private void setAttributeHolderTag(XacmlRequestBuilderAssertion.RequestChildElement holder, String ids,
                                       String dataType,
                                       String issuer,
                                       String attributeValue){

        //AttributeHolderTag creates a new AttributeList with object creation, no need to create a new one
        List<XacmlRequestBuilderAssertion.AttributeTreeNodeTag> attributeListTreeNode = holder.getAttributes();
        Assert.assertNotNull("holder.getAttributes() should always return non null List", attributeListTreeNode);

        List<XacmlRequestBuilderAssertion.AttributeValue> attributeValueList = new ArrayList<XacmlRequestBuilderAssertion.AttributeValue>();
        XacmlRequestBuilderAssertion.AttributeValue value = new XacmlRequestBuilderAssertion.AttributeValue();
        value.setContent(attributeValue);
        attributeValueList.add(value);
        XacmlRequestBuilderAssertion.Attribute attribute = new XacmlRequestBuilderAssertion.Attribute();
        attribute.setId(ids);
        attribute.setDataType(dataType);
        attribute.setIssuer(issuer);
        attribute.setValues(attributeValueList);
        attributeListTreeNode.add(attribute);

        holder.setAttributes(attributeListTreeNode);
    }
    
    private String fixLines(String input) {
        return input.replaceAll("\\r\\n", "\n").replaceAll("\\n\\r", "\n").replaceAll("\\r", "\n");
    }

    private final static String XACML_REQUEST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <L7p:XacmlRequestBuilderAssertion>\n" +
            "        <L7p:Action action=\"included\">\n" +
            "            <L7p:Attributes attributeList=\"included\">\n" +
            "                <L7p:item attribute=\"included\">\n" +
            "                    <L7p:DataType stringValue=\"http://www.w3.org/2001/XMLSchema#string\"/>\n" +
            "                    <L7p:Id stringValue=\"urn:oasis:names:tc:xacml:1.0:action:action-id\"/>\n" +
            "                    <L7p:Values valueList=\"included\">\n" +
            "                        <L7p:item attributeValue=\"included\">\n" +
            "                            <L7p:Content stringValue=\"read\"/>\n" +
            "                        </L7p:item>\n" +
            "                    </L7p:Values>\n" +
            "                </L7p:item>\n" +
            "            </L7p:Attributes>\n" +
            "        </L7p:Action>\n" +
            "        <L7p:Environment environment=\"included\">\n" +
            "            <L7p:Attributes attributeList=\"included\">\n" +
            "                <L7p:item attribute=\"included\">\n" +
            "                    <L7p:DataType stringValue=\"http://www.w3.org/2001/XMLSchema#string\"/>\n" +
            "                    <L7p:Id stringValue=\"ENV\"/>\n" +
            "                    <L7p:Values valueList=\"included\">\n" +
            "                        <L7p:item attributeValue=\"included\">\n" +
            "                            <L7p:Content stringValue=\"EnvValue\"/>\n" +
            "                        </L7p:item>\n" +
            "                    </L7p:Values>\n" +
            "                </L7p:item>\n" +
            "            </L7p:Attributes>\n" +
            "        </L7p:Environment>\n" +
            "        <L7p:Resources resourceList=\"included\">\n" +
            "            <L7p:item resource=\"included\">\n" +
            "                <L7p:Attributes attributeList=\"included\">\n" +
            "                    <L7p:item attribute=\"included\">\n" +
            "                        <L7p:DataType stringValue=\"http://www.w3.org/2001/XMLSchema#anyURI\"/>\n" +
            "                        <L7p:Id stringValue=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\"/>\n" +
            "                        <L7p:Values valueList=\"included\">\n" +
            "                            <L7p:item attributeValue=\"included\">\n" +
            "                                <L7p:Content stringValue=\"http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_XACML\"/>\n" +
            "                            </L7p:item>\n" +
            "                        </L7p:Values>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Attributes>\n" +
            "            </L7p:item>\n" +
            "        </L7p:Resources>\n" +
            "        <L7p:SoapEncapsulation soapEncapsulation=\"v1_1\"/>\n" +
            "        <L7p:Subjects subjectList=\"included\">\n" +
            "            <L7p:item subject=\"included\">\n" +
            "                <L7p:Attributes attributeList=\"included\">\n" +
            "                    <L7p:item attribute=\"included\">\n" +
            "                        <L7p:DataType stringValue=\"urn:oasis:names:tc:xacml:1.0:data-type:rfc822Name\"/>\n" +
            "                        <L7p:Id stringValue=\"urn:oasis:names:tc:xacml:1.0:subject:subject-id\"/>\n" +
            "                        <L7p:Values valueList=\"included\">\n" +
            "                            <L7p:item attributeValue=\"included\">\n" +
            "                                <L7p:Content stringValue=\"darmstrong@layer7tech.com\"/>\n" +
            "                            </L7p:item>\n" +
            "                        </L7p:Values>\n" +
            "                    </L7p:item>\n" +
            "                    <L7p:item attribute=\"included\">\n" +
            "                        <L7p:DataType stringValue=\"http://www.w3.org/2001/XMLSchema#string\"/>\n" +
            "                        <L7p:Id stringValue=\"group\"/>\n" +
            "                        <L7p:Issuer stringValue=\"admin@layer7tech.com\"/>\n" +
            "                        <L7p:Values valueList=\"included\">\n" +
            "                            <L7p:item attributeValue=\"included\">\n" +
            "                                <L7p:Content stringValue=\"developers\"/>\n" +
            "                            </L7p:item>\n" +
            "                        </L7p:Values>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Attributes>\n" +
            "            </L7p:item>\n" +
            "        </L7p:Subjects>\n" +
            "    </L7p:XacmlRequestBuilderAssertion>\n" +
            "</wsp:Policy>";

    private final static String PDP_POLICY_XML = "<Policy PolicyId=\"ExamplePolicy\"\n" +
            "          RuleCombiningAlgId=\"urn:oasis:names:tc:xacml:1.0:rule-combining-algorithm:permit-overrides\">\n" +
            "    <Target>\n" +
            "      <Subjects>\n" +
            "        <AnySubject/>\n" +
            "      </Subjects>\n" +
            "      <Resources>\n" +
            "        <Resource>\n" +
            "          <ResourceMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:anyURI-equal\">\n" +
            "            <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\">http://server.example.com/code/docs/developer-guide.html</AttributeValue>\n" +
            "            <ResourceAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#anyURI\"\n" +
            "                                         AttributeId=\"urn:oasis:names:tc:xacml:1.0:resource:resource-id\"/>\n" +
            "          </ResourceMatch>\n" +
            "        </Resource>\n" +
            "      </Resources>\n" +
            "      <Actions>\n" +
            "        <AnyAction/>\n" +
            "      </Actions>\n" +
            "    </Target>\n" +
            "    <Rule RuleId=\"ReadRule\" Effect=\"Permit\">\n" +
            "      <Target>\n" +
            "        <Subjects>\n" +
            "          <AnySubject/>\n" +
            "        </Subjects>\n" +
            "        <Resources>\n" +
            "          <AnyResource/>\n" +
            "        </Resources>\n" +
            "        <Actions>\n" +
            "          <Action>\n" +
            "            <ActionMatch MatchId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
            "              <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">read</AttributeValue>\n" +
            "              <ActionAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#string\"\n" +
            "                                         AttributeId=\"urn:oasis:names:tc:xacml:1.0:action:action-id\"/>\n" +
            "            </ActionMatch>\n" +
            "          </Action>\n" +
            "        </Actions>\n" +
            "      </Target>\n" +
            "      <Condition FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-equal\">\n" +
            "        <Apply FunctionId=\"urn:oasis:names:tc:xacml:1.0:function:string-one-and-only\">\n" +
            "          <SubjectAttributeDesignator DataType=\"http://www.w3.org/2001/XMLSchema#string\"\n" +
            "                                      AttributeId=\"group\"/>\n" +
            "        </Apply>\n" +
            "        <AttributeValue DataType=\"http://www.w3.org/2001/XMLSchema#string\">developers</AttributeValue>\n" +
            "      </Condition>\n" +
            "    </Rule>\n" +
            "  </Policy>";

    private final static String XACML_PDP_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <L7p:XacmlPdpAssertion>\n" +
            "        <L7p:OutputMessageVariableName stringValue=\"decision\"/>\n" +
            "        <L7p:ResourceInfo staticResourceInfo=\"included\">\n" +
            "            <L7p:Document stringValueReference=\"inline\"><![CDATA["+PDP_POLICY_XML+"]]></L7p:Document>\n" +
            "        </L7p:ResourceInfo>\n" +
            "        <L7p:SoapEncapsulation soapEncapsulation=\"REQUEST\"/>\n" +
            "    </L7p:XacmlPdpAssertion>\n" +
            "</wsp:Policy>";
    
}


