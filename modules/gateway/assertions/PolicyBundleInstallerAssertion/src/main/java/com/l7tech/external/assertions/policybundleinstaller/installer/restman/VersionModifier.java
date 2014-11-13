package com.l7tech.external.assertions.policybundleinstaller.installer.restman;

import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static com.l7tech.external.assertions.policybundleinstaller.installer.restman.MigrationBundleInstaller.*;
import static com.l7tech.objectmodel.EntityType.valueOf;

/**
 * Apply version modifier to entities of a restman migration bundle.
 */
public class VersionModifier {
    protected static final String TAG_NAME_L7_NAME = "l7:Name";
    protected static final String TAG_NAME_L7_TYPE = "l7:Type";
    protected static final String TAG_NAME_L7_URL_PATTERN = "l7:UrlPattern";

    private final RestmanMessage restmanMessage;
    private final String versionModifier;

    public VersionModifier(@NotNull RestmanMessage restmanMessage, String versionModifier) {
        this.restmanMessage = restmanMessage;
        this.versionModifier = versionModifier;
    }

    public void apply() {
        String entityTypeStr;
        Node node;
        for (Element item : restmanMessage.getBundleReferenceItems()) {
            final NodeList nodeList = item.getElementsByTagName(TAG_NAME_L7_TYPE);

            // get entity type
            if (nodeList.getLength() > 0) {
                node = nodeList.item(0);
                entityTypeStr = node.getTextContent();

                // apply version modifier to these entities
                if (!StringUtils.isEmpty(entityTypeStr)) {
                    switch (valueOf(entityTypeStr)) {
                        case FOLDER:
                            applyVersionToDescendantsByTagName(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getSuffixedFolderName(version, name);
                                }
                            });
                            break;
                        case POLICY:
                            applyVersionToDescendantsByTagName(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedPolicyName(version, name);
                                }
                            });
                            break;
                        case ENCAPSULATED_ASSERTION:
                            applyVersionToDescendantsByTagName(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedEncapsulatedAssertionName(version, name);
                                }
                            });
                            break;
                        case SERVICE:
                            applyVersionToDescendantsByTagName(item, TAG_NAME_L7_URL_PATTERN, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedUrl(version, name);
                                }
                            });
                            /* TODO need restman to detect service url conflict checking; see SSG-9579
                                otherwise we'll need to manually look up each url here e.g.
                                    - serviceManager.findByRoutingUri(url).isEmpty()
                                    - use dryRunEvent.addServiceConflict(<service name and conflict URL pattern>) */
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void applyVersionToDescendantsByTagName(Element item, String tagName, Functions.Binary<String, String, String> callback) {
        NodeList nodeList = item.getElementsByTagName(tagName);
        Node node;
        for (int i = 0; i < nodeList.getLength(); i++) {
            node = nodeList.item(i);
            node.setTextContent(callback.call(versionModifier, node.getTextContent()));
        }
    }
}
