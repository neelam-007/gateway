package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 20, 2008
 * Time: 3:22:47 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ServiceAliasManager extends EntityManager<PublishedServiceAlias, ServiceHeader> {

    public PublishedServiceAlias findAliasByServiceAndFolder(Long serviceOid, Long folderOid) throws FindException;

    public Collection<PublishedServiceAlias> findAllAliasesForService(Long serviceOid) throws FindException;
}
