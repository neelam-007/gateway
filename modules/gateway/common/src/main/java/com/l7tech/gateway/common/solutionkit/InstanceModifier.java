package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.solutionkit.SolutionKitManagerContext;
import com.l7tech.policy.solutionkit.SolutionKitManagerUi;
import com.l7tech.util.*;
import com.l7tech.xml.xpath.XpathUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.l7tech.objectmodel.EntityType.*;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Apply instance modifier to entities of a restman migration bundle.
 */
public class InstanceModifier {
    public static final String ATTRIBUTE_NAME_ACTION = "action";
    public static final String ATTRIBUTE_NAME_GUID = "guid";
    public static final String ATTRIBUTE_NAME_SRC_ID = "srcId";
    public static final String ATTRIBUTE_NAME_STRING_VALUE = "stringValue";
    public static final String ATTRIBUTE_NAME_TARGET_ID = "targetId";
    public static final String ATTRIBUTE_NAME_TYPE = "type";
    public static final String TAG_NAME_L7_GUID = "l7:Guid";
    public static final String TAG_NAME_L7_NAME = "l7:Name";
    public static final String TAG_NAME_L7_TYPE = "l7:Type";
    public static final String TAG_NAME_L7_URL_PATTERN = "l7:UrlPattern";
    public static final String TAG_NAME_L7P_ENCASS_CONFIG_GUID = "L7p:EncapsulatedAssertionConfigGuid";
    public static final String TAG_NAME_L7P_POLICY_GUID = "L7p:PolicyGuid";

    private static final Map<String, String> nsMap = CollectionUtils.MapBuilder.<String, String>builder()
            .put("l7", "http://ns.l7tech.com/2010/04/gateway-management")
            .unmodifiableMap();

    public static Map<String, String> getNamespaceMap() {
        return nsMap;
    }

    private final List<Element> bundleReferenceItems;
    private List<Element> bundleMappings;
    private final String versionModifier;

    public InstanceModifier(@NotNull final List<Element> bundleReferenceItems, @NotNull final List<Element> bundleMappings, @Nullable final String versionModifier) {
        this.bundleReferenceItems = bundleReferenceItems;
        this.bundleMappings = bundleMappings;
        this.versionModifier = versionModifier;
    }

    public void apply() {

        // modify items
        String entityTypeStr, actionStr;
        Node node;
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
                            applyModifierToDescendants(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getSuffixedFolderName(version, name);
                                }
                            });
                            break;
                        case POLICY:
                            applyModifierToDescendants(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedPolicyName(version, name);
                                }
                            });
                            applyModifierToDescendants(item, ".//l7:Resource/l7:Policy", ATTRIBUTE_NAME_GUID, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String guid) {
                                    return getModifiedGuid(version, guid);
                                }
                            });
                            applyModifierToDescendants(item, ".//l7:Resource/l7:Policy/l7:PolicyDetail", ATTRIBUTE_NAME_GUID, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String guid) {
                                    return getModifiedGuid(version, guid);
                                }
                            });

                            modifyResourceGuidReferences(item, ".//l7:Resource/l7:Policy/l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]");

                            break;
                        case ENCAPSULATED_ASSERTION:
                            applyModifierToDescendants(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedEncapsulatedAssertionName(version, name);
                                }
                            });
                            applyModifierToDescendants(item, TAG_NAME_L7_GUID, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String guid) {
                                    return getModifiedGuid(version, guid);
                                }
                            });
                            break;
                        case SERVICE:
                            applyModifierToDescendants(item, TAG_NAME_L7_URL_PATTERN, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedUrl(version, name);
                                }
                            });

                            modifyResourceGuidReferences(item, ".//l7:Resource/l7:Service/l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]");

                            /* TODO need restman to detect service url conflict checking; see SSG-9579
                                otherwise we'll need to manually look up each url here e.g.
                                    - serviceManager.findByRoutingUri(url).isEmpty()
                                    - use dryRunEvent.addServiceConflict(<service name and conflict URL pattern>) */
                            break;
                        case SCHEDULED_TASK:
                            applyModifierToDescendants(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedScheduledTaskName(version, name);
                                }
                            });
                            break;
                        case POLICY_BACKED_SERVICE:
                            applyModifierToDescendants(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
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

        // modify mappings
        Mapping.Action action;
        for (Element item : bundleMappings) {
            entityTypeStr = item.getAttribute(ATTRIBUTE_NAME_TYPE);
            actionStr = item.getAttribute(ATTRIBUTE_NAME_ACTION);
            if (StringUtils.isNotEmpty(entityTypeStr) && StringUtils.isNotEmpty(actionStr)) {
                action = Mapping.Action.valueOf(actionStr);
                if (action == Mapping.Action.AlwaysCreateNew || (action == Mapping.Action.NewOrExisting && !isFailOnNewMapping(item))) {
                    if (isModifiableType(entityTypeStr)) {
                        // deterministically set targetId for the version modified entity
                        item.setAttribute(ATTRIBUTE_NAME_TARGET_ID, getModifiedGoid(versionModifier, item.getAttribute(ATTRIBUTE_NAME_SRC_ID)));
                    }
                }
            }
        }
    }

    /**
     * Deterministically version modify the GOID by getting the first 128 bits (16 bytes) of SHA-256( version_modifier + ":" + original_goid ).
     */
    public static String getModifiedGoid(@Nullable String versionModifier, @NotNull final String goid) {
        if (isEmpty(versionModifier)) {
            return goid;
        } else {
            byte[] hash = HexUtils.getSha256Digest((versionModifier + ":" + goid).getBytes(Charsets.UTF8));
            return new Goid(Arrays.copyOf(hash, 16)).toString();
        }
    }

    public static String getModifiedGuid(@Nullable String versionModifier, @NotNull final String guid) {
        // save the dashes (-) in the right place e.g. 506589b0-eba5-4b3f-81b5-be7809817623
        // policy guid requires 36 characters, otherwise an error like this "Invalid Value: guid size must be between 36 and 36"
        String modifiedGoid = getModifiedGoid(versionModifier, guid);
        return modifiedGoid.substring(0, 8) + "-" + modifiedGoid.substring(8, 12) + "-" + modifiedGoid.substring(12, 16) + "-" + modifiedGoid.substring(16, 20) + "-" + modifiedGoid.substring(20, 32);
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

    // format: <l7:Properties><l7:Property key="FailOnNew"><l7:BooleanValue>true</l7:BooleanValue></l7:Property></l7:Properties>
    public static boolean isFailOnNewMapping(@NotNull final Element item) {
        final List<Element> failOnNewBooleanValues = XpathUtil.findElements(item, ".//l7:Properties/l7:Property[@key=\"FailOnNew\"]/l7:BooleanValue", getNamespaceMap());

        if (failOnNewBooleanValues.size() < 1) {
            return false;
        }

        final Element failOnNewBooleanValue = failOnNewBooleanValues.get(0);
        return failOnNewBooleanValue != null && Boolean.valueOf(failOnNewBooleanValue.getTextContent());
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

    /**
     * Add instance modifier to the customizations (custom context).
     */
    public static void setCustomContext(@NotNull final SolutionKitsConfig settings, @NotNull final SolutionKit selectedSolutionKit) {
        final Map<String, Pair<SolutionKit, SolutionKitCustomization>> customizations = settings.getCustomizations();
        final Pair<SolutionKit, SolutionKitCustomization> customization = customizations.get(selectedSolutionKit.getSolutionKitGuid());
        if (customization != null && customization.right != null) {
            final SolutionKitManagerUi customUi = customization.right.getCustomUi();
            if (customUi != null) {
                // test for null b/c implementer can optionally null the context
                SolutionKitManagerContext skContext = customUi.getContext();
                if (skContext != null) {
                    skContext.setInstanceModifier(selectedSolutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));
                }
            }
        }
    }

    // apply modifier to the text content of descendants matching a tag name
    private void applyModifierToDescendants(Element item, String tagName, Functions.Binary<String, String, String> callback) {
        NodeList nodeList = item.getElementsByTagName(tagName);
        Node node;
        for (int i = 0; i < nodeList.getLength(); i++) {
            node = nodeList.item(i);
            node.setTextContent(callback.call(versionModifier, node.getTextContent()));
        }
    }

    // apply modifier to the attribute value of descendants matching the xpath and attribute name
    private void applyModifierToDescendants(Element item, String xpath, String attributeName, Functions.Binary<String, String, String> callback) {
        final List<Element> elements = XpathUtil.findElements(item, xpath, getNamespaceMap());
        for (Element element : elements) {
            element.setAttribute(attributeName, callback.call(versionModifier, element.getAttribute(attributeName)));
        }
    }

    // instance modify guid references in the resource
    private void modifyResourceGuidReferences(Element item, String xpath) {
        final List<Element> textContentResources = XpathUtil.findElements(item, xpath, getNamespaceMap());
        for (Element textContentResource : textContentResources) {
            try {
                // convert text content into DOM element for traversal
                Element resource = XmlUtil.stringToDocument(textContentResource.getTextContent()).getDocumentElement();

                // modify encass guid references (e.g. <L7p:EncapsulatedAssertionConfigGuid stringValue="506589b0-eba5-4b3f-81b5-be7809817623"/>)
                NodeList nodeList = resource.getElementsByTagName(TAG_NAME_L7P_ENCASS_CONFIG_GUID);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
                    element.setAttribute(ATTRIBUTE_NAME_STRING_VALUE, getModifiedGuid(versionModifier, element.getAttribute(ATTRIBUTE_NAME_STRING_VALUE)));
                }

                // modify policy guid references (e.g. <L7p:PolicyGuid stringValue="cfa49381-54aa-4231-b72e-6d483a032bf7"/>)
                nodeList = resource.getElementsByTagName(TAG_NAME_L7P_POLICY_GUID);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element element = (Element) nodeList.item(i);
                    element.setAttribute(ATTRIBUTE_NAME_STRING_VALUE, getModifiedGuid(versionModifier, element.getAttribute(ATTRIBUTE_NAME_STRING_VALUE)));
                }

                // write back changes to the resource
                DomUtils.setTextContent(textContentResource, XmlUtil.nodeToString(resource));
            } catch (SAXException | IOException e) {
                throw new RuntimeException("Unexpected error processing instance modifier; error serializing or de-serializing XML: " + e.getMessage(), e);
            }
        }
    }
}
