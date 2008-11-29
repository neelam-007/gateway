package com.l7tech.gateway.common.service;

import com.l7tech.objectmodel.Alias;
import com.l7tech.objectmodel.folder.Folder;

/**
 * @author darmstrong
 */
public class PublishedServiceAlias extends Alias<PublishedService> {
    @Deprecated // For Serialization and persistence only
    protected PublishedServiceAlias() {
    }

    public PublishedServiceAlias(PublishedService pService, Folder folder) {
        super(pService, folder);
    }
}
