package com.l7tech.external.assertions.apiportalintegration.server.upgrade;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKeyManagerFactory;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManagerImpl;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import com.l7tech.xml.xpath.XpathResultNodeSet;
import com.l7tech.xml.xpath.XpathUtil;
import org.apache.commons.lang.StringEscapeUtils;
import org.jaxen.JaxenException;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Administrative implementation for executing API Portal upgrades.
 */
public class UpgradePortalAdminImpl implements UpgradePortalAdmin {

    public UpgradePortalAdminImpl(@NotNull final ApplicationContext context) {
        serviceManager = context.getBean("serviceManager", ServiceManager.class);
        keyManager = ApiKeyManagerFactory.getInstance();
        portalManagedServiceManager = PortalManagedServiceManagerImpl.getInstance(context);
        clusterPropertyManager = context.getBean("clusterPropertyManager", ClusterPropertyManager.class);
    }

    /**
     * Restricted constructor for unit tests.
     */
    UpgradePortalAdminImpl(@NotNull final ServiceManager serviceManager,
                           @NotNull final PortalGenericEntityManager<ApiKeyData> keyManager,
                           @NotNull final PortalManagedServiceManager portalManagedServiceManager,
                           @NotNull final ClusterPropertyManager clusterPropertyManager) {
        this.serviceManager = serviceManager;
        this.keyManager = keyManager;
        this.portalManagedServiceManager = portalManagedServiceManager;
        this.clusterPropertyManager = clusterPropertyManager;
    }

    /**
     * Upgrades service policies by assigning an apiId (generated UUID) to any service which contains the ApiPortalIntegrationAssertion
     * but is missing the apiId field.
     * <p/>
     * Each policy update should trigger an EntityInvalidationEvent in the background and trigger the ModuleLoadListener
     * to persist a PortalManagedService (this is important for upgrading API keys).
     */
    @Override
    public List<UpgradedEntity> upgradeServicesTo2_1() {
        // update portal managed services to have api ids in policy
        final List<UpgradedEntity> upgradedServices = new ArrayList<UpgradedEntity>();
        try {
            final Collection<ServiceHeader> allHeaders = serviceManager.findAllHeaders();
            for (final ServiceHeader header : allHeaders) {
                final PublishedService service = serviceManager.findByPrimaryKey(header.getOid());
                final boolean modified = upgradePolicy(service);
                if (modified) {
                    // this update should kick off an entity invalidation event
                    // the ModuleLoadListener should persist a PortalManagedService from this event
                    serviceManager.update(service);
                    // must use PortalManagedServiceManager to determine the apiId (takes into account disabled and multiple assertions)
                    final PortalManagedService portalManagedService = portalManagedServiceManager.fromService(service);
                    if (portalManagedService != null) {
                        upgradedServices.add(new UpgradedEntity(portalManagedService.getName(), UpgradedEntity.API, service.getName()));
                    } else {
                        // this can happen if all ApiPortalIntegrationAssertions are disabled (therefore the service is not portal managed)
                        // but we still should indicate that an entity was modified
                        upgradedServices.add(new UpgradedEntity(String.valueOf(service.getOid()), UpgradedEntity.SERVICE, service.getName()));
                    }
                }
            }
        } catch (final Exception e) {
            final String error = "Error upgrading services: " + e.getMessage();
            LOGGER.log(Level.WARNING, error, ExceptionUtils.getDebugException(e));
            throw new UpgradeServiceException(error, e);
        }
        return upgradedServices;
    }

    /**
     * Does 2 things:
     * <p/>
     * 1. detects values set in the OAuth element of the xmlRepresentation and sets then on the corresponding fields
     * <br />
     * 2. updates key mapping (field + xmlRepresentation) to be apiId->plan instead of serviceOid->plan.
     */
    @Override
    public List<UpgradedEntity> upgradeKeysTo2_1() {
        // update any affected keys to reference api id instead of service oid
        final List<UpgradedEntity> updatedKeys = new ArrayList<UpgradedEntity>();
        try {
            final Map<String, String> serviceOidApiIdMap = getServiceOidApiIdMapping();
            final List<ApiKeyData> keys = keyManager.findAll();
            for (final ApiKeyData key : keys) {
                final String oldXmlRepresentation = StringEscapeUtils.unescapeHtml(key.getXmlRepresentation());
                final Document document = XmlUtil.parse(oldXmlRepresentation);
                final boolean requiresOAuthUpdate = upgradeFieldsFromOAuth(key, document);
                final boolean requiresMappingUpdate = upgradeMapping(serviceOidApiIdMap, key, document);
                if (requiresOAuthUpdate || requiresMappingUpdate) {
                    keyManager.update(key);
                    updatedKeys.add(new UpgradedEntity(key.getName(), UpgradedEntity.KEY, key.getLabel()));
                }
            }
        } catch (final Exception e) {
            final String error = "Error upgrading keys: " + e.getMessage();
            LOGGER.log(Level.WARNING, error, ExceptionUtils.getDebugException(e));
            throw new UpgradeKeyException(error, e);
        }
        return updatedKeys;
    }

    @Override
    public void deleteUnusedClusterProperties() {
        try {
            final ClusterProperty plansProperty = clusterPropertyManager.findByUniqueName(ModuleConstants.PORTAL_API_PLANS_UI_PROPERTY);
            if (plansProperty != null) {
                clusterPropertyManager.delete(plansProperty.getGoid());
            }
            final ClusterProperty portalManagedServicesProperty = clusterPropertyManager.findByUniqueName(ModuleConstants.PORTAL_MANAGED_SERVICES_UI_PROPERTY);
            if (portalManagedServicesProperty != null) {
                clusterPropertyManager.delete(portalManagedServicesProperty.getGoid());
            }
        } catch (final Exception e) {
            final String error = "Error deleting cluster properties: " + e.getMessage();
            LOGGER.log(Level.WARNING, error, ExceptionUtils.getDebugException(e));
            throw new UpgradeClusterPropertyException(error, e);
        }
    }

    /**
     * 1. Assigns an apiId to all ApiPortalIntegrationAssertions found in policy<br />
     * 2. Changes LookupApiKeyAssertions which have a default ServiceId (or set to ${service.oid} to ${portal.managed.service.apiId}<br />
     * 3. Changes ComparisonAssertions which have RightValue = ${service.oid} to ${portal.managed.service.apiId}<br />
     */
    private boolean upgradePolicy(final PublishedService service) throws SAXException, IOException, JaxenException {
        boolean modified = false;
        final String policyXml = service.getPolicy().getXml();
        if (policyXml != null && (policyXml.contains(ModuleConstants.TEMP_PORTAL_MANAGED_SERVICE_INDICATOR) ||
                policyXml.contains(ModuleConstants.PORTAL_MANAGED_SERVICE_INDICATOR))) {

            // update ApiPortalIntegrationAssertion(s)
            final Document document = XmlUtil.parse(policyXml);
            final NodeList portalNodes = document.getElementsByTagName(L7P_API_PORTAL_INTEGRATION);
            for (int i = 0; i < portalNodes.getLength(); i++) {
                final Node portalNode = portalNodes.item(i);
                if (portalNode instanceof Element) {
                    final Element portalElement = (Element) portalNode;
                    final NodeList apiIdNodes = portalElement.getElementsByTagName(L7P_API_ID);
                    if (apiIdNodes.getLength() == 0) {
                        // add missing api id element
                        final Element apiIdElement = document.createElement(L7P_API_ID);
                        final String uuid = UUID.randomUUID().toString();
                        apiIdElement.setAttribute(STRING_VALUE, uuid);
                        portalElement.appendChild(apiIdElement);
                        modified = true;
                    }
                }
            }

            // update LookupApiKeyAssertion(s)
            final NodeList lookupNodes = document.getElementsByTagName("L7p:LookupApiKey");
            for (int i = 0; i < lookupNodes.getLength(); i++) {
                final Node lookupNode = lookupNodes.item(i);
                final NodeList lookupChildren = lookupNode.getChildNodes();
                boolean requiresServiceIdElement = true;
                for (int j = 0; j < lookupChildren.getLength(); j++) {
                    final Node lookupChild = lookupChildren.item(j);
                    if (lookupChild.getNodeName().equals("L7p:ServiceId")) {
                        requiresServiceIdElement = false;
                        if (lookupChild.getAttributes().getNamedItem(STRING_VALUE).getTextContent().equals(OLD_VALUE)) {
                            lookupChild.getAttributes().getNamedItem(STRING_VALUE).setTextContent(UPGRADED_VALUE);
                            modified = true;
                        }
                        break;
                    }
                }
                if (requiresServiceIdElement) {
                    // add missing api id element
                    final Element serviceIdElement = document.createElement("L7p:ServiceId");
                    serviceIdElement.setAttribute(STRING_VALUE, UPGRADED_VALUE);
                    lookupNode.appendChild(serviceIdElement);
                    modified = true;
                }

            }

            // update ComparisonAssertion(s)
            final XpathResult xpathResult = XpathUtil.getXpathResultQuietly(new DomElementCursor(document), NAMESPACE_MAP, COMPARISON_ASSERTION_RIGHT_VALUE_EXPRESSION);
            final XpathResultNodeSet nodeSet = xpathResult.getNodeSet();
            final XpathResultIterator iterator = nodeSet.getIterator();
            while (iterator.hasNext()) {
                final ElementCursor cursor = iterator.nextElementAsCursor();
                final Element element = cursor.asDomElement();
                element.getAttributes().getNamedItem(STRING_VALUE).setTextContent(UPGRADED_VALUE);
                modified = true;
            }

            if (modified) {
                final String xml = XmlUtil.nodeToFormattedString(document);
                service.getPolicy().setXml(xml);
            }
        }
        return modified;
    }

    /**
     * Retrieves a map of serviceOid->apiId from all persisted PortalManagedServices.
     */
    private Map<String, String> getServiceOidApiIdMapping() throws FindException {
        final Map<String, String> serviceOidApiIdMap = new HashMap<String, String>();
        final List<PortalManagedService> portalManagedServices = portalManagedServiceManager.findAll();
        for (final PortalManagedService portalManagedService : portalManagedServices) {
            serviceOidApiIdMap.put(portalManagedService.getDescription(), portalManagedService.getName());
        }
        if (serviceOidApiIdMap.isEmpty()) {
            LOGGER.warning("No portal managed services found.");
        }
        return serviceOidApiIdMap;
    }

    private boolean upgradeMapping(final Map<String, String> serviceOidApiIdMap, final ApiKeyData key, final Document document) throws IOException {
        boolean requiresUpdate = false;
        final List<String> toRemove = new ArrayList<String>();
        final Map<String, String> toAdd = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : key.getServiceIds().entrySet()) {
            final String serviceOid = entry.getKey();
            if (serviceOidApiIdMap.containsKey(serviceOid)) {
                requiresUpdate = true;
                // map apiId to plan
                final String plan = key.getServiceIds().get(serviceOid);
                final String apiId = serviceOidApiIdMap.get(serviceOid);
                toAdd.put(apiId, plan);
                toRemove.add(serviceOid);
            }
        }
        for (final String remove : toRemove) {
            key.getServiceIds().remove(remove);
        }
        key.getServiceIds().putAll(toAdd);

        // replace service oids with apiIds in xml representation
        final NodeList nodes = document.getElementsByTagNameNS(ModuleConstants.NAMESPACE_API_KEYS, ModuleConstants.SERVICE_ELEMENT_NAME);
        for (int i = 0; i < nodes.getLength(); i++) {
            final Node serviceNode = nodes.item(i);
            final NamedNodeMap attributes = serviceNode.getAttributes();
            final Node idNode = attributes.getNamedItem("id");
            final String existingId = idNode.getTextContent();
            if (serviceOidApiIdMap.containsKey(existingId)) {
                requiresUpdate = true;
                idNode.setTextContent(serviceOidApiIdMap.get(existingId));
            }
        }
        key.setXmlRepresentation(StringEscapeUtils.escapeHtml(XmlUtil.nodeToFormattedString(document)));

        return requiresUpdate;
    }

    private boolean upgradeFieldsFromOAuth(final ApiKeyData key, final Document document) {
        boolean requiresUpdate = false;
        // set label and callbackUrl from xmlRepresentation on key
        final NodeList oauthNodes = document.getElementsByTagNameNS(ModuleConstants.NAMESPACE_API_KEYS, ModuleConstants.OAUTH_ELEMENT_NAME);
        if (oauthNodes.getLength() > 0) {
            if (oauthNodes.getLength() > 1) {
                LOGGER.warning("Found more than one OAuth node for key=" + key.getKey() + ". Only the first one will be processed.");
            }
            final Node oauthNode = oauthNodes.item(0);
            final NamedNodeMap attributes = oauthNode.getAttributes();
            final Node label = attributes.getNamedItem(LABEL);
            if (label != null && !label.getTextContent().equals(key.getLabel())) {
                key.setLabel(label.getTextContent());
                requiresUpdate = true;
            }
            final Node callbackUrl = attributes.getNamedItem(CALLBACK_URL);
            if (callbackUrl != null && !callbackUrl.getTextContent().equals(key.getOauthCallbackUrl())) {
                key.setOauthCallbackUrl(callbackUrl.getTextContent());
                requiresUpdate = true;
            }
        }
        return requiresUpdate;
    }

    private static final Logger LOGGER = Logger.getLogger(UpgradePortalAdminImpl.class.getName());
    private static final Map<String, String> NAMESPACE_MAP = Collections.singletonMap("L7p", "http://www.layer7tech.com/ws/policy");
    private static final String L7P_API_PORTAL_INTEGRATION = "L7p:ApiPortalIntegration";
    private static final String L7P_API_ID = "L7p:ApiId";
    private static final String STRING_VALUE = "stringValue";
    private static final String LABEL = "label";
    private static final String CALLBACK_URL = "callbackUrl";
    private static final String OLD_VALUE = "${service.oid}";
    private static final String UPGRADED_VALUE = "${portal.managed.service.apiId}";
    private static final String COMPARISON_ASSERTION_RIGHT_VALUE_EXPRESSION = "//L7p:ComparisonAssertion/L7p:Expression1[@stringValue='${apiKeyRecord.service}']/" +
            "following-sibling::L7p:Predicates/L7p:item/L7p:RightValue[@stringValue='${service.oid}']";
    @NotNull
    private final ServiceManager serviceManager;
    @NotNull
    private final PortalGenericEntityManager<ApiKeyData> keyManager;
    @NotNull
    private final PortalManagedServiceManager portalManagedServiceManager;
    @NotNull
    private final ClusterPropertyManager clusterPropertyManager;
}
