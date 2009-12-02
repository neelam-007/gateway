package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.common.protocol.SecureSpanConstants;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE;
import static com.l7tech.uddi.UDDIUtilities.TMODEL_TYPE.WSDL_BINDING;
import com.l7tech.util.Pair;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.wsdl.Wsdl;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.Closeable;
import java.io.IOException;

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
public class BusinessServicePublisher implements Closeable {
    private static final Logger logger = Logger.getLogger(BusinessServicePublisher.class.getName());

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

        this( wsdl, serviceOid, buildUDDIClient(uddiCfg), true);
    }

    public BusinessServicePublisher(final Wsdl wsdl,
                                    final long serviceOid,
                                    final UDDIClient uddiClient) {

        this( wsdl, serviceOid, uddiClient, true);
    }

    /**
     * Publish a set of bindingTemplates to a BusinessService in UDDI. Each bindingTemplate published will represent
     * the same published service. There is more than one bindingTemplate published if the cluster defines a https
     * endpoint in addition to a http endpoint. Note: that as many endpoints will be created as supplied in allEndpointPairs
     * <p/>
     * See UDDIUtilities.validateAllEndpointPairs(allEndpointPairs) for validation of allEndpointPairs
     *
     * @param serviceKey               String serviceKey of the owning BusinessService
     * @param wsdlPortName             String wsdl:port local name of the wsdl:port to publish as a bindingTemplate to UDDI
     * @param wsdlPortBinding          String wsdl:binding local name of the wsdl:binding to publish as a bindingTemplate to UDDI.
     *                                 if no wsdl:port by the name of wsdlPortName is found in the gateway's WSDL, then the first implementing wsdl:port
     *                                 of this binding local name, and namespace if defined, will be published to UDDI.
     * @param wsdlPortBindingNamespace String namespace of the above wsdl:binding. Can be null. Cannot be the empty string
     * @param publishedHostname        String external hostname of the publishing cluster. Cannot be null or empty
     * @param allEndpointPairs         Collection<Pair<String, String>>. Required. All endpoints to publish bindingTemplates for
     * @param removeOthers             boolean, if true all other endpoints will be removed
     * @throws UDDIException any problems searching / updating UDDI
     */
    public void publishBindingTemplate(
            final String serviceKey,
            final String wsdlPortName,
            final String wsdlPortBinding,
            final String wsdlPortBindingNamespace,
            final String publishedHostname,
            final Collection<Pair<String, String>> allEndpointPairs,
            final boolean removeOthers) throws UDDIException {
        if(serviceKey == null || serviceKey.trim().isEmpty()) throw new IllegalArgumentException("serviceKey cannot be null or empty");
        if(wsdlPortName == null || wsdlPortName.trim().isEmpty()) throw new IllegalArgumentException("wsdlPortName cannot be null or empty");
        if(wsdlPortBinding == null || wsdlPortBinding.trim().isEmpty()) throw new IllegalArgumentException("wsdlPortBinding cannot be null or empty");
        if(wsdlPortBindingNamespace == null || wsdlPortBindingNamespace.trim().isEmpty()) throw new IllegalArgumentException("wsdlPortBindingNamespace cannot be null or empty");
        if(publishedHostname == null || publishedHostname.trim().isEmpty()) throw new IllegalArgumentException("publishedHostname cannot be null or empty");

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);

        //Delete any existing bindings for the given publishedHostname and serviceOid
        deleteGatewayBindingTemplates(serviceKey, publishedHostname);

        publishEndPointToExistingService(serviceKey, wsdlPortName, wsdlPortBinding, wsdlPortBindingNamespace, allEndpointPairs, removeOthers);
    }

    /**
     * Delete all gateway endpoints contained by a BusinessService
     * <p/>
     * A gateway endpoint is defined as any bindingTemplate in UDDI which contains an accessPoint with an endPoint value                                     n
     * containing both the supplied publishedHostname and the serviceOid of a published service
     *
     * @param serviceKey String serviceKey of a BusinessService from which to delete gateway bindingTemplates. Required
     * @param publishedHostname String external hostname of the cluster
     * @throws UDDIException any problems searching / updating UDDI
     */
    public void deleteGatewayBindingTemplates(final String serviceKey, final String publishedHostname) throws UDDIException {
        if(serviceKey == null || serviceKey.trim().isEmpty()) throw new IllegalArgumentException("serviceKey cannot be null or empty");
        if(publishedHostname == null || publishedHostname.trim().isEmpty()) throw new IllegalArgumentException("publishedHostname cannot be null or empty");        

        final BusinessService bs = jaxWsUDDIClient.getBusinessService(serviceKey);
        final BindingTemplates bindingTemplates = bs.getBindingTemplates();
        if (bindingTemplates != null && !bindingTemplates.getBindingTemplate().isEmpty()) {
            for (BindingTemplate bt : bindingTemplates.getBindingTemplate()) {
                final AccessPoint accessPoint = bt.getAccessPoint();
                if (!accessPoint.getUseType().equalsIgnoreCase(WsdlToUDDIModelConverter.USE_TYPE_END_POINT)) continue;

                final String endPoint = accessPoint.getValue();
                if (endPoint.indexOf(publishedHostname) != -1 && endPoint.indexOf(SecureSpanConstants.SERVICE_FILE + Long.toString(serviceOid)) != -1) {
                    uddiClient.deleteBindingTemplate(bt.getBindingKey());
                }
            }
        }
    }

    /**
     * Should only be called once for a WSDL. Will do the house cleaning required to publish a proxied wsdl:service
     * to UDDI, which represents an 'overwritten' service
     * <p/>
     * This will overwrite a single BusinessService in UDDI with the contents of a single wsdl:service from the
     * gateways WSDL.
     *
     * @param serviceKey  String serviceKey of a BusinessService to overwrite. Required.
     * @param businessKey Stirng businessKey which owns the BusinessService identified by serviceKey. Required
     * @param allEndpointPairs Collection<Pair<String, String>>. Required. All endpoints to publish bindingTemplates for
     * @return Pair Left side: Set of serviceKeys which should be deleted. Right side: set of services published
     *         as a result of this publish / update operation
     * @throws UDDIException any problems searching / updating UDDI
     */
    public Pair<Set<String>, Set<UDDIBusinessService>> overwriteServiceInUDDI(
            final String serviceKey,
            final String businessKey,
            final Collection<Pair<String, String>> allEndpointPairs) throws UDDIException {
        if (serviceKey == null || serviceKey.trim().isEmpty())
            throw new IllegalArgumentException("serviceKey cannot be null or empty");
        if (businessKey == null || businessKey.trim().isEmpty())
            throw new IllegalArgumentException("businessKey cannot be null or empty");

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);

        final UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient, jaxWsUDDIClient);
        final Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);
        final List<Pair<BusinessService, Map<String, TModel>>> uddiServicesToDependentTModelsPairs = serviceDownloader.getBusinessServiceModels(serviceKeys);

        if (uddiServicesToDependentTModelsPairs.isEmpty())
            throw new UDDIException("No BusinessService found for serviceKey: " + serviceKey);
        final Pair<BusinessService, Map<String, TModel>> serviceToDependentTModels = uddiServicesToDependentTModelsPairs.iterator().next();

        BusinessService overwriteService = serviceToDependentTModels.left;//not final as we will update it's reference
        final BindingTemplates templates = overwriteService.getBindingTemplates();
        if (templates == null) {
            //we can work with this, we don't care as were going to be uploading our own bindingTemplates
            //this happens if all bindings are removed between when we got our record of the service and us
            //initiating this overwrite action
            BindingTemplates newTemplates = new BindingTemplates();
            overwriteService.setBindingTemplates(newTemplates);
        }

        final Map<String, TModel> allDependentTModels = serviceToDependentTModels.right;

        final Pair<Set<String>, Set<String>> deleteAndKeep = getBindingsToDeleteAndKeep(overwriteService.getBindingTemplates().getBindingTemplate(), allDependentTModels);
        final Set<String> deleteSet = deleteAndKeep.left;

        for (String bindingKey : deleteSet) {
            uddiClient.deleteBindingTemplate(bindingKey);
        }
        //Redownload the service
        overwriteService = jaxWsUDDIClient.getBusinessService(overwriteService.getServiceKey());

        //add all kept bind

        //extract the service's local name from it's keyed references - it must exist to be valid
        if (overwriteService.getCategoryBag() == null) {
            throw new UDDIException("Invalid BusinessService. It does not contain a CategoryBag. serviceKey: " + overwriteService.getServiceKey());
        }

        final Pair<String, String> serviceNameAndNameSpace = getServiceNameAndNameSpace(overwriteService);

        //we must find both the service name and the correct namespace in the Gateway's WSDL, otherwise we are mis configured

        //Now work with the WSDL, the information we will publish comes from the Gateway's WSDL
        final WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        try {
            modelConverter.convertWsdlToUDDIModel(allEndpointPairs, null, null);
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
        return publishToUDDI(serviceKeys, pairToUseList, serviceNameToWsdlNameMap, true, null);
    }

    /**
     * Get the set of services which should be deleted and the set of services which were published as a result
     * of publishing the gateway wsdl
     * <p/>
     * This handles both an initial publish and a subsequent publish. For updates, existing serviceKeys are reused
     * where they represent the same wsdl:service from the wsdl
     * <p/>
     * If isOverWriteUpdate is true, then publishedServiceKeys must contain at least 1 key.
     *
     * @param businessKey                 String business key of the BusinessEntity in UDDi which owns all services published
     * @param publishedServiceKeys        Set String of known service keys. Can be empty, but not null
     * @param isOverwriteUpdate           boolean if true, then we aer updating an overwritten service. In this case we need
     *                                    to make sure we only publish a single service from the WSDL. The first serviceKey found in publishedServiceKeys is then
     *                                    assumed to be the serviceKey of the overwritten service.
     * @param registrySpecificMetaData    if not null, the registry specific meta data will be added appropriately to each uddi piece of infomation published
     * @param allEndpointPairs            Collection<Pair<String, String>> all endpoints which should be published to UDDI
     *                                    as distinct endpoints for each service published. This means that a single wsdl:port may have more than one
     *                                    bindingTemplate in UDDI. The left side of each pair is String url which will become the value of the'endPoint'
     *                                    in the accessPoint element of a bindingTemplate. The right hand side of each pair is String url the overview url
     *                                    where the wsdl can be downloaded from the gateway from which will be included in every tModel published
     * @return Pair Left side: Set of serviceKeys which should be deleted. Right side: set of services published
     *         as a result of this publish / update operation
     * @throws UDDIException any problems searching / updating UDDI or with data model
     */
    public Pair<Set<String>, Set<UDDIBusinessService>> publishServicesToUDDIRegistry(
            final String businessKey,
            final Set<String> publishedServiceKeys,
            final boolean isOverwriteUpdate,
            final UDDIRegistrySpecificMetaData registrySpecificMetaData,
            final Collection<Pair<String, String>> allEndpointPairs) throws UDDIException {

        if(businessKey == null || businessKey.trim().isEmpty()) throw new IllegalArgumentException("businessKey cannot be null or empty");
        if(publishedServiceKeys == null) throw new NullPointerException("publishedServiceKeys cannot be null");

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);
        
        final WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        try {
            final String prependServiceName = SyspropUtil.getString("com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "Layer7");
            final String appendServiceName = SyspropUtil.getString("com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", Long.toString(serviceOid));
            if(!isOverwriteUpdate){
                modelConverter.convertWsdlToUDDIModel(allEndpointPairs, prependServiceName, appendServiceName);
            }else{
                modelConverter.convertWsdlToUDDIModel(allEndpointPairs, null, null);
            }

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

        return publishToUDDI(publishedServiceKeys, wsdlServiceNameToDependentTModels, serviceToWsdlServiceName, isOverwriteUpdate, registrySpecificMetaData);
    }

    @Override
    public void close() throws IOException {
        uddiClient.close();
    }

    //- PROTECTED

    /**
     * Create a new BusinessServicePublisher.
     *
     * @param wsdl WSDL to publish to UDDI
     * @param serviceOid services OID, which originates from the WSDL
     * @param uddiClient UDDIClient for the UDDI Registry to publish to
     */
    protected BusinessServicePublisher(final Wsdl wsdl,
                                       final long serviceOid,
                                       final UDDIClient uddiClient,
                                       boolean flagJustIngoreMe) {
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

    Pair<List<BindingTemplate>, List<TModel>> publishEndPointToExistingService(
            final String serviceKey,
            final String wsdlPortName,
            final String wsdlPortBinding,
            final String wsdlPortBindingNamespace,
            final Collection<Pair<String, String>> allEndpointPairs,
            final boolean removeOthers)
            throws UDDIException {

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);
        final Pair<List<BindingTemplate>, Map<String, TModel>> bindingToModels;
        try {
            bindingToModels = UDDIUtilities.createBindingTemplateFromWsdl(wsdl, wsdlPortName, wsdlPortBinding, wsdlPortBindingNamespace, allEndpointPairs);
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
                if (UDDIUtilities.getTModelType(entry.getValue(), true) != UDDIUtilities.TMODEL_TYPE.WSDL_PORT_TYPE)
                    continue;
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
            for (TModel bindingModel : allBindingTModels) {
                jaxWsUDDIClient.publishTModel(bindingModel);
            }

            for (BindingTemplate bt : bindingToModels.left) {
                bt.setServiceKey(businessService.getServiceKey());

                UDDIUtilities.updateBindingTemplateTModelReferences(bt, bindingToModels.right);
                jaxWsUDDIClient.publishBindingTemplate(bt);
            }

            //remove other bindings?
            if (removeOthers) {
                logger.log(Level.FINE, "Deleting other bindingTemplates");
                uddiClient.deleteBindingTemplateFromSingleService(allOtherBindingKeys);
            }

            List<TModel> models = new ArrayList<TModel>();
            models.addAll(bindingToModels.right.values());
            return new Pair<List<BindingTemplate>, List<TModel>>(Collections.unmodifiableList(bindingToModels.left), Collections.unmodifiableList(models));

        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Problem publishing to UDDI: " + ExceptionUtils.getMessage(e));
            for (Map.Entry<String, TModel> entry : tModelsToPublish.entrySet()) {
                final String tModelKey = entry.getValue().getTModelKey();
                if (tModelKey != null && !tModelKey.trim().isEmpty()) {
                    handleTModelRollback(tModelKey);
                }
            }
            for (BindingTemplate bt : bindingToModels.left) {
                final String bindingKey = bt.getBindingKey();
                if (bindingKey != null && !bindingKey.trim().isEmpty()) {
                    handleBindingTemplateRollback(bindingKey);
                }
            }
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
                 //todo may not need to publish if it doesnt have a null / empty key
                final boolean published = jaxWsUDDIClient.publishTModel(tModel);
                if (published) publishedTModels.add(tModel);
            }
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Exception publishing tModels: " + e.getMessage());
            handleUDDIRollback(publishedTModels, Collections.<Pair<String, BusinessService>>emptySet(), e);
            throw e;
        }
        return Collections.unmodifiableList(publishedTModels);
    }

    //- PRIVATE

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
     * @param publishedServiceKeys
     * @param wsdlServiceNameToDependentTModels
     *
     * @param serviceToWsdlServiceName
     * @param isOverwriteUpdate        if true, if a previously published BusinessService is found, along with
     *                                 it's serviceKey, any bindings it has will also be preserved
     * @param registrySpecificMetaData if not null, the registry specific meta data will be added appropriately to each uddi piece of infomation published
     * @return
     * @throws UDDIException
     */
    private Pair<Set<String>, Set<UDDIBusinessService>> publishToUDDI(
            final Set<String> publishedServiceKeys,
            final List<Pair<BusinessService, Map<String, TModel>>> wsdlServiceNameToDependentTModels,
            final Map<String, String> serviceToWsdlServiceName,
            final boolean isOverwriteUpdate,
            final UDDIRegistrySpecificMetaData registrySpecificMetaData) throws UDDIException {
        final UDDIProxiedServiceDownloader serviceDownloader = new UDDIProxiedServiceDownloader(uddiClient, jaxWsUDDIClient);
        //Get the info on all published business services from UDDI
        //Now we know every single tModel reference each existing Business Service contains
        final List<Pair<BusinessService, Map<String, TModel>>>
                uddiServicesToDependentModels = serviceDownloader.getBusinessServiceModels(publishedServiceKeys);

        final List<BusinessService> uddiPreviouslyPublishedServices = new ArrayList<BusinessService>();
        for (Pair<BusinessService, Map<String, TModel>> aServiceToItsModels : uddiServicesToDependentModels) {
            uddiPreviouslyPublishedServices.add(aServiceToItsModels.left);
        }

        //map to look up existing services
        final Map<String, BusinessService> uddiPreviouslyPublishedServiceMap = new HashMap<String, BusinessService>();
        for (BusinessService bs : uddiPreviouslyPublishedServices) {
            final String uniqueKey = getUniqueKeyForService(bs);
            uddiPreviouslyPublishedServiceMap.put(uniqueKey, bs);
        }

        //create a map to look up the serviecs from the wsdl to publish
        final Map<String, BusinessService> wsdlServicesToPublish = new HashMap<String, BusinessService>();
        for (Pair<BusinessService, Map<String, TModel>> serviceToModels : wsdlServiceNameToDependentTModels) {
            BusinessService bs = serviceToModels.left;
            final String uniqueKey = getUniqueKeyForService(bs);
            wsdlServicesToPublish.put(uniqueKey, bs);
        }

        //find which services already exist, and reuse their serviceKeys if found
        //also need to know which BusinessServices to delete

        final Set<String> deleteSet = new HashSet<String>();

        //for overwrite - we keep non soap/http bindings. for these bindings, do not delete any tmodels they reference
        final Map<String, TModel> uddiNonSoapBindingTmodels = new HashMap<String, TModel>();

        for(Map.Entry<String, BusinessService> entry: uddiPreviouslyPublishedServiceMap.entrySet()){
            final BusinessService uddiPublishedService = entry.getValue();
            final String uniquePublishedSvcName = entry.getKey();
            //has this service already been published?
            //a BusinessService represents a wsdl:service, if the name is the same reuse the service key
            //a wsdl:service from a wsdl will always produce the same name, see WsdlToUDDIModelConverter
            if (wsdlServicesToPublish.containsKey(uniquePublishedSvcName)) {
                final BusinessService businessService = wsdlServicesToPublish.get(uniquePublishedSvcName);
                businessService.setServiceKey(uddiPublishedService.getServiceKey());
                if (isOverwriteUpdate) {
                    if (businessService.getBindingTemplates() == null)
                        businessService.setBindingTemplates(new BindingTemplates());
                    if (uddiPublishedService.getBindingTemplates() == null)
                        uddiPublishedService.setBindingTemplates(new BindingTemplates());

                    final Pair<Set<String>, Set<String>> deleteAndKeep =
                            getBindingsToDeleteAndKeep(uddiPublishedService.getBindingTemplates().getBindingTemplate(),
                                    uddiServicesToDependentModels.iterator().next().right);

                    //all tmodels from uddi for this service
                    final Map<String, TModel> keepTModelMap = uddiServicesToDependentModels.iterator().next().right;

                    //only keep the bindings where are not soap / http
                    for (BindingTemplate bt : uddiPublishedService.getBindingTemplates().getBindingTemplate()) {
                        if (!deleteAndKeep.right.contains(bt.getBindingKey())) continue;
                        businessService.getBindingTemplates().getBindingTemplate().add(bt);
                        for (TModelInstanceInfo infos : bt.getTModelInstanceDetails().getTModelInstanceInfo()) {
                            uddiNonSoapBindingTmodels.put(infos.getTModelKey(), keepTModelMap.get(infos.getTModelKey()));
                        }
                    }
                }
            } else {
                deleteSet.add(uddiPublishedService.getServiceKey());
            }
        }

        final Set<Pair<String, BusinessService>> newlyPublishedServices = publishServicesToUDDI(wsdlServiceNameToDependentTModels, registrySpecificMetaData);

        //find out what tModels need to be deleted
        //do this after initial update, so we have valid references

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
        for (Pair<BusinessService, Map<String, TModel>> aServiceToItsModel : uddiServicesToDependentModels) {
            final BusinessService bs = aServiceToItsModel.left;
            //if a service was deleted, so were its tmodels
            if (deleteSet.contains(bs.getServiceKey())) continue;

            //we will delete every tModel the service used to reference, as new ones have been created for it
            //if we get here it's because the service is still in UDDI
            final Set<String> oldTModels = new HashSet<String>();
            for (TModel tModel : aServiceToItsModel.right.values()) {
                //don't delete any tmodels we left after taking over a service
                if (uddiNonSoapBindingTmodels.containsKey(tModel.getTModelKey())) continue;
                oldTModels.add(tModel.getTModelKey());
            }
            try {
                logger.log(Level.FINE, "Deleting old tmodels no longer referenced by business service with serviceKey: " + bs.getServiceKey());
                uddiClient.deleteTModel(oldTModels);
            } catch (UDDIException e) {
                logger.log(Level.WARNING, "Could not delete old tModels from UDDI: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            }
        }

        final Set<UDDIBusinessService> newlyCreatedSet = new HashSet<UDDIBusinessService>();
        for (Pair<String, BusinessService> serviceKeyToObject : newlyPublishedServices) {
            final BusinessService bs = serviceKeyToObject.right;
            final String wsdlServiceName = bs.getName().get(0).getValue();
            final UDDIBusinessService uddiBs = new UDDIBusinessService(
                    bs.getName().get(0).getValue(),
                    bs.getServiceKey(),
                    serviceToWsdlServiceName.get(wsdlServiceName), UDDIUtilities.extractNamespace(bs));

            newlyCreatedSet.add(uddiBs);
        }

        //were any service keys asked for not found? - could happen if something else deletes from UDDI instead of us
        final Set<String> keysDeletedViaUDDI = new HashSet<String>();
        final Set<String> receivedFromUDDISet = new HashSet<String>();
        for (Pair<BusinessService, Map<String, TModel>> aServiceAndModel : uddiServicesToDependentModels) {
            receivedFromUDDISet.add(aServiceAndModel.left.getServiceKey());
        }

        //these are the keys we asked for
        for (String s : publishedServiceKeys) {
            //and these are they keys we got - uddiServicesToDependentModels
            if (!receivedFromUDDISet.contains(s)) keysDeletedViaUDDI.add(s);
        }
        deleteSet.addAll(keysDeletedViaUDDI);

        return new Pair<Set<String>, Set<UDDIBusinessService>>(Collections.unmodifiableSet(deleteSet), Collections.unmodifiableSet(newlyCreatedSet));
    }

    private String getUniqueKeyForService(BusinessService bs) {
        final String nameSpace = UDDIUtilities.extractNamespace(bs);
        final String serviceName = bs.getName().get(0).getValue();
        final String uniqueKey = (nameSpace == null) ? serviceName : serviceName + "_" + nameSpace;
        return uniqueKey;
    }

    /**
     * @param serviceNameToDependentTModels
     * @param registrySpecificMetaData      if not null, the registry specific meta data will be added appropriately to
     *                                      each uddi piece of infomation published
     * @return never null, can be empty
     * @throws UDDIException
     */
    private Set<Pair<String, BusinessService>> publishServicesToUDDI(
            final List<Pair<BusinessService, Map<String, TModel>>> serviceNameToDependentTModels,
            final UDDIRegistrySpecificMetaData registrySpecificMetaData) throws UDDIException {

        final Set<Pair<String, BusinessService>> serviceKeysToNewlyPublishedServices = new HashSet<Pair<String, BusinessService>>();

        for (Pair<BusinessService, Map<String, TModel>> serviceAndModels : serviceNameToDependentTModels) {
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
            UDDIUtilities.updateBusinessServiceReferences(serviceAndModels.left, Collections.unmodifiableMap(dependentTModels));

            addRegistrySpecifcMetaToBusinessService(registrySpecificMetaData, serviceAndModels.left);

            final Pair<String, BusinessService> servicePair =
                    publishBusinessService(
                            serviceAndModels.left,
                            Collections.unmodifiableCollection(dependentTModels.values()),
                            Collections.unmodifiableSet(serviceKeysToNewlyPublishedServices));
            if (servicePair != null) {
                serviceKeysToNewlyPublishedServices.add(servicePair);
            }
        }

        return Collections.unmodifiableSet(serviceKeysToNewlyPublishedServices);
    }

    private void addRegistrySpecifcMetaToBusinessService(final UDDIRegistrySpecificMetaData registrySpecificMetaData,
                                                         final BusinessService businessService) {
        if(registrySpecificMetaData == null) return;

        final CategoryBag categoryBag = businessService.getCategoryBag();
        //this 100% can never be null, as we should have correctly added keyed references to it previously
        if(categoryBag == null) throw new IllegalStateException("Attempt to publish a BusinessService with no categoryBag");

        final Collection<UDDIClient.UDDIKeyedReference> referenceCollection = registrySpecificMetaData.getBusinessServiceKeyedReferences();
        if(referenceCollection != null){
            for(UDDIClient.UDDIKeyedReference kr: referenceCollection){
                final KeyedReference newRef = new KeyedReference();
                newRef.setTModelKey(kr.getKey());
                newRef.setKeyName(kr.getName());
                newRef.setKeyValue(kr.getValue());
                categoryBag.getKeyedReference().add(newRef);
            }
        }

        final Collection<UDDIClient.UDDIKeyedReferenceGroup> keyedReferenceGroups = registrySpecificMetaData.getBusinessServiceKeyedReferenceGroups();
        if(keyedReferenceGroups != null){
            for(UDDIClient.UDDIKeyedReferenceGroup krg: keyedReferenceGroups){
                final KeyedReferenceGroup newGroup = new KeyedReferenceGroup();
                newGroup.setTModelKey(krg.getTModelKey());
                for(UDDIClient.UDDIKeyedReference kr: krg.getKeyedReferences()){
                    final KeyedReference newRef = new KeyedReference();
                    newRef.setTModelKey(kr.getKey());
                    newRef.setKeyName(kr.getName());
                    newRef.setKeyValue(kr.getValue());
                    newGroup.getKeyedReference().add(newRef);
                }
                categoryBag.getKeyedReferenceGroup().add(newGroup);
            }
        }
    }

    /**
     *
     * @return null if a service did not need to be created i.e. an existing one was updated, otherwise a non null pair
     * @throws UDDIException
     */
    private Pair<String, BusinessService> publishBusinessService(
            final BusinessService businessService,
            final Collection<TModel> rollbackTModelsToDelete,
            final Set<Pair<String, BusinessService>> allPublishedServicesSoFar)
            throws UDDIException {
        try {
            final boolean published = jaxWsUDDIClient.publishBusinessService(businessService);
            if (published) {
                return new Pair<String, BusinessService>(businessService.getServiceKey(), businessService);
            }
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Exception publishing BusinesService: " + e.getMessage());
            handleUDDIRollback(rollbackTModelsToDelete, allPublishedServicesSoFar, e);
            throw e;
        }

        return null;
    }

    private void handleUDDIRollback(
            final Collection<TModel> rollbackTModelsToDelete,
            final Set<Pair<String, BusinessService>> allPublishedServicesSoFar,
            final UDDIException exception) {
        //this is just a convenience method. Either rolls back tModels OR BusinessServices
        if(!rollbackTModelsToDelete.isEmpty() && !allPublishedServicesSoFar.isEmpty())
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
            } else if (!allPublishedServicesSoFar.isEmpty()){
                logger.log(Level.WARNING, "Attempting to rollback published BusinessServices");
                Set<String> keysToDelete = new HashSet<String>();
                for(Pair<String, BusinessService> keyAndObject: allPublishedServicesSoFar){
                    keysToDelete.add(keyAndObject.right.getServiceKey());
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
