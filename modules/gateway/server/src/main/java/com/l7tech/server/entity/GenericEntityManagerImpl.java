package com.l7tech.server.entity;

import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.InvalidGenericEntityException;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.util.Charsets;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.PoolByteArrayOutputStream;
import com.l7tech.util.ResourceUtils;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.annotation.Transactional;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.springframework.transaction.annotation.Propagation.REQUIRED;

/**
 * Implementation of GenericEntityManager.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class GenericEntityManagerImpl extends HibernateEntityManager<GenericEntity, GenericEntityHeader> implements GenericEntityManager, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(GenericEntityManagerImpl.class.getName());

    public static final String F_CLASSNAME = "entityClassName";

    private final String HQL_FIND_ALL_OIDS_AND_VERSIONS_BY_CLASSNAME =
            "SELECT " +
                    getTableName() + "." + F_OID + ", " +
                    getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_CLASSNAME + " = ?";

    private final String HQL_FIND_VERSION_BY_OID_AND_CLASSNAME =
            "SELECT " + getTableName() + "." + F_VERSION +
            " FROM " + getTableName() +
            " IN CLASS " + getImpClass().getName() +
            " WHERE " + getTableName() + "." + F_OID + " = ?" +
            "   AND " + getTableName() + "." + F_CLASSNAME + " = ?";

    private final ConcurrentMap<String, Class<? extends GenericEntity>> registeredClasses = new ConcurrentHashMap<String, Class<? extends GenericEntity>>();
    private ApplicationContext applicationContext;

    @Override
    @Transactional(readOnly=true)
    public Class<? extends Entity> getImpClass() {
        return GenericEntity.class;
    }

    @Override
    @Transactional(readOnly=true)
    protected UniqueType getUniqueType() {
        return UniqueType.OTHER;
    }

    @Override
    public void registerClass(@NotNull Class<? extends GenericEntity> entityClass) throws IllegalArgumentException {
        if (!GenericEntity.class.isAssignableFrom(entityClass))
            throw new IllegalArgumentException("Specified entity class is not assignable to GenericEntity");
        if (entityClass == GenericEntity.class)
            throw new IllegalArgumentException("Specified entity class is GenericEntity itself; concrete entity class should be a subclass of GenericEntity");
        String name = entityClass.getName();
        final Class<? extends GenericEntity> prev = registeredClasses.putIfAbsent(name, entityClass);
        if (null != prev)
            throw new IllegalArgumentException("Specified entity classname is already registered");
    }

    @Override
    public boolean unRegisterClass(String entityClassName) {
        final Class<? extends GenericEntity> prev = registeredClasses.remove(entityClassName);
        return prev != null;
    }

    @Override
    @Transactional(readOnly=true)
    public <ET extends GenericEntity> EntityManager<ET, GenericEntityHeader> getEntityManager(@NotNull final Class<ET> entityClass) {
        // Get delegate from Spring so it has tx wrappers
        final GenericEntityManager gem = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        return new EntityManager<ET, GenericEntityHeader>() {
            @Override
            public ET findByPrimaryKey(long oid) throws FindException {
                return gem.findByGenericClassAndPrimaryKey(entityClass, oid);
            }

            @Override
            public Collection<GenericEntityHeader> findAllHeaders() throws FindException {
                return gem.findAllHeaders(entityClass);
            }

            @Override
            public Collection<GenericEntityHeader> findAllHeaders(int offset, int windowSize) throws FindException {
                return gem.findAllHeaders(entityClass, offset, windowSize);
            }

            @Override
            public Collection<ET> findAll() throws FindException {
                return gem.findAll(entityClass);
            }

            @Override
            public long save(ET entity) throws SaveException {
                return gem.save(entityClass, entity);
            }

            @Override
            public Integer getVersion(long oid) throws FindException {
                return gem.getVersion(entityClass, oid);
            }

            @Override
            public Map<Long, Integer> findVersionMap() throws FindException {
                return gem.findVersionMap(entityClass);
            }

            @Override
            public void delete(ET entity) throws DeleteException {
                gem.delete(entityClass, entity);
            }

            @Override
            public ET getCachedEntity(long o, int maxAge) throws FindException {
                return gem.getCachedEntity(entityClass, o, maxAge);
            }

            @Override
            public Class<? extends Entity> getInterfaceClass() {
                return entityClass;
            }

            @Override
            public EntityType getEntityType() {
                return gem.getEntityType();
            }

            @Override
            public String getTableName() {
                return gem.getTableName();
            }

            @Override
            public ET findByUniqueName(String name) throws FindException {
                return gem.findByUniqueName(entityClass, name);
            }

            @Override
            public void delete(long oid) throws DeleteException, FindException {
                gem.delete(entityClass, oid);
            }

            @Override
            public void update(ET entity) throws UpdateException {
                gem.update(entityClass, entity);
            }

            @Override
            public ET findByHeader(EntityHeader header) throws FindException {
                return gem.findByHeader(entityClass, header);
            }

            @Override
            public Class<? extends Entity> getImpClass() {
                return entityClass;
            }
        };
    }

    @Override
    @Transactional(readOnly=true)
    public <ET extends GenericEntity> Collection<ET> findAll(final @NotNull Class<ET> entityClass) throws FindException {
        if (!isRegistered(entityClass))
            throw new FindException(regmsg(entityClass));
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Collection<ET>>() {
                @Override
                protected Collection<ET> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria(getImpClass());
                    criteria.add(Restrictions.eq("entityClassName", entityClass.getName()));

                    List list = criteria.list();
                    List<ET> ret = new ArrayList<ET>();
                    for (Object obj : list) {
                        try {
                            ret.add(asConcreteEntity((GenericEntity)obj, entityClass));
                        } catch (InvalidGenericEntityException e) {
                            logger.log(Level.INFO, "Ignoring invalid generic entity", e);
                        }
                    }
                    return ret;
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find all generic entities of type " + entityClass.getName(), e);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<GenericEntityHeader> findAllHeaders(final @NotNull Class<? extends GenericEntity> entityClass) throws FindException {
        if (registeredClasses.get(entityClass.getName()) == null)
            throw new FindException("No generic entity class named " + entityClass.getName() + " is registered");
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Collection<GenericEntityHeader>>() {
                @Override
                protected Collection<GenericEntityHeader> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria(getImpClass());
                    criteria.add(Restrictions.eq("entityClassName", entityClass.getName()));

                    List list = criteria.list();
                    List<GenericEntityHeader> ret = new ArrayList<GenericEntityHeader>();
                    for (Object obj : list) {
                        ret.add(new GenericEntityHeader((GenericEntity)obj));
                    }
                    return ret;
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find all generic entities of type " + entityClass.getName(), e);
        }
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<GenericEntityHeader> findAllHeaders(final @NotNull Class<? extends GenericEntity> entityClass, final int offset, final int windowSize) throws FindException {
        if (registeredClasses.get(entityClass.getName()) == null)
            throw new FindException("No generic entity class named " + entityClass.getName() + " is registered");
        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Collection<GenericEntityHeader>>() {
                @Override
                protected Collection<GenericEntityHeader> doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    final Criteria criteria = session.createCriteria(getImpClass());
                    criteria.setFirstResult(offset);
                    criteria.setFetchSize(windowSize);
                    criteria.add(Restrictions.eq("entityClassName", entityClass.getName()));

                    List list = criteria.list();
                    List<GenericEntityHeader> ret = new ArrayList<GenericEntityHeader>();
                    for (Object obj : list) {
                        ret.add(new GenericEntityHeader((GenericEntity)obj));
                    }
                    return ret;
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find all generic entities of type " + entityClass.getName(), e);
        }
    }

    @Override
    public <ET extends GenericEntity> long save(@NotNull Class<ET> entityClass, ET entity) throws SaveException {
        if (!isRegistered(entityClass))
            throw new SaveException(regmsg(entityClass));
        if (!entityClass.getName().equals(entity.getEntityClassName()))
            throw new SaveException("Generic entity class cannot be saved with class other than " + entityClass.getName() + ": actual class name is " + entity.getEntityClassName());
        regenerateValueXml(entity);

        GenericEntity pers = new GenericEntity();
        copyBaseFields(entity, pers);
        return save(pers);
    }

    @Override
    @Transactional(readOnly=true)
    public <ET extends GenericEntity> Integer getVersion(final @NotNull Class<ET> entityClass, final long oid) throws FindException {
        if (!isRegistered(entityClass))
            throw new FindException(regmsg(entityClass));

        try {
            return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<Integer>() {
                @Override
                public Integer doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Query q = session.createQuery(HQL_FIND_VERSION_BY_OID_AND_CLASSNAME);
                    q.setLong(0, oid);
                    q.setString(1, entityClass.getName());
                    return (Integer) q.uniqueResult();
                }
            });
        } catch (Exception e) {
            throw new FindException(e.toString(), e);
        }
    }

    private <ET extends GenericEntity> boolean isRegistered(Class<ET> entityClass) {
        return registeredClasses.get(entityClass.getName()) != null;
    }

    @Override
    @Transactional(readOnly=true)
    public <ET extends GenericEntity> Map<Long, Integer> findVersionMap(@NotNull Class<ET> entityClass) throws FindException {
        if (!isRegistered(entityClass))
            throw new FindException(regmsg(entityClass));

        Map<Long, Integer> result = new HashMap<Long, Integer>();
        if (!PersistentEntity.class.isAssignableFrom(getImpClass())) throw new FindException("Can't find non-Entities!");

        Session s = null;
        FlushMode old = null;
        try {
            s = getSession();
            old = s.getFlushMode();
            s.setFlushMode(FlushMode.MANUAL);
            Query q = s.createQuery(HQL_FIND_ALL_OIDS_AND_VERSIONS_BY_CLASSNAME);
            q.setString(0, entityClass.getName());
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
            throw new FindException("Unable to find version map: " + ExceptionUtils.getMessage(e), e);
        } finally {
            if (s != null && old != null) s.setFlushMode(old);
            releaseSession(s);
        }

        return result;
    }

    @Override
    public <ET extends GenericEntity> void delete(@NotNull Class<ET> entityClass, ET entity) throws DeleteException {
        if (!isRegistered(entityClass))
            throw new DeleteException(regmsg(entityClass));
        if (!entityClass.getName().equals(entity.getEntityClassName()))
            throw new DeleteException("Generic entity class cannot be deleted with class other than " + entityClass.getName() + ": actual class name is " + entity.getEntityClassName());
        delete(entity);
    }

    private static <ET extends GenericEntity> String regmsg(Class<ET> entityClass) {
        return "No generic entity class named " + entityClass.getName() + " is registered";
    }

    @Override
    @Transactional(readOnly=true)
    protected GenericEntity checkAndCache(GenericEntity thing) throws FindException {
        String className = thing.getEntityClassName();
        Class<? extends GenericEntity> entityClass = registeredClasses.get(className);
        if (entityClass == null)
            throw new FindException("No generic entity class named " + className + " is registered");

        thing = asConcreteEntity(thing, entityClass);
        return super.checkAndCache(thing);
    }

    @Override
    @Transactional(readOnly=true)
    public <ET extends GenericEntity> ET getCachedEntity(@NotNull Class<ET> entityClass, long o, int maxAge) throws FindException {
        if (!isRegistered(entityClass))
            throw new FindException(regmsg(entityClass));

        GenericEntity ret = super.getCachedEntity(o, maxAge);
        if (ret == null)
            return null;

        return entityClass.isInstance(ret) ? entityClass.cast(ret) : asConcreteEntity(ret, entityClass);
    }

    @Override
    @Transactional(readOnly=true)
    public <ET extends GenericEntity> ET findByUniqueName(@NotNull Class<ET> entityClass, String name) throws FindException {
        if (!isRegistered(entityClass))
            throw new FindException(regmsg(entityClass));

        GenericEntity ret = findByUniqueName(name);
        if (ret == null)
            return null;

        return entityClass.isInstance(ret) ? entityClass.cast(ret) : asConcreteEntity(ret, entityClass);
    }

    @Override
    public <ET extends GenericEntity> void delete(@NotNull Class<ET> entityClass, long oid) throws DeleteException, FindException {
        if (!isRegistered(entityClass))
            throw new DeleteException(regmsg(entityClass));

        GenericEntity got = findByPrimaryKey(oid);
        if (got != null && !entityClass.getName().equals(got.getEntityClassName()))
            throw new DeleteException("Generic entity with oid " + oid + " cannot be deleted as a " + entityClass.getName() + " because it is actually a " + got.getEntityClassName());

        delete(oid);
    }

    @Override
    public <ET extends GenericEntity> void update(@NotNull Class<ET> entityClass, ET entity) throws UpdateException {
        if (!isRegistered(entityClass))
            throw new UpdateException(regmsg(entityClass));

        try {
            final long oid = entity.getOid();
            GenericEntity got = findByPrimaryKey(oid);
            if (got != null && !entityClass.getName().equals(got.getEntityClassName()))
                throw new UpdateException("Generic entity with oid " + oid + " cannot be updated as a " + entityClass.getName() + " because it is actually a " + got.getEntityClassName());
        } catch (FindException e) {
            throw new UpdateException("Unable to update generic entity: " + ExceptionUtils.getMessage(e), e);
        }

        regenerateValueXml(entity);

        GenericEntity pers = new GenericEntity();
        copyBaseFields(entity, pers);
        update(pers);
    }

    @Override
    @Transactional(readOnly=true)
    public <ET extends GenericEntity> ET findByHeader(@NotNull Class<ET> entityClass, EntityHeader header) throws FindException {
        if (!isRegistered(entityClass))
            throw new FindException(regmsg(entityClass));

        GenericEntity ret = findByHeader(header);
        if (ret == null)
            return null;
        if (!entityClass.getName().equals(ret.getEntityClassName()))
            return null;
        return asConcreteEntity(ret, entityClass);
    }

    @Override
    @Transactional(readOnly=true)
    public <ET extends GenericEntity> ET findByGenericClassAndPrimaryKey(@NotNull Class<ET> entityClass, long oid) throws FindException {
        if (!isRegistered(entityClass))
            throw new FindException(regmsg(entityClass));

        GenericEntity ret = findByPrimaryKey(oid);
        if (ret == null)
            return null;
        if (!entityClass.getName().equals(ret.getEntityClassName()))
            return null;
        return asConcreteEntity(ret, entityClass);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    protected GenericEntityHeader newHeader(GenericEntity entity) {
        return new GenericEntityHeader(entity);
    }

    <ET extends GenericEntity>
    ET asConcreteEntity(GenericEntity that, Class<ET> entityClass) throws InvalidGenericEntityException {
        final String entityClassName = that.getEntityClassName();
        if (!entityClass.getName().equals(entityClassName))
            throw new InvalidGenericEntityException("generic entity is not of expected class " + entityClassName + ": actual classname is " + entityClass.getName());

        final String xml = that.getValueXml();
        if (xml == null || xml.length() < 1) {
            // New object -- leave non-base fields as default
            try {
                ET ret = entityClass.newInstance();
                copyBaseFields(that, ret);
                return ret;
            } catch (Exception e) {
                throw new InvalidGenericEntityException("Unable to instantiate " + entityClass.getName() + ": " + ExceptionUtils.getMessage(e), e);
            }
        }

        XMLDecoder decoder = null;
        try {
            decoder = new XMLDecoder(new ByteArrayInputStream(xml.getBytes(Charsets.UTF8)), null, null, entityClass.getClassLoader());
            Object obj = decoder.readObject();
            if (entityClass.isInstance(obj)) {
                ET ret = entityClass.cast(obj);
                copyBaseFields(that, ret);
                return ret;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new InvalidGenericEntityException("Stream does not contain any entities", e);
        } finally {
            if (decoder != null) decoder.close();
        }

        throw new InvalidGenericEntityException("Generic entity XML stream did not contain an instance of " + entityClassName);
    }

    // Set the specified generic entity to have the same base field values as this generic entity, overwriting some if necessary
    static void copyBaseFields(GenericEntity source, GenericEntity dest) {
        dest.setOid(source.getOid());
        dest.setVersion(source.getVersion());
        dest.setName(source.getName());
        dest.setDescription(source.getDescription());
        dest.setEntityClassName(source.getEntityClassName());
        dest.setValueXml(source.getValueXml());
    }

    static void regenerateValueXml(GenericEntity that) {
        PoolByteArrayOutputStream baos = new PoolByteArrayOutputStream(1024);
        String xml = that.getValueXml();
        try {
            that.setValueXml(""); // set to empty while serializing to prevent including the XML in the XML
            XMLEncoder encoder = new XMLEncoder(new NonCloseableOutputStream(baos));
            encoder.writeObject(that);
            encoder.close();
            xml = baos.toString(Charsets.UTF8);
        } finally {
            ResourceUtils.closeQuietly(baos);
            that.setValueXml(xml);
        }
    }

    @Override
    protected Collection<Map<String, Object>> getUniqueConstraints(GenericEntity entity) {
        Map<String,Object> map = new HashMap<String, Object>();
        map.put("name", entity.getName());
        map.put("entityClassName", entity.getEntityClassName());
        return Arrays.asList(map);
    }
}
