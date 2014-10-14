package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.util.DomUtils;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.MGMT_VERSION_NAMESPACE;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getNamespaceMap;

/**
 * Represents a restman request or response messages with methods to extract and manipulate the message.
 */
public class RestmanMessage {
    private static final String NS_L7 = "l7";
    private static final String NODE_NAME_ERROR = NS_L7 + ":Error";
    private static final String XMLNS_L7 = "xmlns:" + NS_L7;

    private List<Element> mappingErrors;
    private List<Element> bundles;
    private List<Element> resourceSetPolicies;
    private List<Element> mappings;

    final Document document;

    public RestmanMessage(final Document document) {
        this.document = document;
    }

    public boolean isError() {
        return NODE_NAME_ERROR.equals(document.getDocumentElement().getNodeName());
    }

    public boolean hasMappingError() {
        if (mappingErrors == null) {
            loadMappingErrors();
        }

        return mappingErrors.size() > 0;
    }

    public String getMappingErrorsAsString() throws IOException {
        if (mappingErrors == null) {
            loadMappingErrors();
        }
        final StringBuilder sb = new StringBuilder();

        for (Element mappingError : mappingErrors) {
            sb.append(XmlUtil.nodeToFormattedString(mappingError));
        }

        return sb.toString();
    }

    public List<Element> getMappingErrors() throws IOException {
        if (mappingErrors == null) {
            loadMappingErrors();
        }

        return mappingErrors;
    }

    private void loadMappingErrors() {
        mappingErrors = XpathUtil.findElements(document.getDocumentElement(), "//l7:Item/l7:Resource/l7:Mappings/l7:Mapping[@errorType]", getNamespaceMap());
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
            setL7Xmlns(bundle);
            sb.append(XmlUtil.nodeToFormattedString(bundle));
        }
        return sb.toString();
    }

    public static Element setL7Xmlns(@NotNull final Element element) {
        element.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, XMLNS_L7, GatewayManagementDocumentUtilities.getNamespaceMap().get(NS_L7));
        return element;
    }

    private void loadBundles() {
        bundles = XpathUtil.findElements(document.getDocumentElement(), "/l7:Item/l7:Resource/l7:Bundle", getNamespaceMap());
    }

    public void addPropertyToAllChildlessMappings(final String propertyKey, final boolean propertyValue) {
        for (Element mapping: XpathUtil.findElements(document.getDocumentElement(), "//l7:Mappings/l7:Mapping", getNamespaceMap())) {
            if (mapping.getFirstChild() == null) {
                final Element properties = DomUtils.createAndAppendElement(mapping, "Properties");
                final Element property = DomUtils.createAndAppendElement(properties, "Property");
                property.setAttribute("key", propertyKey);
                final Element booleanValue = DomUtils.createAndAppendElement(property, "BooleanValue");
                booleanValue.setTextContent(Boolean.toString(propertyValue));
            }
        }
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

    private void loadResourceSetPolicies() {
        resourceSetPolicies = XpathUtil.findElements(document.getDocumentElement(), "//l7:Resources/l7:ResourceSet/l7:Resource[@type=\"policy\"]", getNamespaceMap());
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
            if (!"0000000000000000ffffffffffffec76".equals(mappingEle.getAttribute("srcId")) &&
                "FOLDER".equals(mappingEle.getAttribute("type")) &&
                "NewOrExisting".equals(mappingEle.getAttribute("action"))) {
                List<Element> propertiesEleList = DomUtils.findChildElementsByName(mappingEle, MGMT_VERSION_NAMESPACE, "Properties");
                if (propertiesEleList.size() == 0) {
                    continue;
                }

                Element propertiesEle = DomUtils.findExactlyOneChildElementByName(mappingEle, MGMT_VERSION_NAMESPACE, "Properties");
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
        Element propertiesEle = DomUtils.findExactlyOneChildElementByName(foundMappingEle, MGMT_VERSION_NAMESPACE, "Properties");
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

    private void loadMappings() {
        mappings = XpathUtil.findElements(document.getDocumentElement(), "/l7:Item/l7:Resource/l7:Bundle/l7:Mappings/l7:Mapping", getNamespaceMap());
    }
}