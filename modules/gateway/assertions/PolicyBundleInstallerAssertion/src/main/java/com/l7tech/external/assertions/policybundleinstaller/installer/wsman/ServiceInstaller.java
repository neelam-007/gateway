package com.l7tech.external.assertions.policybundleinstaller.installer.wsman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.event.bundle.DryRunInstallPolicyBundleEvent;
import com.l7tech.server.policy.bundle.*;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.l7tech.external.assertions.policybundleinstaller.PolicyBundleInstaller.InstallationException;
import static com.l7tech.server.policy.bundle.ssgman.wsman.WsmanInvoker.*;
import static com.l7tech.server.policy.bundle.BundleResolver.BundleItem.SERVICE;
import static com.l7tech.server.policy.bundle.BundleResolver.*;
import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.*;

/**
 * Install service.
 */
public class ServiceInstaller extends WsmanInstaller {
    public static final String SERVICES_MGMT_NS = "http://ns.l7tech.com/2010/04/gateway-management/services";
    private final ServiceManager serviceManager;

    public ServiceInstaller(@NotNull final PolicyBundleInstallerContext context,
                            @NotNull final Functions.Nullary<Boolean> cancelledCallback,
                            @NotNull final GatewayManagementInvoker gatewayManagementInvoker,
                            @NotNull final ServiceManager serviceManager) {
        super(context, cancelledCallback, gatewayManagementInvoker);
        this.serviceManager = serviceManager;
    }

    public void dryRunInstall(@NotNull final DryRunInstallPolicyBundleEvent dryRunEvent)
            throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, AccessDeniedManagementResponse {

        checkInterrupted();

        final BundleInfo bundleInfo = context.getBundleInfo();
        final Document serviceEnumDoc = context.getBundleResolver().getBundleItem(bundleInfo.getId(), SERVICE, true);
        final Map<Element, Element> serviceDetailMap = GatewayManagementDocumentUtilities.findServiceNamesAndUrlPatternsFromEnumeration(serviceEnumDoc);
        logger.finest("Dry run checking " + serviceDetailMap.size() + " service(s).");
        for (Element nameElmt: serviceDetailMap.keySet()) {
            checkInterrupted();

            Element urlPatternElmt = serviceDetailMap.get(nameElmt);

            String conflictObject = null;
            try {
                if (urlPatternElmt != null) {
                    conflictObject = getPrefixedUrl(DomUtils.getTextValue(urlPatternElmt));
                    if (hasMatchingServiceByUrl(serviceManager, conflictObject)) {
                        dryRunEvent.addServiceConflict(conflictObject);
                    }
                } else {
                    conflictObject = XmlUtil.getTextValue(nameElmt);
                    if (!findMatchingServiceByNameWithoutResolutionUrl(conflictObject).isEmpty()) {
                        dryRunEvent.addServiceConflict(conflictObject);
                    }
                }
            } catch (UnexpectedManagementResponse e) {
                throw new BundleResolver.InvalidBundleException("Could not check for conflict for url pattern or service name'" + conflictObject + "'", e);
            }
        }
    }

    public void install(@NotNull Map<String, Goid> oldToNewFolderIds,
                        @NotNull Map<String, String> contextOldPolicyGuidsToNewGuids,
                        @NotNull final PolicyInstaller policyInstaller)
            throws InterruptedException, UnknownBundleException, BundleResolverException, InvalidBundleException, InstallationException, UnexpectedManagementResponse, AccessDeniedManagementResponse {

        checkInterrupted();

        // install services
        final Document serviceBundle = context.getBundleResolver().getBundleItem(context.getBundleInfo().getId(), SERVICE, true);
        if (serviceBundle == null) {
            logger.info("No services to install for bundle " + context.getBundleInfo());
        } else {
            try {
                installServices(oldToNewFolderIds, contextOldPolicyGuidsToNewGuids, serviceBundle, policyInstaller);
            } catch (PolicyBundleInstallerCallback.CallbackException e) {
                throw new InstallationException(e);
            }
        }
    }

    /**
     * Note: Services contain nothing unique
     *
     * @param oldToNewFolderIds old to new folder IDs
     * @param contextOldPolicyGuidsToNewGuids context old policy GUIDs to new GUIDs
     *
     * @param serviceMgmtEnumeration service mgmt enumeration
     * @throws com.l7tech.server.policy.bundle.BundleResolver.InvalidBundleException
     *
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.UnexpectedManagementResponse
     * @throws com.l7tech.server.policy.bundle.PolicyBundleInstallerCallback.CallbackException
     *
     * @throws InterruptedException
     * @throws com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.AccessDeniedManagementResponse
     */
    private void installServices(@NotNull final Map<String, Goid> oldToNewFolderIds,
                                 @NotNull final Map<String, String> contextOldPolicyGuidsToNewGuids,
                                 @NotNull final Document serviceMgmtEnumeration,
                                 @NotNull final PolicyInstaller policyInstaller)
            throws BundleResolver.InvalidBundleException,
            UnexpectedManagementResponse,
            PolicyBundleInstallerCallback.CallbackException,
            InterruptedException,
            AccessDeniedManagementResponse,
            InstallationException {


        final List<Element> serviceElms = GatewayManagementDocumentUtilities.getEntityElements(serviceMgmtEnumeration.getDocumentElement(), "Service");
        int serviceElmsSize = serviceElms.size();
        logger.finest("Installing " + serviceElmsSize + " policies.");

        final Map<String, Goid> oldIdsToNewServiceIds = new HashMap<>(serviceElmsSize);
        for (Element serviceElm : serviceElms) {
            checkInterrupted();

            final Element serviceElmWritable = parseQuietly(XmlUtil.nodeToStringQuiet(serviceElm)).getDocumentElement();
            final String id = serviceElmWritable.getAttribute("id");
            if (oldIdsToNewServiceIds.containsKey(id)) {
                continue;
            }

            final Element serviceDetail = getServiceDetailElement(serviceElmWritable);
            final Element urlPatternWriteableEl = XmlUtil.findFirstDescendantElement(serviceDetail, MGMT_VERSION_NAMESPACE, "UrlPattern");

            // lets check if the service has a URL mapping and if so, if any service already exists with that mapping.
            // if it does, then we won't install it.
            // If the service has no resolution url, then we will check if there exists any conflicts with the same service name.

            if (urlPatternWriteableEl != null) {
                final String existingUrl = DomUtils.getTextValue(urlPatternWriteableEl, true);
                final String maybePrefixedUrl = getPrefixedUrl(existingUrl);
                if (!existingUrl.equals(maybePrefixedUrl)) {
                    DomUtils.setTextContent(urlPatternWriteableEl, maybePrefixedUrl);
                }

                if (hasMatchingServiceByUrl(serviceManager, maybePrefixedUrl)) {
                    // Service already exists
                    logger.info("Not installing service with id #{" + id + "} and routing URI '" + maybePrefixedUrl + "' " +
                            "due to existing service with conflicting resolution URI");
                    continue;
                }
            } else {
                final Element serviceNameEl = XmlUtil.findFirstDescendantElement(serviceDetail, MGMT_VERSION_NAMESPACE, "Name");
                final String serviceName = DomUtils.getTextValue(serviceNameEl);
                final List<Goid> matchingService = findMatchingServiceByNameWithoutResolutionUrl(serviceName);
                if (!matchingService.isEmpty()) {
                    // Service already exists
                    logger.info("Not installing service with id #{" + id + "} and service name '" + serviceName + "' " +
                            "due to existing service with conflicting service name");
                    continue;
                }
            }

            final String bundleFolderId = serviceDetail.getAttribute("folderId");
            if (!oldToNewFolderIds.containsKey(bundleFolderId)) {
                throw new BundleResolver.InvalidBundleException("Could not find updated folder for service #{" + id + "} in folder " + bundleFolderId);
            }
            final Goid newFolderId = oldToNewFolderIds.get(bundleFolderId);
            serviceDetail.setAttribute("folderId", String.valueOf(newFolderId));

            final Element policyResourceElmWritable = getPolicyResourceElement(serviceElmWritable, "Service", id);
            if (policyResourceElmWritable == null) {
                throw new BundleResolver.InvalidBundleException("Invalid policy element found. Could not retrieve policy XML from resource for Service with id #{" + id + "}");
            }

            // update any encapsulated assertions in this service
            final Document policyDocumentFromResource = getPolicyDocumentFromResource(policyResourceElmWritable, "Service", id);
            EncapsulatedAssertionInstaller.updatePolicyDoc(policyResourceElmWritable, policyDocumentFromResource, context.getInstallationPrefix(), nodeId);

            // if this service has any includes we need to update them
            final Element serviceDetailElmReadOnly = getServiceDetailElement(serviceElm);
            policyInstaller.updatePolicyDoc(serviceDetailElmReadOnly, "Service", contextOldPolicyGuidsToNewGuids, id, policyResourceElmWritable, policyDocumentFromResource, PolicyUtils.getPolicyIncludes(policyDocumentFromResource), context);

            final String serviceXmlTemplate = XmlUtil.nodeToStringQuiet(serviceElmWritable);
            final String createServiceXml = MessageFormat.format(CREATE_ENTITY_XML, getUuid(), SERVICES_MGMT_NS, serviceXmlTemplate);

            logger.finest("Creating service.");
            final Pair<AssertionStatus, Document> pair = callManagementCheckInterrupted(createServiceXml);

            final Goid createdId = GatewayManagementDocumentUtilities.getCreatedId(pair.right);
            if (createdId == null) {
                throw new GatewayManagementDocumentUtilities.UnexpectedManagementResponse("Could not get the id for service from bundle with id: #{" + id + "}");
            }

            policyInstaller.setRevisionComment(createdId, true);

            oldIdsToNewServiceIds.put(id, createdId);
        }
    }

    private Document parseQuietly(String inputXml) {
        try {
            return XmlUtil.parse(inputXml);
        } catch (SAXException e) {
            throw new RuntimeException("Unexpected internal error parsing XML: " + e.getMessage(), e);
        }
    }

    /**
     * See if any existing service contains a service with the same urlMapping e.g. resolution URI
     *
     * @param serviceManager find by routing uri logic
     * @param urlMapping URI Resolution value for the search
     * @return true if any existing service has this routing uri
     */
    private boolean hasMatchingServiceByUrl(final ServiceManager serviceManager, String urlMapping) throws InvalidBundleException {
        try {
            if (!serviceManager.findByRoutingUri(urlMapping).isEmpty()) {
                return true;
            }
        } catch (FindException e) {
            throw new InvalidBundleException(e);
        }
        return false;
    }

    /**
     * See if any existing service without resolution url contains a service with the same nameMapping e.g. service name
     *
     * @param nameMapping the service name for the search
     * @return list of ids of any existing service which have the same service name as nameMapping and do not have resolution url.
     */
    @NotNull
    private List<Goid> findMatchingServiceByNameWithoutResolutionUrl(String nameMapping) throws UnexpectedManagementResponse, InterruptedException, AccessDeniedManagementResponse {
        logger.finest("Finding service name '" + nameMapping + "'.");
        final String serviceFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(), SERVICES_MGMT_NS, 10,
                "/l7:Service/l7:ServiceDetail/l7:Name[text()='" + nameMapping + "']");
        final Pair<AssertionStatus, Document> documentPair = callManagementCheckInterrupted(serviceFilter);

        // Remove ids associated with some services having resolution url, since service conflict with resolution url has been done by hasMatchingServiceByUrl.
        final List<Goid> nameMatchedServices = GatewayManagementDocumentUtilities.getSelectorId(documentPair.right, true);
        final List<Goid> foundIds = new ArrayList<>(nameMatchedServices.size());
        for (Goid id: nameMatchedServices) {
            String oidFilter = MessageFormat.format(GATEWAY_MGMT_ENUMERATE_FILTER, getUuid(), SERVICES_MGMT_NS, 10,
                    "/l7:Service/l7:ServiceDetail[@id='" + id + "']/l7:ServiceMappings/l7:HttpMapping/l7:UrlPattern");
            Pair<AssertionStatus, Document> matchedPair = callManagementCheckInterrupted(oidFilter);
            List<Goid> matchedIds = GatewayManagementDocumentUtilities.getSelectorId(matchedPair.right, true);

            if (matchedIds.isEmpty()) {
                foundIds.add(id);
            }
        }

        return foundIds;
    }
}
