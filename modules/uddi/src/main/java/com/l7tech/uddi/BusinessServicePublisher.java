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
        } catch (WsdlToUDDIModelConverter.NonSoapBindingFoundException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        }

        //Get the service
        final Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);
        final List<BusinessService> foundServices = uddiClient.getBusinessServices(serviceKeys, false);
        if (foundServices.isEmpty())
            throw new UDDIException("Could not find BusinessService with serviceKey: " + serviceKey);
        final BusinessService businessService = foundServices.get(0);

        final Set<String> allOtherBindingKeys;
        if (businessService.getBindingTemplates() != null) {
            final List<BindingTemplate> bindingTemplates = businessService.getBindingTemplates().getBindingTemplate();
            allOtherBindingKeys = new HashSet<String>();
            for (BindingTemplate template : bindingTemplates) {
                allOtherBindingKeys.add(template.getBindingKey());
            }
        } else {
            allOtherBindingKeys = Collections.emptySet();
        }

        final Map<String, TModel> tModelsToPublish = bindingToModels.right;
        try {
            //publish the wsdl:portType tmodels first so we can update the binding tmodel references to them
            for (Map.Entry<String, TModel> entry : tModelsToPublish.entrySet()) {
                if (UDDIUtilities.getTModelType(entry.getValue(), true) != UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE) continue;
                jaxWsUDDIClient.publishTModel(entry.getValue());
            }

            final List<TModel> allBindingTModels = new ArrayList<TModel>();
            for (final TModel tModel : bindingToModels.right.values()) {
                if (UDDIUtilities.getTModelType(tModel, true) != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
                allBindingTModels.add(tModel);
            }
            if (allBindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");
            UDDIUtilities.updateBindingTModelReferences(allBindingTModels, bindingToModels.right);

            //now publish the binding tmodels after they have been updated
            for(TModel bindingModel: allBindingTModels){
                jaxWsUDDIClient.publishTModel(bindingModel);
            }

            bindingToModels.left.setServiceKey(businessService.getServiceKey());

            UDDIUtilities.updateBindingTemplateTModelReferences(bindingToModels.left, bindingToModels.right);
            jaxWsUDDIClient.publishBindingTemplate(bindingToModels.left);

            //remove other bindings?
            if (removeOthers) {
                logger.log(Level.FINE, "Deleting other bindingTemplates");
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
     * Publish the list of Business Services from the WSDL to the UDDI Registry
     * <p/>
     * Provides best effort commit / rollback behaviour
     *
     * @param protectedServiceExternalURL accessPoint value in UDDI
     * @param protectedServiceWsdlURL     overviewDoc value in UDDI
     * @param businessKey                 String businessKey of the BusinssEntity which owns all of the published BusinessServices
     * @return List of UDDIBusinessService, one for each BusinessService published
     * @throws UDDIException any problems publishing UDDI info
     */                              //todo delete - not used anywhere
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

    /**
     * Should only be called once for a WSDL. Will do the house cleaning required to publish a proxied wsdl:service
     * to UDDI, which represents an 'overwritten' service
     *
     * @param serviceKey
     * @param wsdlPortName
     * @param protectedServiceExternalURL
     * @param protectedServiceWsdlURL
     * @param businessKey
     */
    public Pair<Set<String>, Set<UDDIBusinessService>> overwriteServiceInUDDI(final String serviceKey,
                                       final String wsdlPortName,
                                       final String protectedServiceExternalURL,
                                       final String protectedServiceWsdlURL,
                                       final String businessKey) throws UDDIException {
        
        final UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient);
        final Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);
        final List<Pair<BusinessService, Map<String, TModel>>> uddiServicesToDependentTModelsPairs = serviceDownloader.getBusinessServiceModels(serviceKeys);

        if(uddiServicesToDependentTModelsPairs.isEmpty()) throw new UDDIException("No BusinessService found for serviceKey: " + serviceKey);
        final Pair<BusinessService, Map<String, TModel>> serviceToDependentTModels = uddiServicesToDependentTModelsPairs.iterator().next();

        BusinessService overwriteService = serviceToDependentTModels.left;//not final as we will update it's reference
        final BindingTemplates templates = overwriteService.getBindingTemplates();
        if(templates == null){
            //we can work with this, we don't care as were going to be uploading our own bindingTemplates
            //this happens if all bindings are removed between when we got our record of the service and us
            //initiating this overwrite action
            BindingTemplates newTemplates = new BindingTemplates();
            overwriteService.setBindingTemplates(newTemplates);
        }

        final Map<String, TModel> allDependentTModels = serviceToDependentTModels.right;

        final Pair<Set<String>, Set<String>> deleteAndKeep = getBindingsToDeleteAndKeep(templates.getBindingTemplate(), allDependentTModels);
        final Set<String> deleteSet = deleteAndKeep.left;

        for(String bindingKey: deleteSet){
            uddiClient.deleteBindingTemplate(bindingKey);    
        }
        //Redownload the service
        overwriteService = jaxWsUDDIClient.getBusinessService(overwriteService.getServiceKey());

        //add all kept bind

        //extract the service's local name from it's keyed references - it must exist to be valid
        if(overwriteService.getCategoryBag() == null){
            throw new UDDIException("Invalid BusinessService. It does not contain a CategoryBag. serviceKey: " + overwriteService.getServiceKey());
        }

        final Pair<String, String> serviceNameAndNameSpace = getServiceNameAndNameSpace(overwriteService);

        //we must find both the service name and the correct namespace in the Gateway's WSDL, otherwise we are mis configured

        //Now work with the WSDL, the information we will publish comes from the Gateway's WSDL
        final WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
                protectedServiceExternalURL, businessKey, serviceOid);
        try {
            modelConverter.convertWsdlToUDDIModel();
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException("Unable to convert WSDL from service (#" + serviceOid + ") into UDDI object model.", e);
        }

        final List<Pair<BusinessService, Map<String, TModel>>> wsdlServiceNameToDependentTModels = modelConverter.getServicesAndDependentTModels();

        final List<Pair<BusinessService, Map<String, TModel>>> pairToUseList =
                extractSingleService(serviceNameAndNameSpace.left, serviceNameAndNameSpace.right, wsdlServiceNameToDependentTModels);

        //now we are ready to work with this service. We will publish all soap bindings found and leave any others on
        //the original service in UDDI intact - this will likely be a configurable option in the future
        final Map<String, String> serviceNameToWsdlNameMap = new HashMap<String, String>();
        //overwrite the service's name to be the Layer7 name, we own this service now
        Name name = new Name();
        Pair<BusinessService, Map<String, TModel>> foundPair = pairToUseList.get(0);
        name.setValue(foundPair.left.getName().get(0).getValue());
        overwriteService.getName().clear();
        overwriteService.getName().add(name);
        jaxWsUDDIClient.updateBusinessService(overwriteService);
        serviceNameToWsdlNameMap.put(overwriteService.getName().get(0).getValue(), serviceNameAndNameSpace.left);
        //true here means that we will keep any existing bindings found on the BusinessService
        return publishToUDDI(serviceKeys, pairToUseList, serviceNameToWsdlNameMap, true);
    }

    /**
     * Extract out the real wsdl:service name and namespace if applicable from the supplied BusinessService.
     * @param overwriteService BusinessService to get the information from. Cannot be null
     * @return Pair of strings. The left is the service name, the right is the namespace. The namespace can be null.
     * @throws UDDIException If the service name is not found, or the namepace is empty. No UDDI interactions are done here.
     */
    private Pair<String, String> getServiceNameAndNameSpace(final BusinessService overwriteService) throws UDDIException {
        if(overwriteService.getCategoryBag() == null){
            throw new UDDIException("Invalid BusinessService. It does not contain a CategoryBag. serviceKey: " + overwriteService.getServiceKey());
        }

        String foundServiceName = null;
        String foundNameSpace = null;
        for(KeyedReference kr: overwriteService.getCategoryBag().getKeyedReference()){
            if(kr.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_LOCALNAME)){
                foundServiceName = kr.getKeyValue();
            } else if(kr.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE)){
                foundNameSpace = kr.getKeyValue();
            }
        }

        if(foundServiceName == null || foundServiceName.trim().isEmpty()){
            throw new UDDIException("BusinessService does not contain a local name KeyedReference with the name of the service. serviceKey: " + overwriteService.getServiceKey());
        }
        if(foundNameSpace != null){
            //ok if namespace is null, but if it's supplied it can't be an empty string
            if(foundNameSpace.trim().isEmpty()){
                throw new UDDIException("BusinessService does not contain a non empty namespace KeyedReference. serviceKey: " + overwriteService.getServiceKey());
            }
        }
        return new Pair<String, String>(foundServiceName, foundNameSpace);
    }

    /**
     *
     * @param foundServiceName String service name. Required.
     * @param foundNameSpace String Can be null. If not null it cannot be the empty string after a trim
     * @param wsdlServiceNameToDependentTModels
     * @return
     * @throws UDDIException
     */
    private List<Pair<BusinessService, Map<String, TModel>>> extractSingleService(
            final String foundServiceName,
            final String foundNameSpace,
            final List<Pair<BusinessService, Map<String, TModel>>> wsdlServiceNameToDependentTModels) throws UDDIException {
        if(foundNameSpace != null && foundNameSpace.trim().isEmpty())
            throw new IllegalArgumentException("If foundNameSpace is supplied, it cannot be the empty string");

        Pair<BusinessService, Map<String, TModel>> foundPair = null;
        for (Pair<BusinessService, Map<String, TModel>> pair : wsdlServiceNameToDependentTModels) {
            final BusinessService checkIfItsTheOne = pair.left;

            String uddiServiceLocalName = null;
            String uddiNameSpace = null;
            for (KeyedReference kr : checkIfItsTheOne.getCategoryBag().getKeyedReference()) {
                if (kr.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_LOCALNAME)) {
                    uddiServiceLocalName = kr.getKeyValue();
                } else if (kr.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE)) {
                    uddiNameSpace = kr.getKeyValue();
                }
            }

            final boolean localNameOk = uddiServiceLocalName != null && !uddiServiceLocalName.trim().isEmpty();
            final boolean nameSpaceOk = foundNameSpace != null && (uddiNameSpace != null && !uddiNameSpace.trim().isEmpty());

            if (!localNameOk) {
                continue;//this service is not valid, cannot use
            }

            if (uddiServiceLocalName.equalsIgnoreCase(foundServiceName) &&
                    (!nameSpaceOk || uddiNameSpace.equalsIgnoreCase(foundNameSpace))) {
                foundPair = pair;
                break;
            }
        }

        //if foundPair is null, then the UDDI does not model the WSDL, we cannot work with this
        if (foundPair == null)
            throw new UDDIException("The wsdl:service '" + foundServiceName + "' is not contained in the Gateway's WSDL. Cannot overwrite original service");

        return Arrays.asList(foundPair);
    }

    /**
     * Get the set of services which should be deleted and the set of services which were published as a result
     * of publishing the gateway wsdl
     * <p/>
     * This handles both an initial publish and a subsequent publish. For updates, existing serviceKeys are reused
     * where they represent the same wsdl:service from the wsdl
     *
     * If isOverWriteUpdate is true, then publishedServiceKeys must contain at least 1 key.
     *
     * @param protectedServiceExternalURL String url which will be come the value of 'endPoint' in the accessPoint element
     *                                    of a bindingTemplate
     * @param protectedServiceWsdlURL     String url the overview url where the wsdl can be downloaded from the gateway from
     *                                    which will be included in every tModel published
     * @param businessKey                 String business key of the BusinessEntity in UDDi which owns all services published
     * @param publishedServiceKeys        Set String of known service keys. Can be empty, but not null
     * @param isOverwriteUpdate           boolean if true, then we aer updating an overwritten service. In this case we need
     *                                    to make sure we only publish a single service from the WSDL. The first serviceKey found in publishedServiceKeys is then
     *                                    assumed to be the serviceKey of the overwritten service.
     * @return Pair Left side: Set of serviceKeys which should be deleted. Right side: set of services published
     *         as a result of this publish / update operation
     * @throws UDDIException any problems searching / updating UDDI or with data model
     */
    public Pair<Set<String>, Set<UDDIBusinessService>> publishServicesToUDDIRegistry(
            final String protectedServiceExternalURL,
            final String protectedServiceWsdlURL,
            final String businessKey,
            final Set<String> publishedServiceKeys,
            final boolean isOverwriteUpdate) throws UDDIException {

        final WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, protectedServiceWsdlURL,
                protectedServiceExternalURL, businessKey, serviceOid);
        try {
            modelConverter.convertWsdlToUDDIModel();
        } catch (WsdlToUDDIModelConverter.MissingWsdlReferenceException e) {
            throw new UDDIException("Unable to convert WSDL from service (#" + serviceOid + ") into UDDI object model.", e);
        }

        //not final as we may modify the reference
        List<Pair<BusinessService, Map<String, TModel>>> wsdlServiceNameToDependentTModels = modelConverter.getServicesAndDependentTModels();

        if (isOverwriteUpdate) {
            final BusinessService overwriteService = jaxWsUDDIClient.getBusinessService(publishedServiceKeys.iterator().next());
            final Pair<String, String> serviceNameAndNameSpace = getServiceNameAndNameSpace(overwriteService);
            wsdlServiceNameToDependentTModels =
                    extractSingleService(serviceNameAndNameSpace.left, serviceNameAndNameSpace.right, wsdlServiceNameToDependentTModels);
        }

        final Map<String, String> serviceToWsdlServiceName = modelConverter.getServiceNameToWsdlServiceNameMap();

        return publishToUDDI(publishedServiceKeys, wsdlServiceNameToDependentTModels, serviceToWsdlServiceName, isOverwriteUpdate);
    }

    //- PROTECTED

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
                 //todo dont need to publish if it doesnt have a null / empty key
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

    //- PRIVATE

    /**
     *
     * @param publishedServiceKeys
     * @param wsdlServiceNameToDependentTModels
     * @param serviceToWsdlServiceName
     * @param isOverwriteUpdate if true, if a previously published BusinessService is found, along with
     * it's serviceKey, any bindings it has will also be reused
     * @return
     * @throws UDDIException
     */
    private Pair<Set<String>, Set<UDDIBusinessService>> publishToUDDI(
            final Set<String> publishedServiceKeys,
            final List<Pair<BusinessService, Map<String, TModel>>> wsdlServiceNameToDependentTModels,
            final Map<String, String> serviceToWsdlServiceName,
            final boolean isOverwriteUpdate) throws UDDIException {
        final UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient);
        //Get the info on all published business services from UDDI
        //Now we know every single tModel reference each existing Business Service contains
        final List<Pair<BusinessService, Map<String, TModel>>>
                uddiServicesToDependentModels = serviceDownloader.getBusinessServiceModels(publishedServiceKeys);

        final List<BusinessService> uddiPreviouslyPublishedServices = new ArrayList<BusinessService>();
        for(Pair<BusinessService, Map<String, TModel>> aServiceToItsModels: uddiServicesToDependentModels){
            uddiPreviouslyPublishedServices.add(aServiceToItsModels.left);
        }

        //map to look up existing services
        final Map<String, BusinessService> uddiPreviouslyPublishedServiceMap = new HashMap<String, BusinessService>();
        for (BusinessService bs : uddiPreviouslyPublishedServices) {
            uddiPreviouslyPublishedServiceMap.put(bs.getName().get(0).getValue(), bs);
        }

        //create a map to look up the serviecs from the wsdl to publish
        final Map<String, BusinessService> wsdlServicesToPublish = new HashMap<String, BusinessService>();
        for (Pair<BusinessService, Map<String, TModel>> serviceToModels : wsdlServiceNameToDependentTModels) {
            BusinessService bs = serviceToModels.left;
            wsdlServicesToPublish.put(bs.getName().get(0).getValue(), bs);
        }

        //find which services already exist, and reuse their serviceKeys if found
        //also need to know which BusinessServices to delete
        serviceDeleteSet = new HashSet<BusinessService>();

        //for overwrite - we keep non soap/http bindings. for these bindings, do not delete any tmodels they reference
        final Map<String, TModel> uddiNonSoapBindingTmodels = new HashMap<String, TModel>();

        for (String publishedSvcName : uddiPreviouslyPublishedServiceMap.keySet()) {
            final BusinessService uddiPublishedService = uddiPreviouslyPublishedServiceMap.get(publishedSvcName);
            //has this service already been published?
            //a BusinessService represents a wsdl:service, if the name is the same reuse the service key
            //a wsdl:service from a wsdl will always produce the same name, see WsdlToUDDIModelConverter
            if (wsdlServicesToPublish.containsKey(publishedSvcName)) {
                final BusinessService businessService = wsdlServicesToPublish.get(publishedSvcName);
                businessService.setServiceKey(uddiPublishedService.getServiceKey());
                if (isOverwriteUpdate) {
                    if(businessService.getBindingTemplates() == null) businessService.setBindingTemplates(new BindingTemplates());
                    if(uddiPublishedService.getBindingTemplates() == null) uddiPublishedService.setBindingTemplates(new BindingTemplates());

                    final Pair<Set<String>, Set<String>> deleteAndKeep =
                            getBindingsToDeleteAndKeep(uddiPublishedService.getBindingTemplates().getBindingTemplate(),
                                    uddiServicesToDependentModels.iterator().next().right);

                    //all tmodels from uddi for this service
                    final Map<String, TModel> keepTModelMap = uddiServicesToDependentModels.iterator().next().right;

                    //only keep the bindings where are not soap / http
                    for(BindingTemplate bt: uddiPublishedService.getBindingTemplates().getBindingTemplate()){
                        if(!deleteAndKeep.right.contains(bt.getBindingKey())) continue;
                        businessService.getBindingTemplates().getBindingTemplate().add(bt);
                        for(TModelInstanceInfo infos: bt.getTModelInstanceDetails().getTModelInstanceInfo()){
                            uddiNonSoapBindingTmodels.put(infos.getTModelKey(), keepTModelMap.get(infos.getTModelKey()));
                        }
                    }
                }
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
            logger.log(Level.FINE, "Attemping to delete BusinessServices no longer referenced by Gateway's WSDL");
            try {
                uddiClient.deleteBusinessServicesByKey(deleteSet);
                logger.log(Level.FINE, "Successfully deleted all BusinessServices no longer referenced by Gateway's WSDL");
            } catch (UDDIException e) {
                logger.log(Level.WARNING, "Problem deleting BusinessServices: " + e.getMessage());
            }
        }

        //Now find what tModels are no longer referenced by any remaining service which already existed in UDDI
        for(Pair<BusinessService, Map<String, TModel>> aServiceToItsModel: uddiServicesToDependentModels){
            final BusinessService bs = aServiceToItsModel.left;
            //if a service was deleted, so were its tmodels
            if(deleteSet.contains(bs.getServiceKey())) continue;

            //we will delete every tModel the service used to reference, as new ones have been created for it
            //if we get here it's because the service is still in UDDI
            final Set<String> oldTModels = new HashSet<String>();
            for(TModel tModel: aServiceToItsModel.right.values()){
                //don't delete any tmodels we left after taking over a service
                if(uddiNonSoapBindingTmodels.containsKey(tModel.getTModelKey())) continue;
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

    /**
     * Get all the bindingTemplates which should be removed. Any template which implements a wsdl:binding
     * with soap / http should be returned
     * @param templates List of BindingTemplates from which to determine which should be removed
     * @param allDependentTModels Every single tmodel keyed by it's tModelKey which the owning BusinessService is
     * known to depend on. It is only needed for it's binding tModels, if it only contained that, it would suffice
     * @return Set String of bindingKeys to delete. Can be empty, never null
     */
    private Pair<Set<String>, Set<String>> getBindingsToDeleteAndKeep(List<BindingTemplate> templates, Map<String, TModel> allDependentTModels) {
        final Set<String> bindingKeysToDelete = new HashSet<String>();
        for(final BindingTemplate bindingTemplate: templates){
            //remove all soap/http bindings
            final TModelInstanceDetails modelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
            if(modelInstanceDetails == null){
                bindingKeysToDelete.add(bindingTemplate.getBindingKey());
                continue;
            }

            for(final TModelInstanceInfo tii: modelInstanceDetails.getTModelInstanceInfo()){
                final String tModelRefKey = tii.getTModelKey();
                final TModel tModel = allDependentTModels.get(tModelRefKey);
                if(tModel == null) throw new IllegalStateException("Should have all dependent tModels. Missing tModel: " + tModelRefKey);

                final UDDIUtilities.TMODEL_TYPE type = UDDIUtilities.getTModelType(tModel, false);
                switch (type) {
                    case WSDL_BINDING:
                        break;
                    default:
                        //don't know what this tModel is, it's not what we require so move onto next tModelInstanceInfo
                        continue;
                }

                final CategoryBag categoryBag = tModel.getCategoryBag();
                if(categoryBag == null){
                    //this is a messed up tModel which from the above switch is a wsdl:binding
                    //which means we can't tell what the implementing protocol / transport is - leave it alone
                    //nothing to do, this is for the comment only
                    break;
                }

                boolean protocolIsSoap = false;
                boolean transportIsHttp = false;
                for(KeyedReference kf: categoryBag.getKeyedReference()){
                    if(kf.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_PROTOCOL)) {
                        if (kf.getKeyValue().equalsIgnoreCase(WsdlToUDDIModelConverter.SOAP_PROTOCOL_V3) ||
                                kf.getKeyValue().equalsIgnoreCase(WsdlToUDDIModelConverter.SOAP_PROTOCOL_V2) ||
                                        kf.getKeyValue().equalsIgnoreCase(WsdlToUDDIModelConverter.SOAP_1_2_V3)) {
                            protocolIsSoap = true;
                        }
                    }else if(kf.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_TRANSPORT)){
                        if(kf.getKeyValue().equalsIgnoreCase(WsdlToUDDIModelConverter.HTTP_TRANSPORT_V3) ||
                                kf.getKeyValue().equalsIgnoreCase(WsdlToUDDIModelConverter.HTTP_TRANSPORT_V2)){
                            transportIsHttp= true;
                        }
                    }
                }
                if(protocolIsSoap && transportIsHttp){
                    bindingKeysToDelete.add(bindingTemplate.getBindingKey());
                }
            }
        }

        final Set<String> bindingKeysToKeep = new HashSet<String>();
        for(final BindingTemplate bindingTemplate: templates){
            if(!bindingKeysToDelete.contains(bindingTemplate.getBindingKey())){
                bindingKeysToKeep.add(bindingTemplate.getBindingKey());
            }
        }

        if(bindingKeysToKeep.size() + bindingKeysToDelete.size() != templates.size()){
            throw new IllegalStateException("Invalid calculation of which bindings to keep and which to delete");
        }

        return new Pair<Set<String>, Set<String>>(bindingKeysToDelete, bindingKeysToKeep);
    }

    /**
     * Try to delete a tModel published to UDDI. Will not throw a UDDIException
     * @param tModelKey String tModelKey to delete
     */
    private void handleTModelRollback(final String tModelKey){
        try {
            logger.log(Level.FINE, "Attemping to delete tModel published to UDDI with tModelKey: " + tModelKey);
            uddiClient.deleteTModel(tModelKey);
            logger.log(Level.FINE, "Succesfully deleted tModel with tModelKey: " + tModelKey);
        } catch (UDDIException e1) {
            logger.log(Level.WARNING, "Could not rollback published tModel with key " + tModelKey + "to UDDI: " + ExceptionUtils.getMessage(e1));
        }
    }

    private void handleBindingTemplateRollback(final String bindingKey){
        try {
            logger.log(Level.FINE, "Attemping to delete bindingTemplate published to UDDI with bindingKey: " + bindingKey);
            uddiClient.deleteBindingTemplate(bindingKey);
            logger.log(Level.FINE, "Succesfully deleted tModel with bindingKey: " + bindingKey);
        } catch (UDDIException e1) {
            logger.log(Level.WARNING, "Could not rollback published bindingTemplate with key " + bindingKey + "to UDDI: " + ExceptionUtils.getMessage(e1));
        }
    }

}
