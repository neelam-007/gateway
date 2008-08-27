package com.l7tech.objectmodel;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 25, 2008
 * Time: 6:14:14 PM
 * Interface is implemented by entities which can be stored in a folder and also aliased
 * Allows for all entites which have these characteristics to be processed generically
 */
public interface Organizable extends Aliasable{

    public Long getFolderOid();

    public void setFolderOid(Long folderOid);
}
