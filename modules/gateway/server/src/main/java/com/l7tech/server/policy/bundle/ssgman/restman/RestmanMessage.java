package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.util.*;
import com.l7tech.xml.xpath.XpathUtil;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.MGMT_VERSION_NAMESPACE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getNamespaceMap;

/**
 * Represents a restman request or response messages with methods to extract and manipulate the message.
 */
public class RestmanMessage {
    public static final String MAPPING_ACTION_PROP_KEY_FAIL_ON_EXISTING = "FailOnExisting";
    public static final String MAPPING_ACTION_PROP_KEY_FAIL_ON_NEW = "FailOnNew";
    public static final String MAPPING_ACTION_ATTRIBUTE = "action";
    public static final String MAPPING_ACTION_TAKEN_ATTRIBUTE = "actionTaken";
    public static final String MAPPING_ERROR_TYPE_ATTRIBUTE = "errorType";
    public static final String MAPPING_TYPE_ATTRIBUTE = "type";
    public static final String MAPPING_SRC_ID_ATTRIBUTE = "srcId";
    public static final String MAPPING_TARGET_ID_ATTRIBUTE = "targetId";

    private static final String NS_L7 = "l7";
    private static final String NODE_NAME_L7_ERROR = NS_L7 + ":Error";
    public static final String NODE_NAME_NAME = "Name";
    public static final String NODE_NAME_ID = "Id";
    public static final String NODE_NAME_PROPERTIES = "Properties";
    public static final String NODE_NAME_PROPERTY = "Property";
    private static final String NODE_NAME_BOOLEAN_VALUE = "BooleanValue";
    public static final String NODE_NAME_STRING_VALUE = "StringValue";
    private static final String XMLNS_L7 = "xmlns:" + NS_L7;
    public static final String NODE_ATTRIBUTE_NAME_KEY = "key";
    private static final String ROOT_FOLDER_ID = Folder.ROOT_FOLDER_ID.toHexString();

    private static final String ROOT_FOLDER_REPLACEMENT_MAPPING_TEMPLATE =
        "<l7:Mapping xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" action=\"NewOrExisting\" srcId=\"0000000000000000ffffffffffffec76\" srcUri=\"/1.0/folders/0000000000000000ffffffffffffec76\" targetId=\"{0}\" type=\"FOLDER\">\n" +
        "     <l7:Properties><l7:Property key=\"FailOnNew\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property></l7:Properties>\n" +
        "</l7:Mapping>\n";

    private static final String MAPPING_PROPERTY_NAME_SK_READ_ONLY_ENTITY = "SK_ReadOnlyEntity";
    public static final String MAPPING_PROPERTY_NAME_SK_SAVED_ENTITY_NAME = "SK_SavedEntityName";

    private List<Element> mappingErrors;
    private List<Element> bundles;
    private List<Element> resourceSetPolicies;
    private List<Element> mappings;
    private List<Element> bundleReferenceItems;

    final Document document;

    public RestmanMessage(final Document document) {
        this.document = document;
    }

    public RestmanMessage(final Message message) throws IOException, NoSuchPartException, SAXException {
        this.document = XmlUtil.parse(message.getMimeKnob().getEntireMessageBodyAsInputStream());
    }

    public RestmanMessage(final String xml) throws SAXException {
        this.document = XmlUtil.stringToDocument(xml);
    }

    /**
     * Check if message is error response (e.g. bad Restman request).
     */
    public boolean isErrorResponse() {
        return NODE_NAME_L7_ERROR.equals(document.getDocumentElement().getNodeName());
    }

    /**
     * Check whether message has Restman mapping error (e.g. syntactically correct request, but invalid entity).
     */
    public boolean hasMappingError() {
        if (mappingErrors == null) {
            loadMappingErrors();
        }

        return mappingErrors.size() > 0;
    }

    /**
     * Get Restman mapping error(s) as a string.
     */
    public String getMappingErrorsAsString() throws IOException {
        if (mappingErrors == null) {
            loadMappingErrors();
        }
        final StringBuilder sb = new StringBuilder();

        for (Element mappingError : mappingErrors) {
            sb.append(mappingError.getAttribute("errorType"));
            sb.append(": type=");
            sb.append(mappingError.getAttribute("type"));
            sb.append(", srcId=");
            sb.append(mappingError.getAttribute("srcId"));
            sb.append(", ");
            for (Element mapping: XpathUtil.findElements(mappingError, "l7:Properties/l7:Property/l7:StringValue", getNamespaceMap())) {
                sb.append(DomUtils.getTextValue(mapping));
            }
            sb.append(System.getProperty("line.separator"));
        }

        return sb.toString();
    }

    /**
     * Get more user-friendly Restman mapping error messages.
     */
    public String getAllMappingErrorsDetail(Functions.Unary<String, String> getEntityName) throws IOException {
        if (mappingErrors == null) {
            loadMappingErrors();
        }

        final StringBuilder sb = new StringBuilder();
        for (Element mappingError : mappingErrors) {
            sb.append(getSingleMappingErrorDetail(mappingError, getEntityName.call(mappingError.getAttribute("srcId"))));
        }

        return sb.toString();
    }

    public static String getSingleMappingErrorDetail(Element mappingError, String entityName) {
        final StringBuilder sb = new StringBuilder();
        String errorType = mappingError.getAttribute("errorType");
        String srcId = mappingError.getAttribute("srcId");

        sb.append(errorType);
        sb.append(": type=").append(mappingError.getAttribute("type"));
        sb.append(", name=").append(entityName);
        sb.append(", ");
        if ("UniqueKeyConflict".equals(errorType)) {
            sb.append("already exists");
        } else {
            sb.append("srcId=").append(srcId);
            sb.append(", ");

            StringBuilder errorMessage = new StringBuilder();
            for (Element mapping: XpathUtil.findElements(mappingError, "l7:Properties/l7:Property/l7:StringValue", getNamespaceMap())) {
                if (errorMessage.length() > 0) errorMessage.append(" ");
                errorMessage.append(DomUtils.getTextValue(mapping));
            }

            if ("InvalidResource".equals(errorType)) {
                String particularErrorMessage = "Cannot add or update a child row: a foreign key constraint fails";
                if (errorMessage.toString().startsWith(particularErrorMessage)) {
                    sb.append(particularErrorMessage).append(" in database");
                } else {
                    sb.append(errorMessage.toString());
                }
            } else {
                sb.append(errorMessage.toString());
            }
        }
        sb.append(System.getProperty("line.separator"));

        return sb.toString();
    }

    /**
     * Get Restman mapping error(s) as list of Elements.
     */
    public List<Element> getMappingErrors() throws IOException {
        if (mappingErrors == null) {
            loadMappingErrors();
        }

        return mappingErrors;
    }

    /**
     * Get Restman mapping(s) as list of Elements.
     */
    @NotNull
    public List<Element> getMappings() {
        if (mappings == null || mappings.isEmpty()) {
            loadMappings();
        }

        return mappings;
    }

    public String getAsFormattedString() throws IOException {
        return XmlUtil.nodeToFormattedString(document);
    }

    public String getAsString() throws IOException {
        return XmlUtil.nodeToString(document);
    }

    public String getBundleXml() throws IOException {
        if (bundles == null) {
            loadBundles();
        }

        final StringBuilder sb = new StringBuilder();
        for (Element bundle : bundles) {
            setL7XmlNs(bundle);
            sb.append(XmlUtil.nodeToFormattedString(bundle));
        }
        return sb.toString();
    }

    public List<Element> getBundleReferenceItems() {
        if (bundleReferenceItems == null) {
            loadBundleReferenceItems();
        }
        return bundleReferenceItems;
    }

    public boolean hasRootFolderItem() {
        final List<Element> rootNodeIds = XpathUtil.findElements(document.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item[l7:Id='" + ROOT_FOLDER_ID + "']//l7:Id", getNamespaceMap());
        return rootNodeIds.size() == 1;
    }

    public boolean hasRootFolderMapping() {
        final List<Element> rootNodeMappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:Mappings/l7:Mapping[@srcId='" + ROOT_FOLDER_ID + "']", getNamespaceMap());
        return rootNodeMappings.size() == 1;
    }

    public static Element setL7XmlNs(@NotNull final Element element) {
        element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLNS_L7, GatewayManagementDocumentUtilities.getNamespaceMap().get(NS_L7));
        return element;
    }

    public List<String> getResourceSetPolicies() throws IOException {
        if (resourceSetPolicies == null) {
            loadResourceSetPolicies();
        }

        final List<String> result = new ArrayList<>(resourceSetPolicies.size());
        for (Element resourceSetPolicy : resourceSetPolicies) {
            result.add(resourceSetPolicy.getFirstChild().getTextContent());
        }

        return result;
    }

    public List<Element> getResourceSetPolicyElements() {
        if (resourceSetPolicies == null) {
            loadResourceSetPolicies();
        }

        return resourceSetPolicies;
    }

    /**
     * Get the policy resource set from either a Policy or Service entity.
     */
    public String getResourceSetPolicy(@NotNull final String id) throws IOException {
        final List<Element> policies = XpathUtil.findElements(document.getDocumentElement(),
                "//l7:Bundle/l7:References/l7:Item[l7:Id='" + id + "']/l7:Resource/child::*/l7:Resources/l7:ResourceSet/l7:Resource",
                getNamespaceMap());

        if (policies.size() > 0) {
            return DomUtils.getTextValue(policies.get(0)).trim();
        } else {
            return null;
        }
    }

    public String getEntityName(@NotNull final String id) {
        final List<Element> names = XpathUtil.findElements(document.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item[l7:Id='" + id + "']/l7:Name", getNamespaceMap());

        if (names.size() > 0) {
            String entityName = DomUtils.getTextValue(names.get(0));
            if (StringUtils.isEmpty(entityName))
                return "N/A";
            else
                return entityName.trim();
        } else {
            return "N/A";
        }
    }

    public String getEntityType(@NotNull final String srcId) {
        final List<Element> srcIdMappings = XpathUtil.findElements(document.getDocumentElement(), "//l7:Bundle/l7:Mappings/l7:Mapping[@srcId=\"" + srcId + "\"]", getNamespaceMap());

        // There should only be one action mapping per scrId  in a restman message
        if (srcIdMappings.size() > 0) {
            String entityType = srcIdMappings.get(0).getAttribute("type");
            if (StringUtils.isEmpty(entityType))
                return null;
            else
                return entityType.trim();
        } else {
            return null;
        }
    }

    public String getServiceUrl(@NotNull final String serviceId) {
        final List<Element> urlPatterns = XpathUtil.findElements(document.getDocumentElement(), "//l7:Bundle/l7:References/l7:Item[l7:Id='" + serviceId + "']/descendant::l7:UrlPattern", getNamespaceMap());

        // There should only be one action mapping per scrId  in a restman message
        if (urlPatterns.size() > 0) {
            String urlStr = urlPatterns.get(0).getTextContent();
            if (StringUtils.isBlank(urlStr))
                return null;
            else
                return urlStr.trim();
        } else {
            return null;
        }
    }

    /**
     * Get a set of Folder mappings with "FailOnNew" property with value of "true". The set contains the entity srcUri.
     */
    public Set<String> getMappingsFailOnNewFolders() throws IOException, TooManyChildElementsException, MissingRequiredElementException {
        if (mappings == null) {
            loadMappings();
        }

        Set<String> result = new HashSet<>();
        for (Element mappingEle : mappings) {
            if (!ROOT_FOLDER_ID.equals(mappingEle.getAttribute("srcId")) &&
                "FOLDER".equals(mappingEle.getAttribute("type")) &&
                "NewOrExisting".equals(mappingEle.getAttribute("action"))) {
                List<Element> propertiesEleList = DomUtils.findChildElementsByName(mappingEle, MGMT_VERSION_NAMESPACE, NODE_NAME_PROPERTIES);
                if (propertiesEleList.size() == 0) {
                    continue;
                }

                Element propertiesEle = DomUtils.findExactlyOneChildElementByName(mappingEle, MGMT_VERSION_NAMESPACE, NODE_NAME_PROPERTIES);
                for (Element propertyEle : DomUtils.findChildElementsByName(propertiesEle, MGMT_VERSION_NAMESPACE, "Property")) {
                    if ("FailOnNew".equals(propertyEle.getAttribute("key"))) {
                        Element booleanValueEl = DomUtils.findExactlyOneChildElementByName(propertyEle, MGMT_VERSION_NAMESPACE, "BooleanValue");
                        if ("true".equals(booleanValueEl.getTextContent())) {
                            result.add(mappingEle.getAttribute("srcUri"));
                            break;
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Remove Folder mapping property with "FailOnNew" property with value of "true" and add the Folder Item.
     */
    public void replaceFailOnNewFolderMapping(final String srcUri, final RestmanMessage itemMessage) throws TooManyChildElementsException, MissingRequiredElementException {
        if (mappings == null) {
            loadMappings();
        }

        // 1. Find mapping with matching srcUri
        //
        Element foundMappingEle = null;
        for (Element mappingEle : mappings) {
            if (srcUri.equals(mappingEle.getAttribute("srcUri"))) {
                foundMappingEle = mappingEle;
                break;
            }
        }

        if (foundMappingEle == null) {
            throw new RuntimeException("Folder mapping not found.");
        }

        // 2. Remove Property Element with key "FailOnNew".
        //
        Element propertiesEle = DomUtils.findExactlyOneChildElementByName(foundMappingEle, MGMT_VERSION_NAMESPACE, NODE_NAME_PROPERTIES);
        for (Element propertyEle : DomUtils.findChildElementsByName(propertiesEle, MGMT_VERSION_NAMESPACE, "Property")) {
            if ("FailOnNew".equals(propertyEle.getAttribute("key"))) {
                Element booleanValueEl = DomUtils.findExactlyOneChildElementByName(propertyEle, MGMT_VERSION_NAMESPACE, "BooleanValue");
                if ("true".equals(booleanValueEl.getTextContent())) {
                    propertiesEle.removeChild(propertyEle);
                    break;
                }
            }
        }

        // 3. Remove Link Elements from item message.
        //
        Element itemEle = XpathUtil.findElements(itemMessage.document.getDocumentElement(), "/l7:Item", getNamespaceMap()).get(0);
        DomUtils.removeChildElementsByName(itemEle, MGMT_VERSION_NAMESPACE, "Link");

        // 4. Add itemMessage to this object.
        //
        Element bundleEle = XpathUtil.findElements(document.getDocumentElement(), "/l7:Item/l7:Resource/l7:Bundle", getNamespaceMap()).get(0);
        Element referencesEle = DomUtils.findExactlyOneChildElementByName(bundleEle, MGMT_VERSION_NAMESPACE, "References");

        Node itemNodeToImport = document.importNode(itemEle, true);
        referencesEle.insertBefore(itemNodeToImport, referencesEle.getFirstChild());

        // 5. Reset bundles.
        //
        bundles = null;
    }

    /**
     * Given an entity id, set it's mapping action (and any properties).
     */
    public void setMappingAction(@NotNull final String srcId, @NotNull final EntityMappingInstructions.MappingAction action, @Nullable Properties properties) {
        final List<Element> srcIdMappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:Mappings/l7:Mapping[@srcId=\"" + srcId + "\"]", getNamespaceMap());

        // there should only be one action mapping per id (guid) in a restman message
        if (srcIdMappings.size() > 0) {
            Element srcIdMapping = srcIdMappings.get(0);
            srcIdMapping.setAttribute(MAPPING_ACTION_ATTRIBUTE, action.toString());

            if (properties != null) {
                // there should only be one Properties element in a restman message
                Element propertiesElement = DomUtils.findFirstChildElementByName(srcIdMapping, MGMT_VERSION_NAMESPACE, NODE_NAME_PROPERTIES);
                if (propertiesElement == null) {
                    propertiesElement = DomUtils.createAndAppendElement(srcIdMapping, "Properties");
                }

                for (Map.Entry<Object,Object> property  : properties.entrySet()) {
                    if (MAPPING_TARGET_ID_ATTRIBUTE.equals(property.getKey())) {
                        srcIdMapping.setAttribute(MAPPING_TARGET_ID_ATTRIBUTE, property.getValue() == null ? null : property.getValue().toString());
                    } else {
                        final Element propertyElement = DomUtils.createAndAppendElement(propertiesElement, NODE_NAME_PROPERTY);
                        propertyElement.setAttribute(NODE_ATTRIBUTE_NAME_KEY, property.getKey() == null ? null : property.getKey().toString());
                        final Element propertyValueElement = DomUtils.createAndAppendElement(propertyElement, NODE_NAME_BOOLEAN_VALUE);
                        propertyValueElement.setTextContent(property.getValue() == null ? null : property.getValue().toString());
                    }
                }
            }
        }
    }

    public void setRootFolderMappingTargetId(String targetFolderId) {
        if (ROOT_FOLDER_ID.equals(targetFolderId)) return;

        final List<Element> rootFolderMappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:Mappings/l7:Mapping[@srcId=\"" + ROOT_FOLDER_ID + "\"]", getNamespaceMap());

        // there should only be one action mapping per id in a restman message
        if (rootFolderMappings.size() > 0) {
            Element rootFolderMapping = rootFolderMappings.get(0);
            rootFolderMapping.setAttribute(MAPPING_TARGET_ID_ATTRIBUTE, targetFolderId);
        }
    }

    public void addRootFolderMapping(String targetFolderId) {
        if (ROOT_FOLDER_ID.equals(targetFolderId)) return;

        final String mappingStr = MessageFormat.format(ROOT_FOLDER_REPLACEMENT_MAPPING_TEMPLATE, targetFolderId);

        Element newMappingElement;
        try {
            newMappingElement = XmlUtil.stringToDocument(mappingStr).getDocumentElement();
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
        newMappingElement = (Element) document.importNode(newMappingElement,  true);

        final List<Element> mappingsElements = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:Mappings", getNamespaceMap());
        if (mappingsElements.size() != 1) return;

        final Element mappingsElement = mappingsElements.get(0);
        final Element firstMappingElement = DomUtils.findFirstChildElement(mappingsElement);

        mappingsElement.insertBefore(newMappingElement, firstMappingElement);
    }

    /**
     * Get an entity's mapping action and properties.
     */
    public Pair<EntityMappingInstructions.MappingAction, Properties> getMappingAction(@NotNull final String srcId) {
        Pair<EntityMappingInstructions.MappingAction, Properties> result = null;

        final List<Element> srcIdMappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:Mappings/l7:Mapping[@srcId=\"" + srcId + "\"]", getNamespaceMap());
        if (srcIdMappings.size() > 0) {
            Element srcIdMapping = srcIdMappings.get(0);
            Element propertiesElement = DomUtils.findFirstChildElementByName(srcIdMapping, MGMT_VERSION_NAMESPACE, NODE_NAME_PROPERTIES);
            if (propertiesElement == null) {
                result = new Pair<>(EntityMappingInstructions.MappingAction.valueOf(srcIdMapping.getAttribute(MAPPING_ACTION_ATTRIBUTE)), null);
            } else {
                final Properties resultProperties = new Properties();
                for (Element property : DomUtils.findChildElementsByName(propertiesElement, MGMT_VERSION_NAMESPACE, NODE_NAME_PROPERTY)) {
                    resultProperties.put(property.getAttribute(NODE_ATTRIBUTE_NAME_KEY), DomUtils.getTextValue(DomUtils.findFirstChildElement(property)));
                }
                result = new Pair<>(EntityMappingInstructions.MappingAction.valueOf(srcIdMapping.getAttribute(MAPPING_ACTION_ATTRIBUTE)), resultProperties);
            }
        }

        return result;
    }

    public void removeMappingByEntityType(@NotNull final String toBeRemovedEntityType) {
        final List<Element> mappingsElements = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:Mappings", getNamespaceMap());
        if (mappingsElements.size() != 1) return;  // If more than one or less than one, ignore them.

        final Element mappingsElement = mappingsElements.get(0);
        final List<Element> toBeRemovedMappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:Mappings/l7:Mapping[@type=\"" + toBeRemovedEntityType + "\"]", getNamespaceMap());

        for (Element tobeRemoved: toBeRemovedMappings) {
            mappingsElement.removeChild(tobeRemoved);
        }
    }

    /**
     * Checks if the specified entity mapping is marked as a read-only entity.<br/>
     * Checks whether {@link #MAPPING_PROPERTY_NAME_SK_READ_ONLY_ENTITY} property is present and has {@code BooleanValue} of {@code true}.
     */
    public static boolean isMarkedAsReadOnly(@NotNull final Element mapping) {
        final List<Element> readOnlyEntityBooleanValues = XpathUtil.findElements(
                mapping,
                ".//l7:Properties/l7:Property[@key='" + MAPPING_PROPERTY_NAME_SK_READ_ONLY_ENTITY + "']/l7:BooleanValue",
                getNamespaceMap()
        );

        if (readOnlyEntityBooleanValues.size() < 1) {
            return false;
        }

        final Element readOnlyEntityBooleanValue = readOnlyEntityBooleanValues.get(0);
        return readOnlyEntityBooleanValue != null && Boolean.valueOf(readOnlyEntityBooleanValue.getTextContent());
    }

    // START load methods, can be private access, but made protected for unit testing

    protected void loadMappingErrors() {
        mappingErrors = XpathUtil.findElements(document.getDocumentElement(), "//l7:Item/l7:Resource/l7:Mappings/l7:Mapping[@errorType]", getNamespaceMap());
    }

    protected void loadMappings() {
        mappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Item/l7:Resource/l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());

        // Try this case, where the message is a restman result message.
        if (mappings.isEmpty()) {
            mappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Item/l7:Resource/l7:Mappings/l7:Mapping", getNamespaceMap());
        }

        // Try one more time
        if (mappings.isEmpty()) {
            // l7:Bundle is root node for Restman request
            mappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());
        }
    }

    protected void loadResourceSetPolicies() {
        resourceSetPolicies = XpathUtil.findElements(document.getDocumentElement(), "//l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]", getNamespaceMap());
    }

    protected void loadBundles() {
        bundles = XpathUtil.findElements(document.getDocumentElement(), "/l7:Item/l7:Resource/l7:Bundle", getNamespaceMap());
        if (bundles.isEmpty()) {
            // l7:Bundle is root node for Restman request
            bundles = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle", getNamespaceMap());
        }
    }

    protected void loadBundleReferenceItems() {
        bundleReferenceItems = XpathUtil.findElements(document.getDocumentElement(), "/l7:Item/l7:Resource/l7:Bundle/l7:References/l7:Item", getNamespaceMap());
        if (bundleReferenceItems.isEmpty()) {
            // l7:Bundle is root node for Restman request
            bundleReferenceItems = XpathUtil.findElements(document.getDocumentElement(), "/l7:Bundle/l7:References/l7:Item", getNamespaceMap());
        }
    }

    // END load methods
}