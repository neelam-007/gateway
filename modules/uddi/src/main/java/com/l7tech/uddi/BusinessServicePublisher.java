package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import static com.l7tech.uddi.UDDIReferenceUpdater.TMODEL_TYPE.WSDL_PORT_TYPE;
import static com.l7tech.uddi.UDDIReferenceUpdater.TMODEL_TYPE.WSDL_BINDING;

import java.util.*;
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
     * Provides best effort commit / rollback behaviour
     *
     * @param uddiClient       the UDDIClient configured for the UDDI registry to publish the proxied service to
     * @param businessServices the list of BusinessService s to publish
     * @param dependentTModels the list of dependent TModel s which the BusinessServices depend on
     * @param generalKeyword   the keyword that will be associated with each BusinessService published for this WSDL
     * @throws UDDIException any problems publishing UDDI info
     */
    public void publishServicesToUDDIRegistry(final UDDIClient uddiClient,
                                              final List<BusinessService> businessServices,
                                              final Map<String, TModel> dependentTModels,
                                              final String generalKeyword) throws UDDIException {
        //make sure this gateway wsdl has not already been published
        if (!uddiClient.findMatchingBusinessServices(generalKeyword).isEmpty())
            throw new IllegalStateException("Gateway WSDL has already been published");

        publishServicesToUDDI(uddiClient, businessServices, dependentTModels);
    }

    public void updateServicesToUDDIRegistry(final UDDIClient uddiClient,
                                             final List<BusinessService> wsdlBusinessServices,
                                             final Map<String, TModel> wsdlDependentTModels,
                                             final List<BusinessService> uddiBusinessServices,
                                             final Map<String, TModel> uddiDependentTModels,
                                             final String generalKeyword) throws UDDIException {

        final List<BusinessService> publishedServices = uddiClient.findMatchingBusinessServices(generalKeyword);
        //check if this service has already been published
        if (publishedServices.isEmpty()){
            //it's ok if it hasn't, just log the fact, may have been deleted via UDDI interface
            logger.log(Level.WARNING, "No exising published BusinessService found. Publishing for the first time");
        }

        //map to look up existing services
        Map<String, BusinessService> allPublished = new HashMap<String, BusinessService>();
        for(BusinessService bs: publishedServices){
            allPublished.put(bs.getName().get(0).getValue(), bs);
        }

        Map<String, BusinessService> toPublish = new HashMap<String, BusinessService>();
        for(BusinessService bs: wsdlBusinessServices){
            toPublish.put(bs.getName().get(0).getValue(), bs);
        }

        //find which services already exist, and reuse their serviceKeys if found
        //also need to know which BusinessServices to delete
        Set<BusinessService> serviceDeleteSet = new HashSet<BusinessService>();
        for(String publishedSvcName : allPublished.keySet()){
            final BusinessService publishedService = allPublished.get(publishedSvcName);
            //has this service already been published?
            //a BusinessService represents a wsdl:service, if the name is the same reuse the service key
            //a wsdl:service from a wsdl will always produce the same name, see WsdlToUDDIModelConverter
            if(toPublish.containsKey(publishedSvcName)){
                toPublish.get(publishedSvcName).setServiceKey(publishedService.getServiceKey());
            }else{
                serviceDeleteSet.add(publishedService);
            }
        }

        publishServicesToUDDI(uddiClient, wsdlBusinessServices, wsdlDependentTModels);

        //find out what tModels need to be deleted
        //do this after initial update, so we have valid references

        //build up the actual keys of every dependent tModel just published / looked up from UDDI
        final Set<String> requiredTModels = new HashSet<String>();
        for(TModel model: wsdlDependentTModels.values()){
            requiredTModels.add(model.getTModelKey());    
        }

        final Set<TModel> modelsToDelete = new HashSet<TModel>();
        for(final TModel tModel: uddiDependentTModels.values()){
            if(!requiredTModels.contains(tModel.getTModelKey())){
                modelsToDelete.add(tModel);
            }
        }

        //this is a best effort delete, we will not rollback work done above, as if we are here, it means that
        //we have successfully updated the Gateway's WSDL in UDDI
        if(!modelsToDelete.isEmpty()){
            logger.log(Level.INFO, "Attemping to delete tModels no longer referenced by Gateway's WSDL");
            boolean noErrors = true;
            for(final TModel tModel: modelsToDelete){
                try {
                    uddiClient.deleteTModel(tModel.getTModelKey());
                } catch (UDDIException e) {
                    logger.log(Level.WARNING, "Could not delete tModel with key: " + tModel.getTModelKey());
                    noErrors = false;
                }
            }
            if(!noErrors) logger.log(Level.INFO, "Successfully deleted all tModels no longer referenced by Gateway's WSDL");
        }

        if(!serviceDeleteSet.isEmpty()){
            logger.log(Level.INFO, "Attemping to delete BusinessServices no longer referenced by Gateway's WSDL");
            boolean noErrors = true;
            for(final BusinessService businessService: serviceDeleteSet){
                try {
                    uddiClient.deleteBusinessService(businessService.getServiceKey());
                } catch (UDDIException e) {
                    logger.log(Level.WARNING, "Could not delete BusinessServices with key: " + businessService.getServiceKey());
                    noErrors = false;
                }
            }
            if(!noErrors) logger.log(Level.INFO, "Successfully deleted all BusinessServices no longer referenced by Gateway's WSDL");
        }
    }


    private void publishServicesToUDDI(final UDDIClient uddiClient,
                                       final List<BusinessService> businessServices,
                                       final Map<String, TModel> dependentTModels) throws UDDIException {

        //remember udpates in case they need to be rolled back later
        final List<TModel> publishedTModels = new ArrayList<TModel>();

        //first publish TModels which represent wsdl:portType, as they have no keyedReference dependencies
        publishedTModels.addAll(publishDependentTModels(uddiClient, dependentTModels, WSDL_PORT_TYPE));

        final List<TModel> bindingTModels = new ArrayList<TModel>();
        for (final TModel tModel : dependentTModels.values()) {
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
                handleUDDIRollback(uddiClient, rollbackTModelsToDelete, publishedServices, e);
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
            handleUDDIRollback(uddiClient, publishedTModels, Collections.<BusinessService>emptyList(), e);
            throw e;
        }
        return Collections.unmodifiableList(publishedTModels);
    }

    private void handleUDDIRollback(final UDDIClient uddiClient,
                                    final List<TModel> rollbackTModelsToDelete,
                                    final List<BusinessService> publishedServices,
                                    final UDDIException exception) {
        try {
            //Roll back any tModels published first
            if (!rollbackTModelsToDelete.isEmpty()) {
                logger.log(Level.WARNING, "Attempting to rollback published tModels: " + exception.getMessage());
                boolean deletedTModel = false;
                for (TModel tModel : rollbackTModelsToDelete) {
                    uddiClient.deleteTModel(tModel.getTModelKey());
                    deletedTModel = true;
                }
                if (deletedTModel) logger.log(Level.WARNING, "Delete published tModels: " + exception.getMessage());
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
    }

}
