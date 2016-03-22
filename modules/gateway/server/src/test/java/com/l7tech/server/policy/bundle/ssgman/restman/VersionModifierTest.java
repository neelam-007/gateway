package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.IOUtils;
import com.l7tech.xml.xpath.XpathUtil;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.api.Mapping.Action.AlwaysCreateNew;
import static com.l7tech.objectmodel.EntityType.*;
import static com.l7tech.server.policy.bundle.ssgman.restman.VersionModifier.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 *  Test version modifiers for different entities via restman migration bundle message.
 */
public class VersionModifierTest {
    private final String validRequestXml;

    public VersionModifierTest() throws IOException {
        byte[] bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/server/policy/bundle/bundles/RestmanBundle1/MigrationBundle1.1.xml"));
        validRequestXml = new String(bytes, RestmanInvoker.UTF_8);
    }

    @Test
    public void versionModifiedEntityAttributes() {
        String name = "Simple Folder";
        Assert.assertEquals(name, getSuffixedFolderName(null, name));
        Assert.assertEquals(name, getSuffixedFolderName("", name));
        Assert.assertEquals("Simple Folder Suffixed", getSuffixedFolderName("Suffixed", name));

        name = "Simple Policy";
        Assert.assertEquals(name, getPrefixedDefaultForEntityName(null, name));
        Assert.assertEquals(name, getPrefixedDefaultForEntityName("", name));
        Assert.assertEquals("Prefixed Simple Policy", getPrefixedDefaultForEntityName("Prefixed", name));

        name = "Simple Encapsulated Assertion";
        Assert.assertEquals(name, getPrefixedDefaultForEntityName(null, name));
        Assert.assertEquals(name, getPrefixedDefaultForEntityName("", name));
        Assert.assertEquals("Prefixed Simple Encapsulated Assertion", getPrefixedDefaultForEntityName("Prefixed", name));

        String resolutionUrl = "/query";
        Assert.assertEquals(resolutionUrl, getPrefixedUrl(null, resolutionUrl));
        Assert.assertEquals(resolutionUrl, getPrefixedUrl("", resolutionUrl));
        Assert.assertEquals("/v1/query", getPrefixedUrl("v1", resolutionUrl));

        name = "Simple Scheduled Task";
        Assert.assertEquals(name, getPrefixedDefaultForEntityName(null, name));
        Assert.assertEquals(name, getPrefixedDefaultForEntityName("", name));
        Assert.assertEquals("Prefixed Simple Scheduled Task", getPrefixedDefaultForEntityName("Prefixed", name));

        name = "Simple Policy Backed Service";
        Assert.assertEquals(name, getPrefixedDefaultForEntityName(null, name));
        Assert.assertEquals(name, getPrefixedDefaultForEntityName("", name));
        Assert.assertEquals("Prefixed Simple Policy Backed Service", getPrefixedDefaultForEntityName("Prefixed", name));
    }

    @Test
    public void versionModifiedMessage() throws Exception {
        final RestmanMessage requestMessage = new RestmanMessage(validRequestXml);
        final String versionModifier = "v1";

        // expected these guid modifications from the test bundle (e.g. MigrationBundle1.1.xml)
        // to build this list, put a breakpoint in InstanceModifier.getModifiedGuid()
        final Map<String, String> modifiedToOriginalGuidMap = new HashMap<>(6);
        modifiedToOriginalGuidMap.put("40bbe9d5-e19d-4e91-88a7-52cb6afa73c8", "75062052-9f23-4be2-b7fc-c3caad51620d");
        modifiedToOriginalGuidMap.put("3356fa64-6671-0a17-8c04-6b18d569d6c5", "e9aaee50-21cf-4b3f-bc11-bbbb1711e265");
        modifiedToOriginalGuidMap.put("e07adf85-5560-6c61-4567-96787f0674a9", "cfa49381-54aa-4231-b72e-6d483a032bf7");
        modifiedToOriginalGuidMap.put("f1596c37-480b-bce9-754f-f4c8966336db", "4c8e03fa-3554-40ed-b67a-de5d86f36d7e");
        modifiedToOriginalGuidMap.put("d20d7ea1-1959-18b1-97a8-cd15cca7be87", "506589b0-eba5-4b3f-81b5-be7809817623");
        modifiedToOriginalGuidMap.put("5bca09c5-6622-f83a-2b77-465a88ba6618", "e9f40c8f-6f76-4a74-803d-187afc91e28d");

        // apply version modifier to message
        new VersionModifier(requestMessage, versionModifier).apply();

        // check version modified entity has generated goid set for targetId
        for (Element item : requestMessage.getMappings()) {
            EntityType entityType = EntityType.valueOf(item.getAttribute(ATTRIBUTE_NAME_TYPE));
            Mapping.Action action = Mapping.Action.valueOf(item.getAttribute(ATTRIBUTE_NAME_ACTION));

            if (action == AlwaysCreateNew || (action == Mapping.Action.NewOrExisting && !VersionModifier.isFailOnNewMapping(item))) {
                // list each type as sanity check for VersionModifier.isModifiableType()
                if (entityType == FOLDER || entityType == POLICY || entityType == ENCAPSULATED_ASSERTION ||
                        entityType == SERVICE || entityType == SCHEDULED_TASK || entityType == POLICY_BACKED_SERVICE) {
                    assertEquals(item.getAttribute(ATTRIBUTE_NAME_TARGET_ID), getModifiedGoid(versionModifier, item.getAttribute(ATTRIBUTE_NAME_SRC_ID)));
                } else {
                    assertEquals("", item.getAttribute(ATTRIBUTE_NAME_TARGET_ID));
                }
            } else {
                assertEquals("", item.getAttribute(ATTRIBUTE_NAME_TARGET_ID));
            }
        }

        // check all bundle reference items
        String entityType, entityName, urlPattern, guid;
        NodeList entityTypeNodes, entityNameNodes, urlPatternNodes;
        for (Element item : requestMessage.getBundleReferenceItems()) {
            // get entity type
            entityTypeNodes = item.getElementsByTagName(TAG_NAME_L7_TYPE);
            assertThat(entityTypeNodes.getLength(), greaterThan(0));
            entityType = entityTypeNodes.item(0).getTextContent();

            // get entity name
            entityNameNodes = item.getElementsByTagName(TAG_NAME_L7_NAME);
            assertThat(entityNameNodes.getLength(), greaterThan(0));
            entityName = entityNameNodes.item(0).getTextContent();

            switch (valueOf(entityType)) {
                case FOLDER:
                    // suffix name
                    assertThat(entityName, endsWith(versionModifier));
                    break;
                case POLICY:
                    // prefix name
                    assertThat(entityName, startsWith(versionModifier));

                    // modify guid
                    guid = XpathUtil.findElements(item, ".//l7:Resource/l7:Policy", getNamespaceMap()).get(0).getAttribute(ATTRIBUTE_NAME_GUID);
                    assertEquals(guid, getModifiedGuid(versionModifier, modifiedToOriginalGuidMap.get(guid)));
                    guid = XpathUtil.findElements(item, ".//l7:Resource/l7:Policy/l7:PolicyDetail", getNamespaceMap()).get(0).getAttribute(ATTRIBUTE_NAME_GUID);
                    assertEquals(guid, getModifiedGuid(versionModifier, modifiedToOriginalGuidMap.get(guid)));

                    // modify guid references
                    assertGuidReferences(versionModifier, modifiedToOriginalGuidMap, item, ".//l7:Resource/l7:Policy/l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]");

                    break;
                case ENCAPSULATED_ASSERTION:
                    // prefix name
                    assertThat(entityName, startsWith(versionModifier));

                    // modify guid
                    guid = item.getElementsByTagName(TAG_NAME_L7_GUID).item(0).getTextContent();
                    assertEquals(guid, getModifiedGuid(versionModifier, modifiedToOriginalGuidMap.get(guid)));

                    break;
                case SERVICE:
                    // prefix service url
                    urlPatternNodes = item.getElementsByTagName(TAG_NAME_L7_URL_PATTERN);
                    assertThat(urlPatternNodes.getLength(), greaterThan(0));
                    urlPattern = urlPatternNodes.item(0).getTextContent();
                    assertThat(urlPattern, startsWith("/" + versionModifier));

                    // modify guid references
                    assertGuidReferences(versionModifier, modifiedToOriginalGuidMap, item, ".//l7:Resource/l7:Service/l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]");

                    break;
                default:
                    // no prefix nor suffix on name
                    assertThat(entityName, not(startsWith(versionModifier)));
                    assertThat(entityName, not(endsWith(versionModifier)));
                    break;
            }
        }
    }

    private void assertGuidReferences(String versionModifier, Map<String, String> modifiedToOriginalGuidMap, Element item, String xpath) throws SAXException {
        final List<Element> textContentResources = XpathUtil.findElements(item, xpath, getNamespaceMap());
        for (Element textContentResource : textContentResources) {
            // convert text content into DOM element for traversal
            Element resource = XmlUtil.stringToDocument(textContentResource.getTextContent()).getDocumentElement();

            // encass guid references (e.g. <L7p:EncapsulatedAssertionConfigGuid stringValue="506589b0-eba5-4b3f-81b5-be7809817623"/>)
            NodeList nodeList = resource.getElementsByTagName(TAG_NAME_L7P_ENCASS_CONFIG_GUID);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                String guid = element.getAttribute(ATTRIBUTE_NAME_STRING_VALUE);
                assertEquals(guid, getModifiedGuid(versionModifier, modifiedToOriginalGuidMap.get(guid)));
            }

            // policy guid references (e.g. <L7p:PolicyGuid stringValue="cfa49381-54aa-4231-b72e-6d483a032bf7"/>)
            nodeList = resource.getElementsByTagName(TAG_NAME_L7P_POLICY_GUID);
            for (int i = 0; i < nodeList.getLength(); i++) {
                Element element = (Element) nodeList.item(i);
                String guid = element.getAttribute(ATTRIBUTE_NAME_STRING_VALUE);
                assertEquals(guid, getModifiedGuid(versionModifier, modifiedToOriginalGuidMap.get(guid)));
            }
        }
    }
}