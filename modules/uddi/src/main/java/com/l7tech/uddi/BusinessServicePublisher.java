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
     * Any previously published endpoints with the same publishedHostname and this.serviceOid will be deleted first
     *
     * Provides best effort commit / rollback. If any UDDIException occurs during the published an attempt to roll back
     * any previously published tModes and bindingTemplates will be made.
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
     * <p/>
     * Only soap + http endpoints will be removed from the BusinessService in UDDI
     *
     * @param serviceKey       String serviceKey of a BusinessService to overwrite. Required.
     * @param businessKey      Stirng businessKey which owns the BusinessService identified by serviceKey. Required
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

        final UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(jaxWsUDDIClient);
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
        //todo the name does not need to be changed as we no longer modify the name - see above line: modelConverter.convertWsdlToUDDIModel(allEndpointPairs, null, null);

        //todo delete from here
        Name name = new Name();
        Pair<BusinessService, Map<String, TModel>> foundPair = pairToUseList.get(0);
        name.setValue(foundPair.left.getName().get(0).getValue());
        overwriteService.getName().clear();
        overwriteService.getName().add(name);
        jaxWsUDDIClient.updateBusinessService(overwriteService);
        //todo delete to here

        serviceNameToWsdlNameMap.put(overwriteService.getName().get(0).getValue(), serviceNameAndNameSpace.left);
        //true here means that we will keep any existing bindings found on the BusinessService
        return publishToUDDI(serviceKeys, pairToUseList, serviceNameToWsdlNameMap, true, null);
    }

    /**
     * Get the set of services which should be deleted and the set of services which were published as a result
     * of publishing the WSDL
     * <p/>
     * This handles both an initial publish and a subsequent publish. For updates, existing serviceKeys are reused
     * where they represent the same wsdl:service from the wsdl
     * <p/>
     * If isOverWriteUpdate is true, then publishedServiceKeys must contain at least 1 key.
     *
     * @param businessKey              String business key of the BusinessEntity in UDDI which owns all services published
     * @param publishedServiceKeys     Set String of serviceKeys for BusinessServices which were already published at an earlier time for this WSDL. Can be empty, but not null
     * @param isOverwriteUpdate        boolean if true, then we aer updating an overwritten service. In this case we need
     *                                 to make sure we only publish a single service from the WSDL. The first serviceKey found in publishedServiceKeys is then
     *                                 assumed to be the serviceKey of the overwritten service.
     * @param registrySpecificMetaData if not null, the registry specific meta data will be added appropriately to each uddi piece of infomation published
     * @param allEndpointPairs         Collection<Pair<String, String>> all endpoints which should be published to UDDI
     *                                 as distinct endpoints for each service published. This means that a single wsdl:port may have more than one
     *                                 bindingTemplate in UDDI. The left side of each pair is String URL which will become the value of the'endPoint'
     *                                 in the accessPoint element of a bindingTemplate. The right hand side of each pair is a String WSDL URL.
     *                                 This will be included in every tModel published to UDDI as an overviewURL.
     * @return Pair Left side: Set of serviceKeys which should be deleted. Right side: set of BusinessServices published
     *         as a result of this publish / update operation
     * @throws UDDIException any problems searching / updating UDDI or with data model
     */
    public Pair<Set<String>, Set<UDDIBusinessService>> publishServicesToUDDIRegistry(
            final String businessKey,
            final Set<String> publishedServiceKeys,
            final boolean isOverwriteUpdate,
            final UDDIRegistrySpecificMetaData registrySpecificMetaData,
            final Collection<Pair<String, String>> allEndpointPairs) throws UDDIException {

        if (businessKey == null || businessKey.trim().isEmpty())
            throw new IllegalArgumentException("businessKey cannot be null or empty");
        if (publishedServiceKeys == null) throw new NullPointerException("publishedServiceKeys cannot be null");

        UDDIUtilities.validateAllEndpointPairs(allEndpointPairs);

        final WsdlToUDDIModelConverter modelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        try {
            final String prependServiceName = SyspropUtil.getString("com.l7tech.uddi.BusinessServicePublisher.prependServiceLocalName", "Layer7");
            final String appendServiceName = SyspropUtil.getString("com.l7tech.uddi.BusinessServicePublisher.appendServiceLocalName", Long.toString(serviceOid));
            if (!isOverwriteUpdate) {
                modelConverter.convertWsdlToUDDIModel(allEndpointPairs, prependServiceName, appendServiceName);
            } else {
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
     * @param flagJustIngoreMe as it says
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
        } catch (WsdlToUDDIModelConverter.NonSoapWsdlPortException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        } catch (WsdlToUDDIModelConverter.NonHttpBindingException e) {
            throw new UDDIException(e.getMessage(), ExceptionUtils.getDebugException(e));
        }

        //Get the service
        final Set<String> serviceKeys = new HashSet<String>();
        serviceKeys.add(serviceKey);
        final List<BusinessService> foundServices = jaxWsUDDIClient.getBusinessServices(serviceKeys, false);
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
     * @param dependentTModels          tModels to publish
     * @param tmodelType                TMODEL_TYPE the type of TModel we should publish.
     * @param allPublishedServicesSoFar Set of all published business services published so far. Any errors will cause an
     *                                  attempt to rollback any previously published business serviecs.
     * @return List of TModels which were successfully published
     * @throws UDDIException any exception publishing the tmodels
     */
    List<TModel> publishDependentTModels(final Map<String, TModel> dependentTModels,
                                         final UDDIUtilities.TMODEL_TYPE tmodelType,
                                         final Set<Pair<String, BusinessService>> allPublishedServicesSoFar) throws UDDIException {
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
            handleUDDIRollback(dependentTModels.values(), allPublishedServicesSoFar);
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
     * Publish a Collection of BusinessServices to UDDI. This method provides a best effort attempt to successfully
     * publish all BusinessServices and their tModels in an atomic fashion. This cannot be guaranteed. If any publish
     * fails, then an individual attempt is made to delete any previously published data. This is also best effort. If
     * errors happen then it is very probable that information will be orphaned in UDDI.
     *
     * @param publishedServiceKeys     Set String of all serviceKeys for each BusinessService which has previously been
     *                                 published for the same WSDL. Required. Cannot be null. Can be empty
     * @param wsdlServiceNameToDependentTModels
     *                                 List of Pairs containing a BusinessService and all of it's unique dependent tModels.
     *                                 Each tModel is new and should have a null tModelKey property. Required. Cannot be null or empty.
     * @param serviceToWsdlServiceName Map of String BusinessService name to the wsdl:service localname which generated the business service name.
     *                                 Each BusinessService in wsdlServiceNameToDependentTModels must contain an entry here. It is ok if two share the same entry. Required cannot be null or empty
     * @param isOverwriteUpdate        if true, if a previously published BusinessService is found, along with
     *                                 it's serviceKey, any bindings it has will also be preserved
     * @param registrySpecificMetaData if not null, the registry specific meta data will be added appropriately to each uddi piece of infomation published
     * @return Pair of two String sets. The left side is a Set of serviceKeys which represent the previously published BusinessService which are no longer required and have
     *         been deleted from the UDDI Registry. The right side is a Set of UDDIBusinessServices representing every published BusinessService for the WSDL.
     * @throws UDDIException any problems querying / updating the UDDI Registry
     */
    private Pair<Set<String>, Set<UDDIBusinessService>> publishToUDDI(
            final Set<String> publishedServiceKeys,
            final List<Pair<BusinessService, Map<String, TModel>>> wsdlServiceNameToDependentTModels,
            final Map<String, String> serviceToWsdlServiceName,
            final boolean isOverwriteUpdate,
            final UDDIRegistrySpecificMetaData registrySpecificMetaData) throws UDDIException {
        if (publishedServiceKeys == null) throw new NullPointerException("publishedServiceKeys cannot be null");
        if (wsdlServiceNameToDependentTModels == null)
            throw new NullPointerException("wsdlServiceNameToDependentTModels cannot be null");
        if (wsdlServiceNameToDependentTModels.isEmpty())
            throw new IllegalArgumentException("wsdlServiceNameToDependentTModels cannot be empty. At least one BusinessService is required to be published");
        if (serviceToWsdlServiceName == null) throw new NullPointerException("serviceToWsdlServiceName cannot be null");
        for (Pair<BusinessService, Map<String, TModel>> aBsPair : wsdlServiceNameToDependentTModels) {
            if (!serviceToWsdlServiceName.containsKey(aBsPair.left.getName().get(0).getValue()))
                throw new IllegalArgumentException("Each BusinessService's 0 index Name value contained in wsdlServiceNameToDependentTModels must exist as a key in serviceToWsdlServiceName");
        }

        final UDDIBusinessServiceDownloader serviceDownloader = new UDDIBusinessServiceDownloader(jaxWsUDDIClient);
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

        //create a map to look up the services from the wsdl to publish
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

        for (Map.Entry<String, BusinessService> entry : uddiPreviouslyPublishedServiceMap.entrySet()) {
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

        final Set<Pair<String, BusinessService>> newlyPublishedServices =
                publishServicesToUDDI(Collections.unmodifiableCollection(wsdlServiceNameToDependentTModels), registrySpecificMetaData);
        //NOTE - No UDDI interaction below here should cause a UDDIException to be thrown

        //find out what tModels need to be deleted
        //do this after initial update, so we have valid references
        if (!deleteSet.isEmpty()) {
            logger.log(Level.FINE, "Attemping to delete BusinessServices no longer referenced by Gateway's WSDL");
            for (String deleteKey : deleteSet) {
                try {
                    uddiClient.deleteBusinessServiceByKey(deleteKey);
                } catch (UDDIException e) {
                    logger.log(Level.WARNING, "Problem deleting BusinessServices: " + ExceptionUtils.getMessage(e));
                }
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
     * Publish a collection of BusinessServices to UDDI.
     * <p/>
     * A best effort attempt is made to publish all BusinessServices and their dependent tModels successfully or not at
     * all. If any errors happen individual deletes of any previously published data will be attempted, but it cannot
     * be guaranteed. Data may be orphaned in the UDDI Registry.
     * <p/>
     * Some of the BusinessServices may already exist in UDDI. In this case the BusinessService is being updated. The only
     * property which will remain of the BusinessService in UDDI is it's serviceKey. Everything else will have been
     * recreated.
     *
     * @param serviceNameToDependentTModels Collection of Pairs. Each Pair contains a BusinessService on it's left which
     *                                      will be published to UDDI. The right side is every tModel the BusinessService depends on. tModels are always
     *                                      published first. Each tModel must contain a non null tModelKey property, if it doesn't it will be ignored.
     * @param registrySpecificMetaData      UDDIRegistrySpecificMetaData of UDDI Registry specific meta data which should
     *                                      be published for Each BusinessService contained in serviceNameToDependentTModels. Not required. Only used if supplied.
     * @return Set of Pairs, where the left side is the serviceKey of a newly published BusinessService and the right side
     *         is the newly published BusinessService. This set is never null, but can be empty (i.e. only updates were done).
     *         Any BusinessServices which already existsed in UDDI will not be included in this set. Callers can use this returned
     *         value to know which BusinessServices were published to UDDI for the first time.
     * @throws UDDIException any problems updating / querying UDDI
     */
    private Set<Pair<String, BusinessService>> publishServicesToUDDI(
            
            final Collection<Pair<BusinessService, Map<String, TModel>>> serviceNameToDependentTModels,
            final UDDIRegistrySpecificMetaData registrySpecificMetaData) throws UDDIException {

        final Set<Pair<String, BusinessService>> serviceKeysToNewlyPublishedServices = new HashSet<Pair<String, BusinessService>>();

        for (Pair<BusinessService, Map<String, TModel>> serviceAndModels : serviceNameToDependentTModels) {
            Map<String, TModel> dependentTModels = serviceAndModels.right;
            //first publish TModels which represent wsdl:portType, as they have no keyedReference dependencies
            publishDependentTModels(dependentTModels, WSDL_PORT_TYPE, Collections.unmodifiableSet(serviceKeysToNewlyPublishedServices));
            final List<TModel> bindingTModels = new ArrayList<TModel>();
            for (final TModel tModel : dependentTModels.values()) {
                if (UDDIUtilities.getTModelType(tModel, true) != UDDIUtilities.TMODEL_TYPE.WSDL_BINDING) continue;
                bindingTModels.add(tModel);
            }
            if (bindingTModels.isEmpty()) throw new IllegalStateException("No binding tModels were found");

            UDDIUtilities.updateBindingTModelReferences(bindingTModels, Collections.unmodifiableMap(dependentTModels));
            //next publish TModels which represent wsdl:binding, as they are dependent on wsdl:portType tModels
            publishDependentTModels(dependentTModels, WSDL_BINDING, Collections.unmodifiableSet(serviceKeysToNewlyPublishedServices));
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
     * Publish a BusinessService to UDDI. The BusinessService may already have been published to UDDI.
     *
     * @param businessService BusinessService to publish to UDDI. Required.
     * @param rollbackTModelsToDelete
     * @param allPublishedServicesSoFar
     * @return Pair of String newly created serviceKey to the newly published BusinessService. If this value is null, then
     *         the BusinessService was though to already exist in UDDI due to it already having a non null serviceKey, which was used
     *         as the value when saving the BusinessService.
     * @throws UDDIException any problems updating UDDI
     */
    private Pair<String, BusinessService> publishBusinessService(
            final BusinessService businessService,
            final Collection<TModel> rollbackTModelsToDelete,
            final Set<Pair<String, BusinessService>> allPublishedServicesSoFar)
            throws UDDIException {
        try {
            final boolean newlyPublished = jaxWsUDDIClient.publishBusinessService(businessService);
            if (newlyPublished) {
                return new Pair<String, BusinessService>(businessService.getServiceKey(), businessService);
            }
        } catch (UDDIException e) {
            logger.log(Level.WARNING, "Exception publishing BusinesService: " + e.getMessage());
            handleUDDIRollback(rollbackTModelsToDelete, allPublishedServicesSoFar);
            throw e;
        }

        return null;
    }

    /**
     * Provides best effort to delete any published tmodels and business services from UDDI. Each item is tried
     * individually. It is possible then that some will fail / succeed. It is only a best effort to clean up UDDI.
     *
     * @param rollbackTModelsToDelete Collection of tModels to delete
     * @param allPublishedServicesSoFar Collection of BusinessServices to delete
     */
    private void handleUDDIRollback(
            final Collection<TModel> rollbackTModelsToDelete,
            final Set<Pair<String, BusinessService>> allPublishedServicesSoFar ) {
        logger.log(Level.WARNING, "handleUDDIRollback called. Num services: " + allPublishedServicesSoFar.size()+" num tmodels: " + rollbackTModelsToDelete.size());
        //Deleting a published business service will delete all it's referenced tModels
        if (!allPublishedServicesSoFar.isEmpty()){
            logger.log(Level.WARNING, "Attempting to rollback published BusinessServices");
            for(Pair<String, BusinessService> keyAndObject: allPublishedServicesSoFar){
                if(keyAndObject.right.getServiceKey() != null){
                    try {
                        uddiClient.deleteBusinessServiceByKey(keyAndObject.right.getServiceKey());
                    } catch (UDDIException e) {
                        logger.log(Level.WARNING, "Problems rolling back published Business Services with serviceKey: "
                                + keyAndObject.right.getServiceKey()+", due to: " + ExceptionUtils.getMessage(e));
                    }
                }
            }
        }

        //When allPublishedServicesSoFar is not empty, the tModel collection are only the tmodels for the service
        //which was attempted to be published. If the service failed to publish, then we need to manually delete
        //it's dependent tmodels here
        //otherwise a publish fail happened when publishing tmodels, in which case we need to delete them all if possible
        if (!rollbackTModelsToDelete.isEmpty()) {
            logger.log(Level.WARNING, "Attempting to rollback published tModels");
            for (TModel tModel : rollbackTModelsToDelete) {
                if(tModel.getTModelKey() != null){
                    try {
                        uddiClient.deleteTModel(tModel.getTModelKey());
                    } catch (UDDIException e) {
                        logger.log(Level.WARNING, "Could not rollback published tModel with tModelKey: " + tModel.getTModelKey()+", due to: " + ExceptionUtils.getMessage(e));
                    }
                }
            }
        }
    }

    private static UDDIClient buildUDDIClient( final UDDIClientConfig uddiCfg ) {
        if(uddiCfg == null) throw new NullPointerException("uddiCfg cannot be null");

        UDDIClient uddiClient = UDDIClientFactory.getInstance().newUDDIClient( uddiCfg );
        if(!(uddiClient instanceof JaxWsUDDIClient)){
            uddiClient = new GenericUDDIClient(uddiCfg.getInquiryUrl(), uddiCfg.getPublishUrl(), uddiCfg.getSubscriptionUrl(),
                    uddiCfg.getSecurityUrl(), uddiCfg.getLogin(), uddiCfg.getPassword(),
                    UDDIClientFactory.getDefaultPolicyAttachmentVersion(), uddiCfg.getTlsConfig(), uddiCfg.isCloseSession());
        }

        return uddiClient;
    }

    /**
     * Get all the bindingTemplates which should be removed. Any template which implements a wsdl:binding
     * with soap / http should be returned
     * @param templates List of BindingTemplates from which to determine which should be removed
     * @param allDependentTModels Every single tmodel keyed by it's tModelKey which the owning BusinessService is
     * known to depend on. It is only needed for it's binding tModels, if it only contained that, it would suffice
     * @return Set String of bindingKeys to delete. Can be empty, never null. Left is the set to keep, right is the set to delete
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

                boolean protocolIsSoap = UDDIUtilities.isSoapBinding(tModel);
                boolean transportIsHttp = UDDIUtilities.isHttpBinding(tModel);

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
