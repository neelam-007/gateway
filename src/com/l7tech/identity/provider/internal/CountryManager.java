package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.StandardManager;

import java.util.Collection;

/**
 * @author alex
 */
public interface CountryManager extends StandardManager {
    public void delete( Country country );
    public Country create();
}
