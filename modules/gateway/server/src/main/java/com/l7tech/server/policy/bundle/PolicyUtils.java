package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.DomUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.getNamespaceMap;

/**
 * Utilities related to processing the contents of Layer 7 policy XML
 */
public class PolicyUtils {
    public static void updatePolicyIncludes(@NotNull Map<String, String> oldGuidsToNewGuids,
                                            @NotNull String identifier,
                                            @NotNull String entityType,
                                            @NotNull List<Element> policyIncludes) {
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
    }

    //todo test coverage
    public static List<Element> findJdbcReferences(final Element layer7PolicyDoc) {
        return findElements(layer7PolicyDoc, "//L7p:JdbcQuery");
    }

    //todo test coverage
    public static List<Element> findProtectedUrls(final Element layer7PolicyDoc) {
        return findElements(layer7PolicyDoc, "//L7p:ProtectedServiceUrl");
    }

    //todo test coverage
    public static List<Element> findContextVariables(final Element layer7PolicyDoc) {
        return findElements(layer7PolicyDoc, "//L7p:SetVariable");
    }

    static List<Element> findElements(@NotNull final Element layer7PolicyDoc,
                                             @NotNull final String relativeXPath) {
        final List<Element> toReturn = new ArrayList<Element>();

        final DomElementCursor includeCursor = new DomElementCursor(layer7PolicyDoc, false);
        final XpathResult result = XpathUtil.getXpathResultQuietly(includeCursor, getNamespaceMap(), relativeXPath);

        final XpathResultIterator iterator = result.getNodeSet().getIterator();
        while (iterator.hasNext()) {
            toReturn.add(iterator.nextElementAsCursor().asDomElement());
        }

        return toReturn;
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

    static Element findSingleDescendantElement(@NotNull final Element elementToSearch,
                                               @NotNull final String relativeXPath,
                                               @NotNull final String resourceType,
                                               @NotNull final String identifier,
                                               @NotNull final String moreThanOneErrorMsg) throws BundleResolver.InvalidBundleException {

        final ElementCursor policyCursor = new DomElementCursor(elementToSearch, false);
        // The xpath expression below uses '.' to make sure it runs from the current element and not over the entire document.
        final XpathResult xpathResult = XpathUtil.getXpathResultQuietly(policyCursor, getNamespaceMap(), relativeXPath);

        final Element returnElement;
        if (xpathResult.getType() == XpathResult.TYPE_NODESET && !xpathResult.getNodeSet().isEmpty()) {
            if (xpathResult.getNodeSet().size() != 1) {
                // mgmt api updated exception - wrong version exception
                throw new BundleResolver.InvalidBundleException(moreThanOneErrorMsg +
                        " element found for " + resourceType + " with id #{" + identifier + "}. Not supported.");
            }
            returnElement = xpathResult.getNodeSet().getIterator().nextElementAsCursor().asDomElement();
        } else {
            returnElement = null;
        }

        return returnElement;
    }

    /**
     * @param resourcePolicyElement element which can contain a resource of type policy. Should be an element of type
     *                              &lt;l7:Resource type="policy"&gt;
     * @return
     * @throws java.io.IOException
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
            throw new IllegalArgumentException("Invalid policy element. Cannot extract policy includes");
        }

        try {
            return XmlUtil.parse(DomUtils.getTextValue(resourcePolicyElement));
        } catch (SAXException e) {
            throw new BundleResolver.InvalidBundleException("Could not get policy document from resource element for " + resourceType +
                    " with identifier #{" + identifier + "}");
        }
    }

    /**
     * Allow callers to deal with a not found element, so return is nullable.
     *
     * @param elementWithPolicyDescendant
     * @return
     * @throws Exception
     */
    @Nullable
    public static Element getPolicyResourceElement(@NotNull final Element elementWithPolicyDescendant,
                                                   @NotNull final String resourceType,
                                                   @NotNull final String identifier) throws BundleResolver.InvalidBundleException {

        return findSingleDescendantElement(elementWithPolicyDescendant, ".//l7:Resource[@type='policy']", resourceType, identifier, "More than one policy");
    }

    /**
     * Get the PolicyDetail element from a Gateway Management Policy element.
     * @param elementWithPolicyDescendant
     * @param resourceType
     * @param identifier
     * @return
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     */
    public static Element getPolicyDetailElement(@NotNull final Element elementWithPolicyDescendant,
                                                 @NotNull final String resourceType,
                                                 @NotNull final String identifier) throws BundleResolver.InvalidBundleException {
        return findSingleDescendantElement(elementWithPolicyDescendant, ".//l7:PolicyDetail", resourceType, identifier, "More than one PolicyDetail");
    }

    public static Element getPolicyNameElement(@NotNull final Element elementWithPolicyDescendant,
                                               @NotNull final String resourceType,
                                               @NotNull final String identifier) throws BundleResolver.InvalidBundleException {
        return findSingleDescendantElement(elementWithPolicyDescendant, ".//l7:Name", resourceType, identifier, "More than one Name");
    }
}
