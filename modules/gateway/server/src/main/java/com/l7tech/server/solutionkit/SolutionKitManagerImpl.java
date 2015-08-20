package com.l7tech.server.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.ssgman.GatewayManagementInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.policy.bundle.ssgman.restman.VersionModifier;
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.*;
import org.apache.commons.lang.UnhandledException;
import org.apache.cxf.helpers.XMLUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SolutionKitManagerImpl extends HibernateEntityManager<SolutionKit, SolutionKitHeader> implements SolutionKitManager, PostStartupApplicationListener {
    private static final Logger logger = Logger.getLogger(SolutionKitManagerImpl.class.getName());

    private static final String REST_GATEWAY_MANAGEMENT_POLICY_XML =
        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">" +
            "<wsp:All wsp:Usage=\"Required\">" +
            "<L7p:RESTGatewayManagement>" +
            "<L7p:OtherTargetMessageVariable stringValue=\"request\"/>" +
            "<L7p:Target target=\"OTHER\"/>" +
            "</L7p:RESTGatewayManagement>" +
            "</wsp:All>" +
            "</wsp:Policy>";

    private final String HQL_FIND_BY_SOLUTION_KIT_GUID = "FROM " + getTableName() + " IN CLASS " + getImpClass().getName() + " WHERE " + getTableName() + ".solutionKitGuid = ?";
    private final String HQL_FIND_BY_PARENT_GOID = "FROM " + getTableName() + " IN CLASS " + getImpClass().getName() + " WHERE " + getTableName() + ".parentGoid = ?";

    private ServerAssertion serverRestGatewayManagementAssertion = null;
    private RestmanInvoker restmanInvoker;

    @Inject
    private ProtectedEntityTracker protectedEntityTracker;
    private Callable<Pair<AssertionStatus, RestmanMessage>> protectedEntityTrackerCallable;

    public SolutionKitManagerImpl() {
    }

    @Override
    public void initDao() throws Exception {
        super.initDao();

        if (null == protectedEntityTracker) {
            throw new IllegalStateException("Protected Entity Tracker component is required.");
        }

        updateProtectedEntityTracking();
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SolutionKit.class;
    }

    @Override
    public Goid save(SolutionKit entity) throws SaveException {
        entity.setLastUpdateTime(System.currentTimeMillis());
        if (null != entity.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY))
            logger.log(Level.INFO, "Solution Kit (" + entity.getName() + ") is about to be installed on Instance Modifier : " + entity.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));
        return super.save(entity);
    }

    @Override
    public void update(SolutionKit entity) throws UpdateException {
        entity.setLastUpdateTime(System.currentTimeMillis());
        super.update(entity);
        if (null != entity.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY))
            logger.log(Level.INFO, "Solution Kit (" + entity.getName() + ") has been upgraded on Instance Modifier : " + entity.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));
    }

    @Override
    public void delete(SolutionKit entity) throws DeleteException {
        entity.setLastUpdateTime(System.currentTimeMillis());
        super.delete(entity);
        if (null != entity.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY))
            logger.log(Level.INFO, "Solution Kit (" + entity.getName() + ") has been deleted on Instance Modifier : " + entity.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY));
    }

    @Override
    public void updateProtectedEntityTracking() throws FindException {
        List< Pair< EntityType, String> > solutionKitOwnedEntities = new ArrayList<>();

        for (SolutionKit solutionKit : findAll()) {
            for (EntityOwnershipDescriptor descriptor : solutionKit.getEntityOwnershipDescriptors()) {
                solutionKitOwnedEntities.add( Pair.pair( descriptor.getEntityType(), descriptor.getEntityId() ) );
            }
        }

        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(solutionKitOwnedEntities);
    }

    /**
     * This method's transactional propagation is set to NOT_SUPPORTED because the RESTMAN bundle importer code will import within
     * its own transaction and rollback if necessary.
     */
    @NotNull
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String importBundle(@NotNull final String bundle, @Nullable final String instanceModifier, boolean isTest) throws SaveException, SolutionKitException, Exception {
        final RestmanInvoker restmanInvoker = getRestmanInvoker();

        final String requestXml;
        try {
            if (VersionModifier.isValidVersionModifier(instanceModifier)) {
                final RestmanMessage requestMessage = new RestmanMessage(bundle);
                new VersionModifier(requestMessage.getBundleReferenceItems(), instanceModifier).apply();
                requestXml = requestMessage.getAsString();
            } else {
                requestXml = bundle;
            }

            final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);

            if (isTest) {
                pec.setVariable("RestGatewayMan.uri", "1.0/bundle?test=true");
            }

            // Allow solution kit installation/upgrade to "punch through" read-only entities
            Pair<AssertionStatus, RestmanMessage> result;
            try {
                result = protectedEntityTracker.doWithEntityProtectionDisabled(getProtectedEntityTrackerCallable(restmanInvoker, pec, requestXml));
            } catch ( GatewayManagementDocumentUtilities.AccessDeniedManagementResponse |
                    GatewayManagementDocumentUtilities.UnexpectedManagementResponse |
                    InterruptedException e) {
                throw e;
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }

            if (AssertionStatus.NONE != result.left) {
                String msg = "Unable to install bundle. Failed to invoke REST Gateway Management assertion: " + result.left.getMessage();
                logger.log(Level.WARNING, msg);
                throw new SolutionKitException(result.left.getMessage());
            }

            if (!isTest && result.right.hasMappingError()) {
                String msg = "Unable to install bundle due to mapping errors:\n" + result.right.getAsString();
                logger.log(Level.WARNING, msg);
                throw new SolutionKitException(result.right.getAsString());
            }
            return result.right.getAsString();
        } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse | IOException | SAXException e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw new SolutionKitException(ExceptionUtils.getMessage(e), e);
        } catch (GatewayManagementDocumentUtilities.UnexpectedManagementResponse e) {
            logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            throw getRestmanErrorDetail(e);
        } catch (InterruptedException e) {
            // do nothing.
        }

        return "";
    }

    /**
     * This method will parse out the restman response and look for the <l7:Detail> tag to get the error message
     * details.  If that string contains the word "Exception", it will create a new Exception based on that string
     * value and throw it as a regular Exception (this is for unhandled exceptions).  Not the ideal way to
     * do this, but given the SK codebase as is stands now, there isn't much of a choice.
     *
     * @param ex Restman exception
     * @return The error message detail string
     * @throws Exception
     */
    private SolutionKitException getRestmanErrorDetail(@NotNull GatewayManagementDocumentUtilities.UnexpectedManagementResponse ex) throws Exception {
        try {
            final Document doc = XmlUtil.parse(ExceptionUtils.getMessage(ex));
            final Element msgDetailsNode = XmlUtil.findExactlyOneChildElementByName(doc.getDocumentElement(), doc.getNamespaceURI(), "Detail");
            final String detailMsg = XmlUtil.getTextValue(msgDetailsNode, true);
            if (detailMsg.contains("Exception"))
                throw (new Exception(detailMsg));
            else
                return new SolutionKitException(detailMsg);
        } catch (final SAXException | MissingRequiredElementException | TooManyChildElementsException e) {
            throw ex;
        }
    }

    @NotNull
    @Override
    @Transactional(readOnly=true)
    public List<SolutionKit> findBySolutionKitGuid(@NotNull final String solutionKitGuid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<SolutionKit>>() {
                @Override
                protected List<SolutionKit> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                    final Query q = session.createQuery(HQL_FIND_BY_SOLUTION_KIT_GUID);
                    q.setParameter(0, solutionKitGuid);
                    return (List<SolutionKit>) q.list();
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Override
    public List<SolutionKitHeader> findAllChildrenByParentGoid(@NotNull final Goid parentGoid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<SolutionKitHeader>>() {
                @Override
                protected List<SolutionKitHeader> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                    final Query q = session.createQuery(HQL_FIND_BY_PARENT_GOID);
                    q.setParameter(0, parentGoid);
                    return convertToHTList(q.list());
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Override
    public List<SolutionKitHeader> findAllExcludingChildren() throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<SolutionKitHeader>>() {
                @Override
                protected List<SolutionKitHeader> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                    return convertToHTList(
                            session.createCriteria(SolutionKit.class).add(Restrictions.isNull("parentGoid")).list()
                    );
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Override
    public List<SolutionKitHeader> findParentSolutionKits() throws FindException {
        List<SolutionKitHeader> children = findChildSolutionKits();
        List<String> parentGoidStrList = new ArrayList<>(children.size());
        for (SolutionKitHeader child: children) {
            String parentGoidStr = child.getParentGoid().toString();
            if (parentGoidStrList.contains(parentGoidStr)) {
                continue;
            } else {
                parentGoidStrList.add(parentGoidStr);
            }
        }

        List<SolutionKitHeader> parentList = new ArrayList<>();
        for (String goidStr: parentGoidStrList) {
            parentList.add(new SolutionKitHeader(findByPrimaryKey(Goid.parseGoid(goidStr))));
        }

        return parentList;
    }

    private List<SolutionKitHeader> findChildSolutionKits() throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<SolutionKitHeader>>() {
                @Override
                protected List<SolutionKitHeader> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                    return convertToHTList(
                            session.createCriteria(SolutionKit.class).add(Restrictions.isNotNull("parentGoid")).list()
                    );
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    private List<SolutionKitHeader> convertToHTList(@NotNull List<SolutionKit> skList) {
        List<SolutionKitHeader>  htList = new ArrayList<>(skList.size());
        for (SolutionKit solutionKit: skList) {
            htList.add(new SolutionKitHeader(solutionKit));
        }
        return htList;
    }

    @Override
    protected SolutionKitHeader newHeader(SolutionKit entity) {
        return new SolutionKitHeader(entity);
    }

    @Override
    protected UniqueType getUniqueType() {
        // todo (kpak) - Change to UniqueType.OTHER, and override getUniqueConstraints() method to return name and prefix.
        return UniqueType.NONE;
    }

    // used for unit testing
    protected void setRestmanInvoker(final RestmanInvoker restmanInvoker) {
        this.restmanInvoker = restmanInvoker;
    }
    protected void setProtectedEntityTracker(final ProtectedEntityTracker protectedEntityTracker) {
        this.protectedEntityTracker = protectedEntityTracker;
    }
    protected void setProtectedEntityTrackerCallable(Callable<Pair<AssertionStatus, RestmanMessage>> protectedEntityTrackerCallable) {
        this.protectedEntityTrackerCallable = protectedEntityTrackerCallable;
    }

    private Callable<Pair<AssertionStatus, RestmanMessage>> getProtectedEntityTrackerCallable(final RestmanInvoker restmanInvoker, final PolicyEnforcementContext pec, final String requestXml) {
        if (protectedEntityTrackerCallable != null) {
            // unit test callable
            return protectedEntityTrackerCallable;
        } else {
            return new Callable<Pair<AssertionStatus, RestmanMessage>>() {
                @Override
                public Pair<AssertionStatus, RestmanMessage> call() throws Exception {
                    return restmanInvoker.callManagementCheckInterrupted( pec, requestXml );
                }
            };
        }
    }

    private RestmanInvoker getRestmanInvoker() throws SolutionKitException {
        if (restmanInvoker != null) {
            // unit test invoker
            return restmanInvoker;
        } else {
            // create RestmanInvoker
            if (serverRestGatewayManagementAssertion == null) {
                WspReader wspReader = this.applicationContext.getBean("wspReader", WspReader.class);
                ServerPolicyFactory serverPolicyFactory = this.applicationContext.getBean("policyFactory", ServerPolicyFactory.class);
                try {
                    Assertion assertion = wspReader.parseStrictly(REST_GATEWAY_MANAGEMENT_POLICY_XML, WspReader.Visibility.omitDisabled);
                    serverRestGatewayManagementAssertion = serverPolicyFactory.compilePolicy(assertion, false);
                } catch (IOException | ServerPolicyException | LicenseException e) {
                    logger.log(Level.WARNING, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                    throw new SolutionKitException("Unable to initialize ServerRESTGatewayManagementAssertion.", e);
                }
            }

            GatewayManagementInvoker invoker = new GatewayManagementInvoker() {
                @Override
                public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException, IOException {
                    return serverRestGatewayManagementAssertion.checkRequest(context);
                }
            };

            return new RestmanInvoker(new Functions.Nullary<Boolean>() {
                @Override
                public Boolean call() {
                    // nothing to do in cancelled callback.
                    return true;
                }
            }, invoker);
        }
    }

    public void handleEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent invalidationEvent = (EntityInvalidationEvent) event;
            if (SolutionKit.class.equals(invalidationEvent.getEntityClass())) {
                try {
                    updateProtectedEntityTracking();
                } catch (FindException e) {
                    logger.log(Level.WARNING, "Unable to update ProtectedEntityTracker: " + ExceptionUtils.getMessage(e),
                            ExceptionUtils.getDebugException(e));
                }
            }
        }
    }

    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        handleEvent(event);
    }
}