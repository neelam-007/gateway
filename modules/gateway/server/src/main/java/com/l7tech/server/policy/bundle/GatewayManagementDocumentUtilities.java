package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.DomUtils;
import com.l7tech.util.MissingRequiredElementException;
import com.l7tech.util.TooManyChildElementsException;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Utilities related to processing Gateway management API documents - requests and responses, including the SOAP
 * envelope they are contained within.
 *
 */
public class GatewayManagementDocumentUtilities {

    /**
     * If the Gateay Management namespace is bumped to represent a version change, then the usages of this class
     * will need to be updated to support the new version.
     */
    public static final String MGMT_VERSION_NAMESPACE = "http://ns.l7tech.com/2010/04/gateway-management";

    public static final Map<String, String> nsMap = CollectionUtils.MapBuilder.<String, String>builder()
            .put("env", "http://www.w3.org/2003/05/soap-envelope")
            .put("wsen", "http://schemas.xmlsoap.org/ws/2004/09/enumeration")
            .put("wsman", "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd")
            .put("l7", "http://ns.l7tech.com/2010/04/gateway-management")
            .put("L7p", "http://www.layer7tech.com/ws/policy")
            .put("wxf", "http://schemas.xmlsoap.org/ws/2004/09/transfer")
            .put("wsa", "http://schemas.xmlsoap.org/ws/2004/08/addressing")
            .unmodifiableMap();

    public static Map<String, String> getNamespaceMap() {
        return nsMap;
    }

    public static List<Element> getEntityElements(Element enumerationElement, String type) {
        return XmlUtil.findChildElementsByName(enumerationElement, MGMT_VERSION_NAMESPACE, type);
    }

    /**
     * Find service name and resolution URL for each service defined in the service enumeration document object.
     *
     * @param serviceEnumeration: the Document object defining the service enumeration.
     * @return a map in which each pair is (Service Name Element, Service Resolution URL Element)
     */
    @NotNull
    public static Map<Element, Element> findServiceNamesAndUrlPatternsFromEnumeration(final Document serviceEnumeration) {
        Map<Element, Element> serviceDetailMap = new HashMap<>();
        for (Element serviceDetailElmt: XpathUtil.findElements(serviceEnumeration.getDocumentElement(), ".//l7:ServiceDetail", getNamespaceMap())) {
            List<Element> nameElmts = XpathUtil.findElements(serviceDetailElmt, ".//l7:Name", getNamespaceMap());
            List<Element> urlPatternElmts = XpathUtil.findElements(serviceDetailElmt, ".//l7:UrlPattern", getNamespaceMap());

            if (nameElmts.size() < 0) {
                throw new IllegalArgumentException("Service xml does not contain Name element in ServiceDetail element.");
            }
            if (nameElmts.size() > 1 || urlPatternElmts.size() > 1) {
                throw new IllegalArgumentException("Service xml contains more than one Name or UrlPattern element in ServiceDetail element.");
            }

            if (urlPatternElmts.size() == 0) {
                serviceDetailMap.put(nameElmts.get(0), null);
            } else {
                serviceDetailMap.put(nameElmts.get(0), urlPatternElmts.get(0));
            }
        }
        return serviceDetailMap;
    }

    @NotNull
    public static List<Element> findAllPolicyNamesFromEnumeration(final Document policyEnumeration) {
        return XpathUtil.findElements(policyEnumeration.getDocumentElement(), ".//l7:Name", getNamespaceMap());
    }

    /**
     * Retrieve serial number and name for each trusted certificates defined in a certificate enumeration document.
     *
     * @param certificateEnumeration: a Document object defines all certificates.
     * @return a map in which each pair is (SerialNumber element, Name element).  The map could be empty, but not null.
     */
    @NotNull
    public static Map<Element, Element> findCertificateSerialNumbersAndNamesFromEnumeration(final Document certificateEnumeration) {
        Map<Element, Element> certificatesMap = new HashMap<>();
        if (certificateEnumeration == null) return certificatesMap;

        for (Element trustedCertElmt: XpathUtil.findElements(certificateEnumeration.getDocumentElement(), ".//l7:TrustedCertificate", getNamespaceMap())) {
            List<Element> nameElmts = XpathUtil.findElements(trustedCertElmt, ".//l7:Name", getNamespaceMap());
            List<Element> serialNumElmts = XpathUtil.findElements(trustedCertElmt, ".//l7:SerialNumber", getNamespaceMap());

            if (nameElmts.size() != 1) {
                throw new IllegalArgumentException("Certificate xml does not contain valid Name element in TrustedCertificate element.");
            }
            if (serialNumElmts.size() != 1) {
                throw new IllegalArgumentException("Certificate xml does not contain valid SerialNumber element in TrustedCertificate element.");
            }

            certificatesMap.put(serialNumElmts.get(0), nameElmts.get(0));
        }
        return certificatesMap;
    }

    public static String getEntityName(final Element elementWithName) {
        final Element nameEl = DomUtils.findFirstChildElementByName(elementWithName, MGMT_VERSION_NAMESPACE, "Name");
        if (nameEl == null) {
            throw new RuntimeException("Element does not contain a l7:Name element");
        }

        return DomUtils.getTextValue(nameEl, true);
    }

    public static Element getEntityNameElement(final Element entityEl) {
        final Element nameEl = DomUtils.findFirstChildElementByName(entityEl, MGMT_VERSION_NAMESPACE, "Name");
        if (nameEl == null) {
            throw new RuntimeException("Element does not contain a l7:Name element");
        }

        return nameEl;
    }

    /**
     * Get the PolicyDetail element from a Gateway Management Policy element.
     * @param policyEntityEl Policy element to get the PolicyDetail element from
     * @return
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     */
    public static Element getPolicyDetailElement(@NotNull final Element policyEntityEl)
            throws BundleResolver.InvalidBundleException {

        return getDetailElement(policyEntityEl, "PolicyDetail", "Policy");
    }

    public static Element getServiceDetailElement(@NotNull final Element serviceEntityEl)
            throws BundleResolver.InvalidBundleException {

        return getDetailElement(serviceEntityEl, "ServiceDetail", "Service");
    }

    /**
     * Allow callers to deal with a not found element, so return is nullable.
     *
     * @param entityWithPolicyDescendant
     * @return
     * @throws Exception
     */
    @Nullable
    public static Element getPolicyResourceElement(@NotNull final Element entityWithPolicyDescendant,
                                                   @NotNull final String resourceType,
                                                   @NotNull final String identifier) throws BundleResolver.InvalidBundleException {

        return findSingleDescendantElement(entityWithPolicyDescendant, ".//l7:Resource[@type='policy']", resourceType, identifier, "More than one policy");
    }

    /**
     * Get the Layer 7 policy Document from the resource.
     *
     * @param resourcePolicyElement element which can contain a resource of type policy. Should be an element of type
     *                              &lt;l7:Resource type="policy"&gt;
     * @return Policy Document
     * @throws BundleResolver.InvalidBundleException if resourcePolicyElement is not the expected Element
     */
    public static Document getPolicyDocumentFromResource(@NotNull final Element resourcePolicyElement,
                                                         @NotNull final String resourceType,
                                                         @NotNull final String identifier)
            throws BundleResolver.InvalidBundleException {

        if (!resourcePolicyElement.getLocalName().equals("Resource") ||
                !resourcePolicyElement.getNamespaceURI().equals(BundleUtils.L7_NS_GW_MGMT) ||
                !resourcePolicyElement.getAttribute("type").equals("policy")
                ) {

            // runtime programming error
            throw new IllegalArgumentException("Invalid policy element. Cannot extract policy document");
        }

        try {
            return XmlUtil.parse(DomUtils.getTextValue(resourcePolicyElement));
        } catch (SAXException e) {
            throw new BundleResolver.InvalidBundleException("Could not get policy document from resource element for " + resourceType +
                    " with identifier #{" + identifier + "}");
        }
    }

    public static class UnexpectedManagementResponse extends Exception{
        public UnexpectedManagementResponse(String message) {
            super(message);
        }

        public UnexpectedManagementResponse(Throwable cause) {
            super(cause);
        }

        public UnexpectedManagementResponse(boolean causedByMgmtAssertionInternalError) {
            this.causedByMgmtAssertionInternalError = causedByMgmtAssertionInternalError;
        }

        public boolean isCausedByMgmtAssertionInternalError() {
            return causedByMgmtAssertionInternalError;
        }

        private boolean causedByMgmtAssertionInternalError;
    }

    public static class AccessDeniedManagementResponse extends Exception {
        public AccessDeniedManagementResponse(String message, String deniedRequest) {
            super(message);
            this.deniedRequest = deniedRequest;
        }

        public String getDeniedRequest() {
            return deniedRequest;
        }

        private String deniedRequest;
    }

    /**
     * Get the id values from all found wsman:Selector elements with Name attribute equal to 'id'.
     *
     * @param response      this may be the response from a create request or an enumerate filter with EPR request
     * @param allowMultiple if true multiple ids can be found
     * @return List of found ids. Empty if none found.
     * @throws Exception parsing or xpath
     */
    @NotNull
    public static List<Long> getSelectorId(final Document response, final boolean allowMultiple) throws UnexpectedManagementResponse {
        final ElementCursor cursor = new DomElementCursor(response.getDocumentElement());

        // Find the Selector result either from a create response of from an enumeration filter with
        // EnumerateObjectAndEPR response.

        final XpathResult xpathResult = XpathUtil.getXpathResultQuietly(cursor, getNamespaceMap(), "//wsman:Selector[@Name='id']");

        final List<Long> foundIds = new ArrayList<Long>();
        if (xpathResult.getType() == XpathResult.TYPE_NODESET && !xpathResult.getNodeSet().isEmpty()) {

            final XpathResultNodeSet nodeSet = xpathResult.getNodeSet();
            if (!allowMultiple && nodeSet.size() != 1) {
                final String msg = "Unexpected number of Selector id elements found in response.";
                try {
                    logger.warning(msg + ": " + XmlUtil.nodeToFormattedString(response));
                } catch (IOException e) {
                    logger.warning("Could not parse management response: " + e.getMessage());
                }
                throw new UnexpectedManagementResponse(msg);
            }

            final XpathResultIterator iterator = xpathResult.getNodeSet().getIterator();
            while (iterator.hasNext()) {
                final XpathResultNode template = new XpathResultNode();
                iterator.next(template);
                final String nodeValue = template.getNodeValue();
                foundIds.add(Long.valueOf(nodeValue));
            }
        }

        return foundIds;
    }

    /**
     * Find the id of the created resource
     *
     * @param response gateway mgmt response
     * @return Long id. Null if not found. e.g. resource was not created
     */
    @Nullable
    public static Long getCreatedId(final Document response) throws UnexpectedManagementResponse {
        final List<Long> createdId = getSelectorId(response, false);
        return (createdId.isEmpty()) ? null : createdId.get(0);
    }

    /**
     * Simply true / false for whether the response Document represents an internal error.
     * @param response Document to check for internal error
     * @return true if Internal Error found, false otherwise
     */
    public static boolean isInternalErrorResponse(@NotNull final Document response) throws UnexpectedManagementResponse {
        final List<String> errorDetails = getErrorDetails(response);
        return errorDetails.contains("env:Receiver") && errorDetails.contains("wsman:InternalError");
    }

    public static boolean isAccessDeniedResponse(@NotNull final Document response) throws AccessDeniedManagementResponse,
            UnexpectedManagementResponse {
        final List<String> errorDetails = getErrorDetails(response);
        return errorDetails.contains("env:Sender") && errorDetails.contains("wsman:AccessDenied");
    }

    public static List<String> getErrorDetails(final Document response) throws UnexpectedManagementResponse {
        final ElementCursor cursor = new DomElementCursor(response.getDocumentElement());

        try {
            List<String> errorDetails = new ArrayList<String>();
            final XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/env:Envelope/env:Body/env:Fault/env:Code", getNamespaceMap()).compile());
            if (xpathResult.getType() == XpathResult.TYPE_NODESET) {
                final XpathResultIterator iterator = xpathResult.getNodeSet().getIterator();
                if (iterator.hasNext()) {
                    final ElementCursor resultCursor = iterator.nextElementAsCursor();
                    final XpathResult valueResult = resultCursor.getXpathResult(new XpathExpression("//env:Value", getNamespaceMap()).compile());
                    final XpathResultIterator valueIter = valueResult.getNodeSet().getIterator();
                    final XpathResultNode xpathResultNode = new XpathResultNode();

                    while (valueIter.hasNext()) {
                        valueIter.next(xpathResultNode);
                        final String textValue = xpathResultNode.getNodeValue();
                        errorDetails.add(textValue);
                    }
                }
            }

            return errorDetails;

        } catch (XPathExpressionException e) {
            throw new UnexpectedManagementResponse(e);
        } catch (InvalidXpathException e) {
            throw new UnexpectedManagementResponse(e);
        }
    }

    /**
     * Process a gateway management response
     *
     * @param response the response
     * @return Long if the entity was created, null if it already existed and was allowed to.
     * @throws Exception If the entity was not created, was not allowed to already exist or an unexpected situation
     *                   occured e.g. permission denied.
     */
    public static boolean resourceAlreadyExists(final Document response) throws UnexpectedManagementResponse {
        final List<String> errorDetails = getErrorDetails(response);
        return errorDetails.contains("env:Sender") && errorDetails.contains("wsman:AlreadyExists");
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(GatewayManagementDocumentUtilities.class.getName());

    /**
     * Get the PolicyDetail or ServiceDetail element from a Gateway Management Policy or Service element.
     * @param entityEl Policy or Service entity element to get the PolicyDetail / ServiceDetail element from
     * @return
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     */
    private static Element getDetailElement(@NotNull final Element entityEl,
                                            @NotNull final String detailElementName,
                                            @NotNull final String entityType)
            throws BundleResolver.InvalidBundleException {

        try {
            return XmlUtil.findExactlyOneChildElementByName(entityEl, BundleUtils.L7_NS_GW_MGMT, detailElementName);
        } catch (TooManyChildElementsException e) {
            throw new RuntimeException("Unexpected exception getting the " + detailElementName + " element from " + entityType + " element", e);
        } catch (MissingRequiredElementException e) {
            throw new RuntimeException("Unexpected exception getting the " + detailElementName + " element from " + entityType + " element", e);
        }
    }

    /**
     * Find a single descendent element
     *
     * @param elementToSearch
     * @param relativeXPath       tip: start the expression with '.' to make sure it runs from the current element and not over the entire document.
     * @param resourceType
     * @param identifier
     * @param moreThanOneErrorMsg
     * @return
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     *
     */
    private static Element findSingleDescendantElement(@NotNull final Element elementToSearch,
                                                       @NotNull final String relativeXPath,
                                                       @NotNull final String resourceType,
                                                       @NotNull final String identifier,
                                                       @NotNull final String moreThanOneErrorMsg) throws BundleResolver.InvalidBundleException {

        final List<Element> foundElements = XpathUtil.findElements(elementToSearch, relativeXPath, getNamespaceMap());
        if (foundElements.size() > 1) {
                throw new BundleResolver.InvalidBundleException(moreThanOneErrorMsg +
                        " element found for " + resourceType + " with id #{" + identifier + "}. Not supported.");
        }

        return foundElements.isEmpty() ? null : foundElements.get(0);
    }
}