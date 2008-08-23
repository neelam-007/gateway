package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import com.l7tech.objectmodel.EntityAlias;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 6, 2008
 * Time: 9:06:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class PublishedServiceAlias extends NamedEntityImp implements EntityAlias {

    private long entityOid;
    private long folderOid;

    @Deprecated // For Serialization and persistence only
    public PublishedServiceAlias() {
    }

    public PublishedServiceAlias(PublishedService pService, long folderOid) {
        this._name = pService.getName()+"_Alias";
        this.folderOid = folderOid;
        this.entityOid = pService.getOid();
    }

    /**
     * Create a copy of the given PublishedServiceAlias.
     *
     * <p>This will copy the identity of the orginal, if you don't want this
     * you will need to reset the id and version.</p>
     *
     * @param publishedServiceAlias The PublishedServiceAlias to duplicate.
     */
    public PublishedServiceAlias(final PublishedServiceAlias publishedServiceAlias) {
        super(publishedServiceAlias);
    }

    /*
   * entityOid is the entity oid of the real entity an instance of this interface
   * is aliasing
   * */
    public long getEntityOid() {
        return entityOid;
    }

    public void setEntityOid(long entityOid) {
        this.entityOid = entityOid;
    }

    public void setFolderOid(long folderOid) {
        this.folderOid = folderOid;
    }

    public long getFolderOid() {
        return folderOid;
    }
}
