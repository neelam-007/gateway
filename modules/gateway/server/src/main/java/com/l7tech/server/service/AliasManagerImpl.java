package com.l7tech.server.service;

import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.objectmodel.*;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 20, 2008
 * Time: 3:21:27 PM
 * Common base class for Entities which can be aliases. Paramaterized so that any caller will get the correct
 * type from find methods and we can process any aliases entity generically.
 * Note the abstract method getNewEntityHeader, when we want to create a new EntityHeader we need to get this
 * from the subclass as we can't create a new instance from the paramaratized type alone.
 */
public abstract class AliasManagerImpl<ET extends Alias, HT extends OrganizationHeader>
        extends HibernateEntityManager<ET, HT> implements AliasManager<ET, HT>{

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    public ET findAliasByEntityAndFolder(final Long serviceOid, final Long folderOid) throws FindException {
        if (serviceOid == null || folderOid == null) throw new NullPointerException();
        if (!(PersistentEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not PersistentEntity!");

        try {
            //noinspection unchecked
            return (ET)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityOid", serviceOid));
                    crit.add(Restrictions.eq("folderOid", folderOid));
                    return crit.uniqueResult();
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    public Collection<ET> findAllAliasesForEntity(final Long serviceOid) throws FindException {
        if (serviceOid == null) throw new NullPointerException();
        if (!(PersistentEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not PersistentEntity!");

        try {
            //noinspection unchecked
            return (Collection<ET>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityOid", serviceOid));
                    return crit.list();
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    /**
     * Get a new instance of the paramaratized type HT. Each subclass will return the correct header info
     * @param ht the new header should use ht as the input to it's constructor so that the retured header is identical
     * to the one passed in
     * @return HT an instance of a subclass of OrganizationHeader which is identical to ht passed in.
     */
    public abstract HT getNewEntityHeader(HT ht);

    public Collection<HT> expandEntityWithAliases(Collection<HT> originalHeaders)
            throws FindException{
        Collection<ET> allAliases = findAll();

        Map<Long, Set<ET>> entityIdToAllItsAliases = new HashMap<Long, Set<ET>>();
        for(ET et: allAliases){
            Long origServiceId = et.getEntityOid();
            if(!entityIdToAllItsAliases.containsKey(origServiceId)){
                Set<ET> aliasSet = new HashSet<ET>();
                entityIdToAllItsAliases.put(origServiceId, aliasSet);
            }
            entityIdToAllItsAliases.get(origServiceId).add(et);
        }

        Collection<HT> returnHeaders = new ArrayList<HT>();
        for(HT ht: originalHeaders){
            Long serviceId = ht.getOid();
            returnHeaders.add(ht);
            if(entityIdToAllItsAliases.containsKey(serviceId)){
                Set<ET> aliases = entityIdToAllItsAliases.get(serviceId);
                for(ET pa: aliases){
                    HT newHT = getNewEntityHeader(ht);
                    newHT.setAlias(true);
                    newHT.setFolderOid(pa.getFolderOid());
                    returnHeaders.add(newHT);
                }
            }
        }
        return returnHeaders;

    }
}
