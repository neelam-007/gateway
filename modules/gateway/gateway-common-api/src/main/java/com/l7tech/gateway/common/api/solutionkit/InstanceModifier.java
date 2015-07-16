package com.l7tech.gateway.common.api.solutionkit;

import com.l7tech.gateway.api.Item;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.Functions;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.URLDecoder;
import java.util.List;

import static com.l7tech.objectmodel.EntityType.*;

/**
 * Apply instance modifier to entities of a restman migration bundle.
 * // TODO some methods don't have unit tests
 */
public class InstanceModifier {
    public static final String TAG_NAME_L7_NAME = "l7:Name";
    public static final String TAG_NAME_L7_TYPE = "l7:Type";
    public static final String TAG_NAME_L7_URL_PATTERN = "l7:UrlPattern";

    private final List<Element> bundleReferenceItems;
    private final String instanceModifier;

    public InstanceModifier(@NotNull final List<Element> bundleReferenceItems, @Nullable final String instanceModifier) {
        this.bundleReferenceItems = bundleReferenceItems;
        this.instanceModifier = instanceModifier;
    }

    public void apply() {
        String entityTypeStr;
        Node node;
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

                        // new supported entity type should also be added isModifiableType()

                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * Calculate what the max length of instance modifier could be.
     * The value dynamically depends on given names of folder, service, policy, and encapsulated assertion.
     *
     * @return the minimum allowed length among folder name, service name, policy name, encapsulated assertion name combining with instance modifier.
     */
    public static int getMaxLengthForInstanceModifier(@NotNull final List<Item> bundleReferenceItems) {
        int maxAllowedLengthAllow = Integer.MAX_VALUE;
        int allowedLength;
        String entityName;
        EntityType entityType;

        for (Item item: bundleReferenceItems) {
            entityName = item.getName();
            entityType = EntityType.valueOf(item.getType());

            if (entityType == EntityType.FOLDER || entityType == EntityType.ENCAPSULATED_ASSERTION) {
                // The format of a folder name is "<folder_name> <instance_modifier>".
                // The format of a encapsulated assertion name is "<instance_modifier> <encapsulated_assertion_name>".
                // The max length of a folder name or an encapsulated assertion name is 128.
                allowedLength = 128 - entityName.length() - 1; // 1 represents one char of white space.
            } else if (entityType == EntityType.POLICY) {
                // The format of a policy name is "<instance_modifier> <policy_name>".
                // The max length of a policy name is 255.
                allowedLength = 255 - entityName.length() - 1; // 1 represents one char of white space.
            } else if (entityType == EntityType.SERVICE) {
                // The max length of a service routing uri is 128
                // The format of a service routing uri is "/<instance_modifier>/<service_name>".
                allowedLength = 128 - entityName.length() - 2; // 2 represents two chars of '/' in the routing uri.
            }  else {
                continue;
            }

            if (maxAllowedLengthAllow > allowedLength) {
                maxAllowedLengthAllow = allowedLength;
            }
        }

        if (maxAllowedLengthAllow < 0) maxAllowedLengthAllow = 0;

        return maxAllowedLengthAllow;
    }

    public static boolean isModifiableType(@Nullable final String entityTypeStr) {
        if (StringUtils.isEmpty(entityTypeStr)) {
            return false;
        } else {
            final EntityType entityType = valueOf(entityTypeStr);
            return FOLDER == entityType || POLICY == entityType || ENCAPSULATED_ASSERTION == entityType || SERVICE == entityType ;
        }
    }

    public static String getSuffixedFolderName(@Nullable String instanceModifier, @NotNull String folderName) {
        return isValidVersionModifier(instanceModifier) ? folderName + " " + instanceModifier : folderName;
    }

    public static String getPrefixedPolicyName(@Nullable String instanceModifier, @NotNull String policyName) {
        return isValidVersionModifier(instanceModifier) ? instanceModifier + " " + policyName : policyName;
    }

    public static String getPrefixedUrl(@Nullable String instanceModifier, @NotNull String urlPattern) {
        return isValidVersionModifier(instanceModifier) ? "/" + instanceModifier + urlPattern : urlPattern;
    }

    public static String getPrefixedEncapsulatedAssertionName(@Nullable String instanceModifier, @NotNull String encapsulatedAssertionName) {
        return isValidVersionModifier(instanceModifier) ? instanceModifier + " " + encapsulatedAssertionName : encapsulatedAssertionName;
    }

    public static boolean isValidVersionModifier(@Nullable String instanceModifier) {
        return instanceModifier != null && !instanceModifier.isEmpty();
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

    private void applyVersionToDescendantsByTagName(Element item, String tagName, Functions.Binary<String, String, String> callback) {
        NodeList nodeList = item.getElementsByTagName(tagName);
        Node node;
        for (int i = 0; i < nodeList.getLength(); i++) {
            node = nodeList.item(i);
            node.setTextContent(callback.call(instanceModifier, node.getTextContent()));
        }
    }
}