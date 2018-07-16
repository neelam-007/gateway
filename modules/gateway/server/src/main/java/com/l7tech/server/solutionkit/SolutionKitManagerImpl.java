package com.l7tech.server.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.solutionkit.*;
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
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.server.util.PostStartupTransactionalApplicationListener;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StringType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.inject.Inject;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker.*;

public class SolutionKitManagerImpl extends HibernateEntityManager<SolutionKit, SolutionKitHeader> implements SolutionKitManager, PostStartupTransactionalApplicationListener {
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

    private final String DELETE_BUNDLE = "Delete Bundle";
    private final String INSTALL_BUNDLE = "Install Bundle";

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

    /**
     * Reads and enables entity protection for all Solution Kit-owned entities.
     * This method should be invoked after any EntityOwnershipDescriptors have been added e.g. in the process of Solution Kit installation/upgrade/removal.
     *
     * @throws FindException in case there is an error getting solution kits from the database.
     */
    private void updateProtectedEntityTracking() throws FindException {
        final List< Pair< EntityType, String> > solutionKitOwnedEntities = new ArrayList<>();

        // keeps last updated EntityOwnershipDescriptor (i.e. the one with latest version_stamp)
        final Map<String, EntityOwnershipDescriptor> entityOwnerships = new HashMap<>();

        for (final SolutionKit solutionKit : findAll()) {
            for (final EntityOwnershipDescriptor descriptor : solutionKit.getEntityOwnershipDescriptors()) {
                final EntityOwnershipDescriptor entityOwnership = entityOwnerships.get(descriptor.getEntityId());
                if (entityOwnership != null) {
                    // entity owned by another skar, so make sure we are keeping the latest one
                    if (descriptor.getVersionStamp() >= entityOwnership.getVersionStamp()) {
                        entityOwnerships.put(descriptor.getEntityId(), descriptor);
                    }
                } else {
                    entityOwnerships.put(descriptor.getEntityId(), descriptor);
                }
            }
        }

        // finally loop through our filtered entityOwnerships map
        for (final EntityOwnershipDescriptor descriptor : entityOwnerships.values()) {
            if (descriptor.isReadOnly()) {
                solutionKitOwnedEntities.add(Pair.pair(descriptor.getEntityType(), descriptor.getEntityId()));
            }
        }

        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(solutionKitOwnedEntities);
    }
    /**
     * This method's transactional propagation is set to NOT_SUPPORTED because the RESTMAN bundle/batch importer code will import within
     * its own transaction and rollback if necessary.
     */
    @NotNull
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String importBundles(@NotNull SolutionKitImportInfo solutionKitImportInfo,
                                boolean isTest) throws Exception {
        final RestmanInvoker restmanInvoker = getRestmanInvoker();
        final List<SolutionKit> solutionKitsToDelete = solutionKitImportInfo.getSolutionKitsToDelete();
        final Map<SolutionKit, String> solutionKitsToInstallMap = solutionKitImportInfo.getSolutionKitsToInstall();
        // set restman query parameters including test and versionComment
        final SolutionKit solutionKitForRestMan = SolutionKitUtils.solutionKitToDisplayForUpgrade(solutionKitsToInstallMap.keySet(), solutionKitImportInfo.getParentSolutionKit());
        try {
            //Process delete bundles
            final List<Bundle> processedDeleteBundles = new ArrayList<>();
            for (final SolutionKit solutionKit : solutionKitsToDelete) {
                final Bundle bundle = MarshallingUtils.unmarshal(Bundle.class, new StreamSource(new StringReader(solutionKit.getUninstallBundle())), true);
                bundle.setName(solutionKit.getName() + " " + DELETE_BUNDLE);
                processedDeleteBundles.add(bundle);
            }

            //Process install bundles
            final List<Bundle> processedInstallBundles = new ArrayList<>();
            for (Map.Entry<SolutionKit, String> skWithInstall : solutionKitsToInstallMap.entrySet()) {
                final String instanceModifier = skWithInstall.getKey().getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
                String bundleXml = skWithInstall.getValue();
                if (InstanceModifier.isValidVersionModifier(instanceModifier)) {
                    logger.fine("Applied instance modifier (" + instanceModifier + ") modifications for " + skWithInstall.getKey().getName());
                    final RestmanMessage requestMessage = new RestmanMessage(bundleXml);
                    new InstanceModifier(requestMessage.getBundleReferenceItems(), requestMessage.getMappings(), instanceModifier).apply();
                    bundleXml = requestMessage.getAsString();
                }
                final Bundle bundle = MarshallingUtils.unmarshal(Bundle.class, new StreamSource(new StringReader(bundleXml)), true);
                bundle.setName(skWithInstall.getKey().getName() + " " + INSTALL_BUNDLE);
                processedInstallBundles.add(bundle);
            }

            final String requestXml = SolutionKitUtils.generateBundleListPayload(processedDeleteBundles, processedInstallBundles);

            final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);

            pec.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE_BATCH  + getRestmanQueryParameters(solutionKitForRestMan, isTest));

            // Allow solution kit installation/upgrade to "punch through" read-only entities
            final Pair<AssertionStatus, RestmanMessage> result;
            try {
                result = protectedEntityTracker.doWithEntityProtectionDisabled(getProtectedEntityTrackerCallable(restmanInvoker, pec, requestXml));
            } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse |
                    GatewayManagementDocumentUtilities.UnexpectedManagementResponse |
                    InterruptedException e) {
                throw e;
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            }

            if (AssertionStatus.NONE != result.left) {
                String msg = "Unable to install bundles. Failed to invoke REST Gateway Management assertion: " + result.left.getMessage();
                logger.log(Level.WARNING, msg);
                throw new SolutionKitException(result.left.getMessage());
            }

            if (!isTest && result.right.hasMappingErrorFromBundles()) {
                String msg = "Unable to install bundles due to mapping errors:\n" + result.right.getAsString();
                logger.log(Level.WARNING, msg);
                throw new BadRequestException(result.right.getAsString());
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
     * This method's transactional propagation is set to NOT_SUPPORTED because the RESTMAN bundle importer code will import within
     * its own transaction and rollback if necessary.
     */
    @NotNull
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String importBundle(@NotNull final String bundle, @NotNull final SolutionKit metadata, boolean isTest) throws Exception {
        final RestmanInvoker restmanInvoker = getRestmanInvoker();

        final String requestXml;
        try {
            final String instanceModifier = metadata.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);
            if (InstanceModifier.isValidVersionModifier(instanceModifier)) {
                final RestmanMessage requestMessage = new RestmanMessage(bundle);
                new InstanceModifier(requestMessage.getBundleReferenceItems(), requestMessage.getMappings(), instanceModifier).apply();
                requestXml = requestMessage.getAsString();
            } else {
                requestXml = bundle;
            }

            final PolicyEnforcementContext pec = restmanInvoker.getContext(requestXml);

            // set restman query parameters including test and versionComment
            pec.setVariable(VAR_RESTMAN_URI, URL_1_0_BUNDLE + getRestmanQueryParameters(metadata, isTest));

            // Allow solution kit installation/upgrade to "punch through" read-only entities
            Pair<AssertionStatus, RestmanMessage> result;
            try {
                result = protectedEntityTracker.doWithEntityProtectionDisabled(getProtectedEntityTrackerCallable(restmanInvoker, pec, requestXml));
            } catch (GatewayManagementDocumentUtilities.AccessDeniedManagementResponse |
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
                throw new BadRequestException(result.right.getAsString());
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
            // get error type
            final Element msgTypeNode = XmlUtil.findExactlyOneChildElementByName(doc.getDocumentElement(), doc.getNamespaceURI(), "Type");
            final String errorType = XmlUtil.getTextValue(msgTypeNode, true);
            // get error message
            final Element msgDetailsNode = XmlUtil.findExactlyOneChildElementByName(doc.getDocumentElement(), doc.getNamespaceURI(), "Detail");
            final String detailMsg = XmlUtil.getTextValue(msgDetailsNode, true);
            // BundleResource.importBundle fails with either CONFLICT or BAD_REQUEST (in case one of the entities in the bundle are invalid i.e. throws ResourceFactory.InvalidResourceException)
            // CONFLICT should be handled by test so it is of no interest here
            // BAD_REQUEST i.e. when ResourceFactory.InvalidResourceException is throw the error type is "InvalidResource", as per ExceptionMapper.handleOperationException()
            // TODO: if one of the above methods are changed this logic must be changed as well
            if ("InvalidResource".equalsIgnoreCase(errorType)) {
                return new BadRequestException(detailMsg);
            } else if (detailMsg.contains("Exception")) {
                throw new Exception(detailMsg);
            } else {
                return new SolutionKitException(detailMsg);
            }
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
    public SolutionKit findBySolutionKitGuidAndIM(@NotNull String solutionKitGuid, @Nullable String instanceModifier) throws FindException {
        List<SolutionKit> solutionKits = findBySolutionKitGuid(solutionKitGuid);

        String tempIM;
        for (SolutionKit solutionKit: solutionKits) {
            tempIM = solutionKit.getProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY);

            if ((StringUtils.isBlank(instanceModifier) && StringUtils.isBlank(tempIM)) ||
                (instanceModifier != null && instanceModifier.equals(tempIM))) {
                return solutionKit;
            }
        }

        return null;
    }

    @Override
    public List<SolutionKitHeader> findAllChildrenHeadersByParentGoid(@NotNull final Goid parentGoid) throws FindException {
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

    @NotNull
    @Override
    public Collection<SolutionKit> findAllChildrenByParentGoid(@NotNull final Goid parentGoid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Collection<SolutionKit>>() {
                @Override
                protected Collection<SolutionKit> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                    final Query q = session.createQuery(HQL_FIND_BY_PARENT_GOID);
                    q.setParameter(0, parentGoid);
                    final Collection<SolutionKit> kits = q.list();
                    return kits == null ? Collections.<SolutionKit>emptyList() : kits;
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }


    // SQL for decrementing version_stamp for all entities owned by other solution kits (as this skGoid now takes over readonly-ness)
    private static final String SQL_DECREMENT_ENTITIES_VERSION_STAMP = "UPDATE solution_kit_meta set version_stamp=version_stamp-1 WHERE entity_id in (:ids) and solution_kit_goid<>:skGoid";

    @Override
    @Transactional(readOnly=false)
    public void decrementEntitiesVersionStamp(@NotNull final Collection<String> entityIds, @NotNull final Goid solutionKit) throws UpdateException {
        if (!entityIds.isEmpty()) {
            try {
                getHibernateTemplate().execute(new HibernateCallback<Void>() {
                    @Override
                    public Void doInHibernate(final Session session) throws HibernateException, SQLException {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "Decrementing version_stamp of entities: " + CollectionUtils.mkString(entityIds, ",") + " owning kit is: " + solutionKit.toString() + "...");
                        }
                        final SQLQuery q = session.createSQLQuery(SQL_DECREMENT_ENTITIES_VERSION_STAMP);
                        q.setParameterList("ids", entityIds, new StringType());
                        q.setBinary("skGoid", solutionKit.getBytes());
                        final int update = q.executeUpdate();
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, "version_stamp decremented for " + update + " entities.");
                        }
                        return null;
                    }
                });
            } catch (Exception he) {
                throw new UpdateException(he.toString(), he);
            }
        }
    }

    /**
     * Creates and returns a new ApplicationListener.
     * Currently this method is only used once while initializing PostStartupTransactionalApplicationListener. {@link StartupListenerRegistration#start()}
     * If method usage increases in the future, consider using a lazy initializer for the listener.
     * @return new ApplicationListener
     */
    @NotNull
    @Override
    public ApplicationListener getListener() {
        return new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent event) {
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
        };
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

    protected RestmanInvoker getRestmanInvoker() throws SolutionKitException {
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

    protected List<SolutionKitHeader> convertToHTList(@NotNull List<SolutionKit> skList) {
        List<SolutionKitHeader>  htList = new ArrayList<>(skList.size());
        for (SolutionKit solutionKit: skList) {
            htList.add(new SolutionKitHeader(solutionKit));
        }
        return htList;
    }

    // get restman query parameters (e.g. 1.0/bundle?versionComment=Simple+Service+and+Other+Dependencies+%28v1.1%29&test=true )
    private String getRestmanQueryParameters(final SolutionKit metadata, final boolean isTest) throws UnsupportedEncodingException {
        final String policyRevisionComment = MessageFormat.format("{0} (v{1})", metadata.getName(), metadata.getSolutionKitVersion());

        StringBuilder restmanQueryParams = new StringBuilder().append("?");
        try {
            restmanQueryParams.append("versionComment=").append(URLEncoder.encode(policyRevisionComment, UTF_8));
        } catch (UnsupportedEncodingException e) {
            logger.warning("Unexpected exception encoding policy revision comment '" + policyRevisionComment + "' using " + UTF_8);
            throw e;
        }
        if (isTest) {
            restmanQueryParams.append("&test=true");
        }

        return restmanQueryParams.toString();
    }
}