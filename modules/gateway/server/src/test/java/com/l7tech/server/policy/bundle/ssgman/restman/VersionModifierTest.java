package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.util.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;

import static com.l7tech.server.policy.bundle.ssgman.restman.VersionModifier.*;
import static com.l7tech.objectmodel.EntityType.valueOf;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

/**
 *  Test version modifiers for different entities via restman migration bundle message.
 */
public class VersionModifierTest {
    private final String validRequestXml;

    public VersionModifierTest() throws IOException {
        byte[] bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/server/policy/bundle/bundles/RestmanBundle1/MigrationBundle1.0.xml"));
        validRequestXml = new String(bytes, RestmanInvoker.UTF_8);
    }

    @Test
    public void versionModifiedEntityAttributes() {
        String name = "Simple Folder";
        Assert.assertEquals(name, getSuffixedFolderName(null, name));
        Assert.assertEquals(name, getSuffixedFolderName("", name));
        Assert.assertEquals("Simple Folder Suffixed", getSuffixedFolderName("Suffixed", name));

        name = "Simple Policy";
        Assert.assertEquals(name, getPrefixedPolicyName(null, name));
        Assert.assertEquals(name, getPrefixedPolicyName("", name));
        Assert.assertEquals("Prefixed Simple Policy", getPrefixedPolicyName("Prefixed", name));

        name = "Simple Encapsulated Assertion";
        Assert.assertEquals(name, getPrefixedEncapsulatedAssertionName(null, name));
        Assert.assertEquals(name, getPrefixedEncapsulatedAssertionName("", name));
        Assert.assertEquals("Prefixed Simple Encapsulated Assertion", getPrefixedEncapsulatedAssertionName("Prefixed", name));

        String resolutionUrl = "/query";
        Assert.assertEquals(resolutionUrl, getPrefixedUrl(null, resolutionUrl));
        Assert.assertEquals(resolutionUrl, getPrefixedUrl("", resolutionUrl));
        Assert.assertEquals("/v1/query", getPrefixedUrl("v1", resolutionUrl));
    }

    @Test
    public void versionModifiedMessage() throws Exception {
        final RestmanMessage requestMessage = new RestmanMessage(validRequestXml);
        final String versionModifier = "v1";

        // apply version modifier to message
        new VersionModifier(requestMessage, versionModifier).apply();

        // check all bundle reference items
        String entityType, entityName, urlPattern;
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
                case POLICY: case ENCAPSULATED_ASSERTION:
                    // prefix name
                    assertThat(entityName, startsWith(versionModifier));
                    break;
                case SERVICE:
                    // prefix service url
                    urlPatternNodes = item.getElementsByTagName(TAG_NAME_L7_URL_PATTERN);
                    assertThat(urlPatternNodes.getLength(), greaterThan(0));
                    urlPattern = urlPatternNodes.item(0).getTextContent();
                    assertThat(urlPattern, startsWith("/" + versionModifier));
                    break;
                default:
                    // no prefix nor suffix on name
                    assertThat(entityName, not(startsWith(versionModifier)));
                    assertThat(entityName, not(endsWith(versionModifier)));
                    break;
            }
        }
    }
}