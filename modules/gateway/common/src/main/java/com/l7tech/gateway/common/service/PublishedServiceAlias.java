package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Alias;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Aug 6, 2008
 * Time: 9:06:59 AM
 */
public class PublishedServiceAlias extends Alias<PublishedService> {

    @Deprecated // For Serialization and persistence only
    public PublishedServiceAlias(){
    }

    public PublishedServiceAlias(PublishedService pService, long folderOid) {
        super(pService, folderOid);
    }
}
