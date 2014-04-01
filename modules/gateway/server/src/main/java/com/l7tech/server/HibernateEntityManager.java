package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.imp.PersistentEntityUtil;
import com.l7tech.server.event.RoleAwareEntityDeletionEvent;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.jdbc.Work;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Table;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.reduce;
import static org.hibernate.criterion.Restrictions.conjunction;
import static org.hibernate.criterion.Restrictions.disjunction;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;

/**
 * The default {@link com.l7tech.objectmodel.EntityManager} implementation for Hibernate-managed
 * {@link com.l7tech.objectmodel.EntityManager persistent entities}.
 *
 * Implementations only need to implement {@link #getInterfaceClass()}, {@link #getImpClass()} and
 * {@link #getTableName()}, although two of those are only for historical reasons.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public abstract class HibernateEntityManager<ET extends PersistentEntity, HT extends EntityHeader>
        extends HibernateDaoSupport
        implements EntityManager<ET, HT>, ApplicationContextAware
{
    public static final String EMPTY_STRING = "";
    public static final String F_GOID = "goid";
    public static final String F_NAME = "name";
    public static final String F_VERSION = "version";

    public static final Object NULL = new Object();
    public static final Object NOTNULL = new Object();

    private final String HQL_FIND_ALL_GOIDS_AND_VERSIONS =
            "SELECT " +
                    getTableName() + "." + F_GOID + ", " +
                    getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName();

    private final String HQL_FIND_VERSION_BY_GOID =
            "SELECT " + getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_GOID + " = ?";

    private final String HQL_FIND_BY_GOID =
            "FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_GOID + " = ?";

    private final String HQL_DELETE_BY_GOID =
            "FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_GOID + " = ?";

    protected PlatformTransactionManager transactionManager; // required for TransactionTemplate
    protected ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    @Transactional(readOnly=true)
    public ET findByPrimaryKey(final Goid goid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<ET>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public ET doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_BY_GOID);
                    q.setParameter(0, goid);
                    final ET et = (ET) q.uniqueResult();
                    initializeLazilyLoaded(et);
                    return et;
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    /**
     * Find a single entity by a unique key. Any Entity which defines a unique key can use this.
     *
     * @param uniquePropertyName String name of the property (not the field!) which is unique. This value must be
     * the property from the Entity class without the 'get' prefix.
     * @param uniqueKey long value of the unique field
     * @return the entity by that name, or null if none was found.
     * @throws com.l7tech.objectmodel.FindException in the event of a database problem
     */
    @Transactional(readOnly = true)
    protected ET findByUniqueKey(final String uniquePropertyName, final long uniqueKey) throws FindException {
        if (uniquePropertyName == null) throw new NullPointerException();
        if (uniquePropertyName.trim().isEmpty())
            throw new IllegalArgumentException("uniquePropertyName cannot be empty");

        return findUnique( Collections.<String,Object>singletonMap( uniquePropertyName, uniqueKey ) );
    }

    /**
     * Find a single entity by a unique key. Any Entity which defines a unique key can use this.
     *
     * @param uniquePropertyName String name of the property (not the field!) which is unique. This value must be
     * the property from the Entity class without the 'get' prefix.
     * @param uniqueKey Goid value of the unique field
     * @return the entity by that name, or null if none was found.
     * @throws com.l7tech.objectmodel.FindException in the event of a database problem
     */
    @Transactional(readOnly = true)
    protected ET findByUniqueKey(final String uniquePropertyName, final Goid uniqueKey) throws FindException {
        if (uniquePropertyName == null) throw new NullPointerException();
        if (uniquePropertyName.trim().isEmpty())
            throw new IllegalArgumentException("uniquePropertyName cannot be empty");

        return findUnique( Collections.<String,Object>singletonMap(uniquePropertyName, uniqueKey) );
    }

    /**
     * Finds a unique entity matching the specified criteria.
     *
     * @param map Criteria specification: entries in the map are ANDed.
     * @return The matching unique entity or null
     */
    protected final ET findUnique( final Map<String, Object> map ) throws FindException {
        return findUnique( asCriterion( map ) );
    }

    /**
     * Finds a unique entity matching the specified criteria.
     *
     * @param criterion The criteria to match
     * @return The matching unique entity or null
     */
    protected final ET findUnique( final Criterion criterion ) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<ET>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                protected ET doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria(getImpClass());
                    criteria.add( criterion );
                    final ET et = (ET) criteria.uniqueResult();
                    initializeLazilyLoaded(et);
                    return et;
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public ET findByHeader(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getGoid());
    }

    /**
     * Find the number of entities that are found for the given criterion.
     *
     * @param restrictions The restrictions (e.g before / after dates)
     * @return The number of entities matched
     * @throws com.l7tech.objectmodel.FindException If an error occurs
     */
    protected int findCount( final Class clazz, final Criterion... restrictions ) throws FindException {
        final Class targetClass = clazz==null ? getImpClass() : clazz;
        try {
            if ( useOptimizedCount && restrictions.length == 0 && targetClass.equals( getImpClass() ) ) {
                // Warning: This is not strictly correct since it ignores the possibility of manager
                // specific criteria.
                return doReadOnlyWork( new Functions.UnaryThrows<Integer,Connection,SQLException>() {
                    @Override
                    public Integer call( final Connection connection ) throws SQLException {
                                final SimpleJdbcTemplate template = new SimpleJdbcTemplate( new SingleConnectionDataSource(connection, true) );
                        return template.queryForInt( "select count(*) from " + getTableName() );
                            }
                        } );
            } else {
                return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Integer>() {
                    @Override
                    protected Integer doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                        Criteria criteria = session.createCriteria( targetClass );

                        // Ensure manager specific criteria are added
                        addFindAllCriteria( criteria );

                        // Add additional criteria
                        for (Criterion restriction : restrictions ) {
                            criteria.add( restriction );
                        }

                        criteria.setProjection( Projections.rowCount() );
                        return ((Number) criteria.uniqueResult()).intValue();
                    }
                });
            }
        } catch (Exception e) {
            throw new FindException("Error finding count", e);
        }
    }

    /**
     * Find a "page" worth of entities for the given sort, offset and count.
     *
     * <p>Additional restrictions may be passed in the form of hibernate Criterion.</p>
     *
     * @param sortProperty The property to sort by (e.g. name)
     * @param ascending True to sort in ascending order
     * @param offset The initial offset 0 for the first page
     * @param count The number of items to return
     * @param restrictions Additional restrictions (e.g before / after dates)
     * @return  The matching entities or an empty list if none found
     * @throws com.l7tech.objectmodel.FindException If an error occurs
     */
    protected List<ET> findPage( final Class clazz, final String sortProperty, final boolean ascending, final int offset, final int count, final Criterion... restrictions ) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<ET>>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                protected List<ET> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria criteria = session.createCriteria(clazz==null ? getImpClass() : clazz);

                    // Ensure manager specific criteria are added
                    addFindAllCriteria( criteria );

                    // Add additional criteria
                    for (Criterion restriction : restrictions ) {
                        criteria.add( restriction );
                    }

                    if ( ascending ) {
                        criteria.addOrder( Order.asc(sortProperty) );
                    } else {
                        criteria.addOrder( Order.desc(sortProperty) );
                    }

                    criteria.setFirstResult( offset );
                    criteria.setMaxResults( count );

                    final List<ET> list = (List<ET>) criteria.list();
                    for (final ET et : list) {
                        initializeLazilyLoaded(et);
                    }
                    return list;
                }
            });
        } catch (Exception e) {
            throw new FindException("Couldn't check uniqueness", e);
        }
    }

    //TODO: merge this with findPage
    @Override
    public List<ET> findPagedMatching(final int offset, final int count, final String sortProperty, final Boolean ascending, final Map<String,List<Object>> matchProperties) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<ET>>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                protected List<ET> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria criteria = session.createCriteria(getImpClass());

                    // Ensure manager specific criteria are added
                    addFindAllCriteria( criteria );

                    if(matchProperties!=null && !matchProperties.isEmpty()){
                        Criterion criterion = reduce( matchProperties.entrySet(), conjunction(), new Functions.Binary<Junction, Junction, Map.Entry<String, List<Object>>>() {
                            @Override
                            public Junction call( final Junction junction, final Map.Entry<String, List<Object>> entry ) {
                                if(!entry.getValue().isEmpty()) {
                                    junction.add( reduce(entry.getValue(), disjunction(), new Functions.Binary<Junction, Junction, Object>() {
                                        @Override
                                        public Junction call(Junction junction, Object o) {
                                            if (o == NULL || o == null) {
                                                junction.add(Restrictions.isNull(entry.getKey()));
                                            } else if (o == NOTNULL) {
                                                junction.add(Restrictions.isNotNull(entry.getKey()));
                                            } else if (o != null) {
                                                junction.add(Restrictions.eq(entry.getKey(), o));
                                            }
                                            return junction;
                                        }
                                    }) );
                                }
                                return junction;
                            }
                        } );

                        // Add additional criteria
                        criteria.add( criterion );
                    }


                    if(sortProperty != null){
                        if ( ascending == null || ascending ) {
                            criteria.addOrder( Order.asc(sortProperty) );
                        } else {
                            criteria.addOrder( Order.desc(sortProperty) );
                        }
                    }

                    criteria.setFirstResult( offset );
                    //If the count is negative return the full list.
                    if (count >= 0) {
                        criteria.setFetchSize(count);
                        criteria.setMaxResults(count);
                    }

                    final List<ET> list = (List<ET>) criteria.list();
                    for (final ET et : list) {
                        initializeLazilyLoaded(et);
                    }
                    return list;
                }
            });
        } catch (Exception e) {
            throw new FindException("Couldn't find entities", e);
        }
    }

    /**
     * Finds entities matching the specified criteria.
     *
     * @param maps Criteria specification: entries in a map are ANDed, items in the collection are ORed.
     * @return a list of matching entities, or an empty list if none were found.
     */
    protected final List<ET> findMatching(final Collection<Map<String, Object>> maps) throws FindException {
        return findMatching(asCriterion(maps));
    }

    /*
     * Finds entities matching the specified criterion.
     *
     * @param criterion The criteria to match
     * @return a list of matching entities, or an empty list if none were found.
     */
    protected final List<ET> findMatching( final Criterion criterion ) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<ET>>() {
                    @SuppressWarnings({ "unchecked" })
                    @Override
                    protected List<ET> doInHibernateReadOnly( final Session session ) throws HibernateException, SQLException {
                        Criteria criteria = session.createCriteria(getImpClass());
                        criteria.add( criterion );
                        final List<ET> list = (List<ET>) criteria.list();
                        for (final ET et : list) {
                            initializeLazilyLoaded(et);
                        }
                        return list;
                    }
                });
        } catch (Exception e) {
            throw new FindException("Error finding entities", e);
        }
    }

    /**
     * Build a criterion for the given criteria.
     *
     * @param maps Criteria specification: entries in a map are ANDed, items in the collection are ORed.
     * @param <V> The map value type
     * @return The criterion
     */
    protected final <V> Criterion asCriterion( final Collection<Map<String, V>> maps ) {
        return reduce( maps, disjunction(), new Functions.Binary<Junction, Junction, Map<String, V>>() {
            @Override
            public Junction call( final Junction junction, final Map<String, V> map ) {
                junction.add( asCriterion( map ) );
                return junction;
            }
        } );
    }

    /**
     * Build a criterion for the given criteria.
     *
     * @param map Criteria specification: entries in the map are ANDed
     * @param <V> The map value type
     * @return The criterion
     */
    protected final <V> Criterion asCriterion( final Map<String, V> map ) {
        return reduce(map.entrySet(), conjunction(), new Functions.Binary<Junction, Junction, Map.Entry<String, V>>() {
            @Override
            public Junction call(final Junction junction, final Map.Entry<String, V> entry) {
                final Object value = entry.getValue();
                if (value == NULL) {
                    junction.add(Restrictions.isNull(entry.getKey()));
                } else if (value == NOTNULL) {
                    junction.add(Restrictions.isNotNull(entry.getKey()));
                } else if (value != null) {
                    junction.add(Restrictions.eq(entry.getKey(), entry.getValue()));
                }
                return junction;
            }
        });
    }

    /**
     * Finds headers for entities matching the specified criteria.
     *
     * @param maps Criteria specification: entries in a map are ANDed, items in the collection are ORed.
     * @return a list of matching entity headers, or an empty list if none were found.
     */
    protected List<HT> findMatchingHeaders(final Collection<Map<String, Object>> maps) throws FindException {
        return Functions.map( findMatching(maps), new Functions.Unary<HT,ET>(){
            @Override
            public HT call( final ET entity ) {
                return newHeader( entity );
            }
        } );
    }

    /**
     * Gets the entity uniqueness constraints.
     *
     * Each Map in the collection represents a unique constraint for all entities e.g. name is often a unique constraint
     * for entities. The contents of each Map represent the unique constrains for an individual row
     *
     * If an entity person has a name and an age and the name must be unique then a Collection with a single map
     * with a single value of 'name' can be used. If however no two persons can have the same name and age, then
     * a single Map will contain two entries: name and age. If no two people can have the same name or the same age,
     * then two maps are required, one with the value 'name' and another with the value 'age'
     *
     * @return Uniqueness constraint specification, possibly immutable: entries in a map are ANDed, items in the collection are ORed.
     */
    protected Collection<Map<String, Object>> getUniqueConstraints(ET entity) {
        switch(getUniqueType()) {
            case NAME:
                if (entity instanceof NamedEntity) {
                    NamedEntity namedEntity = (NamedEntity) entity;
                    Map<String,Object> map = new HashMap<String, Object>();
                    map.put("name", namedEntity.getName());
                    return Collections.singletonList( map );
                } else {
                    throw new IllegalArgumentException("UniqueType is NAME, but entity is not a NamedEntity");
                }
            case NONE:
                return Collections.emptySet();
            case OTHER:
            default:
                throw new IllegalArgumentException("UniqueType is OTHER, but getUniqueConstraints() not overridden");
        }
    }

    @Override
    public Goid save(ET entity) throws SaveException {
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Saving {0} ({1})", new Object[] { getImpClass().getSimpleName(), entity==null ? null : entity.toString() });
        try {
            if (getUniqueType() != UniqueType.NONE) {
                final Collection<Map<String, Object>> constraints = getUniqueConstraints(entity);
                List others;
                try {
                    others = findMatching(constraints);
                } catch (FindException e) {
                    throw new SaveException("Couldn't find previous version(s) to check uniqueness", e);
                }

                if (!others.isEmpty()) throw new DuplicateObjectException(describeAttributes(constraints));
            }

            Object key = getHibernateTemplate().save(entity);
            if (!(key instanceof Goid))
                throw new SaveException("Primary key was a " + key.getClass().getName() + ", not a Goid");

            return (Goid)key;
        } catch (RuntimeException e) {
            throw new SaveException("Couldn't save " + entity.getClass().getSimpleName(), e);
        }
    }

    @Override
    public void save(@NotNull final Goid id, @NotNull final ET entity) throws SaveException {
        if (logger.isLoggable(Level.FINE)) logger.log(Level.FINE, "Saving {0} ({1}) with id {2}", new Object[] { getImpClass().getSimpleName(), entity==null ? null : entity.toString(), id.toString() });
        if(GoidRange.RESERVED_RANGE.isInRange(id)) {
            throw new SaveException("Cannot save an entity with an id in the reserved range. ID: " + id);
        }
        try {
            try {
                final ET other = findByPrimaryKey(id);
                if(other != null) throw new DuplicateObjectException("Other entity exists with the given id: " + id);
            } catch (FindException e) {
                //do nothing. This means that there is no other entity with the same goid.
            }

            //tell the entity to preserve its id
            final boolean preserveId = PersistentEntityUtil.preserveId(entity);
            if (!preserveId) {
                throw new SaveException("Cannot save an entity with a specific ID. Was unable to preserve the entity ID for entity: " + entity.getClass().getSimpleName());
            }
            //set the entity id
            entity.setGoid(id);
            //delegate to the save entity method.
            final Goid savedID = save(entity);
            //validate that the ID returned by the above save call equals the one given
            if (!id.equals(savedID)) {
                throw new SaveException("Error saving entity with a specific ID. The save method saved with a different id. Expected: " + id + " used " + savedID + ". Entity: " + entity.getClass().getSimpleName());
            }
        } catch (RuntimeException e) {
            throw new SaveException("Couldn't save " + entity.getClass().getSimpleName(), e);
        }
    }

    protected String describeAttributes(Collection<Map<String, Object>> maps) {
        StringBuilder result = new StringBuilder();
        for (Map<String,Object> map : maps) {
            if (map.size() == 0) continue;
            result.append("(");
            for (String key : map.keySet()) {
                result.append(key).append(", ");
            }
            result.delete(result.length()-2, result.length());
            result.append(") ");
        }

        if (result.length() == 0)
            throw new IllegalStateException("Unique attribute map was empty");

        result.append(" must be unique");
        return result.toString();
    }

    @SuppressWarnings({ "ThrowableInstanceNeverThrown" })
    @Override
    public void update(ET entity) throws UpdateException {
        try {
            if (getUniqueType() != UniqueType.NONE) {
                final Collection<Map<String, Object>> constraints = getUniqueConstraints(entity);
                List<ET> others;
                try {
                    others = findMatching(constraints);
                } catch (FindException e) {
                    throw new UpdateException("Couldn't find previous version(s) to check uniqueness");
                }

                if (!others.isEmpty()) {
                    for (ET other : others) {
                        if (!entity.getId().equals(other.getId()) || !entity.getClass().equals(other.getClass())) {
                            String message = describeAttributes(constraints);
                            // nested since DuplicateObjectException is a SaveException not an UpdateException
                            throw new UpdateException(message, new DuplicateObjectException(message));
                        }
                    }
                }
            }

            // backwards compatibility - changed to only validate the version id on properties that already exist in the db
            try {
                ET original = findByPrimaryKey(entity.getGoid());
                if (original != null && original.getVersion() != entity.getVersion()) {
                    throw new StaleUpdateException("Entity " + entity.getGoid() + ": version mismatch");
                }
            } catch (FindException fe) {
                throw new UpdateException("Couldn't find previous version to check for versioning");
            }

            getHibernateTemplate().merge(entity);
        } catch (RuntimeException e) {
            throw new UpdateException("Couldn't update " + entity.getClass().getSimpleName(), e);
        }
    }

    /**
     * Returns the current version (in the database) of the entity with the specified GOID.
     *
     * @param goid the GOID of the entity whose version should be retrieved
     * @return The version, or null if the entity does not exist.
     * @throws com.l7tech.objectmodel.FindException
     */
    @Override
    @Transactional(readOnly=true)
    public Integer getVersion(final Goid goid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Integer>() {
                @Override
                public Integer doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_VERSION_BY_GOID);
                    q.setParameter(0, goid);
                    return (Integer) q.uniqueResult();
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public Map<Goid,Integer> findVersionMap() throws FindException {
        Map<Goid, Integer> result = new HashMap<Goid, Integer>();
        if (!PersistentEntity.class.isAssignableFrom(getImpClass())) throw new FindException("Can't find non-Entities!");

        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.MANUAL);
            Query q = s.createQuery(HQL_FIND_ALL_GOIDS_AND_VERSIONS);
            List results = q.list();
            if (results.size() > 0) {
                for (Object result1 : results) {
                    Object[] row = (Object[]) result1;
                    if (row[0]instanceof Goid && row[1]instanceof Integer) {
                        result.put((Goid)row[0], (Integer)row[1]);
                    } else {
                        throw new FindException("Got unexpected fields " + row[0] + " and " + row[1] + " from query!");
                    }
                }
            }
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        } finally {
            if (s != null && old != null) s.setFlushMode(old);
            releaseSession(s);
        }

        return result;
    }

    @Override
    @Transactional(propagation=SUPPORTS)
    public EntityType getEntityType() {
        EntityType type = this.entityType;
        if ( type == null ) {
            this.entityType = type = EntityType.findTypeByEntity( getInterfaceClass() );
        }
        if ( type == null ) {
            this.entityType = type = EntityType.ANY;
        }
        return type;
    }

    @Override
    @Transactional(readOnly=true)
    public EntityHeaderSet<HT> findAllHeaders() throws FindException {
        Collection<ET> entities = findAll();
        EntityHeaderSet<HT> headers = new EntityHeaderSet<HT>();
        for (ET entity : entities) {
            headers.add(newHeader(entity));
        }
        return headers;
    }

    @Override
    @Transactional(readOnly=true)
    public EntityHeaderSet<HT> findAllHeaders(final int offset, final int windowSize) {
        List<ET> entities = getHibernateTemplate().execute( new ReadOnlyHibernateCallback<List<ET>>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            protected List<ET> doInHibernateReadOnly( Session session ) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria( getImpClass() );
                criteria.setFirstResult( offset );
                criteria.setFetchSize( windowSize );
                criteria.setMaxResults( windowSize );
                return (List<ET>) criteria.list();
            }
        } );

        EntityHeaderSet<HT> headers = new EntityHeaderSet<HT>();
        for (ET entity : entities) {
            headers.add(newHeader(entity));
        }
        return headers;
    }

    /**
     * Helper for implementing findHeaders in SearchableEntityProviders
     */
    protected EntityHeaderSet<HT> doFindHeaders( final int offset, final int windowSize, final Map<String,?> filters, final boolean disjunction ) {
        List<ET> entities = getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<ET>>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            protected List<ET> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                final Criteria criteria = session.createCriteria(getImpClass());
                criteria.setFirstResult(offset);
                criteria.setFetchSize(windowSize);
                criteria.setMaxResults( windowSize );

                if ( filters != null ) {
                    Junction likeRestriction = disjunction ? Restrictions.disjunction() : Restrictions.conjunction();
                    for ( String filterProperty : filters.keySet() ) {
                        final Object filterObject = filters.get(filterProperty);
                        // todo: test based on the field's type
                        if ( filterObject instanceof Criterion ) {
                            likeRestriction.add( (Criterion)filterObject );
                        } else if ( filterObject instanceof String ) {
                            final String filter = (String) filterObject;
                            if ("true".equalsIgnoreCase(filter) || "false".equalsIgnoreCase(filter)) {
                                likeRestriction.add(Restrictions.eq( filterProperty, Boolean.parseBoolean(filter)));
                            } else {
                                likeRestriction.add(Restrictions.like( filterProperty, filter.replace('*', '%').replace('?', '_')));
                            }
                        } else {
                            likeRestriction.add( Restrictions.eq( filterProperty, filterObject ));
                        }
                    }

                    if ( likeRestriction != null ) {
                        criteria.add( likeRestriction );
                    }
                }

                doFindHeaderCriteria( criteria );

                return (List<ET>)criteria.list();
            }
        });

        EntityHeaderSet<HT> headers = new EntityHeaderSet<HT>();
        for (ET entity : entities) {
            headers.add(newHeader(entity));
        }
        return headers;
    }

    /**
     * Allows implementations to add additional criteria for use with header searching.
     *
     * @param criteria The criteria to update
     */
    protected void doFindHeaderCriteria( final Criteria criteria ) {
    }

    /**
     * Override this method to customize how EntityHeaders get created
     * (if {@link HT} is a subclass of {@link com.l7tech.objectmodel.EntityHeader} it's mandatory)
     *
     * @param entity the PersistentEntity
     * @return a new EntityHeader based on the provided Entity ID and name
     */
    @SuppressWarnings({ "unchecked" })
    protected HT newHeader(ET entity) {
        String name = null;
        if (entity instanceof NamedEntity) name = ((NamedEntity) entity).getName();
        if (name == null) name = "";

        HT ht = (HT) new EntityHeader(
                entity.getGoid(),
                getEntityType(),
                name,
                EMPTY_STRING,
                entity.getVersion());

        if (entity instanceof ZoneableEntity) {
            final ZoneableEntity zoneableEntity = (ZoneableEntity) entity;
            final SecurityZone zone = zoneableEntity.getSecurityZone();
            final ZoneableEntityHeader zoneableHeader = new ZoneableEntityHeader(ht);
            zoneableHeader.setSecurityZoneId(zone == null ? null : zone.getGoid());
            ht = (HT) zoneableHeader;
        }

        return ht;
    }

    /**
     * Override this method to add additional criteria to findAll(), findAllHeaders(), findByName() etc.
     * @param allHeadersCriteria the Hibernate Criteria object that should be customized if some filtering is required
     */
    protected void addFindAllCriteria(Criteria allHeadersCriteria) {
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<ET> findAll() throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Collection<ET>>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public Collection<ET> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria allHeadersCriteria = session.createCriteria(getImpClass());
                    addFindAllCriteria(allHeadersCriteria);
                    return (Collection<ET>)allHeadersCriteria.list();
                }
            });
        } catch (HibernateException e) {
            throw new FindException(e.toString(), e);
        }
    }

    public Collection<ET> findAll(final int offset, final int windowSize) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Collection<ET>>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public Collection<ET> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria allCriteria = session.createCriteria(getImpClass());
                    addFindAllCriteria(allCriteria);
                    allCriteria.setFirstResult(offset);
                    allCriteria.setMaxResults(windowSize);
                    return (Collection<ET>)allCriteria.list();
                }
            });
        } catch (HibernateException e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Transactional(propagation=SUPPORTS)
    public String getAllQuery() {
        String alias = getTableName();
        return "from " + alias + " in class " + getImpClass().getName();
    }

    @Override
    public void delete(final Goid goid) throws DeleteException, FindException {
        try {
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE_BY_GOID);
                    q.setParameter(0, goid);
                    List todelete = q.list();
                    if (todelete.size() == 0) {
                        // nothing to do
                    } else if (todelete.size() == 1) {
                        final Object entity = todelete.get(0);
                        publishRoleAwareEntityDeletionEvent(entity);
                        session.delete(entity);
                    } else {
                        throw new RuntimeException("More than one entity found with goid = " + goid);
                    }
                    return null;
                }
            });
        } catch (Exception he) {
            throw new DeleteException(he.toString(), he);
        }
    }

    @Override
    public void delete(final ET et) throws DeleteException {
        try {
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public Void doInHibernate(Session session) throws HibernateException, SQLException {
                    ET entity = (ET)session.get(getImpClass(), et.getGoid());
                    if (entity == null) {
                        publishRoleAwareEntityDeletionEvent(et);
                        session.delete(et);
                    } else {
                        publishRoleAwareEntityDeletionEvent(entity);
                        // Avoid NonUniqueObjectException if an older version of this is still in the Session
                        session.delete(entity);
                    }
                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new DeleteException("Couldn't delete entity", e);
        }
    }

    @Transactional(propagation=SUPPORTS)
    public boolean isCacheCurrent(Goid goid, int maxAge) {
        Lock read = cacheLock.readLock();
        CacheInfo cacheInfo;
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByGoid.get(goid);
            read.unlock(); read = null;
            cacheInfo = ref == null ? null : ref.get();
            return cacheInfo != null && cacheInfo.timestamp + (long) maxAge >= System.currentTimeMillis();
        } finally {
            if (read != null) read.unlock();
        }

    }

    @SuppressWarnings({"unchecked"})
    @Transactional(propagation=SUPPORTS)
    public ET getCachedEntityByName(final String name, int maxAge) throws FindException {
        if (name == null) throw new NullPointerException();
        if (!(NamedEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not NamedEntities!");
        Lock read = cacheLock.readLock();
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByName.get(name);
            read.unlock(); read = null;
            CacheInfo<ET> cinfo = ref == null ? null : ref.get();

            if (cinfo != null) return freshen(cinfo, maxAge);

            return (ET) new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
                @Override
                public Object doInTransaction(TransactionStatus transactionStatus) {
                    try {
                        ET ent = findByUniqueName(name);
                        return ent == null ? null : checkAndCache(ent);
                    } catch (Exception e) {
//                        transactionStatus.setRollbackOnly();
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (RuntimeException e) {
            throw new FindException(ExceptionUtils.getMessage(e), e);
        } finally {
            if (read != null) read.unlock();
        }
    }

    @Override
    @Transactional(readOnly=true)
    public ET findByUniqueName(final String name) throws FindException {
        if (name == null) throw new NullPointerException();
        if (!(NamedEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not NamedEntities!");

        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<ET>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public ET doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria criteria = session.createCriteria(getImpClass());
                    addFindByNameCriteria(criteria);
                    criteria.add(Restrictions.eq(F_NAME, name));
                    final ET et = (ET) criteria.uniqueResult();
                    initializeLazilyLoaded(et);
                    return et;
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    /**
     * Override to update the criteria used by findByUniqueName() before it's executed
     * @param criteria criteria to be mutated
     */
    protected void addFindByNameCriteria(Criteria criteria) {
    }

    /**
     * Gets the {@link com.l7tech.objectmodel.PersistentEntity} with the specified name from a cache where possible.  If the
     * entity is not present in the cache, it will be retrieved from the database.  If the entity
     * is present in the cache but was cached too long ago, checks whether the cached entity
     * is stale by looking up its {@link com.l7tech.objectmodel.PersistentEntity#getVersion}.  If the cached entity has the same
     * version as the database, the cached version is marked fresh.
     *
     * @param goid the GOID of the object to get
     * @param maxAge the age, in milliseconds, that a cached entity must attain before it is considered stale
     * @return the object with the specified ID, from a cache if possible.
     * @throws com.l7tech.objectmodel.FindException
     */
    @Override
    @Transactional(propagation=SUPPORTS, readOnly=true)
    public ET getCachedEntity(final Goid goid, int maxAge) throws FindException {
        ET entity;

        Lock read = cacheLock.readLock();
        CacheInfo<ET> cacheInfo;
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByGoid.get(goid);
            read.unlock(); read = null;
            cacheInfo = ref == null ? null : ref.get();
            if (cacheInfo == null) {
                // Might be new, or might be first run
                entity = new TransactionTemplate(transactionManager).execute(new TransactionCallback<ET>() {
                    @Override
                    public ET doInTransaction(TransactionStatus transactionStatus) {
                        try {
                            return findByPrimaryKey(goid);
                        } catch (FindException e) {
//                            transactionStatus.setRollbackOnly();
                            throw new RuntimeException(e);
                        }
                    }
                });

                if (entity == null) return null; // Doesn't exist

                // New
                return checkAndCache(entity);
            } else {
                return freshen(cacheInfo, maxAge);
            }
        } finally {
            if (read != null) read.unlock();
        }
    }

    private ET freshen(CacheInfo<ET> cacheInfo, int maxAge) throws FindException {
        if (cacheInfo.timestamp + (long) maxAge < System.currentTimeMillis()) {
            // Time for a version check (getVersion() always goes to the database)
            Integer currentVersion = getVersion(cacheInfo.entity.getGoid());
            if (currentVersion == null) {
                // BALEETED
                cacheRemove(cacheInfo.entity);
                return null;
            } else if (currentVersion != cacheInfo.version) {
                // Updated
                ET thing = findByPrimaryKey(cacheInfo.entity.getGoid());
                return thing == null ? null : checkAndCache(thing);
            }
        }

        return cacheInfo.entity;
    }

    protected void cacheRemove(PersistentEntity thing) {
        final Lock write = cacheLock.writeLock();
        write.lock();
        try {
            cacheInfoByGoid.remove(thing.getGoid());
            if (thing instanceof NamedEntity) {
                cacheInfoByName.remove(((NamedEntity)thing).getName());
            }
            removedFromCache(thing);
        } finally {
            write.unlock();
        }
    }

    /**
     * Override this method to be notified when an Entity has been removed from the cache.
     * This method is called while the cache write lock is held, so it should avoid
     * taking any lengthy action before returning.
     *
     * @param ent the Entity that has been removed
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    protected void removedFromCache(PersistentEntity ent) { }

    /**
     * Override this method to be notified when an Entity has been added to the cache.
     * This method is called while the cache write lock is held, so it should avoid
     * taking any lengthy action before returning.
     *
     * @param ent the Entity that has been added to the cache
     */
    @SuppressWarnings({ "UnusedDeclaration" })
    protected void addedToCache(PersistentEntity ent) { }

    /**
     * Override this method to initialize any lazily loaded fields of retrieved entities.
     *
     * This will be called on retrieved entities unless they are retrieved as part of a find all method.
     *
     * @param retrievedEntity the retrieved entity.
     */
    protected void initializeLazilyLoaded(ET retrievedEntity) {}

    /**
     * Perform some action while holding the cache write lock.
     * It is an error to invoke this method if the current thread already
     * holds the cache read lock.
     *
     * @param stuff  a Callable to invoke with the lock held.  Required.
     * @return the value returned by the Callable.
     * @throws Exception if the Callable throws an exception
     */
    protected final <RT> RT doWithCacheWriteLock(Callable<RT> stuff) throws Exception {
        return doWithLock(cacheLock.writeLock(), stuff);
    }

    /**
     * Perform some action while holding the cache read lock.
     * It is safe to invoke this method while the current thread already
     * holds the cache write lock.
     *
     * @param stuff  a Callable to invoke with the lock held.  Required.
     * @return the value returned by the Callable.
     * @throws Exception if the Callable throws an exception
     */
    protected final <RT> RT doWithCacheReadLock(Callable<RT> stuff) throws Exception {
        return doWithLock(cacheLock.readLock(), stuff);
    }

    /**
     * If the entity allows Role permissions to reference it in its permission scope, publish an event which indicates it is about to be deleted.
     * @param entity the entity which is about to be deleted.
     */
    private void publishRoleAwareEntityDeletionEvent(final Object entity) {
        if (applicationContext != null && entity instanceof Entity) {
            final EntityType type = EntityType.findTypeByEntity((Class<? extends Entity>) entity.getClass());
            if (type != null && type.isAllowSpecificScope()) {
                applicationContext.publishEvent(new RoleAwareEntityDeletionEvent(this, (Entity)entity));
            }
        } else {
            logger.log(Level.WARNING, "Unable to publish RoleAwareEntityDeletionEvent because object is not an Entity or applicationContext is null.");
        }
    }

    private <RT> RT doWithLock(Lock lock, Callable<RT> stuff) throws Exception {
        try {
            lock.lock();
            return stuff.call();
        } finally {
            lock.unlock();
        }
    }

    protected ET checkAndCache(ET thing) throws FindException {
        final Goid goid = thing.getGoid();

        CacheInfo<ET> info = null;

        // Get existing cache info
        Lock read = cacheLock.readLock();
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByGoid.get(goid);
            info = ref == null ? null : ref.get();
        } finally {
            if (read != null) read.unlock();
        }

        Lock write = cacheLock.writeLock();
        try {
            write.lock();

            // new item to cache
            if (info == null) {
                info = new CacheInfo<ET>();
                WeakReference<CacheInfo<ET>> newref = new WeakReference<CacheInfo<ET>>(info);

                cacheInfoByGoid.put(goid, newref);
                if (thing instanceof NamedEntity) {
                    cacheInfoByName.put(((NamedEntity)thing).getName(), newref);
                }
            }

            // set cached info
            info.entity = thing;
            info.version = thing.getVersion();
            info.timestamp = System.currentTimeMillis();
            addedToCache(thing);

        } finally {
            if (write != null) write.unlock();
        }

        return thing;
    }


    /**
     * Retrieve the persistent object instance by it's primary key (object id). If the
     * object is not found returns <code>null</code>
     *
     * @param impClass the object class
     * @param goid      the object id
     * @return the object instance or <code>null</code> if no instance has been found
     * @throws com.l7tech.objectmodel.FindException if there was an data access error
     */
    protected ET findByPrimaryKey(final Class impClass, final Goid goid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<ET>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public ET doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final ET et = (ET) session.get(impClass, goid);
                    initializeLazilyLoaded(et);
                    return et;
                }
            });
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, org.hibernate.ObjectNotFoundException.class) ||
                ExceptionUtils.causedBy(e, ObjectDeletedException.class)) {
                return null;
            }
            throw new FindException("Data access error ", e);
        }
    }

    /**
     * Lookup the entity by goid and delete(ET) it.
     *
     * @param goid The entity goid.
     * @return true if the entity was deleted; false otherwise
     * @throws com.l7tech.objectmodel.FindException if there is a problem finding the entity
     * @throws com.l7tech.objectmodel.DeleteException if there is a problem deleting the entity
     */
    protected boolean findAndDelete(final Goid goid) throws FindException, DeleteException {
        boolean deleted = false;

        ET entity = this.findByPrimaryKey( goid );
        if ( entity != null ) {
            delete(entity);
            deleted = true;
        }

        return deleted;
    }

    @SuppressWarnings({ "unchecked" })
    protected List<ET> findByPropertyMaybeNull(final String property, final Object value) throws FindException {
        try {
            return getHibernateTemplate().executeFind( new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly( Session session ) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria( getImpClass() );
                    if ( value == null ) {
                        criteria.add( Restrictions.isNull( property ) );
                    } else {
                        criteria.add( Restrictions.eq( property, value ) );
                    }
                    final List list = criteria.list();
                    for (final Object o : list) {
                        initializeLazilyLoaded((ET)o);
                    }
                    return list;
                }
            } );
        } catch (DataAccessException e) {
            throw new FindException("Couldn't find cert(s)", e);
        }
    }

    /**
     * Perform (read-only) JDBC work.
     *
     * @param callback The connection callback.
     * @param <R> The result type
     * @return The result value as returned from the callback.
     * @throws java.sql.SQLException If an error occurs
     */
    protected final <R> R doReadOnlyWork( final Functions.UnaryThrows<? extends R, Connection, SQLException> callback ) throws SQLException {
        return getHibernateTemplate().execute( new ReadOnlyHibernateCallback<R>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            protected R doInHibernateReadOnly( final Session session ) throws HibernateException, SQLException {
                final Object[] result = new Object[]{ null };
                session.doWork( new Work(){
                    @Override
                    public void execute( final Connection conn ) throws SQLException {
                        result[0] = callback.call( conn );
                    }
                } );
                return (R) result[0];
            }
        } );
    }

    protected UniqueType getUniqueType() {
        return UniqueType.NAME;
    }

    /**
     * Convenience implementation that accesses the table name from the entity annotation.
     *
     * <p>If the implementation class is not annotated with the table name this method
     * must be overridden.</p>
     *
     * @return The name from the Table annotation on the implementation entity.
     * @throws IllegalStateException if the entity is not annotated.
     */
    @Override
    @Transactional(propagation= Propagation.SUPPORTS)
    public String getTableName() {
        String tableName = this.tableName;
        if ( tableName==null ) {
            Class<?> impClass = getImpClass();
            Table table = impClass.getAnnotation( Table.class );
            if ( table == null ) throw new IllegalStateException( "Implementation class is not annotated" );
            this.tableName = tableName = table.name();
        }
        return tableName;
    }

    /**
     * Convenience implementation that returns the implementation class.
     *
     * @return The implementation class.
     */
    @Override
    @Transactional(propagation= Propagation.SUPPORTS)
    public Class<? extends Entity> getInterfaceClass() {
        return getImpClass();
    }

    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private final Logger logger = Logger.getLogger(getClass().getName());

    private static final boolean useOptimizedCount = ConfigFactory.getBooleanProperty( "com.l7tech.server.hibernate.useOptimizedCount", true );

    private ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private Map<Goid, WeakReference<CacheInfo<ET>>> cacheInfoByGoid = new HashMap<Goid, WeakReference<CacheInfo<ET>>>();
    private Map<String, WeakReference<CacheInfo<ET>>> cacheInfoByName = new HashMap<String, WeakReference<CacheInfo<ET>>>();
    private String tableName;
    private EntityType entityType;

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /** Holds information about a cached Entity. */
    static class CacheInfo<ET extends PersistentEntity> {
        ET entity;
        long timestamp;
        int version;
    }    
}
