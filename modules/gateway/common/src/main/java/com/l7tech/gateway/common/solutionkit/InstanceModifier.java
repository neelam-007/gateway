package com.l7tech.gateway.common.solutionkit;

import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Charsets;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import static com.l7tech.objectmodel.EntityType.*;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Apply instance modifier to entities of a restman migration bundle.
 */
public class InstanceModifier {
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

    public InstanceModifier(@NotNull final List<Element> bundleReferenceItems, @NotNull final List<Element> bundleMappings, @Nullable final String versionModifier) {
        this.bundleReferenceItems = bundleReferenceItems;
        this.bundleMappings = bundleMappings;
        this.versionModifier = versionModifier;
    }

    public void apply() {
        String entityTypeStr, actionStr;
        Node node;

        for (Element item : bundleMappings) {
            entityTypeStr = item.getAttribute(ATTRIBUTE_NAME_TYPE);
            actionStr = item.getAttribute(ATTRIBUTE_NAME_ACTION);
            if (StringUtils.isNotEmpty(entityTypeStr) && StringUtils.isNotEmpty(actionStr)) {
                if (Mapping.Action.valueOf(actionStr) == Mapping.Action.AlwaysCreateNew) {
                    if (isModifiableType(entityTypeStr)) {
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

                    // IMPORTANT cases in switch statement must match ALL the entity types in isModifiableType() below

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

            // IMPORTANT these types must match ALL cases in the switch statement in apply() above

            return FOLDER == entityType || POLICY == entityType || ENCAPSULATED_ASSERTION == entityType || SERVICE == entityType || entityType == SCHEDULED_TASK || entityType == POLICY_BACKED_SERVICE;
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

    /**
     * Validate if an instance modifier is a valid part of a URI.
     *
     * @return null if the instance modifier is valid.  Otherwise, a string explaining invalid reason.
     */
    public static String validatePrefixedURI(@NotNull String instanceModifier) {
        // Service Routing URI must not start with '/ssg'
        if (instanceModifier.startsWith("ssg")) {
            return "Instance modifier must not start with 'ssg', since Service Routing URI must not start with '/ssg'";
        }

        // validate for XML chars and new line char
        String [] invalidChars = new String[]{"\"", "&", "'", "<", ">", "\n"};
        for (String invalidChar : invalidChars) {
            if (instanceModifier.contains(invalidChar)) {
                if (invalidChar.equals("\n")) invalidChar = "\\n";
                return "Invalid character '" + invalidChar + "' is not allowed in the installation prefix.";
            }
        }

        String testUri = "http://ssg.com:8080/" + instanceModifier + "/query";
        if (!ValidationUtils.isValidUrl(testUri)) {
            return "Invalid prefix '" + instanceModifier + "'. It must be possible to construct a valid routing URI using the prefix.";
        }

        try {
            URLDecoder.decode(instanceModifier, "UTF-8");
        } catch (Exception e) {
            return "Invalid prefix '" + instanceModifier + "'. It must be possible to construct a valid routing URL using the prefix.";
        }

        return null;
    }

    /**
     * Calculate the max allowed instance modifier length for this bundle.  The value is calculated dynamically; depending on shortest length of
     * all reference item names (e.g. shortest allowed name of folder, service, policy, encapsulated assertion, etc) and instance modifier length.
     *
     * @param bundleReferenceItems reference items in the bundle (e.g. folder, service, policy, encapsulated assertion, etc)
     * @return the max allowed length
     */
    public static int getMaxAllowedLength(@NotNull final List<Item> bundleReferenceItems) {
        int maxAllowedLength = Integer.MAX_VALUE;
        int allowedLength;
        String entityName;
        EntityType entityType;

        for (Item item: bundleReferenceItems) {
            entityName = item.getName();
            entityType = EntityType.valueOf(item.getType());

            if (entityType == EntityType.FOLDER || entityType == EntityType.ENCAPSULATED_ASSERTION || entityType == EntityType.SCHEDULED_TASK) {
                // The format of a folder name is "<folder_name> <instance_modifier>".
                // The format of a encapsulated assertion name is "<instance_modifier> <encapsulated_assertion_name>".
                // The format of a scheduled task name is "<instance_modifier> <scheduled_task_name>".
                // The max length of a folder name, an encapsulated assertion name, or a scheduled task name is 128.
                allowedLength = 128 - entityName.length() - 1; // 1 represents one char of white space.
            } else if (entityType == EntityType.POLICY || entityType == EntityType.POLICY_BACKED_SERVICE) {
                // The format of a policy name is "<instance_modifier> <policy_name>".
                // The format of a policy backed service name is "<instance_modifier> <policy_backed_service_name>".
                // The max length of a policy name or a policy backed service name is 255.
                allowedLength = 255 - entityName.length() - 1; // 1 represents one char of white space.
            } else if (entityType == EntityType.SERVICE) {
                // The format of a service routing uri is "/<instance_modifier>/<service_name>".
                // The max length of a service routing uri is 128
                allowedLength = 128 - entityName.length() - 2; // 2 represents two chars of '/' in the routing uri.
            } else {
                continue;
            }

            if (maxAllowedLength > allowedLength) {
                maxAllowedLength = allowedLength;
            }
        }

        if (maxAllowedLength < 0) maxAllowedLength = 0;

        return maxAllowedLength;
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
