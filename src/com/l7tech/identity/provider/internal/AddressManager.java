package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.StandardManager;

import java.util.Collection;

/**
 * @author alex
 */
public interface AddressManager extends StandardManager {
    public void delete( Address address );
    public Address create();
}
