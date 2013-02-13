package com.l7tech.server.policy.bundle;

import com.l7tech.xml.xpath.XpathUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

//todo remove
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

    public static List<Element> findJdbcReferences(final Element layer7PolicyDoc) {
        return XpathUtil.findElements(layer7PolicyDoc, "//L7p:JdbcQuery", getNamespaceMap());
    }

    public static List<Element> findProtectedUrls(final Element layer7PolicyDoc) {
        return XpathUtil.findElements(layer7PolicyDoc, "//L7p:ProtectedServiceUrl", getNamespaceMap());
    }

    public static List<Element> findContextVariables(final Element layer7PolicyDoc) {
        return XpathUtil.findElements(layer7PolicyDoc, "//L7p:SetVariable", getNamespaceMap());
    }

    /**
     * Get all policy includes for a policy element.
     *
     * @return List of l7:PolicyGuid Elements found the policyElement
     */
    @NotNull
    public static List<Element> getPolicyIncludes(@NotNull Document policyDocument) {
        return XpathUtil.findElements(policyDocument.getDocumentElement(), "//L7p:Include/L7p:PolicyGuid", getNamespaceMap());
    }

    public static List<Element> findTemplateResponses(final Element layer7PolicyDoc) {
        return XpathUtil.findElements(layer7PolicyDoc, "//L7p:HardcodedResponse", getNamespaceMap());
    }

    public static List<Element> findComparisonAssertions(final Element layer7PolicyDoc) {
        return XpathUtil.findElements(layer7PolicyDoc, "//L7p:ComparisonAssertion", getNamespaceMap());
    }
}
