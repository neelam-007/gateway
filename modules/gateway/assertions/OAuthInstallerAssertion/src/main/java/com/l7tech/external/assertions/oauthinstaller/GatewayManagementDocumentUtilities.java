package com.l7tech.external.assertions.oauthinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Common utilities for working with Gateway Management API documents - responses and requests.
 *
 * There is currently some OTK installer possible logic baked in here.
 *
 * // TODO - look at verifying version information to validate assumptions about document contents
 */
public class GatewayManagementDocumentUtilities {
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

    public static class UnexpectedManagementResponse extends Exception{
        public UnexpectedManagementResponse(String message) {
            super(message);
        }
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
    public static List<Long> getSelectorId(final Document response, final boolean allowMultiple) throws UnexpectedManagementResponse{
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

    public static List<String> getErrorDetails(final Document response) throws Exception {
        final ElementCursor cursor = new DomElementCursor(response.getDocumentElement());

        List<String> errorDetails = new ArrayList<String>();
        final XpathResult xpathResult = cursor.getXpathResult(new XpathExpression("/env:Envelope/env:Body/env:Fault/env:Code", getNamespaceMap()).compile());
        if (xpathResult.getType() == XpathResult.TYPE_NODESET) {
            final XpathResultIterator iterator = xpathResult.getNodeSet().getIterator();
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

        return errorDetails;
    }

    /**
     * Process a gateway management response
     *
     * @param response the response
     * @return Long if the entity was created, null if it already existed and was allowed to.
     * @throws Exception If the entity was not created, was not allowed to already exist or an unexpected situation
     *                   occured e.g. permission denied.
     */
    public static boolean resourceAlreadyExists(final Document response) throws Exception {
        final List<String> errorDetails = getErrorDetails(response);
        return errorDetails.contains("env:Sender") && errorDetails.contains("wsman:AlreadyExists");
    }

    /**
     * Get all policy includes for a policy element.
     * <p/>
     * WARNING: The returned Elements do not belong to PolicyElement.
     *
     * @return List of l7:PolicyGuid Elements found the policyElement
     * @throws Exception
     */
    @NotNull
    public static List<Element> getPolicyIncludes(@NotNull Document policyDocument) {

        final DomElementCursor includeCursor = new DomElementCursor(policyDocument.getDocumentElement(), false);
        final XpathResult includeResult = XpathUtil.getXpathResultQuietly(includeCursor, getNamespaceMap(), "//L7p:Include/L7p:PolicyGuid");

        final XpathResultIterator iterator = includeResult.getNodeSet().getIterator();
        final List<Element> allIncludedGuids = new ArrayList<Element>();
        while (iterator.hasNext()) {
            allIncludedGuids.add(iterator.nextElementAsCursor().asDomElement());
        }

        return allIncludedGuids;
    }

    public static void updatePolicyIncludes(@NotNull Map<String, String> oldGuidsToNewGuids,
                                            @NotNull String identifier,
                                            @NotNull String entityType,
                                            @NotNull List<Element> policyIncludes,
                                            @NotNull Element policyResourceElement,
                                            @NotNull Document policyDocumentFromResource) {
        // update them now that they have been created.
        for (Element policyInclude : policyIncludes) {
            final String includeGuid = policyInclude.getAttribute("stringValue");
            if (!oldGuidsToNewGuids.containsKey(includeGuid)) {
                // programming error
                throw new RuntimeException("Required policy include #{" + includeGuid + "} was not created for " + entityType + " with identifier: #{" + identifier + "}");
            }
            final String newGuid = oldGuidsToNewGuids.get(includeGuid);
            policyInclude.setAttribute("stringValue", newGuid);
        }

        //write changes back to the resource document
        DomUtils.setTextContent(policyResourceElement, XmlUtil.nodeToStringQuiet(policyDocumentFromResource));
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(GatewayManagementDocumentUtilities.class.getName());
}
