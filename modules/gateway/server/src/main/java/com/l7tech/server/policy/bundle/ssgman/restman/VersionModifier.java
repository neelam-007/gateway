package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static com.l7tech.objectmodel.EntityType.*;
import static com.l7tech.server.bundling.EntityMappingInstructions.MappingAction;
import static com.l7tech.server.bundling.EntityMappingInstructions.MappingAction.AlwaysCreateNew;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Apply version modifier to entities of a restman migration bundle.
 */
public class VersionModifier {
    public static final String TAG_NAME_L7_NAME = "l7:Name";
    public static final String TAG_NAME_L7_TYPE = "l7:Type";
    public static final String TAG_NAME_L7_URL_PATTERN = "l7:UrlPattern";
    public static final String ATTRIBUTE_NAME_SRC_ID = "srcId";
    public static final String ATTRIBUTE_NAME_TARGET_ID = "targetId";
    public static final String ATTRIBUTE_NAME_TYPE = "type";
    public static final String ATTRIBUTE_NAME_ACTION = "action";

    private final List<Element> bundleReferenceItems;
    private List<Element> bundleMappings;
    private final String versionModifier;

    public VersionModifier(@NotNull final List<Element> bundleReferenceItems, @NotNull final List<Element> bundleMappings, @Nullable final String versionModifier) {
        this.bundleReferenceItems = bundleReferenceItems;
        this.bundleMappings = bundleMappings;
        this.versionModifier = versionModifier;
    }

    public VersionModifier(@NotNull final RestmanMessage restmanMessage, @Nullable final String versionModifier) {
        this(restmanMessage.getBundleReferenceItems(), restmanMessage.getMappings(), versionModifier);
    }

    public void apply() {
        String entityTypeStr, actionStr;
        EntityType entityType;
        Node node;

        for (Element item : bundleMappings) {
            entityTypeStr = item.getAttribute(ATTRIBUTE_NAME_TYPE);
            actionStr = item.getAttribute(ATTRIBUTE_NAME_ACTION);
            if (StringUtils.isNotEmpty(entityTypeStr) && StringUtils.isNotEmpty(actionStr)) {
                if (MappingAction.valueOf(actionStr) == AlwaysCreateNew) {
                    entityType = EntityType.valueOf(entityTypeStr);

                    // these entity types must include ALL cases in the switch statement below
                    if (entityType == FOLDER || entityType == POLICY || entityType == ENCAPSULATED_ASSERTION ||
                            entityType == SERVICE || entityType == SCHEDULED_TASK || entityType == POLICY_BACKED_SERVICE) {

                        // deterministically set targetId for the version modified entity
                        item.setAttribute(ATTRIBUTE_NAME_TARGET_ID, getVersionModifiedGoid(versionModifier, item.getAttribute(ATTRIBUTE_NAME_SRC_ID)));
                    }
                }
            }
        }

        for (Element item : bundleReferenceItems) {
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
                        case SCHEDULED_TASK:
                            applyVersionToDescendantsByTagName(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedScheduledTaskName(version, name);
                                }
                            });
                            break;
                        case POLICY_BACKED_SERVICE:
                            applyVersionToDescendantsByTagName(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedPolicyBackedServiceName(version, name);
                                }
                            });
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * Deterministically version modify the GOID by getting the first 128 bits (16 bytes) of SHA-256( version_modifier + ":" + original_goid ).
     */
    public static String getVersionModifiedGoid(@Nullable String versionModifier, @NotNull final String goid) {
        if (isEmpty(versionModifier)) {
            return goid;
        } else {
            byte[] hash = HexUtils.getSha256Digest((versionModifier + ":" + goid).getBytes(Charsets.UTF8));
            return new Goid(Arrays.copyOf(hash, 16)).toString();
        }
    }

    public static boolean isModifiableType(@Nullable final String entityTypeStr) {
        if (isEmpty(entityTypeStr)) {
            return false;
        } else {
            final EntityType entityType = valueOf(entityTypeStr);
            return FOLDER == entityType || POLICY == entityType || ENCAPSULATED_ASSERTION == entityType || SERVICE == entityType ;
        }
    }

    public static String getSuffixedFolderName(@Nullable String versionModifier, @NotNull String folderName) {
        return isValidVersionModifier(versionModifier) ? folderName + " " + versionModifier : folderName;
    }

    public static String getPrefixedPolicyName(@Nullable String versionModifier, @NotNull String policyName) {
        return isValidVersionModifier(versionModifier) ? versionModifier + " " + policyName : policyName;
    }

    public static String getPrefixedUrl(@Nullable String versionModifier, @NotNull String urlPattern) {
        return isValidVersionModifier(versionModifier) ? "/" + versionModifier + urlPattern : urlPattern;
    }

    public static String getPrefixedEncapsulatedAssertionName(@Nullable String versionModifier, @NotNull String encapsulatedAssertionName) {
        return isValidVersionModifier(versionModifier) ? versionModifier + " " + encapsulatedAssertionName : encapsulatedAssertionName;
    }

    public static String getPrefixedScheduledTaskName(@Nullable String versionModifier, @NotNull String scheduledTaskName) {
        return isValidVersionModifier(versionModifier) ? versionModifier + " " + scheduledTaskName : scheduledTaskName;
    }

    public static String getPrefixedPolicyBackedServiceName(@Nullable String versionModifier, @NotNull String policyBackedServiceName) {
        return isValidVersionModifier(versionModifier) ? versionModifier + " " + policyBackedServiceName : policyBackedServiceName;
    }

    public static boolean isValidVersionModifier(@Nullable String versionModifier) {
        return versionModifier != null && !versionModifier.isEmpty();
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