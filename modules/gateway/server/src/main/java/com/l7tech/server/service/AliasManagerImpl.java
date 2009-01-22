package com.l7tech.server.service;

import com.l7tech.server.FolderSupportHibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.*;

/**
 * Common base class for Entities which can be aliases. Paramaterized so that any caller will get the correct
 * type from find methods and we can process any aliases entity generically.
 * Note the abstract method getNewEntityHeader, when we want to create a new EntityHeader we need to get this
 * from the subclass as we can't create a new instance from the paramaratized type alone.
 *
 * @author darmstrong
 */
public abstract class AliasManagerImpl<AT extends Alias<ET>, ET extends PersistentEntity, HT extends OrganizationHeader>
    extends FolderSupportHibernateEntityManager<AT, HT>
    implements AliasManager<AT, ET, HT>
{

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    @Override
    public AT findAliasByEntityAndFolder(final Long serviceOid, final Long folderOid) throws FindException {
        if (serviceOid == null || folderOid == null) throw new NullPointerException();
        if (!(PersistentEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not PersistentEntity!");

        try {
            //noinspection unchecked
            return (AT)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityOid", serviceOid));
                    crit.add(Restrictions.eq("folder.oid", folderOid));
                    return crit.uniqueResult();
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    @Override
    public Collection<AT> findAllAliasesForEntity(final Long serviceOid) throws FindException {
        if (serviceOid == null) throw new NullPointerException();
        if (!(PersistentEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not PersistentEntity!");

        try {
            //noinspection unchecked
            return (Collection<AT>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
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

    @Override
    public Collection<HT> expandEntityWithAliases(Collection<HT> originalHeaders)
            throws FindException{
        Collection<AT> allAliases = findAll();

        Map<Long, Set<AT>> entityIdToAllItsAliases = new HashMap<Long, Set<AT>>();
        for(AT AT : allAliases){
            Long origServiceId = AT.getEntityOid();
            if(!entityIdToAllItsAliases.containsKey(origServiceId)){
                Set<AT> aliasSet = new HashSet<AT>();
                entityIdToAllItsAliases.put(origServiceId, aliasSet);
            }
            entityIdToAllItsAliases.get(origServiceId).add(AT);
        }

        Collection<HT> returnHeaders = new ArrayList<HT>();
        for(HT ht: originalHeaders){
            Long serviceId = ht.getOid();
            returnHeaders.add(ht);
            if(entityIdToAllItsAliases.containsKey(serviceId)){
                Set<AT> aliases = entityIdToAllItsAliases.get(serviceId);
                for(AT pa: aliases){
                    HT newHT = getNewEntityHeader(ht);
                    newHT.setAliasOid(pa.getOidAsLong());
                    newHT.setFolderOid(pa.getFolder().getOid());
                    returnHeaders.add(newHT);
                }
            }
        }
        return returnHeaders;

    }

    @Override
    public void updateFolder( final long entityId, final Folder folder ) throws UpdateException {
        setParentFolderForEntity( entityId, folder );
    }

    @Override
    public void updateFolder( final AT entity, final Folder folder ) throws UpdateException {
        if ( entity == null ) throw new UpdateException("Alias is required but missing.");
        setParentFolderForEntity( entity.getOid(), folder );
    }
}
