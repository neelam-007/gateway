package com.l7tech.server.bundling;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;

/**
 * Created by wlui on 25/03/14.
 */
public class PrivateKeyContainer extends EntityContainer<SsgKeyEntry> {
    public PrivateKeyContainer(SsgKeyEntry entity) {
        super(entity);
    }

    @Override
    public String getId() {
        return entity.getId();
    }
}
