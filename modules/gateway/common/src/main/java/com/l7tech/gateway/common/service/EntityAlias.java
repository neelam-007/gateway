package com.l7tech.gateway.common.service;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 6, 2008
 * Time: 8:43:59 AM
 *
 * Implemented by any class which represents an alias instance of a real entity
 * Instances of this class are kept in the alias property of the real entity
 * Instance of this interface will be persisted. Each implementation will be persisted
 * in it's own db table. 
 */
public interface EntityAlias {

    /*
    * entityOid is the entity oid of the real entity an instance of this interface
    * is aliasing
    * */
    public long getEntityOid();

    public void setEntityOid(long oid);

    public void setFolderOid(long oid);

    public long getFolderOid();
}
