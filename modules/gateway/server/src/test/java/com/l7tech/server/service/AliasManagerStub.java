/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.objectmodel.Alias;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.AliasHeader;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.EntityManagerStub;

import java.util.*;

/** @author alex */
public abstract class AliasManagerStub<AT extends Alias<ET>, ET extends PersistentEntity, HT extends OrganizationHeader>
    extends EntityManagerStub<AT, AliasHeader<ET>>
    implements AliasManager<AT, ET, HT>
{
    protected AliasManagerStub(AT... entitiesIn) {
        super(entitiesIn);
    }

    @Override
    public AT findAliasByEntityAndFolder(Long entityOid, Long folderOid) throws FindException {
        for (Map.Entry<Long, AT> entry : entities.entrySet()) {
            AT alias = entry.getValue();
            Folder folder = alias.getFolder();
            if (folder.getOid() == folderOid && alias.getEntityOid() == entityOid) return alias;
        }
        return null;
    }

    @Override
    public Collection<AT> findAllAliasesForEntity(Long entityOid) throws FindException {
        List<AT> aliases = new ArrayList<AT>();
        for (Map.Entry<Long, AT> entry : entities.entrySet()) {
            AT alias = entry.getValue();
            if (alias.getEntityOid() == entityOid) aliases.add(alias);
        }
        return aliases;
    }

    @Override
    public Collection<HT> expandEntityWithAliases(Collection<HT> originalHeaders) throws FindException {
        Collection<AT> allAliases = findAll();

        Map<Long, Set<AT>> entityIdToAllItsAliases = new HashMap<Long, Set<AT>>();
        for (AT AT : allAliases) {
            Long origServiceId = AT.getEntityOid();
            if (!entityIdToAllItsAliases.containsKey(origServiceId)) {
                Set<AT> aliasSet = new HashSet<AT>();
                entityIdToAllItsAliases.put(origServiceId, aliasSet);
            }
            entityIdToAllItsAliases.get(origServiceId).add(AT);
        }

        Collection<HT> returnHeaders = new ArrayList<HT>();
        for (HT ht : originalHeaders) {
            Long serviceId = ht.getOid();
            returnHeaders.add(ht);
            if (entityIdToAllItsAliases.containsKey(serviceId)) {
                Set<AT> aliases = entityIdToAllItsAliases.get(serviceId);
                for (AT pa : aliases) {
                    HT newHT = getNewEntityHeader(ht);
                    newHT.setAliasOid(pa.getOidAsLong());
                    newHT.setFolderOid(pa.getFolder().getOid());
                    returnHeaders.add(newHT);
                }
            }
        }
        return returnHeaders;

    }

    /**
     * Get a new instance of the paramaratized type HT. Each subclass will return the correct header info
     * @param ht the new header should use ht as the input to it's constructor so that the retured header is identical
     * to the one passed in
     * @return HT an instance of a subclass of OrganizationHeader which is identical to ht passed in.
     */
    public abstract HT getNewEntityHeader(HT ht);
}
