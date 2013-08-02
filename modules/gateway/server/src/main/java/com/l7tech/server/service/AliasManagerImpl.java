package com.l7tech.server.service;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.FolderSupportHibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
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
public abstract class AliasManagerImpl<AT extends Alias<ET>, ET extends GoidEntity, HT extends OrganizationHeader>
    extends FolderSupportHibernateEntityManager<AT, AliasHeader<ET>>
    implements AliasManager<AT, ET, HT>
{

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    @Override
    public AT findAliasByEntityAndFolder(final Goid serviceGoid, final Goid folderGoid) throws FindException {
        if (serviceGoid == null || folderGoid == null) throw new NullPointerException();
        if (!(GoidEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not GoidEntity!");

        try {
            //noinspection unchecked
            return (AT)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityGoid", serviceGoid));
                    crit.add(Restrictions.eq("folder.goid", folderGoid));
                    return crit.uniqueResult();
                }
            });
        } catch (HibernateException e) {
            throw new FindException("Couldn't find unique entity", e);
        }
    }

    @Override
    public Collection<AT> findAllAliasesForEntity(final Goid serviceGoid) throws FindException {
        if (serviceGoid == null) throw new NullPointerException();
        if (!(GoidEntity.class.isAssignableFrom(getImpClass()))) throw new IllegalArgumentException("This Manager's entities are not GoidEntity!");

        try {
            //noinspection unchecked
            return (Collection<AT>)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                public Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("entityGoid", serviceGoid));
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

        Map<Goid, Set<AT>> entityIdToAllItsAliases = new HashMap<Goid, Set<AT>>();
        for(AT AT : allAliases){
            Goid origServiceId = AT.getEntityGoid();
            if(!entityIdToAllItsAliases.containsKey(origServiceId)){
                Set<AT> aliasSet = new HashSet<AT>();
                entityIdToAllItsAliases.put(origServiceId, aliasSet);
            }
            entityIdToAllItsAliases.get(origServiceId).add(AT);
        }

        Collection<HT> returnHeaders = new ArrayList<HT>();
        for(HT ht: originalHeaders){
            Goid serviceId = ht.getGoid();
            returnHeaders.add(ht);
            if(entityIdToAllItsAliases.containsKey(serviceId)){
                Set<AT> aliases = entityIdToAllItsAliases.get(serviceId);
                for(AT pa: aliases){
                    HT newHT = getNewEntityHeader(ht);
                    newHT.setAliasGoid(pa.getGoid());
                    newHT.setFolderGoid(pa.getFolder().getGoid());
                    returnHeaders.add(newHT);
                }
            }
        }
        return returnHeaders;

    }

    @Override
    public void updateFolder( final Goid entityId, final Folder folder ) throws UpdateException {
        setParentFolderForEntity( entityId, folder );
    }

    @Override
    public void updateFolder( final AT entity, final Folder folder ) throws UpdateException {
        if ( entity == null ) throw new UpdateException("Alias is required but missing.");
        setParentFolderForEntity( entity.getGoid(), folder );
    }

    @Override
    public AT findByHeader(EntityHeader header) throws FindException {
        if (header instanceof OrganizationHeader) {
            return findByPrimaryKey(((OrganizationHeader)header).getAliasGoid());
        } else if (header.getType().name().endsWith("_ALIAS")) {
            return super.findByHeader(header);
        } else {
            throw new IllegalArgumentException("Unsupported header type: " + header);
        }
    }
}
