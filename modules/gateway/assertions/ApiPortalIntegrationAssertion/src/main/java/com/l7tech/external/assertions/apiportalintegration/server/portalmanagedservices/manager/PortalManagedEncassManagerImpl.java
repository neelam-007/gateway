package com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntity;
import com.l7tech.external.assertions.apiportalintegration.server.AbstractPortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedEncass;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of manager responsible for PortalManagedEncass CRUD operations.
 * <p/>
 * Loaded PortalManagedEncasses are cached in RAM. The whole cache is cleared daily by default.
 * <p/>
 * If a PortalManagedEncass is updated anywhere on the cluster it will be removed from the cache.
 *
 * @author Victor Kazakov
 */
public class PortalManagedEncassManagerImpl extends AbstractPortalGenericEntityManager<PortalManagedEncass> implements PortalManagedEncassManager {
    private static final Logger logger = Logger.getLogger(PortalManagedServiceManagerImpl.class.getName());

    private EntityManager<PortalManagedEncass, GenericEntityHeader> entityManager;
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;
    private static PortalManagedEncassManagerImpl instance;

    private static final String L7P_API_PORTAL_ENCASS_INTEGRATION = "L7p:ApiPortalEncassIntegration";
    private static final String L7P_ENABLED = "L7p:Enabled";
    private static final String BOOLEAN_VALUE = "booleanValue";

    static final int NUM_UPDATE_LOCKS = ConfigFactory.getIntProperty("portalManagedEncassManager.numUpdateLocks", DEFAULT_NUM_UPDATE_LOCKS);
    static final Object[] updateLocks = new Object[NUM_UPDATE_LOCKS];

    static {
        for (int i = 0; i < updateLocks.length; i++) {
            updateLocks[i] = new Object();
        }
    }

    public PortalManagedEncassManagerImpl(@NotNull final ApplicationContext applicationContext) {
        super(applicationContext);
        final GenericEntityManager genericEntityManager = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        genericEntityManager.registerClass(PortalManagedEncass.class);
        entityManager = genericEntityManager.getEntityManager(PortalManagedEncass.class);
        encapsulatedAssertionConfigManager = applicationContext.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class);
        ApplicationEventProxy applicationEventProxy = applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class);
        applicationEventProxy.addApplicationListener(this);
    }

    public static PortalManagedEncassManager getInstance(@NotNull final ApplicationContext context) {
        if (instance == null) {
            instance = new PortalManagedEncassManagerImpl(context);
        }
        return instance;
    }

    @Override
    public EntityManager<PortalManagedEncass, GenericEntityHeader> getEntityManager() {
        return entityManager;
    }

    @Override
    public String getCacheWipeIntervalConfigProperty() {
        return "portalManagedEncassManager.cacheWipeInterval";
    }

    @Override
    public PortalManagedEncass addOrUpdate(PortalManagedEncass portalManagedEncass) throws FindException, UpdateException, SaveException {
        try {
            return update(portalManagedEncass);
        } catch (final ObjectNotFoundException e) {
            // does not yet exist
            return add(portalManagedEncass);
        }
    }

    /**
     * Finds as many PortalManagedEncasses as possible by parsing policy of encapsulated assertion configs. If unable to
     * parse a particular policy, that encass is skipped.
     */
    @Override
    public List<PortalManagedEncass> findAllFromEncass() throws FindException {
        final List<PortalManagedEncass> portalManagedEncasses = new ArrayList<>();
        final Collection<EncapsulatedAssertionConfig> encapsulatedAssertionConfigs = encapsulatedAssertionConfigManager.findAll();
        if (encapsulatedAssertionConfigs != null && !encapsulatedAssertionConfigs.isEmpty()) {
            for (final EncapsulatedAssertionConfig encapsulatedAssertionConfig : encapsulatedAssertionConfigs) {
                try {
                    final PortalManagedEncass portalManagedEncass = fromEncass(encapsulatedAssertionConfig);
                    if (portalManagedEncass != null) {
                        portalManagedEncasses.add(portalManagedEncass);
                    }
                } catch (final FindException e) {
                    // try to find as many as possible
                    logger.log(Level.WARNING, "Unable to create Portal Managed Encass for encapsulated assertion with goid=" + encapsulatedAssertionConfig.getGoid(), ExceptionUtils.getDebugException(e));
                }
            }
        }
        return portalManagedEncasses;
    }

    @Override
    public PortalManagedEncass fromEncass(EncapsulatedAssertionConfig encapsulatedAssertionConfig) throws FindException {
        PortalManagedEncass portalManagedEncass = null;
        final Policy policy = encapsulatedAssertionConfig.getPolicy();
        final String policyXml = policy.getXml();
        final Goid encassId = encapsulatedAssertionConfig.getGoid();
        if (policyXml != null && (policyXml.contains(ModuleConstants.TEMP_PORTAL_MANAGED_ENCASS_INDICATOR) || policyXml.contains(ModuleConstants.PORTAL_MANAGED_ENCASS_INDICATOR))) {
            try {
                final PortalManagedEncass fromPolicy = createPortalManagedEncass(encapsulatedAssertionConfig, policyXml,
                                                                                policy);
                if (fromPolicy != null) {
                    portalManagedEncass = fromPolicy;
                } else if (fromPolicy != null) {
                    logger.log(Level.WARNING, "Encass with id=" + encassId + " is not a valid Portal Managed Encass.");
                }
            } catch (final SAXException e) {
                throw new FindException("Error parsing Portal Managed Encass properties for encass with id=" + encassId, e);
            } catch (IOException e) {
                throw new FindException("Error parsing Portal Managed Encass properties for encass with id=" + encassId, e);
            }
        } else {
            logger.log(Level.FINE, "Encass with id=" + encassId + " is not a Portal Managed Encass.");
        }
        return portalManagedEncass;
    }

    /**
     * Can return null if all ApiPortalEncassIntegration assertions in the policy are disabled.
     */
    private PortalManagedEncass createPortalManagedEncass(EncapsulatedAssertionConfig encapsulatedAssertionConfig,
                                                          String policyXml, Policy policy) throws SAXException, FindException, IOException {
        PortalManagedEncass portalManagedEncass = null;
        final Document document = XmlUtil.parse(policyXml);
        final NodeList nodeList = document.getElementsByTagName(L7P_API_PORTAL_ENCASS_INTEGRATION);
        for (int i = 0; i < nodeList.getLength(); i++) {
            final Node node = nodeList.item(i);
            final NodeList children = node.getChildNodes();
            final PortalManagedEncass temp = new PortalManagedEncass();
            for (int j = 0; j < children.getLength(); j++) {
                final Node child = children.item(j);
                final String nodeName = child.getNodeName();
                if (nodeName != null && nodeName.equals(L7P_ENABLED)) {
                    final String enabledString = child.getAttributes().getNamedItem(BOOLEAN_VALUE).getTextContent();
                    temp.setEnabled(Boolean.valueOf(enabledString));
                }
            }
            // if more than one ApiPortalIntegration assertion exists, we only care about the first enabled one
            if (temp.isEnabled()) {
                portalManagedEncass = temp;
                portalManagedEncass.setEncassGuid(encapsulatedAssertionConfig.getGuid());
                portalManagedEncass.setEncassId(encapsulatedAssertionConfig.getId());
                setPortalManagedEncassDetails(portalManagedEncass, policy);
                break;
            } else {
                logger.log(Level.FINE, "Found disabled ApiPortalIntegration assertion");
            }
        }
        return portalManagedEncass;
    }

    private void setPortalManagedEncassDetails(final PortalManagedEncass portalManagedEncass, final Policy policy) throws IOException {
        final Assertion rootAssertion = policy.getAssertion();
        final RoutingAssertion routingAssertion = Assertion.find(rootAssertion, RoutingAssertion.class, true);
        portalManagedEncass.setHasRouting(routingAssertion != null);
    }

    @Override
    public Object[] getUpdateLocks() {
        return updateLocks;
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
}
