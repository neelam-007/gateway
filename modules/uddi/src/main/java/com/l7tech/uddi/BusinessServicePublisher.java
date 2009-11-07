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

    /**
     * Publish a BindingTemplate to an existing BusinessService in UDDI
     *
     * @param serviceKey                  String serviceKey of the BusinessService to publish the end point to
     * @param wsdlPortName                String wsdl:port name of the bindingTemplate to copy
     * @param protectedServiceExternalURL String Gateway URL for the protected service
     * @param protectedServiceWsdlURL     String Gateway WSDL for the protected service
     * @return String bindingKey of the newly created bindingTemplate
     * @throws UDDIException Any problems updating UDDI
     */
    public String publishEndPointToExistingService(final String serviceKey,
                                                   final String wsdlPortName,
                                                   final String protectedServiceExternalURL,
                                                   final String protectedServiceWsdlURL)
            throws UDDIException {

        UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient);
        final Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);
        final List<Pair<BusinessService, Map<String, TModel>>>
                uddiServicesToDependentModels = serviceDownloader.getBusinessServiceModelsNew(serviceKeys);

        if (uddiServicesToDependentModels.isEmpty())
            throw new UDDIException("No BusinessService with serviceKey " + serviceKey + " found in UDDIRegistry");

        final Pair<BusinessService, Map<String, TModel>> modelFromUDDI = uddiServicesToDependentModels.get(0);
        final BusinessService foundService = modelFromUDDI.left;
        final Map<String, TModel> tModelKeyToTModel = modelFromUDDI.right;
        return publishEndPointToExistingService(foundService, tModelKeyToTModel, wsdlPortName,
                protectedServiceExternalURL, protectedServiceWsdlURL).left.getBindingKey();

    }
    
    Pair<BindingTemplate, List<TModel>> publishEndPointToExistingService(final BusinessService foundService,
                                            final Map<String, TModel> tModelKeyToTModel,
                                            final String wsdlPortName,
                                            final String protectedServiceExternalURL,
                                            final String protectedServiceWsdlURL) throws UDDIException {
        //first find the bindingTemplate we are going to copy
        List<BindingTemplate> bindingTemplates = foundService.getBindingTemplates().getBindingTemplate();
        if(bindingTemplates.isEmpty()) throw new UDDIException("BusinessService with serviceKey " + foundService.getServiceKey() + " contains no bindingTemplate elements");

        BindingTemplate foundTemplate = null;
        for(BindingTemplate template: bindingTemplates){
            if (template.getTModelInstanceDetails() == null){
                logger.log(Level.INFO, "Ignoring bindingTemplate with key: " + template.getBindingKey()+" as it does not contain a tModelInstanceDetails element");
                continue;
            }

            for (TModelInstanceInfo tmii : template.getTModelInstanceDetails().getTModelInstanceInfo()) {
                InstanceDetails instanceDetails = tmii.getInstanceDetails();
                if (instanceDetails == null) continue;

                String instanceParamForWsdlBinding = instanceDetails.getInstanceParms();
                if(wsdlPortName.equals(instanceParamForWsdlBinding)) foundTemplate = template;
            }
        }

        if(foundTemplate == null)
            throw new UDDIException("No bindingTemplate found containing a tModelInstanceInfo with an instanceParams matching wsdl:port local name '" + wsdlPortName+"'");

        //the TModelInstanceDetails element is 0..1 in the UDDI spec

        final List<TModelInstanceInfo> originalInfos = foundTemplate.getTModelInstanceDetails().getTModelInstanceInfo();
        //The amount of TModelInstanceInfo's is unlimited according to the spec its 0..*
        //Get every single one and later find the wsdl:binding and wsdl:port references
        //we will only add the binding and portType referencs and relevant information. Other tModelinstanceInfo
        //information will not be copied, as we do not know what it means.

        final Pair<TModel, TModel> requiredTModels = getPortTypeAndBindingTModels(originalInfos, tModelKeyToTModel);

        final TModel portTypeTModel = requiredTModels.left;
        final TModel bindingTModel = requiredTModels.right;

        //to publish a bindingTemplate, we also need to create the two relevant tModels, and configure them
        //to point back to the gateway for their WSDL addresses
        final TModel newPortTypeTModel = getNewPortTypeTModel(portTypeTModel, protectedServiceWsdlURL);
        TModel newBindingTModel = null;
        try {
            jaxWsUDDIClient.publishTModel(newPortTypeTModel);
            newBindingTModel = getNewBindingTModel(bindingTModel, newPortTypeTModel.getTModelKey(), protectedServiceWsdlURL);
            jaxWsUDDIClient.publishTModel(newBindingTModel);

            final TModelInstanceDetails copyTModelInstanceDetails = new TModelInstanceDetails();
            //create the binding TModelInstanceInfo
            final TModelInstanceInfo bindingTModelInstanceInfo = new TModelInstanceInfo();
            bindingTModelInstanceInfo.setTModelKey(newBindingTModel.getTModelKey());
            final InstanceDetails bindingInstanceDetails = new InstanceDetails();
            bindingInstanceDetails.setInstanceParms(newBindingTModel.getName().getValue());//guaranteed to match according to spec
            final Description bindingDescription = new Description();
            bindingDescription.setLang("en-US");
            bindingDescription.setValue(WsdlToUDDIModelConverter.WSDL_BINDING_INSTANCE_DESCRIPTION);
            bindingInstanceDetails.getDescription().add(bindingDescription);
            bindingTModelInstanceInfo.setInstanceDetails(bindingInstanceDetails);
            copyTModelInstanceDetails.getTModelInstanceInfo().add(bindingTModelInstanceInfo);

            //create the portType TModelInstanceInfo
            final TModelInstanceInfo portTModelInstanceInfo = new TModelInstanceInfo();
            portTModelInstanceInfo.setTModelKey(newPortTypeTModel.getTModelKey());
            copyTModelInstanceDetails.getTModelInstanceInfo().add(portTModelInstanceInfo);

            final BindingTemplate newBindingTemplate = new BindingTemplate();
            newBindingTemplate.setServiceKey(foundService.getServiceKey());
            final AccessPoint accessPoint = new AccessPoint();
            accessPoint.setUseType("endPoint");
            accessPoint.setValue(protectedServiceExternalURL);
            newBindingTemplate.setAccessPoint(accessPoint);
            newBindingTemplate.setTModelInstanceDetails(copyTModelInstanceDetails);

            jaxWsUDDIClient.publishBindingTemplate(newBindingTemplate);
            return new Pair<BindingTemplate, List<TModel>>(newBindingTemplate, Arrays.asList(newBindingTModel, newPortTypeTModel));
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Problem publishing to UDDI: " + ExceptionUtils.getMessage(e));
            //rollback any actions taken
            if(newPortTypeTModel.getTModelKey() != null && !newPortTypeTModel.getTModelKey().trim().isEmpty()){
                handleTModelRollback(newPortTypeTModel.getTModelKey());
            }
            if(newBindingTModel != null && newBindingTModel.getTModelKey() != null && !newBindingTModel.getTModelKey().trim().isEmpty()){
                handleTModelRollback(newBindingTModel.getTModelKey());
            }
            //if the binding published successfully then it is not responsible for this exception, so nothing needed for bindingTemplate here
            throw e;
        }
    }

    private Pair<TModel, TModel> getPortTypeAndBindingTModels(final List<TModelInstanceInfo> originalInfos,
                                                              final Map<String, TModel> tModelKeyToTModel) throws UDDIException {
        //Go through each TModelInstanceInfo and copy any which look like they may represent
        // wsdl:binding and wsdl:portType elements
        final List<TModelInstanceInfo> copyAllTModelInstanceInfo = new ArrayList<TModelInstanceInfo>();
        for(TModelInstanceInfo origTModelInstanceInfo: originalInfos){
            //Each TModelInstanceInfo can have 0..1 InstanceDetails an 0..* descriptions
            final TModelInstanceInfo copyTModelInstanceInfo = new TModelInstanceInfo();
            copyTModelInstanceInfo.setTModelKey(origTModelInstanceInfo.getTModelKey());//this is required

            if(origTModelInstanceInfo.getInstanceDetails() != null){//this may represent a wsdl:binding
                //InstanceDetails can have:
                // 0..* descriptions - will copy
                // choice of InstanceParams OR 0..* OverviewDocs and 0..1 InstanceParams - only care about first option -
                // as the WSDL to UDDI technical note requires a mandatory instanceParam - which is the wsdl:binding localName
                final InstanceDetails origInstanceDetails = origTModelInstanceInfo.getInstanceDetails();
                if(origInstanceDetails.getInstanceParms() == null || origInstanceDetails.getInstanceParms().trim().isEmpty()) {
                    logger.log(Level.INFO, "Ignoring TModelInstanceInfo '" + origTModelInstanceInfo.getTModelKey() +
                            "' as it contains an instanceDetails which a null or empty instanceParams element");
                    continue;
                }

                final InstanceDetails copyInstanceDetails = new InstanceDetails();
                copyInstanceDetails.setInstanceParms(origInstanceDetails.getInstanceParms());//string value
                for(Description origDesc: origInstanceDetails.getDescription()){
                    final Description copyDesc = new Description();
                    copyDesc.setLang(origDesc.getLang());
                    copyDesc.setValue(origDesc.getValue());
                    copyInstanceDetails.getDescription().add(copyDesc);
                }
                copyTModelInstanceInfo.setInstanceDetails(copyInstanceDetails);
            }

            copyAllTModelInstanceInfo.add(copyTModelInstanceInfo);
        }

        //now we have every candidate TModelInstanceInfo from the found bindingTemplate
        //need to ensure it contains a wsdl:binding and wsdl:portType tModelKey references
        //once we have found this, we know which tModels we need to copy
        TModel bindingTModel = null;
        TModel portTypeTModel = null;
        for(TModelInstanceInfo tModelInstanceInfo: copyAllTModelInstanceInfo){
            final TModel tModel = tModelKeyToTModel.get(tModelInstanceInfo.getTModelKey());
            if(tModel == null)
                throw new UDDIException("tModel with tModelKey " + tModelInstanceInfo.getTModelKey()
                        + " referenced from the tModelInstanceInfo was not referenced from BusinessService");

            //what type of tModel is it?
            final UDDIUtilities.TMODEL_TYPE type = UDDIUtilities.getTModelType(tModel, false);
            if(type == null) continue;
            switch (type){
                case WSDL_BINDING:
                    bindingTModel = tModel;
                    break;
                case WSDL_PORT_TYPE:
                    portTypeTModel = tModel;
                    break;
            }
        }

        if(bindingTModel == null || portTypeTModel == null){
            final String msg = "Could not find a valid wsdl:binding and wsdl:portype tModel references from the bindingTemplate's tModelInstanceDetails element";
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg);
        }

        return new Pair<TModel, TModel>(portTypeTModel, bindingTModel);
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

    private TModel getNewPortTypeTModel(final TModel portTypeTModel,
                                        final String protectedServiceWsdlURL) throws UDDIException {
        final TModel newPortTypeTModel = new TModel();
        final Name name = new Name();
        name.setLang(portTypeTModel.getName().getLang());
        name.setValue(portTypeTModel.getName().getValue());
        newPortTypeTModel.setName(name);

        newPortTypeTModel.getOverviewDoc().add(getOverviewDoc(protectedServiceWsdlURL));

        //Need to satisfy all technical note requirements - namespace is optional
        boolean foundPortTypeType = false; //uddi:uddi.org:wsdl:types

        final List<KeyedReference> newRefs = new ArrayList<KeyedReference>();

        for(final KeyedReference keyedReference: portTypeTModel.getCategoryBag().getKeyedReference()){
            //opting to always use the values found in UDDI, not going to supply the value from our knowledge,
            // as there could be subtle case sensitivity differences
            if(keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES)){
                final KeyedReference portTypeRef = new KeyedReference();
                portTypeRef.setKeyValue(keyedReference.getKeyValue());
                portTypeRef.setTModelKey(keyedReference.getTModelKey());
                newRefs.add(portTypeRef);
                foundPortTypeType = true;
                continue;
            }

            if(keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE)){
                final KeyedReference nameSpaceRef = new KeyedReference();
                nameSpaceRef.setKeyName(keyedReference.getKeyName());
                nameSpaceRef.setKeyValue(keyedReference.getKeyValue());
                nameSpaceRef.setTModelKey(keyedReference.getTModelKey());
                newRefs.add(nameSpaceRef);
                continue;
            }
        }

        if(!foundPortTypeType){
            throw new UDDIException("wsdl:portType tModel found does not contain a portType type keyedReference");
        }

        final CategoryBag categoryBag = new CategoryBag();
        categoryBag.getKeyedReference().addAll(newRefs);
        newPortTypeTModel.setCategoryBag(categoryBag);
        return newPortTypeTModel;
    }

    /**
     * Create a new wsdl:binding tModel, based on the original.
     *
     * @param bindingTModel TMOdel original wsdl:binding tModel
     * @param portTypeTModelKey String tModelKey of the newly created wsdl:portType tModel
     * @param protectedServiceWsdlURL String SSG WSDL URL
     * @return TModel newly created tModel. Has not been published to UDDI
     */
    private TModel getNewBindingTModel(final TModel bindingTModel,
                                       final String portTypeTModelKey,
                                       final String protectedServiceWsdlURL) throws UDDIException {
        final TModel newBindingTModel = new TModel();
        final Name name = new Name();
        name.setLang(bindingTModel.getName().getLang());
        name.setValue(bindingTModel.getName().getValue());
        newBindingTModel.setName(name);

        //Overview doc
        newBindingTModel.getOverviewDoc().add(getOverviewDoc(protectedServiceWsdlURL));

        //keyed references - only process those which are a requirement of the technical note for WSDL in UDDI
        //do not copy UDDI specific ones, as we do not know what meaning they have
        //Need to satisfy all technical note requirements - namespace is optional
        boolean foundBindingTypeRef = false; //uddi:uddi.org:wsdl:types
        boolean foundPortTypeRef = false;
        boolean foundWsdlSpecRef = false;
        //protocol is required, transport is optional
        boolean foundProtocolRef = false;

        final List<KeyedReference> newRefs = new ArrayList<KeyedReference>();
        
        for(KeyedReference keyedReference: bindingTModel.getCategoryBag().getKeyedReference()){
            //opting to always use the values found in UDDI, not going to supply the value from our knowledge,
            // as there could be subtle case sensitivity differences
            if(keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES)){
                final KeyedReference bindingTypeRef = new KeyedReference();
                bindingTypeRef.setKeyValue(keyedReference.getKeyValue());
                bindingTypeRef.setTModelKey(keyedReference.getTModelKey());
                newRefs.add(bindingTypeRef);
                foundBindingTypeRef = true;
                continue;
            }
            
            if(keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_PORTTYPEREFERENCE)){
                final KeyedReference portTypeRef = new KeyedReference();
                portTypeRef.setKeyName(keyedReference.getKeyName());
                portTypeRef.setKeyValue(portTypeTModelKey);//this is our newly published tModel!!
                portTypeRef.setTModelKey(keyedReference.getTModelKey());
                newRefs.add(portTypeRef);
                foundPortTypeRef = true;
                continue;
            }

            if(keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_CATEGORIZATION_TYPES)
                    && keyedReference.getKeyValue() != null && keyedReference.getKeyValue().equalsIgnoreCase("wsdlSpec")){
                final KeyedReference wsdlSpecRef = new KeyedReference();
                wsdlSpecRef.setKeyValue(keyedReference.getKeyValue());
                wsdlSpecRef.setTModelKey(keyedReference.getTModelKey());
                newRefs.add(wsdlSpecRef);
                foundWsdlSpecRef = true;
                continue;
            }

            if(keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE)){
                final KeyedReference nameSpaceRef = new KeyedReference();
                nameSpaceRef.setKeyName(keyedReference.getKeyName());
                nameSpaceRef.setKeyValue(keyedReference.getKeyValue());
                nameSpaceRef.setTModelKey(keyedReference.getTModelKey());
                newRefs.add(nameSpaceRef);
                continue;
            }

            if(keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_PROTOCOL)){
                final KeyedReference protocolRef = new KeyedReference();
                protocolRef.setKeyName(keyedReference.getKeyName());
                protocolRef.setKeyValue(keyedReference.getKeyValue());
                protocolRef.setTModelKey(keyedReference.getTModelKey());
                newRefs.add(protocolRef);
                foundProtocolRef = true;
                continue;
            }

            if(keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_WSDL_CATEGORIZATION_TRANSPORT)){
                final KeyedReference transportRef = new KeyedReference();
                transportRef.setKeyName(keyedReference.getKeyName());
                transportRef.setKeyValue(keyedReference.getKeyValue());
                transportRef.setTModelKey(keyedReference.getTModelKey());
                newRefs.add(transportRef);
                continue;
            }

        }

        if(!foundBindingTypeRef){
            throw new UDDIException("wsdl:binding tModel found does not contain a binding type keyedReference");
        }
        if(!foundPortTypeRef){
            throw new UDDIException("wsdl:binding tModel found does not contain a binding portType keyedReference");
        }
        if(!foundWsdlSpecRef){
            throw new UDDIException("wsdl:binding tModel found does not contain a wsdlSpec uddi category keyedReference");
        }
        if(!foundProtocolRef){
            throw new UDDIException("wsdl:binding tModel found does not contain a protocol category keyedReference");
        }

        final CategoryBag categoryBag = new CategoryBag();
        categoryBag.getKeyedReference().addAll(newRefs);
        newBindingTModel.setCategoryBag(categoryBag);
        return newBindingTModel;
    }

    private OverviewDoc getOverviewDoc(final String protectedServiceWsdlURL){
        //Overview doc
        final OverviewDoc overviewDoc = new OverviewDoc();
        //description
        final Description description = new Description();
        description.setLang("en-US");
        description.setValue("the original WSDL document");
        overviewDoc.getDescription().add(description);
        //wsdl url
        final OverviewURL overviewUrl = new OverviewURL();
        overviewUrl.setUseType("wsdlInterface");
        overviewUrl.setValue(protectedServiceWsdlURL);
        overviewDoc.setOverviewURL(overviewUrl);

        return overviewDoc;
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
                uddiServicesToDependentModels = serviceDownloader.getBusinessServiceModelsNew(publishedServiceKeys);

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
