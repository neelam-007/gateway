package com.l7tech.gateway.common.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.objectmodel.EntityHeader;
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
import java.util.*;

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
    public static final String TAG_NAME_L7_ID = "l7:Id";
    public static final String TAG_NAME_L7_GUID = "l7:Guid";
    public static final String TAG_NAME_L7_NAME = "l7:Name";
    public static final String TAG_NAME_L7_TYPE = "l7:Type";
    public static final String TAG_NAME_L7_IP_TYPE = "l7:IdentityProviderType";
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
        final List<String> pbidList = new ArrayList<>(); // An id list of policy-backed identity providers
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
                                    return getPrefixedDefaultForEntityName(version, name);
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
                                    return getPrefixedDefaultForEntityName(version, name);
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
                                    return getPrefixedDefaultForEntityName(version, name);
                                }
                            });
                            break;
                        case POLICY_BACKED_SERVICE:
                            applyModifierToDescendants(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedDefaultForEntityName(version, name);
                                }
                            });
                            break;
                        case SSG_CONNECTOR:
                            applyModifierToDescendants(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                @Override
                                public String call(String version, String name) {
                                    return getPrefixedDefaultForEntityName(version, name);
                                }
                            });
                            break;
                        case ID_PROVIDER_CONFIG:
                            if (IdentityProviderType.POLICY_BACKED.description().equals(getIdentityProviderType(item))) {
                                final NodeList idNodeList = item.getElementsByTagName(TAG_NAME_L7_ID);
                                if (idNodeList.getLength() > 0) {
                                    node = idNodeList.item(0);
                                    pbidList.add(node.getTextContent());
                                }

                                applyModifierToDescendants(item, TAG_NAME_L7_NAME, new Functions.Binary<String, String, String>() {
                                    @Override
                                    public String call(String version, String name) {
                                        return getPrefixedDefaultForEntityName(version, name);
                                    }
                                });
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        // modify mappings
        Mapping.Action action;
        String srcId;
        for (Element item : bundleMappings) {
            entityTypeStr = item.getAttribute(ATTRIBUTE_NAME_TYPE);
            actionStr = item.getAttribute(ATTRIBUTE_NAME_ACTION);
            srcId = item.getAttribute(ATTRIBUTE_NAME_SRC_ID);
            if (StringUtils.isNotEmpty(entityTypeStr) && StringUtils.isNotEmpty(actionStr)) {
                action = Mapping.Action.valueOf(actionStr);
                if (action == Mapping.Action.AlwaysCreateNew
                        || (action == Mapping.Action.NewOrExisting && !isFailOnNewMapping(item))
                        || (action == Mapping.Action.NewOrUpdate && !isFailOnNewMapping(item))) {
                    if (isModifiableType(entityTypeStr, (pbidList.contains(srcId)? IdentityProviderType.POLICY_BACKED.description(): null))) {
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

    /**
     * Check if an entity is modifiable (i.e., non-shareable).
     *
     * @param entityTypeStr: the string of an entity type
     * @param subEntityTypeStr: the string of sub-type of an entity.  This parameter is only applied to identity providers so far.
     * @return true if the entity is a folder, service, encapsulated assertion, scheduled task, policy-backed server,
     *         or policy-backed identity provider.  Otherwise, return false.
     */
    public static boolean isModifiableType(@Nullable final String entityTypeStr, @Nullable final String subEntityTypeStr) {
        if (isEmpty(entityTypeStr)) {
            return false;
        } else {
            // TODO (tveninov): seems that these types must match ALL cases in the switch statement in apply() above.
            // TODO (tveninov): if that's the case then replace this TODO with IMPORTANT/NOTE.

            final EntityType entityType = valueOf(entityTypeStr);

            // If it is an identity provider, check subEntityTypeStr.
            // If it is a policy-backed identity provider, return true.  If it is other types of identity providers, return false.
            if (ID_PROVIDER_CONFIG == entityType) {
                return IdentityProviderType.POLICY_BACKED.description().equals(subEntityTypeStr);
            }

            return FOLDER == entityType || POLICY == entityType || ENCAPSULATED_ASSERTION == entityType || SERVICE == entityType || entityType == SCHEDULED_TASK || entityType == POLICY_BACKED_SERVICE || entityType == SSG_CONNECTOR;
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

    public static String getPrefixedDefaultForEntityName(@Nullable String versionModifier, @NotNull String entityName) {
        return isValidVersionModifier(versionModifier) ? versionModifier + " " + entityName : entityName;
    }

    public static String getPrefixedUrl(@Nullable String versionModifier, @NotNull String urlPattern) {
        return isValidVersionModifier(versionModifier) ? "/" + versionModifier + urlPattern : urlPattern;
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

            if (entityType == EntityType.FOLDER || entityType == EntityType.ENCAPSULATED_ASSERTION || entityType == EntityType.SCHEDULED_TASK  || entityType == EntityType.SSG_CONNECTOR) {
                // The format of a folder name is "<folder_name> <instance_modifier>".
                // The format of a encapsulated assertion name is "<instance_modifier> <encapsulated_assertion_name>".
                // The format of a scheduled task name is "<instance_modifier> <scheduled_task_name>".
                // The max length of a folder name, an encapsulated assertion name, a scheduled task name, or an SSG Connector name is 128.
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

    public static boolean isSame(final String im1, final String im2) {
        return
                (StringUtils.isBlank(im1) && StringUtils.isBlank(im2)) ||
                        (im1 != null && im1.equals(im2));
    }

    /**
     * Check if the instance modifier of a selected solution kit is unique or not.
     *
     * @param solutionKit: a solution kit whose instance modifier will be checked.
     * @param usedInstanceModifiersMap: a map of solution kit guid and a list of instance modifiers used by all solution kits with such guid.
     * @return true if the instance modifier is unique.  That is, the instance modifier is not used by other solution kit instances.
     */
    public static boolean isUnique(@NotNull final SolutionKit solutionKit, @NotNull final Map<String, List<String>> usedInstanceModifiersMap) {
        final String solutionKitGuid = solutionKit.getSolutionKitGuid();
        if (usedInstanceModifiersMap.keySet().contains(solutionKitGuid)) {
            final List<String> usedInstanceModifiers = usedInstanceModifiersMap.get(solutionKitGuid);
            final String newInstanceModifier = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);

            if (usedInstanceModifiers != null && usedInstanceModifiers.contains(newInstanceModifier)) {
                return false;
            }
        }

        return true;
    }

    public static String getDisplayName(@Nullable final String instanceModifier) {
        return StringUtils.isBlank(instanceModifier)? "N/A" : instanceModifier;
    }

    /**
     * Find all instance modifiers used by all solution kit instances.
     *
     * @param solutionKitHeaders: solution kit headers
     * @return a map of solution kit guid and a list of instance modifiers used by all solution kits with such guid.
     */
    public static Map<String, List<String>> getInstanceModifiers(@NotNull final Collection<SolutionKitHeader> solutionKitHeaders) {
        final Map<String, List<String>> instanceModifiers = new HashMap<>();

        for (EntityHeader header: solutionKitHeaders) {
            if (! (header instanceof SolutionKitHeader)) continue;  // This line is to avoid to break the test, signedSkar() in SolutionKitManagerResourceTest.

            SolutionKitHeader solutionKitHeader = (SolutionKitHeader) header;
            String solutionKitGuid = solutionKitHeader.getSolutionKitGuid();
            java.util.List<String> usedInstanceModifiers = instanceModifiers.get(solutionKitGuid);
            if (usedInstanceModifiers == null) {
                usedInstanceModifiers = new ArrayList<>();
            }
            usedInstanceModifiers.add(solutionKitHeader.getInstanceModifier());
            instanceModifiers.put(solutionKitGuid, usedInstanceModifiers);
        }

        return instanceModifiers;
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

    private static String getIdentityProviderType(@NotNull final Element item) {
        final NodeList nodeList = item.getElementsByTagName(TAG_NAME_L7_IP_TYPE);

        // Get identity provider type
        if (nodeList.getLength() > 0) {
            final Node node = nodeList.item(0);
            final String type = node.getTextContent();

            return StringUtils.isBlank(type)? null : type.trim();
        }

        return null;
    }

}
