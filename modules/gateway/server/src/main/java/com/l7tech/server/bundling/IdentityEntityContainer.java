package com.l7tech.server.bundling;

import com.l7tech.identity.Identity;

public class IdentityEntityContainer<E extends Identity> extends EntityContainer<E> {

    public IdentityEntityContainer(final E entity) {
        super(entity);
    }

    public String getId() {
        return getEntity().getId();
    }
}
