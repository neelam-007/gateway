package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.Manager;

import java.util.Collection;

/**
 * @author alex
 */
public interface AddressManager extends Manager {
    public void delete( Address address );
    public Address create();
}
