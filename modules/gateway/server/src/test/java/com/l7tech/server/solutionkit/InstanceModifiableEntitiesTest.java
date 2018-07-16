package com.l7tech.server.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.solutionkit.InstanceModifier;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.test.BugId;
import com.l7tech.xml.xpath.XpathUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.UUID;

import static junit.framework.Assert.assertEquals;

/**
 * Test the following non-sharable entities are instance modifiable.
 * - Folder, Service, Policy, Encapsulated Assertion, Policy Backed Identity Provider, Policy Backed Service, and Scheduled Task
 *
 * Also test the function of applying instance modifier on the above entity types.
 */
public class InstanceModifiableEntitiesTest {
    private final static String SAMPLE_INSTANCE_MODIFIER = "sample_instance_modifier";

    private final static String FOLDER_NAME = "FolderForInstanceModifierDemo";
    private final static String FOLDER_ID = "3456de076a2ec6eb19c0183bafbc55cf";
    private final static String SERVICE_NAME = "ServiceForInstanceModifierDemo";
    private final static String SERVICE_URL = "/sampleServiceUrl";
    private final static String SERVICE_ID = "94bf0377485be42734d3e4cfeea93d04";
    private final static String PBIP_POLICY_NAME = "PBIP_PolicyForInstanceModifierDemo";
    private final static String PBIP_POLICY_ID = "94bf0377485be42734d3e4cfeea93d88";
    private final static String PBS_POLICY_NAME = "PBS_PolicyForInstanceModifierDemo";
    private final static String PBS_POLICY_ID = "94bf0377485be42734d3e4cfeea93da4";
    private final static String ENCAP_POLICY_NAME = "Encap_PolicyForInstanceModifierDemo";
    private final static String ENCAP_POLICY_ID = "94bf0377485be42734d3e4cfeea93dc2";
    private final static String ENCAPSULATED_ASSERTION_NAME = "EncapsulatedAssertionForInstanceModifierDemo";
    private final static String ENCAPSULATED_ASSERTION_ID = "3456de076a2ec6eb19c0183bafbca0a5";
    private final static String POLICY_BACKED_IDENTITY_PROVIDER_NAME = "PolicyBackedIPForInstanceModifierDemo";
    private final static String POLICY_BACKED_IDENTITY_PROVIDER_ID = "94bf0377485be42734d3e4cfeea93e43";
    private final static String POLICY_BACKED_SERVICE_NAME = "PolicyBackedServiceSample";
    private final static String POLICY_BACKED_SERVICE_ID = "15c270b7aa19e0a29ad1f5e728d17d52";
    private final static String SCHEDULED_TASK_NAME = "ScheduledTaskForInstanceModifierDemo";
    private final static String SCHEDULED_TASK_ID = "94bf0377485be42734d3e4cfeea93e27";
    private final static String SSG_CONNECTOR_NAME = "SSGConnectorForInstanceModifierDemo";
    private final static String SSG_CONNECTOR_ID = "f7370df418628b0f050789e387987e2d";

    private SolutionKit sampleSolutionKit;

    @Before
    public void before() {
        initializeSolutionKits();
    }

    @Test
    public void testInstanceModifiableEntities() throws Exception {
        final String instanceModifier = sampleSolutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
        final RestmanMessage requestMessage = new RestmanMessage(SAMPLE_SOLUTION_KIT_INSTALL_BUNDLE_XML);

        ////////////////////////////////////////////////////////////////
        // Verify entity names before an instance modifier is applied.//
        ////////////////////////////////////////////////////////////////

        // Folder
        assertEquals(FOLDER_NAME, requestMessage.getEntityName(FOLDER_ID));
        // Service URL
        assertEquals(SERVICE_URL, requestMessage.getServiceUrl(SERVICE_ID));
        // Policy
        assertEquals(PBIP_POLICY_NAME, requestMessage.getEntityName(PBIP_POLICY_ID));
        assertEquals(PBS_POLICY_NAME, requestMessage.getEntityName(PBS_POLICY_ID));
        assertEquals(ENCAP_POLICY_NAME, requestMessage.getEntityName(ENCAP_POLICY_ID));
        // Encapsulated Assertion
        assertEquals(ENCAPSULATED_ASSERTION_NAME, requestMessage.getEntityName(ENCAPSULATED_ASSERTION_ID));
        // Policy Backed Identity Provider
        assertEquals(POLICY_BACKED_IDENTITY_PROVIDER_NAME, requestMessage.getEntityName(POLICY_BACKED_IDENTITY_PROVIDER_ID));
        // Policy Backed Service
        assertEquals(POLICY_BACKED_SERVICE_NAME, requestMessage.getEntityName(POLICY_BACKED_SERVICE_ID));
        // Scheduled Task
        assertEquals(SCHEDULED_TASK_NAME, requestMessage.getEntityName(SCHEDULED_TASK_ID));
        // SSG Connector
        assertEquals(SSG_CONNECTOR_NAME, requestMessage.getEntityName(SSG_CONNECTOR_ID));

        // Apply an instance modifier on these non-sharable entities
        new InstanceModifier(requestMessage.getBundleReferenceItems(), requestMessage.getMappings(), instanceModifier).apply();

        ///////////////////////////////////////////////////////////////
        // Verify entity names after an instance modifier is applied.//
        ///////////////////////////////////////////////////////////////

        // Folder
        assertEquals(FOLDER_NAME + " " + SAMPLE_INSTANCE_MODIFIER, requestMessage.getEntityName(FOLDER_ID));
        // Service URL
        assertEquals("/" + SAMPLE_INSTANCE_MODIFIER + SERVICE_URL, requestMessage.getServiceUrl(SERVICE_ID));
        // Policy
        assertEquals(SAMPLE_INSTANCE_MODIFIER + " " + PBIP_POLICY_NAME, requestMessage.getEntityName(PBIP_POLICY_ID));
        assertEquals(SAMPLE_INSTANCE_MODIFIER + " " + PBS_POLICY_NAME, requestMessage.getEntityName(PBS_POLICY_ID));
        assertEquals(SAMPLE_INSTANCE_MODIFIER + " " + ENCAP_POLICY_NAME, requestMessage.getEntityName(ENCAP_POLICY_ID));
        // Encapsulated Assertion
        assertEquals(SAMPLE_INSTANCE_MODIFIER + " " + ENCAPSULATED_ASSERTION_NAME, requestMessage.getEntityName(ENCAPSULATED_ASSERTION_ID));
        // Policy Backed Identity Provider
        assertEquals(SAMPLE_INSTANCE_MODIFIER + " " + POLICY_BACKED_IDENTITY_PROVIDER_NAME, requestMessage.getEntityName(POLICY_BACKED_IDENTITY_PROVIDER_ID));
        // Policy Backed Service
        assertEquals(SAMPLE_INSTANCE_MODIFIER + " " + POLICY_BACKED_SERVICE_NAME, requestMessage.getEntityName(POLICY_BACKED_SERVICE_ID));
        // Scheduled Task
        assertEquals(SAMPLE_INSTANCE_MODIFIER + " " + SCHEDULED_TASK_NAME, requestMessage.getEntityName(SCHEDULED_TASK_ID));
        // SSG Connector
        assertEquals(SAMPLE_INSTANCE_MODIFIER + " " + SSG_CONNECTOR_NAME, requestMessage.getEntityName(SSG_CONNECTOR_ID));
    }

    @Test
    @BugId("DE322011")
    public void testTargetIdMappingModified() throws Exception{
        final String skInstanceModifier = sampleSolutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
        final Document documentToVerify = XmlUtil.stringToDocument(SAMPLE_SOLUTION_KIT_INSTALL_BUNDLE_XML);
        final RestmanMessage requestMessage = new RestmanMessage(documentToVerify);

        // Apply an instance modifier on these non-sharable entities
        new InstanceModifier(requestMessage.getBundleReferenceItems(), requestMessage.getMappings(), skInstanceModifier).apply();

        //Test AlwaysCreateNew mapping generates modified Goid SERVICE_ID
        assertEquals(InstanceModifier.getModifiedGoid(skInstanceModifier, SERVICE_ID), getTargetId(SERVICE_ID, documentToVerify));
        //Test NewOrUpdate mapping generates modified Goid SSG_CONNECTOR_ID
        assertEquals(InstanceModifier.getModifiedGoid(skInstanceModifier, SSG_CONNECTOR_ID), getTargetId(SSG_CONNECTOR_ID, documentToVerify));
        //Test NewOrExisting mapping generates modified Goid POLICY_BACKED_SERVICE_ID
        assertEquals(InstanceModifier.getModifiedGoid(skInstanceModifier, POLICY_BACKED_SERVICE_ID), getTargetId(POLICY_BACKED_SERVICE_ID, documentToVerify));
        //Test Existing mapping only does not change targetId
        assertEquals(null, getTargetId(FOLDER_ID, documentToVerify));
        //Test Update mapping only does not change targetId
        assertEquals(null, getTargetId(SCHEDULED_TASK_ID, documentToVerify));
    }

    private String getTargetId(@NotNull final String srcId, @NotNull final Document document) {
        final List<Element> srcIdMappings = XpathUtil.findElements(document.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping[@srcId=\"" + srcId + "\"]", GatewayManagementDocumentUtilities.getNamespaceMap());

        // There should only be one action mapping per scrId  in a restman message
        if (srcIdMappings.size() > 0) {
            String entityType = srcIdMappings.get(0).getAttribute("targetId");
            if (StringUtils.isEmpty(entityType))
                return null;
            else
                return entityType.trim();
        } else {
            return null;
        }
    }

    private void initializeSolutionKits() {
        sampleSolutionKit = new SolutionKit();
        sampleSolutionKit.setSolutionKitGuid(UUID.randomUUID().toString());
        sampleSolutionKit.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, SAMPLE_INSTANCE_MODIFIER);
        sampleSolutionKit.setName("Solution Kit Instance Modifier Demo");
        sampleSolutionKit.setProperty(SolutionKit.SK_PROP_DESC_KEY, "Demonstrate some entities are instance modifiable.");
        sampleSolutionKit.setSolutionKitVersion("1.0");
    }

    private static final String SAMPLE_SOLUTION_KIT_INSTALL_BUNDLE_XML =
        "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
        "  <l7:References>\n" +
        "    <l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
        "      <l7:Name>" + FOLDER_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + FOLDER_ID + "</l7:Id>\n" +
        "      <l7:Type>FOLDER</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-05T15:34:12.493-08:00</l7:TimeStamp>\n" +
        "      <l7:Resource>\n" +
        "        <l7:Folder folderId=\"0000000000000000ffffffffffffec76\" id=\"" + FOLDER_ID + "\" version=\"2\">\n" +
        "            <l7:Name>" + FOLDER_NAME + "</l7:Name>\n" +
        "        </l7:Folder>\n" +
        "      </l7:Resource>\n" +
        "\t</l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>" + PBIP_POLICY_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + PBIP_POLICY_ID + "</l7:Id>\n" +
        "      <l7:Type>POLICY</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-04T16:54:53.938-08:00</l7:TimeStamp>\n" +
        "      <l7:Resource>\n" +
        "        <l7:Policy guid=\"79c9c98e-897d-4ed7-9290-2becb28df8c6\" id=\"" + PBIP_POLICY_ID + "\" version=\"1\">\n" +
        "          <l7:PolicyDetail folderId=\"" + FOLDER_ID + "\" guid=\"79c9c98e-897d-4ed7-9290-2becb28df8c6\" id=\"" + PBIP_POLICY_ID + "\" version=\"1\">\n" +
        "            <l7:Name>" + PBIP_POLICY_NAME + "</l7:Name>\n" +
        "            <l7:PolicyType>Identity Provider</l7:PolicyType>\n" +
        "            <l7:Properties>\n" +
        "              <l7:Property key=\"revision\">\n" +
        "                <l7:LongValue>1</l7:LongValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"soap\">\n" +
        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"tag\">\n" +
        "                <l7:StringValue>password-auth</l7:StringValue>\n" +
        "              </l7:Property>\n" +
        "            </l7:Properties>\n" +
        "          </l7:PolicyDetail>\n" +
        "          <l7:Resources>\n" +
        "            <l7:ResourceSet tag=\"policy\">\n" +
        "              <l7:Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:AuditDetailAssertion&gt;\n" +
        "            &lt;L7p:Detail stringValue=&quot;Policy Fragment: " + PBIP_POLICY_NAME + "&quot;/&gt;\n" +
        "        &lt;/L7p:AuditDetailAssertion&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</l7:Resource>\n" +
        "            </l7:ResourceSet>\n" +
        "          </l7:Resources>\n" +
        "        </l7:Policy>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>" + PBS_POLICY_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + PBS_POLICY_ID + "</l7:Id>\n" +
        "      <l7:Type>POLICY</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-04T16:54:53.939-08:00</l7:TimeStamp>\n" +
        "      <l7:Resource>\n" +
        "        <l7:Policy guid=\"c33389bd-1d6a-4f02-848a-3f8c0d721e08\" id=\"" + PBS_POLICY_ID + "\" version=\"0\">\n" +
        "          <l7:PolicyDetail folderId=\"" + FOLDER_ID + "\" guid=\"c33389bd-1d6a-4f02-848a-3f8c0d721e08\" id=\"" + PBS_POLICY_ID + "\" version=\"0\">\n" +
        "            <l7:Name>" + PBS_POLICY_NAME + "</l7:Name>\n" +
        "            <l7:PolicyType>Service Operation</l7:PolicyType>\n" +
        "            <l7:Properties>\n" +
        "              <l7:Property key=\"revision\">\n" +
        "                <l7:LongValue>1</l7:LongValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"soap\">\n" +
        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"subtag\">\n" +
        "                <l7:StringValue>run</l7:StringValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"tag\">\n" +
        "                <l7:StringValue>com.l7tech.objectmodel.polback.BackgroundTask</l7:StringValue>\n" +
        "              </l7:Property>\n" +
        "            </l7:Properties>\n" +
        "          </l7:PolicyDetail>\n" +
        "          <l7:Resources>\n" +
        "            <l7:ResourceSet tag=\"policy\">\n" +
        "              <l7:Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:AuditDetailAssertion&gt;\n" +
        "            &lt;L7p:Detail stringValue=&quot;Policy Fragment: " + PBS_POLICY_NAME + "&quot;/&gt;\n" +
        "        &lt;/L7p:AuditDetailAssertion&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</l7:Resource>\n" +
        "            </l7:ResourceSet>\n" +
        "          </l7:Resources>\n" +
        "        </l7:Policy>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>" + ENCAPSULATED_ASSERTION_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + ENCAPSULATED_ASSERTION_ID + "</l7:Id>\n" +
        "      <l7:Type>ENCAPSULATED_ASSERTION</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-04T16:54:53.939-08:00</l7:TimeStamp>\n" +
        "      <l7:Resource>\n" +
        "        <l7:EncapsulatedAssertion id=\"" + ENCAPSULATED_ASSERTION_ID + "\" version=\"2\">\n" +
        "          <l7:Name>" + ENCAPSULATED_ASSERTION_NAME + "</l7:Name>\n" +
        "          <l7:Guid>0c1f31c6-0222-4ad4-9188-399dabeae8ed</l7:Guid>\n" +
        "          <l7:PolicyReference id=\"" + ENCAP_POLICY_ID + "\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
        "          <l7:EncapsulatedArguments>\n" +
        "            <l7:EncapsulatedAssertionArgument>\n" +
        "              <l7:Ordinal>1</l7:Ordinal>\n" +
        "              <l7:ArgumentName>inSimpleEncapsulatedAssertionFragment</l7:ArgumentName>\n" +
        "              <l7:ArgumentType>string</l7:ArgumentType>\n" +
        "              <l7:GuiPrompt>false</l7:GuiPrompt>\n" +
        "            </l7:EncapsulatedAssertionArgument>\n" +
        "          </l7:EncapsulatedArguments>\n" +
        "          <l7:EncapsulatedResults>\n" +
        "            <l7:EncapsulatedAssertionResult>\n" +
        "              <l7:ResultName>outSimpleEncapsulatedAssertionFragment</l7:ResultName>\n" +
        "              <l7:ResultType>string</l7:ResultType>\n" +
        "            </l7:EncapsulatedAssertionResult>\n" +
        "          </l7:EncapsulatedResults>\n" +
        "          <l7:Properties>\n" +
        "            <l7:Property key=\"allowTracing\">\n" +
        "              <l7:StringValue>true</l7:StringValue>\n" +
        "            </l7:Property>\n" +
        "            <l7:Property key=\"description\">\n" +
        "              <l7:StringValue>An encapsulated assertion for user configurable entity</l7:StringValue>\n" +
        "            </l7:Property>\n" +
        "            <l7:Property key=\"paletteFolder\">\n" +
        "              <l7:StringValue>routing</l7:StringValue>\n" +
        "            </l7:Property>\n" +
        "            <l7:Property key=\"policyGuid\">\n" +
        "              <l7:StringValue>c9daaf69-d39c-4518-94bc-8112c61562dd</l7:StringValue>\n" +
        "            </l7:Property>\n" +
        "          </l7:Properties>\n" +
        "        </l7:EncapsulatedAssertion>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>" + ENCAP_POLICY_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + ENCAP_POLICY_ID + "</l7:Id>\n" +
        "      <l7:Type>POLICY</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-04T16:54:53.940-08:00</l7:TimeStamp>\n" +
        "      <l7:Resource>\n" +
        "        <l7:Policy guid=\"c9daaf69-d39c-4518-94bc-8112c61562dd\" id=\"" + ENCAP_POLICY_ID + "\" version=\"0\">\n" +
        "          <l7:PolicyDetail folderId=\"" + FOLDER_ID + "\" guid=\"c9daaf69-d39c-4518-94bc-8112c61562dd\" id=\"" + ENCAP_POLICY_ID + "\" version=\"0\">\n" +
        "            <l7:Name>" + ENCAP_POLICY_NAME + "</l7:Name>\n" +
        "            <l7:PolicyType>Include</l7:PolicyType>\n" +
        "            <l7:Properties>\n" +
        "              <l7:Property key=\"revision\">\n" +
        "                <l7:LongValue>1</l7:LongValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"soap\">\n" +
        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
        "              </l7:Property>\n" +
        "            </l7:Properties>\n" +
        "          </l7:PolicyDetail>\n" +
        "          <l7:Resources>\n" +
        "            <l7:ResourceSet tag=\"policy\">\n" +
        "              <l7:Resource type=\"policy\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:AuditDetailAssertion&gt;\n" +
        "            &lt;L7p:Detail stringValue=&quot;Policy Fragment: " + ENCAP_POLICY_NAME + "&quot;/&gt;\n" +
        "        &lt;/L7p:AuditDetailAssertion&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</l7:Resource>\n" +
        "            </l7:ResourceSet>\n" +
        "          </l7:Resources>\n" +
        "        </l7:Policy>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>" + POLICY_BACKED_IDENTITY_PROVIDER_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + POLICY_BACKED_IDENTITY_PROVIDER_ID + "</l7:Id>\n" +
        "      <l7:Type>ID_PROVIDER_CONFIG</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-04T16:54:53.944-08:00</l7:TimeStamp>\n" +
        "      <l7:Resource>\n" +
        "        <l7:IdentityProvider id=\"" + POLICY_BACKED_IDENTITY_PROVIDER_ID + "\" version=\"0\">\n" +
        "          <l7:Name>" + POLICY_BACKED_IDENTITY_PROVIDER_NAME + "</l7:Name>\n" +
        "          <l7:IdentityProviderType>Policy-Backed</l7:IdentityProviderType>\n" +
        "          <l7:Properties>\n" +
        "            <l7:Property key=\"adminEnabled\">\n" +
        "              <l7:BooleanValue>false</l7:BooleanValue>\n" +
        "            </l7:Property>\n" +
        "          </l7:Properties>\n" +
        "          <l7:Extension>\n" +
        "            <l7:PolicyBackedIdentityProviderDetail>\n" +
        "              <l7:AuthenticationPolicyId>" + PBIP_POLICY_ID + "</l7:AuthenticationPolicyId>\n" +
        "            </l7:PolicyBackedIdentityProviderDetail>\n" +
        "          </l7:Extension>\n" +
        "        </l7:IdentityProvider>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>" + SERVICE_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + SERVICE_ID + "</l7:Id>\n" +
        "      <l7:Type>SERVICE</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-04T16:54:53.949-08:00</l7:TimeStamp>\n" +
        "      <l7:Resource>\n" +
        "        <l7:Service id=\"" + SERVICE_ID + "\" version=\"2\">\n" +
        "          <l7:ServiceDetail folderId=\"" + FOLDER_ID + "\" id=\"" + SERVICE_ID + "\" version=\"2\">\n" +
        "            <l7:Name>" + SERVICE_NAME + "</l7:Name>\n" +
        "            <l7:Enabled>true</l7:Enabled>\n" +
        "            <l7:ServiceMappings>\n" +
        "              <l7:HttpMapping>\n" +
        "                <l7:UrlPattern>" + SERVICE_URL + "</l7:UrlPattern>\n" +
        "                <l7:Verbs>\n" +
        "                  <l7:Verb>GET</l7:Verb>\n" +
        "                  <l7:Verb>POST</l7:Verb>\n" +
        "                  <l7:Verb>PUT</l7:Verb>\n" +
        "                  <l7:Verb>DELETE</l7:Verb>\n" +
        "                </l7:Verbs>\n" +
        "              </l7:HttpMapping>\n" +
        "            </l7:ServiceMappings>\n" +
        "            <l7:Properties>\n" +
        "              <l7:Property key=\"internal\">\n" +
        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"policyRevision\">\n" +
        "                <l7:LongValue>4</l7:LongValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"soap\">\n" +
        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"tracingEnabled\">\n" +
        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
        "              </l7:Property>\n" +
        "              <l7:Property key=\"wssProcessingEnabled\">\n" +
        "                <l7:BooleanValue>false</l7:BooleanValue>\n" +
        "              </l7:Property>\n" +
        "            </l7:Properties>\n" +
        "          </l7:ServiceDetail>\n" +
        "          <l7:Resources>\n" +
        "            <l7:ResourceSet tag=\"policy\">\n" +
        "              <l7:Resource type=\"policy\" version=\"3\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:Authentication&gt;\n" +
        "            &lt;L7p:IdentityProviderOid goidValue=&quot;" + POLICY_BACKED_IDENTITY_PROVIDER_ID + "&quot;/&gt;\n" +
        "        &lt;/L7p:Authentication&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</l7:Resource>\n" +
        "            </l7:ResourceSet>\n" +
        "          </l7:Resources>\n" +
        "        </l7:Service>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>" + POLICY_BACKED_SERVICE_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + POLICY_BACKED_SERVICE_ID + "</l7:Id>\n" +
        "      <l7:Type>POLICY_BACKED_SERVICE</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-04T17:13:51.551-08:00</l7:TimeStamp>\n" +
        "      <l7:Link rel=\"self\" uri=\"https://127.0.0.1:8443/restman/1.0/policyBackedServices/" + POLICY_BACKED_SERVICE_ID + "\"/>\n" +
        "      <l7:Resource>\n" +
        "        <l7:PolicyBackedService id=\"" + POLICY_BACKED_SERVICE_ID + "\" version=\"1\">\n" +
        "          <l7:Name>" + POLICY_BACKED_SERVICE_NAME + "</l7:Name>\n" +
        "          <l7:InterfaceName>com.l7tech.objectmodel.polback.BackgroundTask</l7:InterfaceName>\n" +
        "          <l7:PolicyBackedServiceOperations>\n" +
        "            <l7:PolicyBackedServiceOperation>\n" +
        "              <l7:PolicyId>" + PBS_POLICY_ID + "</l7:PolicyId>\n" +
        "              <l7:OperationName>run</l7:OperationName>\n" +
        "            </l7:PolicyBackedServiceOperation>\n" +
        "          </l7:PolicyBackedServiceOperations>\n" +
        "        </l7:PolicyBackedService>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>" + SCHEDULED_TASK_NAME + "</l7:Name>\n" +
        "      <l7:Id>" + SCHEDULED_TASK_ID + "</l7:Id>\n" +
        "      <l7:Type>SCHEDULED_TASK</l7:Type>\n" +
        "      <l7:TimeStamp>2016-01-04T17:15:07.227-08:00</l7:TimeStamp>\n" +
        "      <l7:Link rel=\"self\" uri=\"https://127.0.0.1:8443/restman/1.0/scheduledTasks/" + SCHEDULED_TASK_ID + "\"/>\n" +
        "      <l7:Resource>\n" +
        "        <l7:ScheduledTask id=\"" + SCHEDULED_TASK_ID + "\" version=\"0\">\n" +
        "          <l7:Name>" + SCHEDULED_TASK_NAME + "</l7:Name>\n" +
        "          <l7:PolicyReference id=\"" + PBS_POLICY_ID + "\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
        "          <l7:OneNode>false</l7:OneNode>\n" +
        "          <l7:JobType>One time</l7:JobType>\n" +
        "          <l7:JobStatus>Scheduled</l7:JobStatus>\n" +
        "          <l7:ExecutionDate>2016-01-24T16:13:36-08:00</l7:ExecutionDate>\n" +
        "          <l7:ExecuteOnCreate>false</l7:ExecuteOnCreate>\n" +
        "          <l7:Properties/>\n" +
        "        </l7:ScheduledTask>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>\n" +
        "    <l7:Item>\n" +
        "      <l7:Name>SSGConnectorForInstanceModifierDemo</l7:Name>\n" +
        "      <l7:Id>f7370df418628b0f050789e387987e2d</l7:Id>\n" +
        "      <l7:Type>SSG_CONNECTOR</l7:Type>\n" +
        "      <l7:Resource>\n" +
        "        <l7:ListenPort id=\"f7370df418628b0f050789e387987e2d\">\n" +
        "          <l7:Name>MAS Messaging</l7:Name>\n" +
        "          <l7:Enabled>true</l7:Enabled>\n" +
        "          <l7:Protocol>MQTT-TLS</l7:Protocol>\n" +
        "          <l7:Port>8883</l7:Port>\n" +
        "          <l7:EnabledFeatures>\n" +
        "            <l7:StringValue>Published service message input</l7:StringValue>\n" +
        "          </l7:EnabledFeatures>\n" +
        "          <l7:TargetServiceReference id=\"f7370df418628b0f050789e387987de5\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/services\"/>\n" +
        "          <l7:TlsSettings>\n" +
        "            <l7:ClientAuthentication>Required</l7:ClientAuthentication>\n" +
        "            <l7:EnabledVersions>\n" +
        "              <l7:StringValue>TLSv1</l7:StringValue>\n" +
        "              <l7:StringValue>TLSv1.1</l7:StringValue>\n" +
        "              <l7:StringValue>TLSv1.2</l7:StringValue>\n" +
        "            </l7:EnabledVersions>\n" +
        "          </l7:TlsSettings>\n" +
        "          <l7:Properties>\n" +
        "            <l7:Property key=\"l7.mqtt.idleTimeout\">\n" +
        "              <l7:StringValue>300</l7:StringValue>\n" +
        "            </l7:Property>\n" +
        "            <l7:Property key=\"l7.mqtt.maxClients\">\n" +
        "              <l7:StringValue>0</l7:StringValue>\n" +
        "            </l7:Property>\n" +
        "            <l7:Property key=\"useExtendedFtpCommandSet\">\n" +
        "              <l7:StringValue>false</l7:StringValue>\n" +
        "            </l7:Property>\n" +
        "          </l7:Properties>\n" +
        "        </l7:ListenPort>\n" +
        "      </l7:Resource>\n" +
        "    </l7:Item>" +
        "  </l7:References>\n" +
        "  <l7:Mappings>\n" +
        "    <l7:Mapping action=\"NewOrExisting\" srcId=\"" + FOLDER_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/folders/" + FOLDER_ID + "\" type=\"FOLDER\">\n" +
        "      <l7:Properties>\n" +
        "        <l7:Property key=\"FailOnNew\">\n" +
        "          <l7:BooleanValue>true</l7:BooleanValue>\n" +
        "        </l7:Property>\n" +
        "      </l7:Properties>\n" +
        "    </l7:Mapping>\n" +
        "    <l7:Mapping action=\"NewOrExisting\" srcId=\"" + PBIP_POLICY_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/policies/" + PBIP_POLICY_ID + "\" type=\"POLICY\"/>\n" +
        "    <l7:Mapping action=\"NewOrExisting\" srcId=\"" + PBS_POLICY_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/policies/" + PBS_POLICY_ID + "\" type=\"POLICY\"/>\n" +
        "    <l7:Mapping action=\"NewOrExisting\" srcId=\"" + ENCAPSULATED_ASSERTION_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/encapsulatedAssertions/" + ENCAPSULATED_ASSERTION_ID + "\" type=\"ENCAPSULATED_ASSERTION\"/>\n" +
        "    <l7:Mapping action=\"NewOrExisting\" srcId=\"" + ENCAP_POLICY_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/policies/" + ENCAP_POLICY_ID + "\" type=\"POLICY\"/>\n" +
        "    <l7:Mapping action=\"NewOrExisting\" srcId=\"" + POLICY_BACKED_IDENTITY_PROVIDER_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/identityProviders/" + POLICY_BACKED_IDENTITY_PROVIDER_ID + "\" type=\"ID_PROVIDER_CONFIG\"/>\n" +
        "    <l7:Mapping action=\"AlwaysCreateNew\" srcId=\"" + SERVICE_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/services/" + SERVICE_ID + "\" type=\"SERVICE\"/>\n" +
        "    <l7:Mapping action=\"NewOrExisting\" srcId=\"" + POLICY_BACKED_SERVICE_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/policyBackedServices/" + POLICY_BACKED_SERVICE_ID + "\" type=\"POLICY_BACKED_SERVICE\"/>\n" +
        "    <l7:Mapping action=\"NewOrUpdate\" srcId=\"" + SCHEDULED_TASK_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/scheduledTasks/" + SCHEDULED_TASK_ID + "\" type=\"SCHEDULED_TASK\">\n" +
        "      <l7:Properties>\n" +
        "        <l7:Property key=\"FailOnNew\">\n" +
        "          <l7:BooleanValue>true</l7:BooleanValue>\n" +
        "        </l7:Property>\n" +
        "      </l7:Properties>\n" +
        "    </l7:Mapping>\n" +
        "    <l7:Mapping action=\"NewOrUpdate\" srcId=\"" + SSG_CONNECTOR_ID + "\" srcUri=\"https://127.0.0.1:8443/restman/1.0/listenPorts/" + SSG_CONNECTOR_ID + "\" type=\"SSG_CONNECTOR\"/>\n" +
        "  </l7:Mappings>\n" +
        "</l7:Bundle>";
}