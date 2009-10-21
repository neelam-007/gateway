package com.l7tech.server.uddi;

import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.objectmodel.*;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.uddi.UDDIClient;
import com.l7tech.uddi.UDDIException;

import java.util.List;
import java.util.Map;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
public interface UDDIProxiedServiceManager extends EntityManager<UDDIProxiedService, EntityHeader> {

    /**
     * Save a UDDIProxiedService. This manages the publish of information to a UDDI Registry
     *
     * See http://sarek.l7tech.com/mediawiki/index.php?title=CentraSite_ActiveSOA_Design#Strategy_for_publishing_and_updating_BusinessServices_to_UDDI
     * for implementation strategy
     *
     * @param uddiProxiedService the UDDIProxiedService to save or update
     * @param uddiClient         the UDDIClient configured for the UDDI registry to publish the proxied service to
     * @param businessServices   the list of BusinessService s to publish
     * @param dependentTModels   the list of dependent TModel s which the BusinessServices depend on
     * @return
     * @throws com.l7tech.objectmodel.SaveException
     *
     * @throws com.l7tech.objectmodel.VersionException
     *
     */
    long saveUDDIProxiedService(final UDDIProxiedService uddiProxiedService,
                                final UDDIClient uddiClient,
                                final List<BusinessService> businessServices,
                                final Map<String, TModel> dependentTModels)
            throws SaveException, VersionException, UDDIException;

    /**
     * Update a UDDIProxiedService. This manages the update of information previously published to a UDDI Registry
     *
     * See http://sarek.l7tech.com/mediawiki/index.php?title=CentraSite_ActiveSOA_Design#Strategy_for_publishing_and_updating_BusinessServices_to_UDDI
     * for implementation strategy
     *
     * @param uddiProxiedService the UDDIProxiedService to save or update
     * @param uddiClient         the UDDIClient configured for the UDDI registry to publish the proxied service to
     * @param businessServices   the list of BusinessService s to publish
     * @param dependentTModels   the list of dependent TModel s which the BusinessServices depend on
     * @return
     * @throws com.l7tech.objectmodel.UpdateException
     *
     * @throws com.l7tech.objectmodel.VersionException
     *
     */
    void updateUDDIProxiedService(final UDDIProxiedService uddiProxiedService,
                                  final UDDIClient uddiClient,
                                  final List<BusinessService> businessServices,
                                  final Map<String, TModel> dependentTModels)
            throws UpdateException, VersionException;

}
