package com.l7tech.portal.metrics;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for retrieving gateway cluster properties.
 */
public interface GenericEntityService {


    /**
     * Retrieves the value of a cluster property as a string.
     *
     * @param clusterPropertyName the name of the cluster property to retrieve.
     * @return the value of the cluster property identified by the given clusterPropertyName.
     * @throws com.l7tech.portal.metrics.ClusterPropertyException if an error occurs retrieving the cluster property value or
     * there is no cluster property with the given name.
     */

    Map<Long,String> getGenericEntityValue(final String clusterPropertyName) throws ClusterPropertyException;
}
