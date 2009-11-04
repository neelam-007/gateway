package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.common.uddi.guddiv3.BusinessService;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_BINDING;
import com.l7tech.util.Pair;
import com.l7tech.wsdl.Wsdl;

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
    private Set<BusinessService> serviceDeleteSet;
    private Set<BusinessService> newlyPublishedServices;

    private Wsdl wsdl;
    private final UDDIClient uddiClient;
    private final JaxWsUDDIClient jaxWsUDDIClient;
    private long serviceOid;
    private final UDDIClientConfig uddiClientConfig;

    /**
     * @param wsdl WSDL to publish to UDDI
     * @param uddiClient UDDIClient configured for the UDDI Registry to publish to
     * @param serviceOid services OID, which originates from the WSDL
     */
    public BusinessServicePublisher(final Wsdl wsdl,
                                    final UDDIClient uddiClient,
                                    final long serviceOid,
                                    final UDDIClientConfig uddiCfg) {
        if(wsdl == null) throw new NullPointerException("wsdl cannot be null");
        if(uddiClient == null) throw new NullPointerException("uddiClient cannot be null");
        if(uddiCfg == null) throw new NullPointerException("uddiCfg cannot be null");

        this.wsdl = wsdl;
        this.uddiClient = uddiClient;
        this.serviceOid = serviceOid;
        if(uddiClient instanceof JaxWsUDDIClient){
            jaxWsUDDIClient = (JaxWsUDDIClient) uddiClient;
        }else{
            jaxWsUDDIClient = new GenericUDDIClient(uddiCfg.getInquiryUrl(), uddiCfg.getPublishUrl(), uddiCfg.getSubscriptionUrl(),
            uddiCfg.getSecurityUrl(), uddiCfg.getLogin(), uddiCfg.getPassword(), UDDIClientFactory.getDefaultPolicyAttachmentVersion());
        }
        uddiClientConfig = uddiCfg;
    }


    /**
     * Publish the list of Business Services from the WSDL to the UDDI Registry
     *
     * Provides best effort commit / rollback behaviour
     *
     * @param protectedServiceExternalURL accessPoint value in UDDI
     * @param protectedServiceWsdlURL overviewDoc value in UDDI
     * @param businessKey String businessKey of the BusinssEntity which owns all of the published BusinessServices
     * @throws UDDIException any problems publishing UDDI info
     * @return List of UDDIBusinessService, one for each BusinessService published
     */
    public List<UDDIBusinessService> publishServicesToUDDIRegistry(final String protectedServiceExternalURL,
                                              final String protectedServiceWsdlURL,
                                              final String businessKey) throws UDDIException {
        //todo do we need a way to be able to tell if this wsdl for a published service was already published?
        final WsdlToUDDIModelConverter modelConverter;
        modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
        protectedServiceExternalURL, businessKey, serviceOid);
        try {
            modelConverter.convertWsdlToUDDIModel();
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException("Unable to convert WSDL from service (#" + serviceOid+") into UDDI object model.", e);
        }

        final List<BusinessService> wsdlBusinessServices = modelConverter.getBusinessServices();
        final Map<String, TModel> wsdlDependentTModels = modelConverter.getKeysToPublishedTModels();
        final Map<String, String> serviceToWsdlServiceName = modelConverter.getServiceNameToWsdlServiceNameMap();

        publishServicesToUDDI(wsdlBusinessServices, wsdlDependentTModels);

        List<UDDIBusinessService> wsdlServicesInUDDI = new ArrayList<UDDIBusinessService>();
        for(BusinessService bs: wsdlBusinessServices){
            UDDIBusinessService uddiBusService = new UDDIBusinessService(bs.getName().get(0).getValue(),
                    bs.getServiceKey(), serviceToWsdlServiceName.get(bs.getName().get(0).getValue()));
            wsdlServicesInUDDI.add(uddiBusService);
        }

        return wsdlServicesInUDDI;
    }

    public Set<BusinessService> getServiceDeleteSet() {
        return serviceDeleteSet;
    }

    public Set<BusinessService> getNewlyPublishedServices() {
        return newlyPublishedServices;
    }

    public Pair<Set<String>, Set<UDDIBusinessService>> updateServicesToUDDIRegistry(final String protectedServiceExternalURL,
                                             final String protectedServiceWsdlURL,
                                             final String businessKey,
                                             final Set<String> publishedServiceKeys) throws UDDIException {

        WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
                protectedServiceExternalURL, businessKey, serviceOid);
        try {
            modelConverter.convertWsdlToUDDIModel();
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException("Unable to convert WSDL from service (#" + serviceOid+") into UDDI object model.", e);
        }

        final List<BusinessService> wsdlBusinessServices = modelConverter.getBusinessServices();
        final Map<String, TModel> wsdlDependentTModels = modelConverter.getKeysToPublishedTModels();
        final Map<String, String> serviceToWsdlServiceName = modelConverter.getServiceNameToWsdlServiceNameMap();

        //Get the info on all published business services from UDDI
        UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient, uddiClientConfig);
        Pair<List<BusinessService>, Map<String, TModel>> modelFromUddi = serviceDownloader.getBusinessServiceModels(publishedServiceKeys);
        //TODO [Donal] some keys we requested may not have been returned. When this happens delete from our database
        final List<BusinessService> publishedServices = modelFromUddi.left;
        //check if this service has already been published
        if (publishedServices.isEmpty()) {
            //it's ok if it hasn't, just log the fact, may have been deleted via UDDI interface
            logger.log(Level.WARNING, "No exising published BusinessService found.");
        }

        //map to look up existing services
        Map<String, BusinessService> allPublished = new HashMap<String, BusinessService>();
        for (BusinessService bs : publishedServices) {
            allPublished.put(bs.getName().get(0).getValue(), bs);
        }

        Map<String, BusinessService> toPublish = new HashMap<String, BusinessService>();
        for (BusinessService bs : wsdlBusinessServices) {
            toPublish.put(bs.getName().get(0).getValue(), bs);
        }

        //find which services already exist, and reuse their serviceKeys if found
        //also need to know which BusinessServices to delete
        serviceDeleteSet = new HashSet<BusinessService>();
        for (String publishedSvcName : allPublished.keySet()) {
            final BusinessService publishedService = allPublished.get(publishedSvcName);
            //has this service already been published?
            //a BusinessService represents a wsdl:service, if the name is the same reuse the service key
            //a wsdl:service from a wsdl will always produce the same name, see WsdlToUDDIModelConverter
            if (toPublish.containsKey(publishedSvcName)) {
                toPublish.get(publishedSvcName).setServiceKey(publishedService.getServiceKey());
            } else {
                serviceDeleteSet.add(publishedService);
            }
        }

        publishServicesToUDDI(wsdlBusinessServices, wsdlDependentTModels);

        //find out what tModels need to be deleted
        //do this after initial update, so we have valid references

        //build up the actual keys of every dependent tModel just published / looked up from UDDI
        final Set<String> requiredTModels = new HashSet<String>();
        for (TModel model : wsdlDependentTModels.values()) {
            requiredTModels.add(model.getTModelKey());
        }

        final Set<TModel> modelsToDelete = new HashSet<TModel>();
        for (final TModel tModel : modelFromUddi.right.values()) {
            if (!requiredTModels.contains(tModel.getTModelKey())) {
                modelsToDelete.add(tModel);
            }
        }

        //this is a best effort delete, we will not rollback work done above, as if we are here, it means that
        //we have successfully updated the Gateway's WSDL in UDDI
        if (!modelsToDelete.isEmpty()) {
            logger.log(Level.INFO, "Attemping to delete tModels no longer referenced by Gateway's WSDL");
            boolean noErrors = true;
            for (final TModel tModel : modelsToDelete) {
                try {
                    uddiClient.deleteTModel(tModel.getTModelKey());
                } catch (UDDIException e) {
                    logger.log(Level.WARNING, "Could not delete tModel with key: " + tModel.getTModelKey());
                    noErrors = false;
                }
            }
            if (!noErrors)
                logger.log(Level.INFO, "Successfully deleted all tModels no longer referenced by Gateway's WSDL");
        }

        final Set<String> deleteSet = new HashSet<String>();
        for(BusinessService bs: serviceDeleteSet){
            deleteSet.add(bs.getServiceKey());
        }

        if (!deleteSet.isEmpty()) {
            logger.log(Level.INFO, "Attemping to delete BusinessServices no longer referenced by Gateway's WSDL");
            try {
                uddiClient.deleteBusinessServicesByKey(deleteSet);
                logger.log(Level.INFO, "Successfully deleted all BusinessServices no longer referenced by Gateway's WSDL");
            } catch (UDDIException e) {
                logger.log(Level.WARNING, "Problem deleting BusinessServices: " + e.getMessage());
            }
        }

        Set<UDDIBusinessService> newlyCreatedSet = new HashSet<UDDIBusinessService>();
        for(BusinessService bs: newlyPublishedServices){
            UDDIBusinessService uddiBs = new UDDIBusinessService(bs.getName().get(0).getValue(),
                    bs.getServiceKey(), serviceToWsdlServiceName.get(bs.getName().get(0).toString()));
            newlyCreatedSet.add(uddiBs);
        }


        return new Pair<Set<String>, Set<UDDIBusinessService>>(deleteSet, newlyCreatedSet);
    }

    private void publishServicesToUDDI(final List<BusinessService> businessServices,
                                       final Map<String, TModel> dependentTModels) throws UDDIException {

        //remember udpates in case they need to be rolled back later
        final List<TModel> publishedTModels = new ArrayList<TModel>();

        //first publish TModels which represent wsdl:portType, as they have no keyedReference dependencies
        publishedTModels.addAll(publishDependentTModels(dependentTModels, WSDL_PORT_TYPE));

        final List<TModel> bindingTModels = new ArrayList<TModel>();
        for (final TModel tModel : dependentTModels.values()) {
            if (UDDIUtilities.getTModelType(tModel, true) != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
            bindingTModels.add(tModel);
        }
        if (bindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");

        UDDIUtilities.updateBindingTModelReferences(bindingTModels, dependentTModels);
        //next publish TModels which represent wsdl:binding, as they are dependent on wsdl:portType tModels
        publishedTModels.addAll(publishDependentTModels(dependentTModels, WSDL_BINDING));

        UDDIUtilities.updateBusinessServiceReferences(businessServices, dependentTModels);

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
    private void publishBusinessServices(final UDDIClient uddiClient,
                                         final List<BusinessService> businessServices,
                                         final List<TModel> rollbackTModelsToDelete) throws UDDIException {

        newlyPublishedServices = new HashSet<BusinessService>();
        for (BusinessService businessService : businessServices) {
            try {
                final boolean published = jaxWsUDDIClient.publishBusinessService(businessService);
                if (published) newlyPublishedServices.add(businessService);
            } catch (UDDIException e) {
                logger.log(Level.WARNING, "Exception publishing BusinesService: " + e.getMessage());
                handleUDDIRollback(uddiClient, rollbackTModelsToDelete, newlyPublishedServices, e);
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
     * @param dependentTModels tModels to publish
     * @param tmodelType       TMODEL_TYPE the type of TModel we should publish.
     * @return List of TModels which were successfully published
     * @throws UDDIException any exception publishing the tmodels
     */
    List<TModel> publishDependentTModels(final Map<String, TModel> dependentTModels,
                                         final UDDIUtilities.TMODEL_TYPE tmodelType) throws UDDIException {
        final List<TModel> publishedTModels = new ArrayList<TModel>();
        try {
            for (Map.Entry<String, TModel> entrySet : dependentTModels.entrySet()) {
                final TModel tModel = entrySet.getValue();
                //only publish the type were currently interested in
                if (UDDIUtilities.getTModelType(tModel, true) != tmodelType) continue;

                final boolean published = jaxWsUDDIClient.publishTModel(tModel);
                if (published) publishedTModels.add(tModel);
            }
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Exception publishing tModels: " + e.getMessage());
            handleUDDIRollback(uddiClient, publishedTModels, Collections.<BusinessService>emptySet(), e);
            throw e;
        }
        return Collections.unmodifiableList(publishedTModels);
    }

    private void handleUDDIRollback(final UDDIClient uddiClient,
                                    final List<TModel> rollbackTModelsToDelete,
                                    final Set<BusinessService> publishedServices,
                                    final UDDIException exception) {
        //this is just a convenience method. Either rolls back tModels OR BusinessServices
        if(!rollbackTModelsToDelete.isEmpty() && !publishedServices.isEmpty())
            throw new IllegalArgumentException("Can only roll back either tModels or BusinessServices");
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
            } else if (!publishedServices.isEmpty()){
                logger.log(Level.WARNING, "Attempting to rollback published BusinessServices");
                Set<String> keysToDelete = new HashSet<String>();
                for(BusinessService bs: publishedServices){
                    keysToDelete.add(bs.getServiceKey());
                }
                uddiClient.deleteBusinessServicesByKey(keysToDelete);
                logger.log(Level.WARNING, "Deleted published BusinessServices");
            }

        } catch (UDDIException e1) {
            //Not going to throw e1, just log it, as the main error happend in the above publish try block
            //just log it
            logger.log(Level.WARNING, "Could not undo published BusinessServices following exception: " + e1.getMessage());
        }
    }

}
