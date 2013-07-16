package com.l7tech.external.assertions.authandmgmtserviceinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.*;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utilities class for generating or updating policy xml content
 *
 * @author ghuang
 */
public class ApiPortalAuthAndMgmtServicePolicyResolverUtils {

    public static Document readPolicyFile(String policyResourceFile) throws Exception {
        final InputStream resourceAsStream = ApiPortalAuthAndMgmtServicePolicyResolverUtils.class.getClassLoader().getResourceAsStream(policyResourceFile);
        if (resourceAsStream == null) {
            throw new Exception("Policy resource file does not exist: " + policyResourceFile);
        }
        final byte[] fileBytes;
        try {
            fileBytes = IOUtils.slurpStream(resourceAsStream);
        } catch (IOException e) {
            throw new IOException("Cannot read the policy file, " + policyResourceFile);
        }
        try {
            resourceAsStream.close();
        } catch (IOException e) {
            throw new IOException("Cannot close the resource stream for the policy file, " + policyResourceFile);
        }
        Document policyDocument;
        try {
            policyDocument = XmlUtil.parse(new ByteArrayInputStream(fileBytes));
        } catch (Exception e) {
            throw new IOException("Cannot parse the policy file (" + policyResourceFile + ") into a Document object.");
        }

        return policyDocument;
    }

    public static void updateAttributeValueByLeftCommentName(Document policyDocument, String leftCommentName, String newValue) throws Exception {
        final List<Element> foundComments = findElements(policyDocument.getDocumentElement(), ".//L7p:value[@stringValue='" + leftCommentName + "']", getNamespaceMap());
        for (Element foundComment : foundComments) {
            // verify it's a left comment
            final Element entryParent = (Element) foundComment.getParentNode();
            final Node keyElm = DomUtils.findFirstChildElement(entryParent);
            final Node stringValue = keyElm.getAttributes().getNamedItem("stringValue");
            if (! "LEFT.COMMENT".equals(stringValue.getNodeValue())) {
                continue;
            }

            final Element setVariableAssertionElm = (Element) entryParent.getParentNode().getParentNode().getParentNode();
            final Element base64ExpressionElm;
            try {
                base64ExpressionElm = XmlUtil.findExactlyOneChildElementByName(setVariableAssertionElm, "http://www.layer7tech.com/ws/policy", "Base64Expression");
            } catch (Exception e) {
                throw new Exception("Error during processing the element 'Base64Expression' in the policy file");
            }
            base64ExpressionElm.setAttribute("stringValue", HexUtils.encodeBase64(HexUtils.encodeUtf8(newValue), true));
        }
    }

    public static void updateLdapProviderIdByLeftCommentName(Document authPolicyDocument, String leftCommentName, String ldapProviderId) throws Exception {
        final List<Element> foundComments = findElements(authPolicyDocument.getDocumentElement(), ".//L7p:value[@stringValue='" + leftCommentName + "']", getNamespaceMap());
        for (Element foundComment : foundComments) {
            // verify it's a left comment
            final Element entryParent = (Element) foundComment.getParentNode();
            final Node keyElm = DomUtils.findFirstChildElement(entryParent);
            final Node stringValue = keyElm.getAttributes().getNamedItem("stringValue");
            if (! "LEFT.COMMENT".equals(stringValue.getNodeValue())) {
                continue;
            }

            final Element ldapQueryAssertionElm = (Element) entryParent.getParentNode().getParentNode().getParentNode();
            final Element ldapProviderOidElm;
            try {
                ldapProviderOidElm = XmlUtil.findExactlyOneChildElementByName(ldapQueryAssertionElm, "http://www.layer7tech.com/ws/policy", "LdapProviderOid");
            } catch (Exception e) {
                throw new Exception("Error during processing the element 'LdapProviderOid' in the policy file");
            }
            ldapProviderOidElm.setAttribute("longValue", ldapProviderId);
        }
    }

    public static void updateLdapProviderIdByElementName(Document authPolicyDocument, String elementName, String ldapProviderId) {//throws TooManyChildElementsException, MissingRequiredElementException {
        final List<Element> foundComments = findElements(authPolicyDocument.getDocumentElement(), ".//L7p:" + elementName + "[@longValue]", getNamespaceMap());
        for (Element ldapProviderOidElm : foundComments) {
            ldapProviderOidElm.setAttribute("longValue", ldapProviderId);
        }
    }


    public static void insertAuthenticationPartIntoPolicy(Document policyDocument, Document authenticationDoc, String leftCommentName) {
        final List<Element> foundComments = findElements(policyDocument.getDocumentElement(), ".//L7p:value[@stringValue='" + leftCommentName + "']", getNamespaceMap());
        for (Element foundComment : foundComments) {
            // verify it's a left comment
            final Element entryParent = (Element) foundComment.getParentNode();
            final Node keyElm = DomUtils.findFirstChildElement(entryParent);
            final Node stringValue = keyElm.getAttributes().getNamedItem("stringValue");
            if (! "LEFT.COMMENT".equals(stringValue.getNodeValue())) {
                continue;
            }

            final Element oneOrMoreAssertionElm = (Element) entryParent.getParentNode().getParentNode().getParentNode();
            final Element firstChild = XmlUtil.findFirstChildElement(oneOrMoreAssertionElm);

            final Element authDocElement = authenticationDoc.getDocumentElement();
            final Element allAssertion = XmlUtil.findFirstChildElement(authDocElement);
            final Element authElement = XmlUtil.findFirstChildElement(allAssertion);

            final Node node = oneOrMoreAssertionElm.getOwnerDocument().importNode(authElement, true);
            oneOrMoreAssertionElm.insertBefore(node, firstChild);
        }
    }

    public static void removeManagementPartFromPolicy(Document policyDocument, String leftCommentName) {
        final List<Element> foundComments = findElements(policyDocument.getDocumentElement(), ".//L7p:value[@stringValue='" + leftCommentName + "']", getNamespaceMap());
        for (Element foundComment : foundComments) {
            // verify it's a left comment
            final Element entryParent = (Element) foundComment.getParentNode();
            final Node keyElm = DomUtils.findFirstChildElement(entryParent);
            final Node stringValue = keyElm.getAttributes().getNamedItem("stringValue");
            if (! "LEFT.COMMENT".equals(stringValue.getNodeValue())) {
                continue;
            }

            final Element deletedElement = (Element) entryParent.getParentNode().getParentNode().getParentNode();
            final Node parentNode = deletedElement.getParentNode();
            parentNode.removeChild(deletedElement);
        }
    }

    public static List<Element> findElements(@NotNull final Element elementToSearch,
                                       @NotNull final String xpath,
                                       @NotNull final Map<String, String> namespaceMap) {
        final List<Element> toReturn = new ArrayList<Element>();
        final DomElementCursor includeCursor = new DomElementCursor(elementToSearch, false);
        final XpathResult result = XpathUtil.getXpathResultQuietly(includeCursor, namespaceMap, xpath);
        if (result.getType() == XpathResult.TYPE_NODESET) {
            final XpathResultIterator iterator = result.getNodeSet().getIterator();
            while (iterator.hasNext()) {
                toReturn.add(iterator.nextElementAsCursor().asDomElement());
            }
        }
        return toReturn;
    }

    public static Map<String, String> getNamespaceMap() {
        return CollectionUtils.MapBuilder.<String, String>builder()
            .put("env", "http://www.w3.org/2003/05/soap-envelope")
            .put("wsen", "http://schemas.xmlsoap.org/ws/2004/09/enumeration")
            .put("wsman", "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd")
            .put("l7", "http://ns.l7tech.com/2010/04/gateway-management")
            .put("L7p", "http://www.layer7tech.com/ws/policy")
            .put("wxf", "http://schemas.xmlsoap.org/ws/2004/09/transfer")
            .put("wsa", "http://schemas.xmlsoap.org/ws/2004/08/addressing")
            .unmodifiableMap();
    }
}