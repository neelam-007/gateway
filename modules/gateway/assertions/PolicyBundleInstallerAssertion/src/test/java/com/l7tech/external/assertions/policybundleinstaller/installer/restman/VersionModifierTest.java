package com.l7tech.external.assertions.policybundleinstaller.installer.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstallerTestBase;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.util.IOUtils;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.IOException;

import static com.l7tech.external.assertions.policybundleinstaller.installer.restman.VersionModifier.*;
import static com.l7tech.objectmodel.EntityType.valueOf;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *  Test version modifiers for different entities via restman migration bundle message.
 */
public class VersionModifierTest extends PolicyBundleInstallerTestBase {
    private final String validRequestXml;

    public VersionModifierTest() throws IOException {
        byte[] bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/server/policy/bundle/bundles/RestmanBundle1/MigrationBundle1.0.xml"));
        validRequestXml = new String(bytes, RestmanInvoker.UTF_8);
    }

    @Test
    public void versionModifiedEntityAttributes() {
        String name = "Simple Folder";
        assertEquals(name, MigrationBundleInstaller.getSuffixedFolderName(null, name));
        assertEquals(name, MigrationBundleInstaller.getSuffixedFolderName("", name));
        assertEquals("Simple Folder Suffixed", MigrationBundleInstaller.getSuffixedFolderName("Suffixed", name));

        name = "Simple Policy";
        assertEquals(name, MigrationBundleInstaller.getPrefixedPolicyName(null, name));
        assertEquals(name, MigrationBundleInstaller.getPrefixedPolicyName("", name));
        assertEquals("Prefixed Simple Policy", MigrationBundleInstaller.getPrefixedPolicyName("Prefixed", name));

        name = "Simple Encapsulated Assertion";
        assertEquals(name, MigrationBundleInstaller.getPrefixedEncapsulatedAssertionName(null, name));
        assertEquals(name, MigrationBundleInstaller.getPrefixedEncapsulatedAssertionName("", name));
        assertEquals("Prefixed Simple Encapsulated Assertion", MigrationBundleInstaller.getPrefixedEncapsulatedAssertionName("Prefixed", name));

        String resolutionUrl = "/query";
        assertEquals(resolutionUrl, MigrationBundleInstaller.getPrefixedUrl(null, resolutionUrl));
        assertEquals(resolutionUrl, MigrationBundleInstaller.getPrefixedUrl("", resolutionUrl));
        assertEquals("/v1/query", MigrationBundleInstaller.getPrefixedUrl("v1", resolutionUrl));
    }

    @Test
    public void versionModifiedMessage() throws Exception {
        final RestmanMessage requestMessage = new RestmanMessage(XmlUtil.stringToDocument(validRequestXml));
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
