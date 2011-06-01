package com.l7tech.server;

import com.l7tech.objectmodel.*;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.SyspropUtil;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.jdbc.Work;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import static org.springframework.transaction.annotation.Propagation.SUPPORTS;
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

/**
 * The default {@link com.l7tech.objectmodel.EntityManager} implementation for Hibernate-managed
 * {@link com.l7tech.objectmodel.PersistentEntity persistent entities}.
 *
 * Implementations only need to implement {@link #getInterfaceClass()}, {@link #getImpClass()} and
 * {@link #getTableName()}, although two of those are only for historical reasons.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public abstract class HibernateEntityManager<ET extends PersistentEntity, HT extends EntityHeader>
        extends HibernateDaoSupport
        implements EntityManager<ET, HT>
{
    public static final String EMPTY_STRING = "";
    public static final String F_OID = "oid";
    public static final String F_NAME = "name";
    public static final String F_VERSION = "version";

    public static final Object NULL = new Object();
    public static final Object NOTNULL = new Object();

    private final String HQL_FIND_ALL_OIDS_AND_VERSIONS =
            "SELECT " +
                    getTableName() + "." + F_OID + ", " +
                    getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName();

    private final String HQL_FIND_VERSION_BY_OID =
            "SELECT " + getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_OID + " = ?";

    private final String HQL_FIND_BY_OID =
            "FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_OID + " = ?";

    private final String HQL_DELETE_BY_OID =
            "FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_OID + " = ?";

    protected PlatformTransactionManager transactionManager; // required for TransactionTemplate

    @Override
    @Transactional(readOnly=true)
    public ET findByPrimaryKey(final long oid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<ET>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public ET doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_BY_OID);
                    q.setLong(0, oid);
                    return (ET)q.uniqueResult();
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
     * @throws FindException in the event of a database problem
     */
    @Transactional(readOnly = true)
    protected ET findByUniqueKey(final String uniquePropertyName, final long uniqueKey) throws FindException {
        if (uniquePropertyName == null) throw new NullPointerException();
        if (uniquePropertyName.trim().isEmpty())
            throw new IllegalArgumentException("uniquePropertyName cannot be empty");

        return findUnique( Collections.<String,Object>singletonMap( uniquePropertyName, uniqueKey ) );
    }

    /**
     * Finds a unique entity matching the specified criteria.
     *
     * @param map Criteria specification: entries in the map are ANDed.
     * @return a list of matching entities, or an empty list if none were found.
     */
    protected ET findUnique(final Map<String, Object> map) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<ET>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                protected ET doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria(getImpClass());
                    for ( final Map.Entry<String, Object> entry : map.entrySet() ) {
                        final Object value = entry.getValue();
                        if (value == null) continue;
                        if (value == NULL) {
                            criteria.add(Restrictions.isNull(entry.getKey()));
                        } else if ( value == NOTNULL ) {
                            criteria.add(Restrictions.isNotNull(entry.getKey()));
                        } else {
                            criteria.add(Restrictions.eq(entry.getKey(), entry.getValue()));
                        }
                    }
                    return (ET)criteria.uniqueResult();
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public ET findByHeader(EntityHeader header) throws FindException {
        return findByPrimaryKey(header.getOid());
    }

    /**
     * Find the number of entities that are found for the given criterion.
     *
     * @param restrictions The restrictions (e.g before / after dates)
     * @return The number of entities matched
     * @throws FindException If an error occurs
     */
    protected int findCount( final Class clazz, final Criterion... restrictions ) throws FindException {
        final Class targetClass = clazz==null ? getImpClass() : clazz;
        try {            if ( useOptimizedCount && restrictions.length == 0 && targetClass.equals( getImpClass() ) ) {
                // Warning: This is not strictly correct since it ignores the possibility of manager
                // specific criteria.
                return getHibernateTemplate().execute( new ReadOnlyHibernateCallback<Integer>() {
                    @Override
                    protected Integer doInHibernateReadOnly( final Session session ) throws HibernateException, SQLException {
                        final int[] count = new int[1];
                        session.doWork( new Work(){
                            @Override
                            public void execute( final Connection connection ) throws SQLException {
                                final SimpleJdbcTemplate template = new SimpleJdbcTemplate( new SingleConnectionDataSource(connection, true) );
                                count[0] = template.queryForInt( "select count(*) from " + getTableName() );
                            }
                        } );
                        return count[0];
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
     * @throws FindException If an error occurs
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

                    return (List<ET>)criteria.list();
                }
            });
        } catch (Exception e) {
            throw new FindException("Couldn't check uniqueness", e);
        }
    }

    /**
     * Finds entities matching the specified criteria.
     *
     * @param maps Criteria specification: entries in a map are ANDed, items in the collection are ORed.
     * @return a list of matching entities, or an empty list if none were found.
     */
    protected List<ET> findMatching(final Collection<Map<String, Object>> maps) throws FindException {
        List<ET> result = new ArrayList<ET>();
        try {
            for (final Map<String, Object> map : maps) {
                result.addAll(getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<ET>>() {
                    @SuppressWarnings({ "unchecked" })
                    @Override
                    protected List<ET> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                        Criteria criteria = session.createCriteria(getImpClass());
                        for (Map.Entry<String, ?> entry : map.entrySet()) {
                            Object value = entry.getValue();
                            if (value == null) continue;
                            if (value == NULL) {
                                criteria.add(Restrictions.isNull(entry.getKey()));
                            } else if ( value == NOTNULL ) {
                                criteria.add(Restrictions.isNotNull(entry.getKey()));
                            } else {
                                criteria.add(Restrictions.eq(entry.getKey(), entry.getValue()));
                            }
                        }
                        return (List<ET>)criteria.list();
                    }
                }));
            }
        } catch (Exception e) {
            throw new FindException("Couldn't check uniqueness", e);
        }
        return result;
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
     * @return Uniqueness constraint specification: entries in a map are ANDed, items in the collection are ORed.
     */
    protected Collection<Map<String, Object>> getUniqueConstraints(ET entity) {
        switch(getUniqueType()) {
            case NAME:
                if (entity instanceof NamedEntity) {
                    NamedEntity namedEntity = (NamedEntity) entity;
                    Map<String,Object> map = new HashMap<String, Object>();
                    map.put("name", namedEntity.getName());
                    return Arrays.asList(map);
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
    public long save(ET entity) throws SaveException {
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
            if (!(key instanceof Long))
                throw new SaveException("Primary key was a " + key.getClass().getName() + ", not a Long");

            return ((Long)key);
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
                ET original = findByPrimaryKey(entity.getOid());                
                if (original != null && original.getVersion() != entity.getVersion()) {
                    throw new StaleUpdateException("Entity " + entity.getOid() + ": version mismatch");
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
     * Returns the current version (in the database) of the entity with the specified OID.
     *
     * @param oid the OID of the entity whose version should be retrieved
     * @return The version, or null if the entity does not exist.
     * @throws FindException
     */
    @Override
    @Transactional(readOnly=true)
    public Integer getVersion(final long oid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Integer>() {
                @Override
                public Integer doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_VERSION_BY_OID);
                    q.setLong(0, oid);
                    return (Integer) q.uniqueResult();
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public Map<Long,Integer> findVersionMap() throws FindException {
        Map<Long, Integer> result = new HashMap<Long, Integer>();
        if (!PersistentEntity.class.isAssignableFrom(getImpClass())) throw new FindException("Can't find non-Entities!");

        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.MANUAL);
            Query q = s.createQuery(HQL_FIND_ALL_OIDS_AND_VERSIONS);
            List results = q.list();
            if (results.size() > 0) {
                for (Object result1 : results) {
                    Object[] row = (Object[]) result1;
                    if (row[0]instanceof Long && row[1]instanceof Integer) {
                        result.put((Long)row[0], (Integer)row[1]);
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
        List<ET> entities = getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<ET>>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            protected List<ET> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(getImpClass());
                criteria.setFirstResult(offset);
                criteria.setFetchSize(windowSize);
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
     * Helper for implementing findHeaders in SearchableEntityProviders 
     */
    protected EntityHeaderSet<HT> doFindHeaders( final int offset, final int windowSize, final Map<String,?> filters, final boolean disjunction ) {
        List<ET> entities = getHibernateTemplate().execute(new ReadOnlyHibernateCallback<List<ET>>() {
            @SuppressWarnings({ "unchecked" })
            @Override
            protected List<ET> doInHibernateReadOnly(final Session session) throws HibernateException, SQLException {
                Criteria criteria = session.createCriteria(getImpClass());
                criteria.setFirstResult(offset);
                criteria.setFetchSize(windowSize);

                if ( filters != null ) {
                    Junction likeRestriction = disjunction ? Restrictions.disjunction() : Restrictions.conjunction();
                    for ( String filterProperty : filters.keySet() ) {
                        final Object filterObject = filters.get(filterProperty);
                        // todo: test based on the field's type
                        if ( filterObject instanceof String ) {
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
     * (if {@link HT} is a subclass of {@link EntityHeader} it's mandatory)
     *
     * @param entity the PersistentEntity
     * @return a new EntityHeader based on the provided Entity ID and name 
     */
    @SuppressWarnings({ "unchecked" })
    protected HT newHeader(ET entity) {
        String name = null;
        if (entity instanceof NamedEntity) name = ((NamedEntity) entity).getName();
        if (name == null) name = "";


        return (HT) new EntityHeader(
                entity.getOid(),
                getEntityType(),
                name,
                EMPTY_STRING,
                entity.getVersion());
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
    public void delete(final long oid) throws DeleteException, FindException {
        try {
            getHibernateTemplate().execute(new HibernateCallback<Void>() {
                @Override
                public Void doInHibernate(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_DELETE_BY_OID);
                    q.setLong(0, oid);
                    List todelete = q.list();
                    if (todelete.size() == 0) {
                        // nothing to do
                    } else if (todelete.size() == 1) {
                        session.delete(todelete.get(0));
                    } else {
                        throw new RuntimeException("More than one entity found with oid = " + oid);
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
                    ET entity = (ET)session.get(getImpClass(), et.getOid());
                    if (entity == null) {
                        session.delete(et);
                    } else {
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
    public boolean isCacheCurrent(long objectid, int maxAge) {
        Lock read = cacheLock.readLock();
        CacheInfo cacheInfo;
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByOid.get(objectid);
            read.unlock(); read = null;
            cacheInfo = ref == null ? null : ref.get();
            return cacheInfo != null && cacheInfo.timestamp + maxAge >= System.currentTimeMillis();
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
                    return (ET)criteria.uniqueResult();
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
     * Gets the {@link PersistentEntity} with the specified name from a cache where possible.  If the
     * entity is not present in the cache, it will be retrieved from the database.  If the entity
     * is present in the cache but was cached too long ago, checks whether the cached entity
     * is stale by looking up its {@link PersistentEntity#getVersion}.  If the cached entity has the same
     * version as the database, the cached version is marked fresh.
     *
     * @param objectid the OID of the object to get
     * @param maxAge the age, in milliseconds, that a cached entity must attain before it is considered stale
     * @return the object with the specified ID, from a cache if possible.
     * @throws FindException
     */
    @Override
    @Transactional(propagation=SUPPORTS, readOnly=true)
    public ET getCachedEntity(final long objectid, int maxAge) throws FindException {
        ET entity;

        Lock read = cacheLock.readLock();
        CacheInfo<ET> cacheInfo;
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByOid.get(objectid);
            read.unlock(); read = null;
            cacheInfo = ref == null ? null : ref.get();
            if (cacheInfo == null) {
                // Might be new, or might be first run
                entity = new TransactionTemplate(transactionManager).execute(new TransactionCallback<ET>() {
                    @Override
                    public ET doInTransaction(TransactionStatus transactionStatus) {
                        try {
                            return findByPrimaryKey(objectid);
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
        if (cacheInfo.timestamp + maxAge < System.currentTimeMillis()) {
            // Time for a version check (getVersion() always goes to the database)
            Integer currentVersion = getVersion(cacheInfo.entity.getOid());
            if (currentVersion == null) {
                // BALEETED
                cacheRemove(cacheInfo.entity);
                return null;
            } else if (currentVersion != cacheInfo.version) {
                // Updated
                ET thing = findByPrimaryKey(cacheInfo.entity.getOid());
                return thing == null ? null : checkAndCache(thing);
            }
        }

        return cacheInfo.entity;
    }

    protected void cacheRemove(PersistentEntity thing) {
        Lock write = cacheLock.writeLock();
        try {
            write.lock();
            cacheInfoByOid.remove(thing.getOid());
            if (thing instanceof NamedEntity) {
                cacheInfoByName.remove(((NamedEntity)thing).getName());
            }
            removedFromCache(thing);
            write.unlock();
            write = null;

        } finally {
            if (write != null) write.unlock();
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
    protected void removedFromCache(Entity ent) { }

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

    private <RT> RT doWithLock(Lock lock, Callable<RT> stuff) throws Exception {
        try {
            lock.lock();
            return stuff.call();
        } finally {
            lock.unlock();
        }
    }

    protected ET checkAndCache(ET thing) {
        final Long oid = thing.getOid();

        CacheInfo<ET> info = null;

        // Get existing cache info
        Lock read = cacheLock.readLock();
        try {
            read.lock();
            WeakReference<CacheInfo<ET>> ref = cacheInfoByOid.get(oid);
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

                cacheInfoByOid.put(oid, newref);
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
     * @param oid      the object id
     * @return the object instance or <code>null</code> if no instance has been found
     * @throws FindException if there was an data access error
     */
    protected ET findByPrimaryKey(final Class impClass, final long oid) throws FindException {
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<ET>() {
                @SuppressWarnings({ "unchecked" })
                @Override
                public ET doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    return (ET)session.get(impClass, oid);
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
     * Lookup the entity by oid and delete(ET) it.
     *
     * @param oid The entity oid.
     * @return true if the entity was deleted; false otherwise
     * @throws com.l7tech.objectmodel.FindException if there is a problem finding the entity
     * @throws com.l7tech.objectmodel.DeleteException if there is a problem deleting the entity
     */
    protected boolean findAndDelete(final long oid) throws FindException, DeleteException {
        boolean deleted = false;

        ET entity = this.findByPrimaryKey( oid );
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
                    return criteria.list();
                }
            } );
        } catch (DataAccessException e) {
            throw new FindException("Couldn't find cert(s)", e);
        }
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
    public Class<? extends Entity> getInterfaceClass() {
        return getImpClass();
    }

    @SuppressWarnings({ "FieldNameHidesFieldInSuperclass" })
    private final Logger logger = Logger.getLogger(getClass().getName());

    private static final boolean useOptimizedCount = SyspropUtil.getBoolean( "com.l7tech.server.hibernate.useOptimizedCount", true );

    private ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    private Map<Long, WeakReference<CacheInfo<ET>>> cacheInfoByOid = new HashMap<Long, WeakReference<CacheInfo<ET>>>();
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
