package com.l7tech.uddi;

import java.util.Collection;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * This interface allows for UDDI Registry specific meta data to be associated with UDDI information model objects
 * before they are published to UDDI.
 *
 * For example, when publishing a proxy business service to ActiveSOA, when the original service is also in ActiveSOA,
 * then the proxy business service requires 2 uddi registry specific keyed references
 *
 * @author darmstrong
 */
public interface UDDIRegistrySpecificMetaData {

    /**
     * Get the collection of keyed references which should be added as is to each Business Service published to UDDI
     * which are generated from a published service's gateway WSDL
     *
     * @return Collection<UDDIClient.UDDIKeyedReference> each keyed reference contained in the collection should be
     * added to each business service published to UDDI. Can be null and empty.
     */
    public Collection<UDDIClient.UDDIKeyedReference> getBusinessServiceKeyedReferences();

    /**
     * Get the collection of KeyedReferenceGroups which should be added to each Business Service published to UDDI
     * which are generated from a published service's gateway WSDL
     *
     * @return Collection<UDDIClient.UDDIKeyedReferenceGroup> each keyed reference group contained in the collection should be 
     * added to each business service published to UDDI. Can be null and empty.
     */
    public Collection<UDDIClient.UDDIKeyedReferenceGroup> getBusinessServiceKeyedReferenceGroups();

    //add more as needed e.g. service specific, tmodels etc
}
