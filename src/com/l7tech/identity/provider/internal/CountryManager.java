package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.Manager;

/**
 * @author alex
 */
public interface CountryManager extends Manager {
    public void delete( Country country );
    public Country create();
}
