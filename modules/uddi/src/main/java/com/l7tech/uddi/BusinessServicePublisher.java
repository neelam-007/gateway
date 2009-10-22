package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import static com.l7tech.uddi.UDDIReferenceUpdater.TMODEL_TYPE.WSDL_PORT_TYPE;
import static com.l7tech.uddi.UDDIReferenceUpdater.TMODEL_TYPE.WSDL_BINDING;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Responsible for publishing a set of BusinessServices extracted from a WSDL to a UDDI Registry.
 *
 * Part of publishing requires publising tModels in the correct order and following the publishing of tModels,
 * updating any dependent tModels and BusinessServices with the tModelKeys.
 *
 * @author darmstrong
 */
public class BusinessServicePublisher {
    private static final Logger logger = Logger.getLogger(BusinessServicePublisher.class.getName());

    /**
     * Publish the list of Business Services to the UDDI Registry via the supplied UDDIClient
     *
     * @param uddiClient         the UDDIClient configured for the UDDI registry to publish the proxied service to
     * @param businessServices   the list of BusinessService s to publish
     * @param dependentTModels   the list of dependent TModel s which the BusinessServices depend on
     * @throws UDDIException
     */
    public void publishServicesToUDDIRegistry(final UDDIClient uddiClient,
                                              final List<BusinessService> businessServices,
                                              final Map<String, TModel> dependentTModels) throws UDDIException {
        //remember udpates in case they need to be rolled back later
        final List<TModel> publishedTModels = new ArrayList<TModel>();

        //first publish TModels which represent wsdl:portType, as they have no keyedReference dependencies
        publishedTModels.addAll(publishDependentTModels(uddiClient, dependentTModels, WSDL_PORT_TYPE));

        List<TModel> bindingTModels = new ArrayList<TModel>();
        for (TModel tModel : dependentTModels.values()) {
            if (UDDIReferenceUpdater.getTModelType(tModel) != UDDIReferenceUpdater.TMODEL_TYPE.WSDL_BINDING) continue;
            bindingTModels.add(tModel);
        }
        if (bindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");

        UDDIReferenceUpdater.updateBindingTModelReferences(bindingTModels, dependentTModels);
        //next publish TModels which represent wsdl:binding, as they are dependent on wsdl:portType tModels
        publishedTModels.addAll(publishDependentTModels(uddiClient, dependentTModels, WSDL_BINDING));

        UDDIReferenceUpdater.updateBusinessServiceReferences(businessServices, dependentTModels);

        publishBusinessServices(uddiClient, businessServices, publishedTModels);
    }

    /**
     * Publish the BusinessServices to the UDDI Registry. If any publish operation fails, a roll back will
     * be attempted. An attempt will also be made to delete all tModels in the List rollbackTModelsToDelete
     * <p/>
     * All BusinessServices in the list should be ready to be published, all keyedReferences to wsdl:portType
     * and wsdl:binding tModels should have been resolved already.
     *
     * @param uddiClient              UDDIClient for publishing to the UDDI Registry
     * @param businessServices        List of BusinessServices to publish
     * @param rollbackTModelsToDelete list of TModels to attempt to delete on any failure.
     * @throws com.l7tech.uddi.UDDIException if any problems publishing the Business Services to UDDI
     */
    public void publishBusinessServices(final UDDIClient uddiClient,
                                        final List<BusinessService> businessServices,
                                        final List<TModel> rollbackTModelsToDelete) throws UDDIException {

        final List<BusinessService> publishedServices = new ArrayList<BusinessService>();
        for (BusinessService businessService : businessServices) {
            try {
                final boolean published = uddiClient.publishBusinessService(businessService);
                if (published) publishedServices.add(businessService);

            } catch (UDDIException e) {
                logger.log(Level.WARNING, "Exception publishing BusinesService: " + e.getMessage());
                try {
                    //Roll back any tModels published first
                    if (!rollbackTModelsToDelete.isEmpty()) {
                        logger.log(Level.WARNING, "Attempting to rollback published tModels: " + e.getMessage());
                        boolean deletedTModel = false;
                        for (TModel tModel : rollbackTModelsToDelete) {
                            uddiClient.deleteTModel(tModel.getTModelKey());
                            deletedTModel = true;
                        }
                        if (deletedTModel) logger.log(Level.WARNING, "Delete published tModels: " + e.getMessage());
                    }

                    if (!publishedServices.isEmpty()) {
                        logger.log(Level.WARNING, "Attempting to rollback published BusinessServices");
                    }
                    boolean deletedService = false;
                    for (BusinessService publishedService : publishedServices) {
                        uddiClient.deleteBusinessService(publishedService.getServiceKey());
                        deletedService = true;
                    }
                    if (deletedService) logger.log(Level.WARNING, "Deleted published BusinessServices");
                } catch (UDDIException e1) {
                    //Not going to throw e1, just log it, as the main error happend in the above publish try block
                    //just log it
                    logger.log(Level.WARNING, "Could not undo published BusinessServices following exception: " + e1.getMessage());
                }
                throw e;

            }
        }
    }

    /**
     * Publish all TModels contained in the dependentTModels map, which are not already published. Only the type
     * of TModel represented by tmodelType will be published. This is to allow for TModels to be published in their
     * dependency order
     * <p/>
     * Any exception occurs and an attempt is made to delete any tModels which were successfully published.
     *
     * @param uddiClient       UDDIClient to publish with
     * @param dependentTModels tModels to publish
     * @param tmodelType       TMODEL_TYPE the type of TModel we should publish.
     * @return List of TModels which were successfully published
     * @throws UDDIException any exception publishing the tmodels
     */
    public List<TModel> publishDependentTModels(final UDDIClient uddiClient,
                                                final Map<String, TModel> dependentTModels,
                                                final UDDIReferenceUpdater.TMODEL_TYPE tmodelType) throws UDDIException {
        final List<TModel> publishedTModels = new ArrayList<TModel>();
        try {
            for (Map.Entry<String, TModel> entrySet : dependentTModels.entrySet()) {
                final TModel tModel = entrySet.getValue();
                //only publish the type were currently interested in
                if (UDDIReferenceUpdater.getTModelType(tModel) != tmodelType) continue;

                final boolean published = uddiClient.publishTModel(tModel);
                if (published) publishedTModels.add(tModel);
            }
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Exception publishing tModels: " + e.getMessage());
            //Can we roll back any work done so far
            try {
                if (!publishedTModels.isEmpty()) {
                    logger.log(Level.WARNING, "Attempting to rollback published TModels");
                }
                boolean deletedModel = false;
                for (TModel tModel : publishedTModels) {
                    uddiClient.deleteTModel(tModel.getTModelKey());
                    deletedModel = true;
                }
                if (deletedModel) logger.log(Level.WARNING, "Deleted published TModels");
            } catch (UDDIException e1) {
                //Not going to throw e1, just log it, as the main error happend in the above publish try block
                //just log it
                logger.log(Level.WARNING, "Could not undo published TModels following exception: " + e1.getMessage());
            }
            throw e;
        }
        return Collections.unmodifiableList(publishedTModels);
    }

}
