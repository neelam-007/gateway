package com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of manager responsible for PortalManagedService CRUD operations.
 * <p/>
 * Loaded PortalManagedServices are cached in RAM. The whole cache is cleared daily by default.
 * <p/>
 * If a PortalManagedService is updated anywhere on the cluster it will be removed from the cache.
 */
public class PortalManagedServiceManagerImpl extends AbstractPortalGenericEntityManager<PortalManagedService> implements PortalManagedServiceManager {
    public PortalManagedServiceManagerImpl(@NotNull final ApplicationContext applicationContext) {
        super(applicationContext);
        final GenericEntityManager genericEntityManager = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        genericEntityManager.registerClass(PortalManagedService.class);
        entityManager = genericEntityManager.getEntityManager(PortalManagedService.class);
        serviceManager = applicationContext.getBean("serviceManager", ServiceManager.class);
        applicationEventProxy = applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(this);
    }

    public static PortalManagedServiceManager getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new PortalManagedServiceManagerImpl(context);
        }
        return instance;
    }

    @Override
    public String getCacheWipeIntervalConfigProperty() {
        return "portalManagedServiceManager.cacheWipeInterval";
    }

    @Override
    public GoidEntityManager<PortalManagedService, GenericEntityHeader> getEntityManager() {
        return entityManager;
    }

    @Override
    public Object[] getUpdateLocks() {
        return updateLocks;
    }

    @Override
    public PortalManagedService addOrUpdate(@NotNull final PortalManagedService portalManagedService) throws FindException, UpdateException, SaveException {
        try {
            return update(portalManagedService);
        } catch (final ObjectNotFoundException e) {
            // does not yet exist
            return add(portalManagedService);
        }
    }

    /**
     * Finds as many PortalManagedServices as possible by parsing policy. If unable to parse a particular policy for a service, that service is skipped.
     * <p/>
     * TODO caching?
     */
    @Override
    public List<PortalManagedService> findAllFromPolicy() throws FindException {
        final List<PortalManagedService> portalManagedServices = new ArrayList<PortalManagedService>();
        final Collection<ServiceHeader> serviceHeaders = serviceManager.findAllHeaders();
        if (serviceHeaders != null && !serviceHeaders.isEmpty()) {
            for (final ServiceHeader serviceHeader : serviceHeaders) {
                try {
                    final PublishedService publishedService = serviceManager.findByPrimaryKey(serviceHeader.getGoid());
                    if (publishedService != null) {
                        final PortalManagedService portalManagedService = fromService(publishedService);
                        if (portalManagedService != null) {
                            portalManagedServices.add(portalManagedService);
                        }
                    }
                } catch (final FindException e) {
                    // try to find as many as possible
                    LOGGER.log(Level.WARNING, "Unable to create Portal Managed Service for service with goid=" + serviceHeader.getGoid(), ExceptionUtils.getDebugException(e));
                }
            }
        }
        return portalManagedServices;
    }

    @Override
    public PortalManagedService fromService(@NotNull final PublishedService publishedService) throws FindException {
        PortalManagedService portalManagedService = null;
        final String policyXml = publishedService.getPolicy().getXml();
        final Goid serviceId = publishedService.getGoid();
        if (policyXml != null && (policyXml.contains(ModuleConstants.TEMP_PORTAL_MANAGED_SERVICE_INDICATOR) || policyXml.contains(ModuleConstants.PORTAL_MANAGED_SERVICE_INDICATOR))) {
            try {
                final PortalManagedService fromPolicy = createPortalManagedService(serviceId, policyXml);
                if (fromPolicy != null && StringUtils.isNotBlank(fromPolicy.getName())) {
                    portalManagedService = fromPolicy;
                } else if (fromPolicy != null) {
                    LOGGER.log(Level.WARNING, "Service with goid=" + serviceId + " is not a valid Portal Managed Service.");
                }
            } catch (final SAXException e) {
                throw new FindException("Error parsing Portal Managed Service properties for service with goid=" + serviceId, e);
            }
        } else {
            LOGGER.log(Level.FINE, "Service with goid=" + serviceId + " is not a Portal Managed Service.");
        }
        return portalManagedService;
    }

    /**
     * Provide restricted access to the name cache for unit tests.
     */
    ConcurrentMap<Goid, String> getNameCache() {
        return nameCache;
    }

    /**
     * Provide restricted access to the cache for unit tests.
     */
    ConcurrentMap<String, AbstractPortalGenericEntity> getCache() {
        return cache;
    }

    private static final Logger LOGGER = Logger.getLogger(PortalManagedServiceManagerImpl.class.getName());
    private static final String L7P_API_PORTAL_INTEGRATION = "L7p:ApiPortalIntegration";
    private static final String L7P_API_ID = "L7p:ApiId";
    private static final String L7P_API_GROUP = "L7p:ApiGroup";
    private static final String L7P_ENABLED = "L7p:Enabled";
    private static final String STRING_VALUE = "stringValue";
    private static final String BOOLEAN_VALUE = "booleanValue";
    private final GoidEntityManager<PortalManagedService, GenericEntityHeader> entityManager;
    private final ServiceManager serviceManager;
    private final ApplicationEventProxy applicationEventProxy;
    private static PortalManagedServiceManager instance;

    static final int NUM_UPDATE_LOCKS = ConfigFactory.getIntProperty("portalManagedServiceManager.numUpdateLocks", DEFAULT_NUM_UPDATE_LOCKS);
    static final Object[] updateLocks = new Object[NUM_UPDATE_LOCKS];

    static {
        for (int i = 0; i < updateLocks.length; i++) {
            updateLocks[i] = new Object();
        }
    }

    /**
     * Can return null if all ApiPortalIntegration assertions in the policy are disabled.
     */
    private PortalManagedService createPortalManagedService(final Goid serviceId, final String policyXml) throws SAXException {
        PortalManagedService portalManagedService = null;
        final Document document = XmlUtil.parse(policyXml);
        final NodeList nodeList = document.getElementsByTagName(L7P_API_PORTAL_INTEGRATION);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            final NodeList children = node.getChildNodes();
            final PortalManagedService temp = new PortalManagedService();
            for (int j = 0; j < children.getLength(); j++) {
                final Node child = children.item(j);
                final String nodeName = child.getNodeName();
                if (nodeName != null && nodeName.equals(L7P_API_ID)) {
                    final String apiId = child.getAttributes().getNamedItem(STRING_VALUE).getTextContent();
                    temp.setName(apiId);
                } else if (nodeName != null && nodeName.equals(L7P_API_GROUP)) {
                    final String apiGroup = child.getAttributes().getNamedItem(STRING_VALUE).getTextContent();
                    temp.setApiGroup(apiGroup);
                } else if (nodeName != null && nodeName.equals(L7P_ENABLED)) {
                    final String enabledString = child.getAttributes().getNamedItem(BOOLEAN_VALUE).getTextContent();
                    temp.setEnabled(Boolean.valueOf(enabledString));
                }
            }
            // if more than one ApiPortalIntegration assertion exists, we only care about the first enabled one
            if (temp.isEnabled()) {
                portalManagedService = temp;
                portalManagedService.setDescription(String.valueOf(serviceId));
                break;
            } else {
                LOGGER.log(Level.FINE, "Found disabled ApiPortalIntegration assertion");
            }
        }
        return portalManagedService;
    }
}
