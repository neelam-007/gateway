package com.l7tech.gateway.common.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * A collection of resource entries.
 */
public class ResourceEntryBag implements Iterable<ResourceEntry>, Serializable {

    //- PUBLIC

    public ResourceEntryBag(){
    }

    public ResourceEntryBag( final Collection<ResourceEntry> resourceEntries ){
        this.resourceEntries = new ArrayList<ResourceEntry>( resourceEntries );
    }

    public Collection<ResourceEntry> getResourceEntries() {
        return Collections.unmodifiableCollection( resourceEntries );
    }

    @Override
    public Iterator<ResourceEntry> iterator() {
        return getResourceEntries().iterator();
    }

    //- PRIVATE

    private Collection<ResourceEntry> resourceEntries;
}
