package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AdminAuditRecord;
import com.l7tech.gateway.common.audit.AuditRecord;
import com.l7tech.gateway.common.audit.AuditRecordHeader;
import com.l7tech.gateway.common.audit.AuditSearchCriteria;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Conjunction;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class SimpleAuditRecordManagerImpl extends HibernateEntityManager<AuditRecord, AuditRecordHeader>
        implements SimpleAuditRecordManager {

    //- PUBLIC

    @Override
    @Transactional(readOnly = true)
    public List<AuditRecordHeader> findHeaders(final AuditSearchCriteria criteria) throws FindException {
        final Class findClass = getFindClass(criteria);

        // Enable discover of whether an audit is a message audit through the presence of not null request id property.
        // If this property is not available then the audit record is not a message audit.
        // This property can only be searched for when it may exist. If it will not exist e.g. were looking for admin
        // or system records, then it cannot be included in the projection or in the result processing.
        final boolean hasRequestIdProperty = findClass == getInterfaceClass() || findClass == MessageSummaryAuditRecord.class;

        final Functions.UnaryVoid<Criteria> criteriaConfigurator = new Functions.UnaryVoid<Criteria>(){
            @Override
            public void call(final Criteria hibernateCriteria) {

                final ProjectionList projectionList = Projections.projectionList()
                        .add(Property.forName( PROP_OID ))
                        .add(Property.forName( PROP_NAME ))
                        .add(Property.forName( PROP_MESSAGE ))
                        .add(Property.forName( PROP_SIGNATURE ))
                        .add(Property.forName( PROP_NODEID ))
                        .add(Property.forName( PROP_TIME ))
                        .add(Property.forName( PROP_LEVEL ))
                        ;

                if (hasRequestIdProperty) {
                    projectionList.add(Property.forName( PROP_REQUEST_ID ));
                }

                if (criteria.messageId != null) {
                    //ensure distinct results only - only an issue when joining with audit_detail
                    hibernateCriteria.setProjection(Projections.distinct(projectionList));

                    hibernateCriteria.createAlias("details", "ad");
                    final SimpleExpression eq = Restrictions.eq( "ad.messageId", criteria.messageId );
                    hibernateCriteria.add(eq);
                } else {
                    hibernateCriteria.setProjection(projectionList);
                }
            }
        };

        //todo: Could a ResultTransformer be useful here and simplify the mapping of results to AuditRecordHeader?
        List<AuditRecordHeader> auditRecordHeaders = find(criteria, criteriaConfigurator, new Functions.BinaryVoid<List<AuditRecordHeader>, Object>() {
            @Override
            public void call(List<AuditRecordHeader> auditRecordHeaders, Object o) {
                final Object [] values = (Object[]) o;

                boolean isMessageAudit = hasRequestIdProperty && values[7] != null;

                final Long id = Long.valueOf(values[0].toString());
                final String name = (values[1] == null || !isMessageAudit) ? null : values[1].toString();
                final String description = values[2].toString();
                final String signature = (values[3] == null) ? null : values[3].toString();
                final String nodeId = values[4].toString();
                final Long timestamp = Long.valueOf(values[5].toString());
                final Level level = Level.parse(values[6].toString());

                auditRecordHeaders.add(new AuditRecordHeader(
                        id,
                        name,
                        description,
                        null,
                        signature,
                        nodeId,
                        timestamp,
                        level,
                        0 //version property is not used for AuditRecord or sub classes!
                ));
            }
        });

        return Collections.unmodifiableList( auditRecordHeaders );
    }

    @Override
    @Transactional(readOnly = true)
    public int findCount( final AuditSearchCriteria criteria ) throws FindException {
        return findCount( criteria.recordClass, asCriterion( criteria ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<AuditRecord> findPage( final SortProperty sortProperty,
                                             final boolean ascending,
                                             final int offset,
                                             final int count,
                                             final AuditSearchCriteria criteria) throws FindException {
        return findPage( criteria.recordClass, sortProperty.getPropertyName(), ascending, offset, count, asCriterion( criteria ) );
    }


    @Override
    public Map<Long, byte[]> getDigestForAuditRecords(final Collection<Long> auditRecordIds) throws FindException {
        if(auditRecordIds == null) throw new NullPointerException("auditRecordIds is null");

        final Map<Long, byte[]> returnMap = new HashMap<Long, byte[]>();

        if (auditRecordIds.isEmpty()) {
            return returnMap;
        }

        Session session = null;
        try {

            final int maxRecords = config.getIntProperty( ServerConfigParams.PARAM_AUDIT_SIGN_MAX_VALIDATE, 100);

            if (auditRecordIds.size() > maxRecords) {
                int difference = auditRecordIds.size() - maxRecords;
                final Iterator<Long> iterator = auditRecordIds.iterator();
                int index = 0;
                while (iterator.hasNext() && index < difference) {
                    iterator.remove();
                    index++;
                }
                logger.log( Level.INFO, "Number of audits to digest reduced to limit of " + maxRecords + " from " + auditRecordIds.size() );
            }

            session = getSession();
            // Note: this produces exactly the same set of queries as session.createCriteria(interfaceClass).scroll()
            // This query must perform multiple queries to avoid cartesian product as there are several possible collections
            // to fill in the audit record hierarchy.
            final Query query = session.createQuery( HQL_SELECT_AUDIT_RECORDS_SIZE_PROTECTED );
            query.setParameterList( IDS_PARAMETER, auditRecordIds);
            final int maxMsgSize = config.getIntProperty( ServerConfigParams.PARAM_AUDIT_SEARCH_MAX_MESSAGE_SIZE, 2621440);
            query.setInteger( MAX_SIZE_PARAMETER, maxMsgSize);

            final ScrollableResults results = query.scroll();

            while (results.next()) {
                AuditRecord record = (AuditRecord) results.get(0);
                String sig = record.getSignature();
                if (sig != null && !sig.isEmpty()) {
                    final byte[] digest = record.computeSignatureDigest();
                    returnMap.put(record.getOid(), digest);
                }
                session.evict(record);
            }
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        } finally {
            releaseSession(session);
        }

        return returnMap;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Class<AuditRecord> getImpClass() {
        return AuditRecord.class;
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public String getTableName() {
        return "audit_main";
    }

    public void setServerConfig(Config config ) {
        this.config = config;
    }

    //- PROTECTED

    protected Config config;

    @Override
    protected AuditRecordHeader newHeader(final AuditRecord auditRecord) {
        final AuditRecordHeader arh = new AuditRecordHeader(auditRecord);
        final String sig = auditRecord.getSignature();
        arh.setSignatureDigest(sig != null && sig.length() > 0 ? auditRecord.computeSignatureDigest() : null);
        return arh;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    //- PRIVATE

    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private static final Logger logger = Logger.getLogger( SimpleAuditRecordManagerImpl.class.getName() );

    private static final Level[] LEVELS_IN_ORDER = { Level.ALL, Level.FINEST, Level.FINER, Level.FINE, Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE, Level.OFF };
    private static final String PROP_TIME = "millis";
    private static final String PROP_LEVEL = "strLvl";
    private static final String PROP_OID = "oid";
    private static final String PROP_NODEID = "nodeId";
    private static final String PROP_MESSAGE = "message";
    private static final String PROP_SIGNATURE = "signature";
    private static final String PROP_SERVICE_NAME = "name";
    private static final String PROP_REQUEST_ID = "strRequestId";
    private static final String PROP_NAME = "name";
    private static final String PROP_PROV_ID = "identityProviderOid";
    private static final String PROP_USER_ID = "userId";
    private static final String PROP_USER_NAME = "userName";
    private static final String PROP_ENTITY_CLASS = "entityClassname";
    private static final String PROP_ENTITY_ID = "entityOid";

    private static final String IDS_PARAMETER = "ids";
    private static final String MAX_SIZE_PARAMETER = "maxSize";
    /**
     * Concrete class tables are outer joined and hibernate generates correct sql to safely reference message_audit only
     * properties.
     */
    private static final String HQL_SELECT_AUDIT_RECORDS_SIZE_PROTECTED = "from AuditRecord where oid in (:"+ IDS_PARAMETER +") and (requestXml is null or length(requestXml) < :"+ MAX_SIZE_PARAMETER +") and (responseXml is null or length(responseXml) < :"+ MAX_SIZE_PARAMETER +")";

    private <T> List<T> find( final AuditSearchCriteria criteria,
                              final Functions.UnaryVoid<Criteria> criteriaConfigurator,
                              final Functions.BinaryVoid<List<T>, Object> resultProcessor) throws FindException {
        if (criteria == null) throw new IllegalArgumentException("Criteria must not be null");

        int maxRecords = getMaxRecords(criteria);
        List<T> result = new ArrayList<T>(maxRecords);

        // If search criteria contain conflicts (for example, Request ID and Entity ID are specified in "criteria"),
        // then no search results will be returned.
        if (hasConflictSearchCriteria(criteria)) return result;

        Session session = null;
        try {
            session = getSession();

            final Criteria hibernateCriteria = getHibernateCriteriaFromAuditCriteria(criteria, session, maxRecords);
            if (criteriaConfigurator != null) {
                criteriaConfigurator.call(hibernateCriteria);
            }

            ScrollableResults results = hibernateCriteria.scroll();
            while( results.next() ) {
                //note if a project is used, this will be an array, otherwise it will be the entity type.
                //todo: refactor as only used for projections
                final Object object = results.get();
                resultProcessor.call(result, object);
                session.evict(object);
            }

            return result;
        } catch ( HibernateException e ) {
            throw new FindException("Couldn't find Audit Records", e);
        } finally {
            releaseSession(session);
        }
    }

    private int getMaxRecords(AuditSearchCriteria criteria) {
        int maxRecords = criteria.maxRecords;
        if (maxRecords <= 0) maxRecords = 4096; //todo: this default is different to UI default. Should be consistent.
        return maxRecords;
    }

    private Criteria getHibernateCriteriaFromAuditCriteria(final AuditSearchCriteria criteria,
                                                           final Session session,
                                                           final int maxRecords) throws FindException {
        Class findClass = getFindClass(criteria);

        Criteria hibernateCriteria = session.createCriteria(findClass);
        hibernateCriteria.setMaxResults(maxRecords);

        for ( Criterion criterion : asCriterion( criteria ) ) {
            hibernateCriteria.add( criterion );
        }

        hibernateCriteria.addOrder( Order.desc( PROP_TIME ));

        return hibernateCriteria;
    }

    private Class getFindClass(AuditSearchCriteria criteria) {
        Class findClass = criteria.recordClass;
        if (findClass == null) findClass = getInterfaceClass();

        if(criteria.requestId != null && findClass != MessageSummaryAuditRecord.class) findClass = MessageSummaryAuditRecord.class;
        return findClass;
    }

    /**
     * Check if search criteria have conflict.  The "conflict" means that Request ID and Entity Search Parameters can exist at the same time in the search criteria.
     * @param criteria: the search criteria.
     * @return true if the search criteria include Request ID and Entity Search Parameters.
     */
    private boolean hasConflictSearchCriteria(final AuditSearchCriteria criteria) {
        final boolean requestIdEnabled = criteria.requestId != null && !criteria.requestId.trim().isEmpty();
        final boolean entityClassEnabled = criteria.entityClassName != null && !criteria.entityClassName.trim().isEmpty();
        final boolean entityIdEnabled = criteria.entityId != null;

        return requestIdEnabled && (entityClassEnabled || entityIdEnabled);
    }

    private Criterion[] asCriterion( final AuditSearchCriteria criteria ) throws FindException {
        List<Criterion> criterion = new ArrayList<Criterion>();

        Date fromTime = criteria.fromTime;
        Date toTime = criteria.toTime;

        if (fromTime != null) criterion.add(Restrictions.ge( PROP_TIME, fromTime.getTime()));
        if (toTime != null) criterion.add(Restrictions.lt( PROP_TIME, toTime.getTime()));

        Level fromLevel = criteria.fromLevel;
        if (fromLevel == null) fromLevel = Level.FINEST;
        Level toLevel = criteria.toLevel;
        if (toLevel == null) toLevel = Level.SEVERE;

        if (fromLevel.equals(toLevel)) {
            criterion.add(Restrictions.eq( PROP_LEVEL, fromLevel.getName()));
        } else if (fromLevel.equals( Level.ALL ) && toLevel.equals( Level.OFF )) {
            // no restriction
        } else {
            if (fromLevel.intValue() > toLevel.intValue())
                throw new FindException("fromLevel " + fromLevel.getName() + " is not lower in value than toLevel " + toLevel.getName());

            Set<String> levels = new HashSet<String>();
            for (Level level : LEVELS_IN_ORDER ) {
                if (level.intValue() >= fromLevel.intValue() && level.intValue() <= toLevel.intValue()) {
                    levels.add(level.getName());
                }
            }
            criterion.add(Restrictions.in( PROP_LEVEL, levels));
        }

        // Note: a start or end constraint cannot be supplied with out an associated node id.
        // object ids do not increment across a cluster, they increment for a node.

        final Map<String, Long> nodeIdToStartMsg = criteria.nodeIdToStartMsg;
        if (!nodeIdToStartMsg.isEmpty()) {
            criterion.add(getObjectIdForNodeDisjunction(nodeIdToStartMsg, true));
        }

        final Map<String, Long> nodeIdToEndMsg = criteria.nodeIdToEndMsg;
        if (!nodeIdToEndMsg.isEmpty()) {
            criterion.add(getObjectIdForNodeDisjunction(nodeIdToEndMsg, false));
        }

        if (criteria.requestId != null) criterion.add(Restrictions.ilike( PROP_REQUEST_ID, criteria.requestId));
        if (criteria.serviceName != null) criterion.add(Restrictions.ilike( PROP_SERVICE_NAME, criteria.serviceName));

        if (criteria.message != null) criterion.add(Restrictions.ilike( PROP_MESSAGE, criteria.message));
        if (criteria.nodeId != null) criterion.add(Restrictions.eq( PROP_NODEID, criteria.nodeId));

        if (criteria.user != null) {
            criterion.add(Restrictions.eq( PROP_PROV_ID, criteria.user.getProviderId()));
            criterion.add(Restrictions.eq( PROP_USER_ID, criteria.user.getId()));
        } else {
            if (criteria.userName != null) {
                criterion.add(Restrictions.ilike( PROP_USER_NAME, criteria.userName));
            }
            if (criteria.userIdOrDn != null) {
                criterion.add(Restrictions.ilike( PROP_USER_ID, criteria.userIdOrDn));
            }
        }

        // Entity Type and Entity ID search criteria are only applied to Audit Type, ANY and Admin.
        final Class auditRecordType = criteria.recordClass;
        if (auditRecordType == null || auditRecordType.equals(AdminAuditRecord.class)) {
            if (criteria.entityClassName != null && !criteria.entityClassName.trim().isEmpty()) {
                criterion.add(Restrictions.eq( PROP_ENTITY_CLASS, criteria.entityClassName));
            }

            if (criteria.entityId != null) {
                criterion.add(Restrictions.eq( PROP_ENTITY_ID, criteria.entityId));
            }
        }

        return criterion.toArray( new Criterion[criterion.size()] );
    }

    /**
     * Get a disjunction which contains all nodes in the supplied map. If the returned Disjunction is added to a criteria
     * then only results for nodes contained within nodeIdToObjectIdValue will be returned.
     *
     * Returned disjunction will represent the following SQL:
     * ((nodeid=? and objectid <|> ?) or (nodeid=? and objectid <|> ?)) , one AND block for each node id in the supplied map.
     *
     * @param nodeIdToObjectIdValue map of node id to an object id, which will have a constraint applied to it.
     * Cannot be null or empty.
     * @param greaterThan if true, then the constraint added is Restrictions.gt, otherwise Restrictions.lt
     * @return Disjunction to add to a hibernate criteria object.
     */
    private Disjunction getObjectIdForNodeDisjunction(final Map<String, Long> nodeIdToObjectIdValue, boolean greaterThan) {
        final Disjunction disjunction = Restrictions.disjunction();
        for (Map.Entry<String, Long> nodeToObjectId : nodeIdToObjectIdValue.entrySet()) {
            final Conjunction conjunction = Restrictions.conjunction();
            conjunction.add(Restrictions.eq( PROP_NODEID, nodeToObjectId.getKey()));
            if (greaterThan) {
                conjunction.add(Restrictions.gt( PROP_OID, nodeToObjectId.getValue()));
            } else {
                conjunction.add(Restrictions.lt( PROP_OID, nodeToObjectId.getValue()));
            }

            disjunction.add(conjunction);
        }

        return disjunction;
    }
}
