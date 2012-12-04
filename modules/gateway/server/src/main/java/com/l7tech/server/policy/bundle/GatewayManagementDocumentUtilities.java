package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utilities related to processing Gateway management API document - requests and responses, including the SOAP
 * envelope they are contained within.
 *
 * //todo verify this is no longer the case
 * There is currently some OTK installer possible logic baked in here.
 *
 * // TODO - look at verifying version information to validate assumptions about document contents
 */
public class GatewayManagementDocumentUtilities {
    //todo turn into enum class
    public static Map<String, String> getNamespaceMap() {
        Map<String, String> nsMap = new HashMap<String, String>();
        nsMap.put("env", "http://www.w3.org/2003/05/soap-envelope");
        nsMap.put("wsen", "http://schemas.xmlsoap.org/ws/2004/09/enumeration");
        nsMap.put("wsman", "http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd");
        nsMap.put("l7", "http://ns.l7tech.com/2010/04/gateway-management");
        nsMap.put("L7p", "http://www.layer7tech.com/ws/policy");
        nsMap.put("wxf", "http://schemas.xmlsoap.org/ws/2004/09/transfer");
        nsMap.put("wsa", "http://schemas.xmlsoap.org/ws/2004/08/addressing");

        return nsMap;
    }

    public static List<Element> getEntityElements(Element enumerationElement, String type) {
        return XmlUtil.findChildElementsByName(enumerationElement, BundleUtils.L7_NS_GW_MGMT, type);
    }

    @NotNull
    public static List<Element> findAllUrlPatternsFromEnumeration(final Document serviceEnumeration) {
        return PolicyUtils.findElements(serviceEnumeration.getDocumentElement(), ".//l7:UrlPattern");
    }

    @NotNull
    public static List<Element> findAllPolicyNamesFromEnumeration(final Document policyEnumeration) {
        return PolicyUtils.findElements(policyEnumeration.getDocumentElement(), ".//l7:Name");
    }

    public static String findEntityNameFromElement(final Element entityElement) {
        return DomUtils.getTextValue(DomUtils.findFirstChildElementByName(entityElement, BundleUtils.L7_NS_GW_MGMT, "Name"), true);
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

        //todo - update xpath to select the string value directly from the node
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
}
