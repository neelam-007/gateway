package com.l7tech.objectmodel;

import com.l7tech.identity.provider.internal.Country;

import java.util.Collection;

/**
 * @author alex
 */
public interface Manager {
    public Collection findAll();
    public Collection findAll( int offset, int windowSize );
}
