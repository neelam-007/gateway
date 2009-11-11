package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_BINDING;
import com.l7tech.util.Pair;
import com.l7tech.util.ExceptionUtils;
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

    /**
     * Create a new BusinessServicePublisher.
     *
     * @param wsdl WSDL to publish to UDDI
     * @param serviceOid services OID, which originates from the WSDL
     * @param uddiCfg UDDIClientConfig for the UDDI Registry to publish to
     */
    public BusinessServicePublisher(final Wsdl wsdl,
                                    final long serviceOid,
                                    final UDDIClientConfig uddiCfg) {

        this( wsdl, serviceOid, buildUDDIClient(uddiCfg));
    }

    /**
     * Create a new BusinessServicePublisher.
     *
     * @param wsdl WSDL to publish to UDDI
     * @param serviceOid services OID, which originates from the WSDL
     * @param uddiClient UDDIClient for the UDDI Registry to publish to
     */
    protected BusinessServicePublisher(final Wsdl wsdl,
                                       final long serviceOid,
                                       final UDDIClient uddiClient) {
        if(wsdl == null) throw new NullPointerException("wsdl cannot be null");
        if(uddiClient == null) throw new NullPointerException("uddiClient cannot be null");

        this.wsdl = wsdl;
        this.uddiClient = uddiClient;
        this.serviceOid = serviceOid;
        if( uddiClient instanceof JaxWsUDDIClient ){
            jaxWsUDDIClient = (JaxWsUDDIClient) uddiClient;
        }else{
            throw new IllegalStateException( "JaxWsUDDIClient is required." );
        }
    }

    public String publishBindingTemplate(final String serviceKey,
                                         final String wsdlPortName,
                                         final String wsdlPortBinding,
                                         final String protectedServiceExternalURL,
                                         final String protectedServiceWsdlURL,
                                         final boolean removeOthers) throws UDDIException {

        Pair<BindingTemplate, List<TModel>> templateToModels = publishEndPointToExistingService(serviceKey,
                wsdlPortName, wsdlPortBinding, protectedServiceExternalURL, protectedServiceWsdlURL, removeOthers);

        return templateToModels.left.getBindingKey();
    }

    Pair<BindingTemplate, List<TModel>> publishEndPointToExistingService(final String serviceKey,
                                                                         final String wsdlPortName,
                                                                         final String wsdlPortBinding,
                                                                         final String protectedServiceExternalURL,
                                                                         final String protectedServiceWsdlURL,
                                                                         final boolean removeOthers)
            throws UDDIException {

        final Pair<BindingTemplate, Map<String, TModel>> bindingToModels;
        try {
            bindingToModels = UDDIUtilities.createBindingTemplateFromWsdl(wsdl, wsdlPortName, wsdlPortBinding, protectedServiceExternalURL, protectedServiceWsdlURL);
        } catch (UDDIUtilities.PortNotFoundException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        }

        //Get the service
        Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);
        final List<BusinessService> foundServices = uddiClient.getBusinessServices(serviceKeys);
        if (foundServices.isEmpty())
            throw new UDDIException("Could not find BusinessService with serviceKey: " + serviceKey);
        final BusinessService businessService = foundServices.get(0);

        final Set<String> allOtherBindingKeys;
        if (businessService.getBindingTemplates() != null) {
            List<BindingTemplate> bindingTemplates = businessService.getBindingTemplates().getBindingTemplate();
            allOtherBindingKeys = new HashSet<String>();
            for (BindingTemplate template : bindingTemplates) {
                allOtherBindingKeys.add(template.getBindingKey());
            }
        } else {
            allOtherBindingKeys = Collections.emptySet();
        }

//        Publish the tModels first
        final Map<String, TModel> tModelsToPublish = bindingToModels.right;
        try {
            for (Map.Entry<String, TModel> entry : tModelsToPublish.entrySet()) {
                jaxWsUDDIClient.publishTModel(entry.getValue());
            }

//            publishDependentTModels(bindingToModels.right, WSDL_PORT_TYPE);
            final List<TModel> allBindingTModels = new ArrayList<TModel>();
            for (final TModel tModel : bindingToModels.right.values()) {
                if (UDDIUtilities.getTModelType(tModel, true) != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
                allBindingTModels.add(tModel);
            }
            if (allBindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");
            UDDIUtilities.updateBindingTModelReferences(allBindingTModels, bindingToModels.right);

            bindingToModels.left.setServiceKey(businessService.getServiceKey());

            UDDIUtilities.updateBindingTemplateTModelReferences(bindingToModels.left, bindingToModels.right);
            jaxWsUDDIClient.publishBindingTemplate(bindingToModels.left);

            //remove other bindings?
            if (removeOthers) {
                logger.log(Level.INFO, "Deleting other bindingTemplates");
                uddiClient.deleteBindingTemplateFromSingleService(allOtherBindingKeys);
            }

            List<TModel> models = new ArrayList<TModel>();
            models.addAll(bindingToModels.right.values());
            return new Pair<BindingTemplate, List<TModel>>(bindingToModels.left, models);

        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Problem publishing to UDDI: " + ExceptionUtils.getMessage(e));
            for (Map.Entry<String, TModel> entry : tModelsToPublish.entrySet()) {
                final String tModelKey = entry.getValue().getTModelKey();
                if (tModelKey != null && !tModelKey.trim().isEmpty()) {
                    handleTModelRollback(tModelKey);
                }
            }
            final String bindingKey = bindingToModels.left.getBindingKey();
            if (bindingKey != null && !bindingKey.trim().isEmpty()) {
                handleBindingTemplateRollback(bindingKey);
            }
            throw e;
        }
    }

    /**
     * Try to delete a tModel published to UDDI. Will not throw a UDDIException
     * @param tModelKey String tModelKey to delete
     */
    private void handleTModelRollback(final String tModelKey){
        try {
            logger.log(Level.INFO, "Attemping to delete tModel published to UDDI with tModelKey: " + tModelKey);
            uddiClient.deleteTModel(tModelKey);
            logger.log(Level.INFO, "Succesfully deleted tModel with tModelKey: " + tModelKey);
        } catch (UDDIException e1) {
            logger.log(Level.WARNING, "Could not rollback published tModel with key " + tModelKey + "to UDDI: " + ExceptionUtils.getMessage(e1));
        }
    }

    private void handleBindingTemplateRollback(final String bindingKey){
        try {
            logger.log(Level.INFO, "Attemping to delete bindingTemplate published to UDDI with bindingKey: " + bindingKey);
            uddiClient.deleteBindingTemplate(bindingKey);
            logger.log(Level.INFO, "Succesfully deleted tModel with bindingKey: " + bindingKey);
        } catch (UDDIException e1) {
            logger.log(Level.WARNING, "Could not rollback published bindingTemplate with key " + bindingKey + "to UDDI: " + ExceptionUtils.getMessage(e1));
        }
    }

    /**
     * Publish the list of Business Services from the WSDL to the UDDI Registry
     * <p/>
     * Provides best effort commit / rollback behaviour
     *
     * @param protectedServiceExternalURL accessPoint value in UDDI
     * @param protectedServiceWsdlURL     overviewDoc value in UDDI
     * @param businessKey                 String businessKey of the BusinssEntity which owns all of the published BusinessServices
     * @return List of UDDIBusinessService, one for each BusinessService published
     * @throws UDDIException any problems publishing UDDI info
     */
    public List<UDDIBusinessService> publishServicesToUDDIRegistry(final String protectedServiceExternalURL,
                                                                   final String protectedServiceWsdlURL,
                                                                   final String businessKey) throws UDDIException {
        final WsdlToUDDIModelConverter modelConverter;
        modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
                protectedServiceExternalURL, businessKey, serviceOid);
        try {
            modelConverter.convertWsdlToUDDIModel();
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException("Unable to convert WSDL from service (#" + serviceOid + ") into UDDI object model.", e);
        }

        final List<Pair<BusinessService, Map<String, TModel>>> serviceNameToDependentTModels = modelConverter.getServicesAndDependentTModels();
        final Map<String, String> serviceToWsdlServiceName = modelConverter.getServiceNameToWsdlServiceNameMap();

        publishServicesToUDDI(serviceNameToDependentTModels);

        List<UDDIBusinessService> wsdlServicesInUDDI = new ArrayList<UDDIBusinessService>();
        for (Pair<BusinessService, Map<String, TModel>> businessToModels : serviceNameToDependentTModels) {
            BusinessService bs = businessToModels.left;
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
            throw new UDDIException("Unable to convert WSDL from service (#" + serviceOid + ") into UDDI object model.", e);
        }

        final List<Pair<BusinessService, Map<String, TModel>>> wsdlServiceNameToDependentTModels = modelConverter.getServicesAndDependentTModels();
        final Map<String, String> serviceToWsdlServiceName = modelConverter.getServiceNameToWsdlServiceNameMap();

        //Get the info on all published business services from UDDI
        UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient);

        //TODO [Donal] some keys we requested may not have been returned. When this happens delete from our database
        //Now we know every single tModel reference each existing Business Service contains
        final List<Pair<BusinessService, Map<String, TModel>>>
                uddiServicesToDependentModels = serviceDownloader.getBusinessServiceModels(publishedServiceKeys);

        final List<BusinessService> uddiPreviouslyPublishedServices = new ArrayList<BusinessService>();
        for(Pair<BusinessService, Map<String, TModel>> aServiceToItsModels: uddiServicesToDependentModels){
            uddiPreviouslyPublishedServices.add(aServiceToItsModels.left);
        }

        //check if this service has already been published
        if (uddiPreviouslyPublishedServices.isEmpty()) {
            //it's ok if it hasn't, just log the fact, may have been deleted via UDDI interface
            logger.log(Level.WARNING, "No exising published BusinessService found.");
        }

        //map to look up existing services
        final Map<String, BusinessService> previouslyPublishedServiceMap = new HashMap<String, BusinessService>();
        for (BusinessService bs : uddiPreviouslyPublishedServices) {
            previouslyPublishedServiceMap.put(bs.getName().get(0).getValue(), bs);
        }

        final Map<String, BusinessService> wsdlServicesToPublish = new HashMap<String, BusinessService>();
        for (Pair<BusinessService, Map<String, TModel>> serviceToModels : wsdlServiceNameToDependentTModels) {
            BusinessService bs = serviceToModels.left;
            wsdlServicesToPublish.put(bs.getName().get(0).getValue(), bs);
        }

        //find which services already exist, and reuse their serviceKeys if found
        //also need to know which BusinessServices to delete
        serviceDeleteSet = new HashSet<BusinessService>();
        for (String publishedSvcName : previouslyPublishedServiceMap.keySet()) {
            final BusinessService uddiPublishedService = previouslyPublishedServiceMap.get(publishedSvcName);
            //has this service already been published?
            //a BusinessService represents a wsdl:service, if the name is the same reuse the service key
            //a wsdl:service from a wsdl will always produce the same name, see WsdlToUDDIModelConverter
            if (wsdlServicesToPublish.containsKey(publishedSvcName)) {
                wsdlServicesToPublish.get(publishedSvcName).setServiceKey(uddiPublishedService.getServiceKey());
            } else {
                serviceDeleteSet.add(uddiPublishedService);
            }
        }

        publishServicesToUDDI(wsdlServiceNameToDependentTModels);

        //find out what tModels need to be deleted
        //do this after initial update, so we have valid references

        final Set<String> deleteSet = new HashSet<String>();
        for (BusinessService bs : serviceDeleteSet) {
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

        //Now find what tModels are no longer referenced by any remaining service which already existed in UDDI
        for(Pair<BusinessService, Map<String, TModel>> aServiceToItsModel: uddiServicesToDependentModels){
            BusinessService bs = aServiceToItsModel.left;
            //if a service was deleted, so were its tmodels
            if(deleteSet.contains(bs.getServiceKey())) continue;

            //we will delete every tModel the service used to reference, as new ones have been created for it
            //if we get here it's because the service is still in UDDI
            Set<String> oldTModels = new HashSet<String>();
            for(TModel tModel: aServiceToItsModel.right.values()){
                oldTModels.add(tModel.getTModelKey());
            }
            try {
                uddiClient.deleteTModel(oldTModels);
            } catch (UDDIException e) {
                logger.log(Level.WARNING, "Could not delete old tModels from UDDI: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        Set<UDDIBusinessService> newlyCreatedSet = new HashSet<UDDIBusinessService>();
        for (BusinessService bs : newlyPublishedServices) {
            final String wsdlServiceName = bs.getName().get(0).getValue();
            UDDIBusinessService uddiBs = new UDDIBusinessService(bs.getName().get(0).getValue(),
                    bs.getServiceKey(), serviceToWsdlServiceName.get(wsdlServiceName));
            newlyCreatedSet.add(uddiBs);
        }

        //were any service keys asked for not found? - could happen if something else deletes from UDDI instead of us
        final Set<String> keysDeletedViaUDDI = new HashSet<String>();
        final Set<String> receivedFromUDDISet = new HashSet<String>();
        for(Pair<BusinessService, Map<String, TModel>> aServiceAndModel: uddiServicesToDependentModels){
            receivedFromUDDISet.add(aServiceAndModel.left.getServiceKey());
        }

        //these are the keys we asked for
        for(String s: publishedServiceKeys){
            //and these are they keys we got - uddiServicesToDependentModels
            if(!receivedFromUDDISet.contains(s)) keysDeletedViaUDDI.add(s);
        }
        deleteSet.addAll(keysDeletedViaUDDI);

        return new Pair<Set<String>, Set<UDDIBusinessService>>(deleteSet, newlyCreatedSet);
    }

    private void publishServicesToUDDI(final List<Pair<BusinessService, Map<String, TModel>>> serviceNameToDependentTModels) throws UDDIException {

        for(Pair<BusinessService, Map<String, TModel>> serviceAndModels: serviceNameToDependentTModels){
            Map<String, TModel> dependentTModels = serviceAndModels.right;
            //first publish TModels which represent wsdl:portType, as they have no keyedReference dependencies
            publishDependentTModels(dependentTModels, WSDL_PORT_TYPE);
            final List<TModel> bindingTModels = new ArrayList<TModel>();
            for (final TModel tModel : dependentTModels.values()) {
                if (UDDIUtilities.getTModelType(tModel, true) != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
                bindingTModels.add(tModel);
            }
            if (bindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");

            UDDIUtilities.updateBindingTModelReferences(bindingTModels, dependentTModels);
            //next publish TModels which represent wsdl:binding, as they are dependent on wsdl:portType tModels
            publishDependentTModels(dependentTModels, WSDL_BINDING);
            UDDIUtilities.updateBusinessServiceReferences(serviceAndModels.left, dependentTModels);

            publishBusinessServices(uddiClient, serviceAndModels.left, dependentTModels.values());
        }
    }

    private void publishBusinessServices(final UDDIClient uddiClient,
                                         final BusinessService businessService,
                                         final Collection<TModel> rollbackTModelsToDelete) throws UDDIException {
        newlyPublishedServices = new HashSet<BusinessService>();
        try {
            final boolean published = jaxWsUDDIClient.publishBusinessService(businessService);
            if (published) newlyPublishedServices.add(businessService);
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Exception publishing BusinesService: " + e.getMessage());
            handleUDDIRollback(uddiClient, rollbackTModelsToDelete, newlyPublishedServices, e);
            throw e;
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
                                    final Collection<TModel> rollbackTModelsToDelete,
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

    private static UDDIClient buildUDDIClient( final UDDIClientConfig uddiCfg ) {
        if(uddiCfg == null) throw new NullPointerException("uddiCfg cannot be null");

        UDDIClient uddiClient = UDDIClientFactory.getInstance().newUDDIClient( uddiCfg );
        if(!(uddiClient instanceof JaxWsUDDIClient)){
            uddiClient = new GenericUDDIClient(uddiCfg.getInquiryUrl(), uddiCfg.getPublishUrl(), uddiCfg.getSubscriptionUrl(),
                    uddiCfg.getSecurityUrl(), uddiCfg.getLogin(), uddiCfg.getPassword(),
                    UDDIClientFactory.getDefaultPolicyAttachmentVersion(), uddiCfg.getTlsConfig());
        }

        return uddiClient;
    }    
}
