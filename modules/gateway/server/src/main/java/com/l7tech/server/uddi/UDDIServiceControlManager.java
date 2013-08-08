package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIServiceControl;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.GoidEntityManager;

import java.util.Collection;

/**
 * EntityManager for UDDIServiceControl
 */
public interface UDDIServiceControlManager extends GoidEntityManager<UDDIServiceControl,EntityHeader> {

    /**
     * Find a UDDIServiceControl by published service goid.
     *
     * @param serviceGoid The service goid
     * @return The UDDIServiceControl or null
     * @throws FindException If an error occurs.
     */
    UDDIServiceControl findByPublishedServiceGoid( Goid serviceGoid ) throws FindException;

    /**
     * Find UDDIServiceControls by UDDI registry goid.
     *
     * @param registryGoid The UDDI registry goid
     * @return The collection of UDDIServiceControls (may be empty but not null)
     * @throws FindException If an error occurs.
     */
    Collection<UDDIServiceControl> findByUDDIRegistryGoid( Goid registryGoid ) throws FindException;

    /**
     * Find UDDIServiceControls by UDDI business service key within the given registry.
     *
     * @param registryGoid The UDDI registry goid
     * @param serviceKey The UDDI business service key
     * @param uddiControlled True to find only UDDI controlled services (null for any)
     * @return The collection of UDDIServiceControls (may be empty but not null)
     * @throws FindException If an error occurs.
     */
    Collection<UDDIServiceControl> findByUDDIRegistryAndServiceKey( Goid registryGoid,
                                                                    String serviceKey,
                                                                    Boolean uddiControlled )  throws FindException;

    /**
     * Find UDDIServiceControls with the specified metrics flag within the given registry.
     *
     * @param registryGoid The UDDI registry goid
     * @param metricsEnabled the state of the metrics enabled flag to match
     * @return The collection of UDDIServiceControls (may be empty but not null)
     * @throws FindException If an error occurs.
     */
    Collection<UDDIServiceControl> findByUDDIRegistryAndMetricsState( Goid registryGoid,
                                                                      boolean metricsEnabled ) throws FindException;

    /**
     * Find UDDIServiceControls by UDDI business service key.
     *
     * @param serviceKey The UDDI business service key
     * @return The collection of UDDIServiceControls (may be empty but not null)
     * @throws FindException If an error occurs.
     */
    Collection<UDDIServiceControl> findByUDDIServiceKey( String serviceKey ) throws FindException;
}
