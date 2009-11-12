package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.lang.reflect.Proxy;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Binding;
import javax.xml.ws.Holder;
import javax.xml.ws.handler.Handler;
import javax.xml.namespace.QName;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.net.ssl.SSLContext;

/**
 * UDDIv3 client implementation using generated JAX-WS UDDI API
 */
public class GenericUDDIClient implements UDDIClient, JaxWsUDDIClient {

    //- PUBLIC

    /**
     *
     */
    @Override
    public void authenticate() throws UDDIException {
        getAuthToken();   
    }

    @Override
    public void publishBindingTemplate(final BindingTemplate bindingTemplate) throws UDDIException {
        if(bindingTemplate == null) throw new NullPointerException("bindingTemplate cannot be null");

        SaveBinding saveBinding = new SaveBinding();
        saveBinding.setAuthInfo(getAuthToken());
        saveBinding.getBindingTemplate().add(bindingTemplate);
        try {
            BindingDetail bindingDetail = getPublishPort().saveBinding(saveBinding);
            logger.log(Level.FINE, "Saved bindingTemplate with key: " + bindingDetail.getBindingTemplate().get(0).getBindingKey() +
                    " with serviceKey: " + bindingTemplate.getServiceKey());
            final String bindingKey =  bindingDetail.getBindingTemplate().get(0).getBindingKey();
            bindingTemplate.setBindingKey(bindingKey);
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error publishing binding template", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error publishing binding template", e);
        }
    }
    /**
     *
     */
    @Override
    public boolean publishBusinessService(final BusinessService businessService) throws UDDIException {
        if(businessService == null) throw new NullPointerException("businessService cannot be null");
        final String preSaveKey = businessService.getServiceKey();

        SaveService saveService = buildSaveService( getAuthToken(), businessService );
        try {
            ServiceDetail serviceDetail = getPublishPort().saveService(saveService);
            businessService.setServiceKey(serviceDetail.getBusinessService().get(0).getServiceKey());
            logger.log(Level.FINE, "Saved BusinessService with key: " + businessService.getServiceKey());
            //Caller has to make this happen by reusing serviceKeys when logically correct
            return preSaveKey == null;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error publishing business service", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error publishing business service", e);
        }
    }

    @Override
    public boolean publishTModel(TModel tModelToPublish) throws UDDIException {
        if(tModelToPublish == null) throw new NullPointerException("tModelToPublish cannot be null");
//        if(searchFirst){
//            TModelList tModelList = findMatchingTModels(tModelToPublish, false);
//            TModelInfos tModelInfos = tModelList.getTModelInfos();
//            if(tModelInfos != null && !tModelInfos.getTModelInfo().isEmpty()){
//                List<TModelInfo> allTModelInfos = tModelInfos.getTModelInfo();
//
//                //tModels published by the gateway will only have a single OverviewDoc with use type wsdlInterface
//                String tModelToPublishWsdlUrl = null;
//                for(OverviewDoc overviewDoc: tModelToPublish.getOverviewDoc()){
//                    OverviewURL overviewURL = overviewDoc.getOverviewURL();
//                    if(!overviewURL.getUseType().equals("wsdlInterface")) continue;
//                    tModelToPublishWsdlUrl = overviewURL.getValue();
//                }
//                if(tModelToPublishWsdlUrl == null) throw new IllegalStateException("tModel to publish does not have an OverviewURL of type 'wsdlInterface'");
//
//                List<TModel> matchingTModels = new ArrayList<TModel>();
//                for(TModelInfo tModelInfo: allTModelInfos){
//                    TModel foundModel = getTModel(tModelInfo.getTModelKey());
//                    if(foundModel == null) continue;
//                    //compare overviewDocs and overviewUrls
//                    boolean foundMatchingOverviewUrl = false;
//                    for(OverviewDoc overviewDoc: foundModel.getOverviewDoc()){
//                        OverviewURL overviewURL = overviewDoc.getOverviewURL();
//                        if(!overviewURL.getUseType().equals("wsdlInterface")) continue;
//                        if(!overviewURL.getValue().equalsIgnoreCase(tModelToPublishWsdlUrl)) continue;
//                        foundMatchingOverviewUrl = true;
//                    }
//                    if(foundMatchingOverviewUrl) matchingTModels.add(foundModel);
//                }
//
//                if(!matchingTModels.isEmpty()){
//                    if(matchingTModels.size() != 1)
//                        logger.log(Level.FINE, "Found " + matchingTModels.size()+" matching TModels. The first will be used");
//
//                    tModelToPublish.setTModelKey(allTModelInfos.get(0).getTModelKey());
//                    logger.log(Level.FINE, "Found matching tModel in UDDI Registry. Not publishing supplied tModel");
//                    return false;
//                }
//                //fall through to publish below
//            }
//        }
        //TModel does not exist yet in registry. Publish it

        UDDIPublicationPortType uddiPublicationPortType = getPublishPort();
        SaveTModel saveTModel = new SaveTModel();
        saveTModel.setAuthInfo(getAuthToken());
        saveTModel.getTModel().add(tModelToPublish);
        try {
            TModelDetail tModelDetail = uddiPublicationPortType.saveTModel(saveTModel);
            TModel saved = tModelDetail.getTModel().get(0);
            tModelToPublish.setTModelKey(saved.getTModelKey());
            logger.log(Level.FINE, "Published tModel to UDDI with key: " + tModelToPublish.getTModelKey());
            return true;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error publishing TModel", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error publishing TModel", e);
        }
    }

    @Override
    public String publishTModel( final String tModelKey,
                                 final String name,
                                 final String description,
                                 final Collection<UDDIKeyedReference> keyedReferences ) throws UDDIException {
        String publishedTModelKey;

        final TModel tModel = new TModel();
        tModel.setTModelKey( tModelKey );
        tModel.setName( buildName(name) );
        if ( description != null ) {
            tModel.getDescription().add( buildDescription(description) );
        }
        final CategoryBag cbag = new CategoryBag();
        tModel.setCategoryBag( cbag );

        for ( UDDIKeyedReference uddiKeyedReference : keyedReferences ) {
            cbag.getKeyedReference().add( buildKeyedReference(
                uddiKeyedReference.getKey(),
                uddiKeyedReference.getName(),
                uddiKeyedReference.getValue() ) );
        }

        final UDDIPublicationPortType uddiPublicationPortType = getPublishPort();
        final SaveTModel saveTModel = new SaveTModel();
        saveTModel.setAuthInfo(getAuthToken());
        saveTModel.getTModel().add(tModel);
        try {
            TModelDetail tModelDetail = uddiPublicationPortType.saveTModel(saveTModel);
            TModel saved = get(tModelDetail.getTModel(), "TModel", true);
            publishedTModelKey = saved.getTModelKey();
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error publishing TModel", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error publishing TModel", e);
        }

        return publishedTModelKey;
    }

    @Override
    public Collection<TModel> getTModels(Set<String> tModelKeys) throws UDDIException {
        if(tModelKeys == null) throw new NullPointerException("tModelKeys cannot be null");
        if(tModelKeys.isEmpty()){
            logger.log(Level.INFO, "tModelKeys is empty. Nothing at do");
            return Collections.emptyList();
        }

        //Turn the tModelInfo into a TModel
        GetTModelDetail getTModelDetail = new GetTModelDetail();
        getTModelDetail.setAuthInfo(getAuthToken());
        getTModelDetail.getTModelKey().addAll(tModelKeys);

        try {
            TModelDetail tModelDetail = getInquirePort().getTModelDetail(getTModelDetail);
            return tModelDetail.getTModel();
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting TModel", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting TModel", e);
        }
    }

    @Override
    public TModel getTModel(String tModelKey) throws UDDIException {
        if(tModelKey == null || tModelKey.trim().isEmpty()) throw new IllegalArgumentException("tModelKey cannot be null or empty");

        Set<String> tModelKeys = new HashSet<String>();
        tModelKeys.add(tModelKey);
        Collection<TModel> foundModels = getTModels(tModelKeys);
        if(foundModels.isEmpty()) return null;
        return foundModels.iterator().next();
    }

    @Override
    public String getBusinessEntityName(String businessKey) throws UDDIException {
        if(businessKey == null || businessKey.trim().isEmpty()) throw new IllegalArgumentException("businessKey cannot be null or empty");
        GetBusinessDetail getBusinessDetail = new GetBusinessDetail();
        getBusinessDetail.setAuthInfo(getAuthToken());
        getBusinessDetail.getBusinessKey().add(businessKey);
        try {
            BusinessDetail businessDetail = getInquirePort().getBusinessDetail(getBusinessDetail);
            List<BusinessEntity> services = businessDetail.getBusinessEntity();
            if(services.isEmpty()) return null;
            return services.get(0).getName().get(0).getValue();
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting business entity name", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting business entity name", e);
        }
    }

    /**
     * Retrieve the BusinessService with the supplied key
     *
     * @param serviceKey String serviceKey of the BusinessService to get
     * @return BusinessService of the supplied key. Null if not found
     * @throws UDDIException if any problem retireving the BusinessService from the UDDI registry
     */
    @Override
    public BusinessService getBusinessService(final String serviceKey) throws UDDIException {
        if(serviceKey == null || serviceKey.trim().isEmpty()) throw new IllegalArgumentException("serviceKey cannot be null or empty");
        
        GetServiceDetail getServiceDetail = new GetServiceDetail();
        getServiceDetail.setAuthInfo(getAuthToken());
        getServiceDetail.getServiceKey().add(serviceKey);
        try {
            ServiceDetail serviceDetail = getInquirePort().getServiceDetail(getServiceDetail);
            List<BusinessService> services = serviceDetail.getBusinessService();
            if(services.isEmpty()) return null;
            return services.get(0);
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting business service", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting business service", e);
        }
    }

    @Override
    public void deleteTModel(Set<String> tModelKeys) throws UDDIException {
        if(tModelKeys == null) throw new IllegalArgumentException("tModelKeys cannot be null or empty");
        if(tModelKeys.isEmpty()){
            logger.log(Level.FINE, "tModelKeys is empty. Nothing at do");
            return;
        }

        DeleteTModel deleteTModel = new DeleteTModel();
        deleteTModel.setAuthInfo(getAuthToken());
        deleteTModel.getTModelKey().addAll(tModelKeys);
        try {
            getPublishPort().deleteTModel(deleteTModel);
            logger.log(Level.FINE, "Deleted tModels with keys: ");
            for(String s: tModelKeys){
                logger.log(Level.FINE, "tModelKey deleted: " + s);
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error deleting TModels", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error deleting TModels", e);
        }
    }

    @Override
    public void deleteTModel(String tModelKey) throws UDDIException {
        if(tModelKey == null || tModelKey.trim().isEmpty()) throw new IllegalArgumentException("tModelKey cannot be null or empty");

        Set<String> aSet = new HashSet<String>();
        aSet.add(tModelKey);
        deleteTModel(aSet);
    }

    @Override
    public void deleteBindingTemplateFromSingleService(final Set<String> bindingKeys) throws UDDIException {
        if(bindingKeys == null) throw new IllegalArgumentException("bindingKeys cannot be null or empty");
        if(bindingKeys.isEmpty()){
            logger.log(Level.FINE, "bindingKeys is empty. Nothing at do");
            return;
        }
        
        final GetBindingDetail getBindingDetail = new GetBindingDetail();
        getBindingDetail.setAuthInfo(getAuthToken());
        getBindingDetail.getBindingKey().addAll(bindingKeys);
        final BindingDetail bindingDetail;
        final Set<String> tModelKeysToDelete = new HashSet<String>();
        String owningServiceKey = null;
        try {
            bindingDetail = getInquirePort().getBindingDetail(getBindingDetail);
            if(bindingDetail.getBindingTemplate().isEmpty()){
                logger.log(Level.FINE, "No bindingTemplates found for binding keys");
                return;
            }
            for (BindingTemplate bt : bindingDetail.getBindingTemplate()) {
                if(owningServiceKey == null) owningServiceKey = bt.getServiceKey();
                for(TModelInstanceInfo to: bt.getTModelInstanceDetails().getTModelInstanceInfo()){
                    tModelKeysToDelete.add(to.getTModelKey());
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting binding details", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting binding details", e);
        }

        //Get the service, we will delete actual bindings through the service only
        //this allows us to manage the case when no bindings are left, as ActiveSOA will not allow the final delete
        //of a binding, when no more remain using the DeleteBinding api

        if(owningServiceKey == null) {
            logger.log(Level.WARNING, "Cannot delete binding as cannot find it's owning service with serviceKey: " + owningServiceKey);
            return;
        }

        final GetServiceDetail getServiceDetail = new GetServiceDetail();
        getServiceDetail.setAuthInfo(getAuthToken());
        getServiceDetail.getServiceKey().add(owningServiceKey);
        final ServiceDetail serviceDetail;
        try {
            serviceDetail = getInquirePort().getServiceDetail(getServiceDetail);
            if(serviceDetail.getBusinessService().isEmpty()){
                logger.log(Level.WARNING, "No owning service for bindingTemplate found with serviceKey: " + owningServiceKey);
                return;
            }
        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception getting BusinessService with key: " + owningServiceKey+" " +
                    "to check if any bindingTemplate elements remain", drfm);
            logger.log(Level.WARNING, msg);
            return;
        }

        final BusinessService owningService = serviceDetail.getBusinessService().get(0);
        final List<BindingTemplate> bindingTemplatesToKeep = new ArrayList<BindingTemplate>();
        final BindingTemplates owningSvcTemplates = owningService.getBindingTemplates();

        if(owningSvcTemplates != null){
            for(final BindingTemplate bt: owningSvcTemplates.getBindingTemplate()){
                if(!bindingKeys.contains(bt.getBindingKey())){
                    bindingTemplatesToKeep.add(bt);
                }
            }
        }

        if(bindingTemplatesToKeep.isEmpty()){
            owningService.setBindingTemplates(null);//none left
        }else if(owningSvcTemplates != null){
            owningSvcTemplates.getBindingTemplate().clear();
            owningSvcTemplates.getBindingTemplate().addAll(bindingTemplatesToKeep);
        }

        final SaveService saveService = new SaveService();
        saveService.setAuthInfo(getAuthToken());
        saveService.getBusinessService().add(owningService);
        try {
            final ServiceDetail serviceDetail1 = getPublishPort().saveService(saveService);
            serviceDetail1.getBusinessService().get(0);
        } catch (DispositionReportFaultMessage dispositionReportFaultMessage) {
            logger.log(Level.WARNING, "Found no BusinessService for serviceKey: " + owningServiceKey);
        }

        if (!tModelKeysToDelete.isEmpty()) deleteTModel(tModelKeysToDelete);
    }

    @Override
    public void deleteBindingTemplate(String bindingKey) throws UDDIException {
        if(bindingKey == null || bindingKey.trim().isEmpty()) throw new IllegalArgumentException("bindingKey cannot be null or empty");
        Set<String> deleteSet = new HashSet<String>();
        deleteSet.add(bindingKey);
        deleteBindingTemplateFromSingleService(deleteSet);
    }

    @Override
    public List<BusinessService> getBusinessServices(final Set<String> serviceKeys) throws UDDIException {
        if(serviceKeys == null) throw new NullPointerException("serviceKeys cannot be null or empty");
        if(serviceKeys.isEmpty()){
            logger.log(Level.FINE, "serviceKeys is empty. Nothing at do");
            return Collections.emptyList();
        }

        if(serviceKeys.isEmpty()) throw new IllegalArgumentException("serviceKeys cannot be null");
        final GetServiceDetail getServiceDetail = new GetServiceDetail();
        getServiceDetail.setAuthInfo(getAuthToken());
        getServiceDetail.getServiceKey().addAll(serviceKeys);

        final List<BusinessService> businessServices = new ArrayList<BusinessService>();
        try {
            final ServiceDetail serviceDetail = getInquirePort().getServiceDetail(getServiceDetail);
            if(serviceDetail.getBusinessService() != null){
                for(BusinessService businessService: serviceDetail.getBusinessService()){
                    businessServices.add(businessService);
                }
            }else{
                logger.log(Level.FINE, "No matching BusinessServices were found");
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting business services", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting business services", e);
        }

        return businessServices;
    }

    @Override
    public void deleteUDDIBusinessServices(Collection<UDDIBusinessService> businessServices) throws UDDIException {
        if(businessServices == null) throw new NullPointerException("businessServices cannot be null or empty");
        if(businessServices.isEmpty()){
            logger.log(Level.FINE, "businessServices is empty. Nothing at do");
            return;
        }

        Set<String> serviceKeys = new HashSet<String>();
        for(UDDIBusinessService businessService: businessServices){
            serviceKeys.add(businessService.getServiceKey());
        }
        deleteBusinessServicesByKey(serviceKeys);
    }

    @Override
    public void deleteBusinessServicesByKey(Collection<String> serviceKeys) throws UDDIException {
        if(serviceKeys == null) throw new NullPointerException("serviceKeys cannot be null or empty");
        if(serviceKeys.isEmpty()){
            logger.log(Level.FINE, "serviceKeys is empty. Nothing at do");
            return;
        }

        Set<String> tModelsToDelete = new HashSet<String>();
        for(String serviceKey: serviceKeys){
            tModelsToDelete.addAll(deleteBusinessService(serviceKey));
        }

        for(String tModelKey: tModelsToDelete){
            deleteTModel(tModelKey);
        }
    }

    @Override
    public Set<String> deleteBusinessService(String serviceKey) throws UDDIException {
        if(serviceKey == null || serviceKey.trim().isEmpty()) throw new IllegalArgumentException("serviceKey cannot be null or empty");
        //We need to delete any tModels it references
        //find the service
        final BusinessService businessService = getBusinessService(serviceKey);
        BindingTemplates bindingTemplates = businessService.getBindingTemplates();

        Set<String> tModelsToDelete = new HashSet<String>();

        for(BindingTemplate bindingTemplate: bindingTemplates.getBindingTemplate()){

            //the binding template references both the wsdl:portType and wsdl:binding tModels
            TModelInstanceDetails tModelInstanceDetails = bindingTemplate.getTModelInstanceDetails();
            for(TModelInstanceInfo tModelInstanceInfo: tModelInstanceDetails.getTModelInstanceInfo()){
                tModelsToDelete.add(tModelInstanceInfo.getTModelKey());
            }
        }

        //Now delete the service followed by it's dependent tModels
        //this is not strictly required, but seems like the logical approach, as the services refer to the tModels,
        //so deleting the service first seems more correct
        try {
            final DeleteService deleteService = new DeleteService();
            deleteService.setAuthInfo(getAuthToken());
            deleteService.getServiceKey().add(serviceKey);
            
            final UDDIPublicationPortType publicationPortType = getPublishPort();
            publicationPortType.deleteService(deleteService);
            logger.log(Level.FINE, "Deleted service with key: " + businessService.getServiceKey());

//            for(String tModelKey: tModelsToDelete){
//                deleteTModel(tModelKey);
//            }

        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error deleting business service '"+serviceKey+"'", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error deleting business service '"+serviceKey+"'", e);
        }

        return tModelsToDelete;
    }

    /**
     *
     */
    @Override
    public String publishPolicy(final String tModelKey,
                                final String name,
                                final String description,
                                final String url) throws UDDIException {
        validateKey(tModelKey);
        validateName(name);
        validateDescription(description);
        validateKeyValue(url);

        // create a tmodel to save
        TModel tmodel = new TModel();
        CategoryBag cbag = new CategoryBag();
        cbag.getKeyedReference().add(buildKeyedReference(tModelKeyPolicyType, POLICY_TYPE_KEY_NAME, POLICY_TYPE_KEY_VALUE));
        cbag.getKeyedReference().add(buildKeyedReference(tModelKeyRemotePolicyReference, description, url));

        tmodel.setTModelKey(tModelKey);
        tmodel.setName(buildName(name));
        tmodel.setCategoryBag(cbag);
        tmodel.getOverviewDoc().add(buildOverviewDoc(url));
        tmodel.getDescription().add(buildDescription(description));

        try {
            UDDIPublicationPortType publicationPort = getPublishPort();
            SaveTModel saveTModel = new SaveTModel();
            saveTModel.setAuthInfo(getAuthToken());
            saveTModel.getTModel().add(tmodel);
            TModelDetail tModelDetail = publicationPort.saveTModel(saveTModel);
            TModel saved = get(tModelDetail.getTModel(), "policy technical model", true);

            return saved.getTModelKey();
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error publishing model", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error publishing model", e);
        }
    }

    @Override
    public Collection<UDDINamedEntity> listBusinessEntities(String businessName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        validateName(businessName);
        Collection<UDDINamedEntity> businesses = new ArrayList<UDDINamedEntity>();
        moreAvailable = false;

        try {
            String authToken = getAuthToken();
            Name[] names = buildNames( businessName );
            UDDIInquiryPortType inquiryPort = getInquirePort();

            FindBusiness findBusiness = new FindBusiness();
            findBusiness.setAuthInfo(authToken);
            if (maxRows>0)
                findBusiness.setMaxRows(maxRows);
            if (offset>0)
                findBusiness.setListHead(offset);
            findBusiness.setFindQualifiers(buildFindQualifiers(businessName, caseSensitive));
            if (names != null)
                findBusiness.getName().addAll(Arrays.asList(names));

            BusinessList businessList = inquiryPort.findBusiness(findBusiness);

            // check if any more results
            ListDescription listDescription = businessList.getListDescription();
            setMoreAvailable(listDescription);

            // process
            if ( businessList.getBusinessInfos() != null ) {
                for (BusinessInfo businessInfo : businessList.getBusinessInfos().getBusinessInfo() ) {
                    UDDINamedEntity namedEntity = new UDDINamedEntityImpl(businessInfo.getBusinessKey(), businessInfo.getName().get(0).getValue());
                    businesses.add(namedEntity);
                }
            }
            return businesses;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing businesses", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error listing businesses", e);
        }
    }

    /**
     * //TODO this entire method needs to be converted into only requiring 2 single searches of UDDI, which is possible
     */
    @Override
    public Collection<WsdlPortInfo> listServiceWsdls(final String servicePattern,
                                                     final boolean caseSensitive,
                                                     final int offset,
                                                     final int maxRows) throws UDDIException {
        validateName(servicePattern);

        Collection<WsdlPortInfo> allWsdlPorts = new ArrayList<WsdlPortInfo>();
        moreAvailable = false;

        try {
            String authToken = getAuthToken();

            FindService findService = buildFindService(servicePattern, caseSensitive, offset, maxRows);
            UDDIInquiryPortType inquiryPort = getInquirePort();
            ServiceList serviceList = inquiryPort.findService(findService);

            // check if any more results
            ListDescription listDescription = serviceList.getListDescription();
            setMoreAvailable(listDescription);

            // process
            if (serviceList.getServiceInfos() != null) {
                //Get the actual service, so we can get it's local name => the wsdl:service unique name
                final GetServiceDetail getServiceDetail = new GetServiceDetail();
                getServiceDetail.setAuthInfo(authToken);
                for (ServiceInfo serviceInfo : serviceList.getServiceInfos().getServiceInfo()) {
                    getServiceDetail.getServiceKey().add(serviceInfo.getServiceKey());
                }

                final ServiceDetail serviceDetail = getInquirePort().getServiceDetail(getServiceDetail);
                List<BusinessService> services = serviceDetail.getBusinessService();
                if (services.isEmpty()) {
                    logger.log(Level.WARNING, "Could not find BusinessService's from UDDI");
                    return allWsdlPorts;
                }

                final Map<String, BusinessService> keyToService = new HashMap<String, BusinessService>();
                for (BusinessService businessService : services) {
                    keyToService.put(businessService.getServiceKey(), businessService);
                }

                for (ServiceInfo serviceInfo : serviceList.getServiceInfos().getServiceInfo()) {

                    FindBinding findBinding = buildFindBinding(serviceInfo);

                    // Find tModel keys for the WSDL portType/binding
                    BindingDetail bd = inquiryPort.findBinding(findBinding);

                    final String businessServiceKey = serviceInfo.getServiceKey();
                    final String businessServiceName = get(serviceInfo.getName(), "BusinessService Name", false).getValue();
                    final String businessEntityKey = serviceInfo.getBusinessKey();

                    String businessServiceWsdlLocalName = null;
                    //get the wsdl:service name from the local name keyed reference
                    CategoryBag categoryBag = keyToService.get(serviceInfo.getServiceKey()).getCategoryBag();
                    List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
                    for (KeyedReference keyedReference : keyedReferences) {
                        if (keyedReference.getTModelKey().equalsIgnoreCase(WsdlToUDDIModelConverter.UDDI_XML_LOCALNAME)) {
                            //fyi - this helps prevent circular references on save - our published business services
                            //value for local name will never match the wsdl by default
                            businessServiceWsdlLocalName = keyedReference.getKeyValue();
                        }
                    }

                    //for every valid BindingTemplate we will add one WsdlPortInfo to the allWsdlPorts collection
                    for (BindingTemplate bindingTemplate : bd.getBindingTemplate()) {
                        List<String> modelInstanceUrl = new ArrayList<String>();

                        //return a WSDLInfo per BindingTemplate - as it maps 1:1 to wsdl:port
                        AccessPoint accessPoint = bindingTemplate.getAccessPoint();

                        //for systinet, if accessPoint.getUseType is null, we will just use the url value
                        //checking for version 2 implementations of techinca note - supporting "http" aswell for oracle
                        if (bindingTemplate.getTModelInstanceDetails() == null ||
                                accessPoint == null ||
                                (accessPoint.getUseType() != null &&
                                        !accessPoint.getUseType().trim().equals("") &&
                                        (!accessPoint.getUseType().equals( USE_TYPE_END_POINT ) && !accessPoint.getUseType().trim().equals("http")))) {
                            logger.log(Level.INFO, "Invalid / unsupported accessPoint useType found: " + accessPoint.getUseType());
                            continue;
                        }

                        final String accessPointURL = accessPoint.getValue();
                        //this should only ever contain two keys
                        List<String> modelKeys = new ArrayList<String>();

                        String instanceParamForWsdlBinding = null;
                        String bindingTModelKeyToValidate = null;

                        for (TModelInstanceInfo tmii : bindingTemplate.getTModelInstanceDetails().getTModelInstanceInfo()) {
                            modelKeys.add(tmii.getTModelKey());

                            // bug 5330 - workaround for Centrasite problem with fetching wsdl
                            String tmiiWsdlUrl = extractOverviewUrl(tmii);
                            if (tmiiWsdlUrl != null) {
                                modelInstanceUrl.add(tmiiWsdlUrl);
                            }

                            //we need the instance param from the instance of for the wsdl:binding so we know what
                            //the name of the wsdl:port is
                            InstanceDetails instanceDetails = tmii.getInstanceDetails();
                            if (instanceDetails == null) continue;
                            instanceParamForWsdlBinding = instanceDetails.getInstanceParms();
                            bindingTModelKeyToValidate = tmii.getTModelKey();

                        }

                        if (modelKeys.isEmpty() || modelKeys.size() < 2) {
                            logger.log(Level.INFO,
                                    "Not including binding in results as it contains less than 2 tModels so is not spec " +
                                            "conformant. serviceKey: " + serviceInfo.getServiceKey());
                            continue;
                        }

                        //Get all the TModels
                        final GetTModelDetail getTModels = new GetTModelDetail();
                        getTModels.setAuthInfo(authToken);
                        getTModels.getTModelKey().addAll(modelKeys);
                        final TModelDetail tmd = inquiryPort.getTModelDetail(getTModels);
                        final List<TModel> resolvedTMomdels = tmd.getTModel();
                        if (resolvedTMomdels.isEmpty() || resolvedTMomdels.size() < 2) {
                            logger.log(Level.INFO,
                                    "Not including binding in results as we could not resolve at least 2 tModels " +
                                            "serviceKey: " + serviceInfo.getServiceKey());
                            continue;
                        }

                        //extract the specific tModels - should only be one of each
                        TModel bindingTModel = null;
                        TModel portTypeTModel = null;
                        for (final TModel tModel : resolvedTMomdels) {
                            UDDIUtilities.TMODEL_TYPE tModelType = UDDIUtilities.getTModelType(tModel, false);
                            if(tModelType == null) continue;
                            switch (tModelType) {
                                case WSDL_BINDING:
                                    bindingTModel = tModel;
                                    break;
                                case WSDL_PORT_TYPE:
                                    portTypeTModel = tModel;
                                    break;
                            }
                        }

                        if (bindingTModel == null) {
                            logger.log(Level.INFO,
                                    "Not including binding in results as we could not find the wsdl:binding tModel " +
                                            "serviceKey: " + serviceInfo.getServiceKey());
                            continue;
                        }

                        if (portTypeTModel == null) {
                            logger.log(Level.INFO,
                                    "Not including binding in results as we could not find the wsdl:portType tModel " +
                                            "serviceKey: " + serviceInfo.getServiceKey());
                            continue;
                        }

                        if (!bindingTModel.getTModelKey().equals(bindingTModelKeyToValidate)) {
                            logger.log(Level.INFO,
                                    "Invalid instanceParam value found for binding tModel with key: " + bindingTModelKeyToValidate);
                            continue;
                        }

                        final WsdlPortInfoImpl wsdlPortInfo = new WsdlPortInfoImpl();
                        wsdlPortInfo.setBusinessEntityKey(businessEntityKey);
                        wsdlPortInfo.setBusinessServiceKey(businessServiceKey);
                        wsdlPortInfo.setBusinessServiceName(businessServiceName);
                        wsdlPortInfo.setWsdlServiceName(businessServiceWsdlLocalName);
                        //the instance param value from the bindingTemplate is the name of the wsdl:port
                        wsdlPortInfo.setWsdlPortName(instanceParamForWsdlBinding);
                        wsdlPortInfo.setWsdlPortBinding(bindingTModel.getName().getValue());
                        wsdlPortInfo.setAccessPointURL(accessPointURL);

                        // Get the WSDL url
                        if (!modelInstanceUrl.isEmpty()) {
                            wsdlPortInfo.setWsdlUrl(modelInstanceUrl.get(0));
                        } else if (!modelKeys.isEmpty()) {
                            for (OverviewDoc doc : bindingTModel.getOverviewDoc()) {
                                OverviewURL url = doc.getOverviewURL();

                                if (url != null && OVERVIEW_URL_TYPE_WSDL.equalsIgnoreCase(url.getUseType())) {
                                    wsdlPortInfo.setWsdlUrl(url.getValue());
                                }
                            }
                        }

                        //check everything required is found, otherwise don't add it to collection
                        final String validateMsg = wsdlPortInfo.validate();
                        if (validateMsg != null) {
                            logger.log(Level.INFO,
                                    "Ignoring bindingTemplate from serviceKey: " + serviceInfo.getServiceKey() +
                                            " as some required information was not found: " + validateMsg);
                            continue;
                        }
                        allWsdlPorts.add(wsdlPortInfo);
                    }
                }
            }

            return allWsdlPorts;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing services", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error listing services", e);
        }
    }

    /**
     *
     */
    @Override
    public Collection<UDDINamedEntity> listServices(final String servicePattern,
                                                    final boolean caseSensitive,
                                                    final int offset,
                                                    final int maxRows) throws UDDIException {
        validateName(servicePattern);

        Collection<UDDINamedEntity> services = new ArrayList<UDDINamedEntity>();
        moreAvailable = false;

        try {
            String authToken = getAuthToken();
            Name[] names = buildNames( servicePattern );

            UDDIInquiryPortType inquiryPort = getInquirePort();
            
            // find service
            FindService findService = new FindService();
            findService.setAuthInfo(authToken);
            if (maxRows>0)
                findService.setMaxRows(maxRows);
            if (offset>0)
                findService.setListHead(offset);
            findService.setFindQualifiers(buildFindQualifiers(servicePattern, caseSensitive));
            if (names != null)
                findService.getName().addAll(Arrays.asList(names));

            // WSDL services only
            KeyedReference keyedReference = new KeyedReference();
            keyedReference.setKeyValue(WSDL_TYPES_SERVICE);
            keyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
            CategoryBag cb2 = new CategoryBag();
            cb2.getKeyedReference().add(keyedReference);
            findService.setCategoryBag(cb2);

            ServiceList serviceList = inquiryPort.findService(findService);

            // check if any more results
            ListDescription listDescription = serviceList.getListDescription();
            setMoreAvailable(listDescription);

            // display those services in the list instead
            ServiceInfos serviceInfos = serviceList.getServiceInfos();
            if (serviceInfos != null) {
                for (ServiceInfo serviceInfo : serviceInfos.getServiceInfo()) {
                    String name = get(serviceInfo.getName(), "service name", false).getValue();
                    services.add(new UDDINamedEntityImpl(serviceInfo.getServiceKey(), name));
                }
            }
            
            return services;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing services", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error listing services", e);
        }
    }

    /**
     *
     */
    @Override
    public Collection<UDDINamedEntity> listEndpoints(final String servicePattern,
                                                     final boolean caseSensitive,
                                                     final int offset,
                                                     final int maxRows) throws UDDIException {
        validateName(servicePattern);        

        Collection<UDDINamedEntity> endpoints = new ArrayList<UDDINamedEntity>();

        try {
            Collection<UDDINamedEntity> services = listServices(servicePattern, caseSensitive, offset, maxRows);

            String authToken = getAuthToken();

            UDDIInquiryPortType inquiryPort = getInquirePort();

            for (UDDINamedEntity info : services) {
                // find binding
                FindBinding findBinding = new FindBinding();
                findBinding.setAuthInfo(authToken);
                findBinding.setServiceKey(info.getKey());
                BindingDetail bindingDetail = inquiryPort.findBinding(findBinding);

                // display those services in the list instead
                List<BindingTemplate> bindingTemplates = bindingDetail.getBindingTemplate();
                if (bindingTemplates != null) {
                    for (BindingTemplate bindingTemplate : bindingTemplates) {
                        String name = bindingTemplate.getAccessPoint().getValue();
                        endpoints.add(new UDDINamedEntityImpl(bindingTemplate.getBindingKey(), name));
                    }
                }
            }

            return endpoints;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing endpoints", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error listing endpoints", e);
        }
    }


    /**
     *
     */
    @Override
    public Collection<UDDINamedEntity> listOrganizations(final String organizationPattern,
                                                         final boolean caseSensitive,
                                                         final int offset,
                                                         final int maxRows) throws UDDIException {
        validateName(organizationPattern);

        Collection<UDDINamedEntity> services = new ArrayList<UDDINamedEntity>();
        moreAvailable = false;

        try {
            String authToken = getAuthToken();
            Name[] names = buildNames( organizationPattern );

            UDDIInquiryPortType inquiryPort = getInquirePort();

            // find organization
            FindBusiness findBusiness = new FindBusiness();
            findBusiness.setAuthInfo(authToken);
            if (maxRows>0)
                findBusiness.setMaxRows(maxRows);
            if (offset>0)
                findBusiness.setListHead(offset);
            findBusiness.setFindQualifiers(buildFindQualifiers(organizationPattern, caseSensitive));
            if (names != null)
                findBusiness.getName().addAll(Arrays.asList(names));
            BusinessList uddiBusinessListRes = inquiryPort.findBusiness(findBusiness);

            // check if any more results
            ListDescription listDescription = uddiBusinessListRes.getListDescription();
            setMoreAvailable(listDescription);

            // display those services in the list instead
            BusinessInfos businessInfos = uddiBusinessListRes.getBusinessInfos();
            if (businessInfos != null) {
                for (BusinessInfo businessInfo : businessInfos.getBusinessInfo()) {
                    String name = get(businessInfo.getName(), "business name", false).getValue();
                    services.add(new UDDINamedEntityImpl(businessInfo.getBusinessKey(), name));
                }
            }

            return services;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing businesses", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error listing businesses", e);
        }
    }

    /**
     * Note that this currently does not list policies that are remotely
     * attached (only ones with tModels)
     */
    @Override
    public Collection<UDDINamedEntity> listPolicies(final String policyPattern,
                                                    final String policyUrl) throws UDDIException {
        validateName(policyPattern);
        validateKeyValue(policyUrl);

        List<UDDINamedEntity> policies = new ArrayList<UDDINamedEntity>();
        moreAvailable = false;
        try {
            String authToken = getAuthToken();
            Integer maxRows = 100;

            CategoryBag cbag = new CategoryBag();
            cbag.getKeyedReference().add(buildKeyedReference(tModelKeyPolicyType, null, POLICY_TYPE_KEY_VALUE));
            if (policyUrl != null && policyUrl.trim().length() > 0)
                cbag.getKeyedReference().add(buildKeyedReference(tModelKeyRemotePolicyReference, null, policyUrl));
            Name name = null;
            FindQualifiers findQualifiers = null;
            if (policyPattern != null && policyPattern.trim().length() > 0) {
                // if approximate match is used for the URL then CentraSite will perform a prefix match ...
                findQualifiers = buildFindQualifiers(policyPattern, false);
                name = buildName(policyPattern);
            }

            UDDIInquiryPortType inquiryPort = getInquirePort();
            
            // find policy tmodel(s)
            FindTModel findTModel = new FindTModel();
            findTModel.setAuthInfo(authToken);
            findTModel.setMaxRows(maxRows);
            findTModel.setFindQualifiers(findQualifiers);
            findTModel.setName(name);
            findTModel.setCategoryBag(cbag);
            TModelList tModelList = inquiryPort.findTModel(findTModel);

            // check if any more results
            ListDescription listDescription = tModelList.getListDescription();
            setMoreAvailable(listDescription);

            List<String> policyKeys = new ArrayList<String>();
            TModelInfos tModelInfos = tModelList.getTModelInfos();
            if (tModelInfos != null) {
                for (TModelInfo tModel : tModelInfos.getTModelInfo()) {
                    if (tModel.getName() != null) {
                        String key = tModel.getTModelKey();
                        policyKeys.add(key);
                        String tmodelname = tModel.getName().getValue();
                        policies.add(new UDDINamedEntityImpl(key, tmodelname));
                    }
                }
            }

            // Get policy urls the named info
            if ( !policyKeys.isEmpty() ) {
                GetTModelDetail getTModelDetail = new GetTModelDetail();
                getTModelDetail.setAuthInfo(authToken);
                getTModelDetail.getTModelKey().addAll(policyKeys);
                TModelDetail res = inquiryPort.getTModelDetail(getTModelDetail);
                if (res != null) {
                    for (TModel tModel : res.getTModel()) {
                        CategoryBag categoryBag = tModel.getCategoryBag();
                        if (categoryBag == null)
                            continue;

                        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
                        if (keyedReferences == null || keyedReferences.isEmpty())
                            continue;

                        for (KeyedReference keyedReference : keyedReferences) {
                            if (keyedReference.getTModelKey().equals(tModelKeyRemotePolicyReference)) {
                                mergePolicyUrlToInfo(
                                        tModel.getTModelKey(),
                                        keyedReference.getKeyValue(),
                                        policies);
                                break;
                            }
                        }
                    }
                }
            }

            return policies;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policies", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error listing policies", e);
        }
    }

    /**
     *
     */
    @Override
    public boolean listMoreAvailable() {
        return moreAvailable;
    }

    @Override
    public String getBindingKeyForService( final String uddiServiceKey ) throws UDDIException {
        validateKey(uddiServiceKey);

        String bindingKey = null;
        try {
            String authToken = getAuthToken();
            UDDIInquiryPortType inquiryPort = getInquirePort();

            GetServiceDetail getServiceDetail = new GetServiceDetail();
            getServiceDetail.setAuthInfo(authToken);
            getServiceDetail.getServiceKey().add(uddiServiceKey);
            ServiceDetail detail = inquiryPort.getServiceDetail(getServiceDetail);

            List<BusinessService> services = detail.getBusinessService();
            if ( services != null && !services.isEmpty() ) {
                BusinessService service = services.get( 0 );
                if ( service != null ) {
                    BindingTemplates bindingTemplates = service.getBindingTemplates();
                    if ( bindingTemplates != null ) {
                        List<BindingTemplate> bindingTemplateList = bindingTemplates.getBindingTemplate();
                        if ( bindingTemplateList != null ) {
                            for ( BindingTemplate bindingTemplate : bindingTemplateList ) {
                                if ( bindingTemplate.getAccessPoint() != null &&
                                     USE_TYPE_END_POINT.equals(bindingTemplate.getAccessPoint().getUseType()) ) {
                                    bindingKey = bindingTemplate.getBindingKey();
                                    break;
                                }
                            }

                            if ( bindingKey == null ) {
                                // fall back to "http" useType.
                                for ( BindingTemplate bindingTemplate : bindingTemplateList ) {
                                    if ( bindingTemplate.getAccessPoint() != null &&
                                         "http".equals(bindingTemplate.getAccessPoint().getUseType()) ) {
                                        bindingKey = bindingTemplate.getBindingKey();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting binding key for service", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting binding key for service", e);
        }

        return bindingKey;
    }

    /**
     *
     */
    @Override
    public String getPolicyUrl(final String policyKey) throws UDDIException {
        validateKey(policyKey);

        String policyURL = null;
        try {
            String authToken = getAuthToken();
            
            // get the policy url and try to fetch it
            UDDIInquiryPortType inquiryPort = getInquirePort();

            GetTModelDetail getTModelDetail = new GetTModelDetail();
            getTModelDetail.setAuthInfo(authToken);
            getTModelDetail.getTModelKey().add(policyKey);
            TModelDetail res = inquiryPort.getTModelDetail(getTModelDetail);
            if (res != null) {
                TModel tModel = get(res.getTModel(), "policy technical model", true);

                CategoryBag categoryBag = tModel.getCategoryBag();
                if (categoryBag == null)
                    throw new UDDIException("Invalid policy '"+policyKey+"' (missing category bag).");

                List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
                if (keyedReferences == null || keyedReferences.isEmpty())
                    throw new UDDIException("Invalid policy '"+policyKey+"' (missing policy reference).");

                for (KeyedReference keyedReference : keyedReferences) {
                    if (keyedReference.getTModelKey().equals(tModelKeyRemotePolicyReference)) {
                        policyURL = keyedReference.getKeyValue();
                        break;
                    }
                }
                if (policyURL == null) {
                    throw new UDDIException("Invalid policy '"+policyKey+"' (policy reference not found).");
                }
            } else {
                throw new UDDIException("ERROR get_tModelDetail returned zero or multiple tModels");
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting policy URL", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting policy URL", e);
        }

        return policyURL;
    }

    /**
     *
     */
    @Override
    public Collection<String> listPolicyUrlsByEndpoint(final String key) throws UDDIException {
        validateKey(key);
        
        Collection<String> policyUrls = new ArrayList<String>();
        try {
            String authToken = getAuthToken();

            // get the policy url and try to fetch it
            UDDIInquiryPortType inquiryPort = getInquirePort();

            GetBindingDetail getBindingDetail = new GetBindingDetail();
            getBindingDetail.setAuthInfo(authToken);
            getBindingDetail.getBindingKey().add(key);
            BindingDetail detail = inquiryPort.getBindingDetail(getBindingDetail);

            extractPolicyUrls(get(detail.getBindingTemplate(), "service endpoint", true).getCategoryBag(), policyUrls);
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting policy URL", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting policy URL", e);
        }

        return policyUrls;
    }

    /**
     *
     */
    @Override
    public Collection<String> listPolicyUrlsByOrganization(final String key) throws UDDIException {
        validateKey(key);

        Collection<String> policyUrls = new ArrayList<String>();
        try {
            String authToken = getAuthToken();

            // get the policy url and try to fetch it
            UDDIInquiryPortType inquiryPort = getInquirePort();

            GetBusinessDetail getBusinessDetail = new GetBusinessDetail();
            getBusinessDetail.setAuthInfo(authToken);
            getBusinessDetail.getBusinessKey().add(key);
            BusinessDetail detail = inquiryPort.getBusinessDetail(getBusinessDetail);

            extractPolicyUrls(get(detail.getBusinessEntity(), "business", true).getCategoryBag(), policyUrls);
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policy URLs for organization '"+key+"'", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error listing policy URLs for organization '"+key+"'", e);
        }

        return policyUrls;
    }

    /**
     *
     */
    @Override
    public Collection<String> listPolicyUrlsByService(final String key) throws UDDIException {
        validateKey(key);

        Collection<String> policyUrls = new ArrayList<String>();
        try {
            String authToken = getAuthToken();

            // get the policy url and try to fetch it
            UDDIInquiryPortType inquiryPort = getInquirePort();

            GetServiceDetail getServiceDetail = new GetServiceDetail();
            getServiceDetail.setAuthInfo(authToken);
            getServiceDetail.getServiceKey().add(key);
            ServiceDetail detail = inquiryPort.getServiceDetail(getServiceDetail);

            extractPolicyUrls(get(detail.getBusinessService(), "service", true).getCategoryBag(), policyUrls);
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing policy URLs for service '"+key+"'", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error listing policy URLs for service '"+key+"'", e);
        }

        return policyUrls;
    }

    /**
     * 
     */
    @Override
    public void referencePolicy(final String serviceKey,
                                final String serviceUrl,
                                final String policyKey,
                                final String policyUrl,
                                final String description,
                                final Boolean force) throws UDDIException {
        validateKey(serviceKey);
        validateUrl(serviceUrl);
        validateKey(policyKey);
        validateKeyValue(policyUrl);
        validateDescription(description);

        // first, get service from the service key
        boolean localReference = policyKey!=null && policyKey.trim().length()>0;
        boolean remoteReference = policyUrl!=null && policyUrl.trim().length()>0;
        if (!localReference && !remoteReference)
            throw new UDDIException("No policy to attach.");

        boolean isEndpoint = serviceUrl != null &&  serviceUrl.trim().length()>0;
        
        String authToken = getAuthToken();
        ServiceDetail serviceDetail = getServiceDetail( serviceKey, authToken );

        //get the bag for the service
        BusinessService toUpdate = get(serviceDetail.getBusinessService(), "service", true);
        Collection<CategoryBag> cbags = new ArrayList<CategoryBag>();
        {
            CategoryBag cbag = toUpdate.getCategoryBag();
            if (cbag == null) {
                cbag = new CategoryBag();
                toUpdate.setCategoryBag(cbag);
            }
            cbags.add(cbag);
        }

        try {
            // check for existing references and remove them
            for (CategoryBag cbag : cbags) {
                Collection<KeyedReference> updated = new ArrayList<KeyedReference>();
                if (cbag.getKeyedReference() != null) {
                    for (KeyedReference kref : cbag.getKeyedReference()) {
                        if (kref.getTModelKey().equals(tModelKeyLocalPolicyReference) ||
                            kref.getTModelKey().equals(tModelKeyRemotePolicyReference)) {
                            if (force == null)
                                throw new UDDIExistingReferenceException(kref.getKeyValue());
                            if (!force)
                                updated.add(kref);
                        }
                        else {
                            updated.add(kref);
                        }
                    }
                }

                if ( localReference ) {
                    updated.add(buildKeyedReference(tModelKeyLocalPolicyReference, description, policyKey));
                }

                if ( remoteReference ) {
                    updated.add(buildKeyedReference(tModelKeyRemotePolicyReference, description, policyUrl));
                }

                cbag.getKeyedReference().clear();
                cbag.getKeyedReference().addAll(updated);
            }            

            // Are we updating the endpoints?
            if (isEndpoint) {
                if (toUpdate.getBindingTemplates() != null && serviceUrl != null) {
                    for (BindingTemplate bt : toUpdate.getBindingTemplates().getBindingTemplate()) {
                        bt.setAccessPoint(buildAccessPoint("http", serviceUrl));
                    }
                }
            }

            // update service in uddi
            UDDIPublicationPortType publicationPort = getPublishPort();
            publicationPort.saveService(buildSaveService( authToken, toUpdate ));
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error updating service details", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error updating service details", e);
        }
    }

    @Override
    public boolean removePolicyReference( final String serviceKey, final String policyKey, final String policyUrl ) throws UDDIException {
        boolean policyReferenceRemoved = false;

        validateKey(serviceKey);
        validateKey(policyKey);

        // first, get service from the service key
        String authToken = getAuthToken();
        ServiceDetail serviceDetail = getServiceDetail( serviceKey, authToken );

        //get the bag for the service
        BusinessService toUpdate = get(serviceDetail.getBusinessService(), "service", true);
        CategoryBag cbag = toUpdate.getCategoryBag();
        if (cbag != null) {
            for ( Iterator<KeyedReference> keyedReferenceIter = cbag.getKeyedReference().iterator(); keyedReferenceIter.hasNext(); ) {
                KeyedReference keyedReference = keyedReferenceIter.next();

                if ( policyKey != null &&
                     PolicyAttachmentVersion.isAnyLocalReference(keyedReference.getTModelKey()) &&
                     policyKey.equals( keyedReference.getKeyValue() ) ) {
                    keyedReferenceIter.remove();
                    policyReferenceRemoved = true;
                } else if ( policyUrl != null &&
                     PolicyAttachmentVersion.isAnyRemoteReference(keyedReference.getTModelKey()) &&
                     policyUrl.equals( keyedReference.getKeyValue() )) {
                    keyedReferenceIter.remove();
                    policyReferenceRemoved = true;
                }
            }

            if ( policyReferenceRemoved ) {
                // update service in uddi
                UDDIPublicationPortType publicationPort = getPublishPort();
                try {
                    publicationPort.saveService(buildSaveService( authToken, toUpdate ));
                } catch (DispositionReportFaultMessage drfm) {
                    throw buildFaultException("Error updating service details", drfm);
                } catch (RuntimeException e) {
                    throw buildErrorException("Error updating service details", e);
                }
            }
        }

        return policyReferenceRemoved;
    }

    @Override
    public void addKeyedReference( final String serviceKey,
                                   final String keyedReferenceKey,
                                   final String keyedReferenceName,
                                   final String keyedReferenceValue ) throws UDDIException {
        validateKey(serviceKey);
        validateKey(keyedReferenceKey);

        // first, get service from the service key
        String authToken = getAuthToken();
        ServiceDetail serviceDetail = getServiceDetail( serviceKey, authToken );

        //get the bag for the service
        BusinessService toUpdate = get(serviceDetail.getBusinessService(), "service", true);
        CategoryBag cbag = toUpdate.getCategoryBag();
        if (cbag == null) {
            cbag = new CategoryBag();
            toUpdate.setCategoryBag(cbag);
        }

        cbag.getKeyedReference().add( buildKeyedReference(
                keyedReferenceKey,
                keyedReferenceName,
                keyedReferenceValue ) );

        // update service in uddi
        UDDIPublicationPortType publicationPort = getPublishPort();
        try {
            publicationPort.saveService(buildSaveService( authToken, toUpdate ));
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error updating service details", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error updating service details", e);
        }
    }

    @Override
    public boolean removeKeyedReference( final String serviceKey,
                                         final String keyedReferenceKey,
                                         final String keyedReferenceName,
                                         final String keyedReferenceValue ) throws UDDIException {
        boolean referenceRemoved = false;

        validateKey(serviceKey);
        validateKey(keyedReferenceKey);

        // first, get service from the service key
        String authToken = getAuthToken();
        ServiceDetail serviceDetail = getServiceDetail( serviceKey, authToken );

        //get the bag for the service
        BusinessService toUpdate = get(serviceDetail.getBusinessService(), "service", true);
        CategoryBag cbag = toUpdate.getCategoryBag();
        if (cbag != null) {
            for ( Iterator<KeyedReference> keyedReferenceIter = cbag.getKeyedReference().iterator(); keyedReferenceIter.hasNext(); ) {
                KeyedReference keyedReference = keyedReferenceIter.next();

                if ( keyedReferenceKey.equals(keyedReference.getTModelKey()) &&
                     (keyedReferenceName == null || keyedReferenceName.equals(keyedReference.getKeyName())) &&
                     (keyedReferenceValue == null || keyedReferenceValue.equals(keyedReference.getKeyValue())) ) {
                    keyedReferenceIter.remove();
                    referenceRemoved = true;
                }
            }

            if ( referenceRemoved ) {
                // update service in uddi
                UDDIPublicationPortType publicationPort = getPublishPort();
                try {
                    publicationPort.saveService(buildSaveService( authToken, toUpdate ));
                } catch (DispositionReportFaultMessage drfm) {
                    throw buildFaultException("Error updating service details", drfm);
                } catch (RuntimeException e) {
                    throw buildErrorException("Error updating service details", e);
                }
            }
        }

        return referenceRemoved;
    }

    @Override
    public UDDIOperationalInfo getOperationalInfo( final String entityKey ) throws UDDIException {
        UDDIOperationalInfo info = null;

        Collection<UDDIOperationalInfo> uddiInfos = getOperationalInfos( entityKey );
        if ( uddiInfos != null ) {
            for ( UDDIOperationalInfo uddiInfo : uddiInfos ) {
                if ( entityKey.equals(uddiInfo.getEntityKey()) ) {
                    info = uddiInfo;
                    break;
                }
            }
        }

        if ( info == null ) {
            // This should not occur, the getOperationalInfo call should fault for invalid keys
            throw new UDDIInvalidKeyException("Entity key not found '"+entityKey+"'.");
        }

        return info;
    }

    @Override
    public Collection<UDDIOperationalInfo> getOperationalInfos( final String... entityKey ) throws UDDIException {
        Collection<UDDIOperationalInfo> uddiInfos = new ArrayList<UDDIOperationalInfo>();

        if ( entityKey != null && entityKey.length>0 ) {
            try {
                final String authToken = getAuthToken();

                final UDDIInquiryPortType inquiryPort = getInquirePort();

                // find info
                final GetOperationalInfo getOperationalInfo = new GetOperationalInfo();
                getOperationalInfo.setAuthInfo(authToken);
                getOperationalInfo.getEntityKey().addAll(Arrays.asList(entityKey));

                OperationalInfos infos = inquiryPort.getOperationalInfo(getOperationalInfo);
                if (infos != null) {
                    List<OperationalInfo> OperationalInfos = infos.getOperationalInfo();
                    for ( OperationalInfo operationalInfo : OperationalInfos ) {
                        uddiInfos.add(new UDDIOperationalInfoImpl( operationalInfo ));
                    }
                }
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error accessing entity operational information", drfm);
            } catch (RuntimeException e) {
                throw buildErrorException("Error accessing entity operational information", e);
            }
        }

        return uddiInfos;
    }

    @Override
    public String subscribe( final long expiryTime,
                             final long notificationInterval,
                             final String bindingKey ) throws UDDIException {
        String subscriptionKey = null;
        try {
            final DatatypeFactory factory = DatatypeFactory.newInstance();
            final UDDISubscriptionPortType subscriptionPort = getSubscriptionPort();

            final FindService findService = new FindService();
            KeyedReference keyedReference = new KeyedReference();
            keyedReference.setKeyValue(WSDL_TYPES_SERVICE);
            keyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
            CategoryBag categoryBag = new CategoryBag();
            categoryBag.getKeyedReference().add(keyedReference);
            findService.setCategoryBag(categoryBag);            

            final SubscriptionFilter filter = new SubscriptionFilter();
            filter.setFindService( findService );

            final GregorianCalendar expiresGC = new GregorianCalendar();
            expiresGC.setTimeInMillis( expiryTime );
            final XMLGregorianCalendar expiresAfter = factory.newXMLGregorianCalendar( expiresGC );

            final Subscription subscription = new Subscription();
            subscription.setBrief( true );
            subscription.setExpiresAfter( expiresAfter );
            subscription.setSubscriptionFilter( filter );
            if ( bindingKey != null ) {
                subscription.setBindingKey( bindingKey );
                if ( notificationInterval < 1 ) {
                    throw new UDDIException("notifcationInterval is required when bindingKey is present");
                }
                subscription.setNotificationInterval( factory.newDuration( notificationInterval ) );
            } else if ( notificationInterval > 0 ) {
                subscription.setNotificationInterval( factory.newDuration( notificationInterval ) );
            }

            Holder<List<Subscription>> holder = new Holder<List<Subscription>>( Collections.singletonList( subscription ));

            subscriptionPort.saveSubscription( getAuthToken(), holder );

            if ( !holder.value.isEmpty() ) {
                subscriptionKey = holder.value.get( 0 ).getSubscriptionKey();
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error subscribing", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error subscribing", e);
        } catch (DatatypeConfigurationException e) {
            throw new UDDIException("Error subscribing.", e);
        }

        if ( subscriptionKey == null ) {
            throw new UDDIException( "Subscription Key error." );
        }

        return subscriptionKey;
    }

    @Override
    public void deleteSubscription( final String subscriptionKey ) throws UDDIException {
        try {
            final UDDISubscriptionPortType subscriptionPort = getSubscriptionPort();

            final DeleteSubscription deleteSubscription = new DeleteSubscription();
            deleteSubscription.setAuthInfo( getAuthToken() );
            deleteSubscription.getSubscriptionKey().add( subscriptionKey );

            subscriptionPort.deleteSubscription( deleteSubscription );
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error deleting subscription '"+subscriptionKey+"'", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error deleting subscription '"+subscriptionKey+"'", e);
        }
    }

    @Override
    public UDDISubscriptionResults pollSubscription( final long startTime,
                                                     final long endTime,
                                                     final String subscriptionKey ) throws UDDIException {
        final UDDISubscriptionResultsImpl uddiResults = new UDDISubscriptionResultsImpl( subscriptionKey, startTime, endTime );

        try {
            if ( subscriptionKey != null ) {
                final UDDISubscriptionPortType subscriptionPort = getSubscriptionPort();
                final DatatypeFactory factory = DatatypeFactory.newInstance();

                final CoveragePeriod period = new CoveragePeriod();
                final GregorianCalendar startGC = new GregorianCalendar();
                startGC.setTimeInMillis( startTime );
                period.setStartPoint( factory.newXMLGregorianCalendar( startGC ) );

                if ( endTime > 0 ) {
                    GregorianCalendar endGC = new GregorianCalendar();
                    endGC.setTimeInMillis( endTime );
                    period.setEndPoint( factory.newXMLGregorianCalendar( endGC ) );
                }

                GetSubscriptionResults getSubscriptionResults = new GetSubscriptionResults();
                getSubscriptionResults.setAuthInfo( getAuthToken() );
                getSubscriptionResults.setSubscriptionKey( subscriptionKey );
                getSubscriptionResults.setCoveragePeriod( period );

                String chunkToken = null;
                boolean moreResults = true;
                while ( moreResults ) {
                    getSubscriptionResults.setChunkToken( chunkToken );
                    SubscriptionResultsList results = subscriptionPort.getSubscriptionResults( getSubscriptionResults );
                    if ( results != null ) {
                        chunkToken = results.getChunkToken();
                        if ( chunkToken != null && !"0".equals(chunkToken.trim()) ) {
                            chunkToken = chunkToken.trim();
                        } else {
                            moreResults = false;
                        }

                        // Long format results
                        ServiceList serviceList = results.getServiceList();
                        if ( serviceList != null ) {
                            ServiceInfos serviceInfos = serviceList.getServiceInfos();
                            if ( serviceInfos != null ) {
                                List<ServiceInfo> serviceInfoList = serviceInfos.getServiceInfo();
                                if ( serviceInfoList != null ) {
                                    for ( ServiceInfo serviceInfo : serviceInfoList ) {
                                        uddiResults.add( new UDDISubscriptionResultsImpl.ResultImpl( serviceInfo.getServiceKey(), false ) );
                                    }
                                }
                            }
                        }

                        // Brief format/deleted entity results
                        List<KeyBag> bags = results.getKeyBag();
                        if ( bags != null ) {
                            for ( KeyBag bag : bags ) {
                                boolean deleted = bag.isDeleted();
                                List<String> serviceKeys = bag.getServiceKey();
                                if ( serviceKeys != null ) {
                                    for ( String servicekey : serviceKeys ) {
                                        uddiResults.add( new UDDISubscriptionResultsImpl.ResultImpl( servicekey, deleted ) );
                                    }
                                }
                            }
                        }
                    } else {
                        moreResults = false;
                    }
                }
            }
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting subscription results", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting subscription results", e);
        } catch (DatatypeConfigurationException e) {
            throw new UDDIException("Error getting subscription results.", e);
        }

        return uddiResults;
    }

    /**
     * 
     */
    @Override
    public void close() {
        if (authenticated()) {
            try {
                UDDISecurityPortType securityPort = getSecurityPort();
                DiscardAuthToken discardAuthToken = new DiscardAuthToken();
                discardAuthToken.setAuthInfo(getAuthToken());
                securityPort.discardAuthToken(discardAuthToken);
                authToken = null;
            } catch (DispositionReportFaultMessage drfm) {
                logger.log(Level.INFO, "Error logging out.", buildFaultException("Error logging out", drfm));
            } catch (RuntimeException e) {
                logger.log(Level.INFO, "Error logging out.", e);
            } catch (UDDIException e) {
                logger.log(Level.INFO, "Error logging out.", e);
            }
        }
    }

    //- PROTECTED

    /**
     * Create a new UDDI client instance.
     *
     * @param inquiryUrl the inquiry API url
     * @param publishUrl the publish API url (may be null, then inquiry is used)
     * @param securityUrl the security API url (may be null, then publish is used)
     * @param login the username (may be null)
     * @param password the password (may be null if login is null)
     * @param policyAttachmentVersion The version of policy attachment to use.
     */
    protected GenericUDDIClient(final String inquiryUrl,
                                final String publishUrl,
                                final String subscriptionUrl,
                                final String securityUrl,
                                final String login,
                                final String password,
                                final PolicyAttachmentVersion policyAttachmentVersion,
                                final UDDIClientTLSConfig tlsConfig ) {
        if ( inquiryUrl == null ) throw new IllegalArgumentException("inquiryUrl must not be null");
        if ( login != null && password == null ) throw new IllegalArgumentException("password must not be null");
        if ( policyAttachmentVersion == null ) throw new IllegalArgumentException("policyAttachmentVersion must not be null");

        // service urls
        this.inquiryUrl = inquiryUrl;
        this.publishUrl = coalesce(publishUrl, inquiryUrl);
        this.subscriptionUrl = coalesce(subscriptionUrl, inquiryUrl);
        this.securityUrl = coalesce(securityUrl, publishUrl, inquiryUrl);

        // creds
        this.login = login;
        this.password = password;

        // TLS
        this.tlsConfig = tlsConfig;

        // keys for ws-policy versions
        this.tModelKeyPolicyType = policyAttachmentVersion.getTModelKeyPolicyTypes();
        this.tModelKeyLocalPolicyReference = policyAttachmentVersion.getTModelKeyLocalPolicyReference();
        this.tModelKeyRemotePolicyReference = policyAttachmentVersion.getTModelKeyRemotePolicyReference();
    }

    protected UDDISecurityPortType getSecurityPort() {
        UDDISecurity security = new UDDISecurity(buildUrl("resources/uddi_v3_service_s.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDISecurity"));
        UDDISecurityPortType securityPort = security.getUDDISecurityPort();
        stubConfig(securityPort, getSecurityUrl());
        return securityPort;
    }

    protected UDDIInquiryPortType getInquirePort() {
        UDDIInquiry inquiry = new UDDIInquiry(buildUrl("resources/uddi_v3_service_i.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDIInquiry"));
        UDDIInquiryPortType inquiryPort = inquiry.getUDDIInquiryPort();
        stubConfig(inquiryPort, getInquiryUrl());
        return inquiryPort;
    }

    protected UDDIPublicationPortType getPublishPort() {
        UDDIPublication publication = new UDDIPublication(buildUrl("resources/uddi_v3_service_p.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDIPublication"));
        UDDIPublicationPortType publicationPort = publication.getUDDIPublicationPort();
        stubConfig(publicationPort, getPublicationUrl());
        return publicationPort;
    }

    protected UDDISubscriptionPortType getSubscriptionPort() throws UDDIException {
        UDDISubscription subscription = new UDDISubscription(buildUrl("resources/uddi_v3_service_sub.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDISubscription"));
        UDDISubscriptionPortType subscriptionPort = subscription.getUDDISubscriptionPort();
        stubConfig(subscriptionPort, getSubscriptionUrl());
        return subscriptionPort;
    }

    protected String getAuthToken(final String login,
                                  final String password) throws UDDIException {
        String authToken;

        try {
            UDDISecurityPortType securityPort = getSecurityPort();
            GetAuthToken getAuthToken = new GetAuthToken();
            getAuthToken.setUserID(login);
            getAuthToken.setCred(password);
            authToken = securityPort.getAuthToken(getAuthToken).getAuthInfo();
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting authentication token", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting authentication token", e);
        }

        return authToken;
    }

    /**
     * Handle a fault from the UDDI registry.
     *
     * <p>Common faults are:</p>
     *
     * <ul>
     * <li>E_unknownUser 10150</li>
     * <li>E_userMismatch 10140</li>
     * <li>E_authTokenExpired 10110</li>
     * <li>E_authTokenRequired 10120</li>
     * <li>E_busy 10400</li>
     * <li>E_unrecognizedVersion 10040</li>
     * <li>E_unsupported 10050</li>
     * </ul>
     *
     * Note that Systinet can also throw <code>E_invalidKeyPassed</code> on an
     * authorization error.
     *
     * @param context Contextual message for exception
     * @param faultMessage The fault to handle
     * @throws UDDIException always
     */
    protected UDDIException buildFaultException(final String context,
                                                final DispositionReportFaultMessage faultMessage) {
        UDDIException exception;
        String contextMessage = context;
        if ( !contextMessage.endsWith( ": " )) {
            contextMessage += ": ";
        }

        if ( hasResult(faultMessage, 10150) ) {
            exception = new UDDIAccessControlException("Authentication failed for '" + login + "'.");
        } else if ( hasResult(faultMessage, 10140) ||
                    hasResult(faultMessage, 10120)) {
                exception = new UDDIAccessControlException("Authorization failed for '" + login + "'.");
        } else if ( hasResult(faultMessage, 10110)) {
                exception = new UDDIAccessControlException("Session expired or invalid.");
        } else if ( hasResult(faultMessage, 10400)) {
                exception = new UDDIException("UDDI registry is too busy.");
        } else if ( hasResult(faultMessage, 10040)) {
                exception = new UDDIException("UDDI registry version mismatch.");
        } else if ( hasResult(faultMessage, 10050)) {
                exception = new UDDIException("UDDI registry does not support a required feature.");
        } else if ( hasResult(faultMessage, 10210)) {
                exception = new UDDIInvalidKeyException(contextMessage + toString(faultMessage));            
        } else {
            // handle general exception
            exception = new UDDIException(contextMessage + toString(faultMessage));
        }

        return exception;
    }

    protected UDDIException buildErrorException( final String context,
                                                 final RuntimeException exception ) {
        if ( isNetworkException(exception) ) {
            return new UDDINetworkException( context + ", due to " + describeNetworkException(exception) + ".", exception );
        } else {
            return new UDDIException( context + ".", exception );
        }
    }

    protected boolean hasResult(DispositionReportFaultMessage faultMessage, int errorCode) {
        boolean foundResult = false;

        DispositionReport report = faultMessage.getFaultInfo();
        if ( report != null ) {
            for (Result result : report.getResult()) {
                if ( result.getErrno() == errorCode ) {
                    foundResult = true;
                    break;
                }
            }
        }

        return foundResult;
    }

    protected URL buildUrl(String relativeUrl) {
        URL url = UDDIInquiry.class.getResource(relativeUrl);

        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Using url ''{0}''.", url);

        return url;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(GenericUDDIClient.class.getName());

    private static final String UDDIV3_NAMESPACE = "urn:uddi-org:api_v3_service";

    private static final String POLICY_TYPE_KEY_NAME = "policy";
    private static final String POLICY_TYPE_KEY_VALUE = "policy";
    private static final String TMODEL_KEY_WSDL_TYPES = "uddi:uddi.org:wsdl:types";
    private static final String WSDL_TYPES_SERVICE = "service";
    //private static final String WSDL_TYPES_PORT = "port"; // don't use port since some UDDIs don't have this v3 feature (CentraSite)
    //private static final String WSDL_TYPES_BINDING = "binding";
    private static final String OVERVIEW_URL_TYPE_WSDL = "wsdlInterface";
    private static final String FINDQUALIFIER_APPROXIMATE = "approximateMatch";
    private static final String FINDQUALIFIER_CASEINSENSITIVE = "caseInsensitiveMatch";
    private static final String USE_TYPE_END_POINT = "endPoint";

    // From http://www.uddi.org/pubs/uddi_v3.htm#_Toc85908004
    private static final int MAX_LENGTH_KEY = 255;
    private static final int MAX_LENGTH_NAME = 255;
    private static final int MAX_LENGTH_DESC = 255;
    private static final int MAX_LENGTH_KEYVALUE = 255;
    private static final int MAX_LENGTH_URL = 4096;

    static {
        UDDIClientTLSConfig.addDefaultAdapter( new SunJaxWsTLSConfigAdapter() );
    }

    private final String inquiryUrl;
    private final String publishUrl;
    private final String subscriptionUrl;
    private final String securityUrl;
    private final String login;
    private final String password;
    private final UDDIClientTLSConfig tlsConfig;
    private final String tModelKeyPolicyType;
    private final String tModelKeyLocalPolicyReference;
    private final String tModelKeyRemotePolicyReference;
    private String authToken;
    private boolean moreAvailable;

    private String getInquiryUrl() {
        return inquiryUrl;
    }

    private String getPublicationUrl() {
        return publishUrl;
    }

    private String getSubscriptionUrl() throws UDDIException {
        if ( subscriptionUrl == null ) throw new UDDIException("Subscriptions not available"); 
        return subscriptionUrl;
    }

    private String getSecurityUrl() {
        return securityUrl;
    }

    private void stubConfig(Object proxy, String url) {
        BindingProvider bindingProvider = (BindingProvider) proxy;
        Binding binding = bindingProvider.getBinding();
        Map<String,Object> context = bindingProvider.getRequestContext();
        List<Handler> handlerChain = new ArrayList<Handler>();

        // Add handler to fix any issues with invalid faults
        handlerChain.add(new FaultRepairSOAPHandler());

        // Set handlers
        binding.setHandlerChain(handlerChain);

        // Set endpoint
        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);

        if ( tlsConfig != null ) {
            boolean isTLS = url.toLowerCase().startsWith("https" );
            for ( UDDIClientTLSConfig.TLSConfigAdapter adapter : UDDIClientTLSConfig.getDefaultAdapters() ) {
                if ( adapter.configure( proxy, tlsConfig, isTLS )) {
                    break;
                }
            }
        }
    }

    private String getAuthToken() throws UDDIException {
        if (authToken == null && (login!=null && login.trim().length()>0)) {
            authToken = getAuthToken(login.trim(), password);
        }

        return authToken;
    }

    private boolean authenticated() {
        return authToken != null;
    }

    private String coalesce(String ... args) {
        String value = null;

        for ( String arg : args ) {
            value = arg;
            if (value != null)
                break;
        }

        return value;
    }

    private <T> T get(List<T> list, String description, boolean onlyOne) throws UDDIException {
        if (list == null || list.isEmpty()) {
            throw new UDDIException("Missing " + description);
        } else if (onlyOne && list.size()!=1) {
            throw new UDDIException("Duplicate " + description);
        }

        return list.get(0);
    }

    private void extractPolicyUrls(CategoryBag categoryBag, Collection<String> policyUrls) throws UDDIException {
        if (categoryBag != null) {
            List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
            if (keyedReferences != null) {
                for (KeyedReference keyedReference : keyedReferences) {
                    if (tModelKeyLocalPolicyReference.equals(keyedReference.getTModelKey())) {
                        policyUrls.add(getPolicyUrl(keyedReference.getKeyValue()));
                    } else if (tModelKeyRemotePolicyReference.equals(keyedReference.getTModelKey())) {
                        policyUrls.add(keyedReference.getKeyValue());
                    }
                }
            }
        }
    }

    /**
     * Extract fault information to a string.
     *
     * <pre>
     *  &lt;Envelope xmlns="http://schemas.xmlsoap.org/soap/envelope/">
     *   &lt;Body>
     *   &lt;Fault>
     *   &lt;faultcode xmlns="">Server&lt;/faultcode>
     *   &lt;faultstring xmlns="">Server Error&lt;/faultstring>
     *   &lt;detail xmlns="">
     *   &lt;dispositionReport xmlns="urn:uddi-org:api_v3">
     *   &lt;result errno="10150">
     *   &lt;errInfo errCode="E_unknownUser">(19009) Incorrect login name or password.&lt;/errInfo>
     *   &lt;/result>
     *   &lt;/dispositionReport>
     *   &lt;/detail>
     *   &lt;/Fault>
     *   &lt;/Body>
     *  &lt;/Envelope>
     * </pre>
     *
     * @param dispositionReport The fault message (see example above) 
     * @return The fault String (e.g. errno:10150/errcode:E_unknownUser/description:(19009) Incorrect login name or password.)
     */
    private String toString(DispositionReportFaultMessage dispositionReport) {
        StringBuffer buffer = new StringBuffer(512);

        DispositionReport report = dispositionReport.getFaultInfo();
        if ( report != null ) {
            for (Result result : report.getResult()) {
                buffer.append("errno:");
                buffer.append(result.getErrno());
                ErrInfo info = result.getErrInfo();
                buffer.append("/errcode:");
                buffer.append(info.getErrCode());
                buffer.append("/description:");
                buffer.append(info.getValue());
            }
        }

        return buffer.toString();
    }

    private String getExceptionMessage(String prefix, DispositionReportFaultMessage drfm){
        final String errorInfo = getFirstFaultMessage(drfm);
        return prefix + ((errorInfo != null)? errorInfo: drfm.getMessage());
    }

    /**
     * Get the first Fault info's Result's ErrInfo from the disposition report
     * @param disp DispositionReportFaultMessage to get the error info from
     * @return String error message, null if none found
     */
    private String getFirstFaultMessage(DispositionReportFaultMessage disp){
        Result result = disp.getFaultInfo().getResult().iterator().next();
        if(result != null) return result.getErrInfo().getValue();
        return null;
    }

    private boolean isNetworkException( final Exception exception ) {
        return ExceptionUtils.causedBy( exception, ConnectException.class ) ||
               ExceptionUtils.causedBy( exception, NoRouteToHostException.class ) ||
               ExceptionUtils.causedBy( exception, UnknownHostException.class ) ||
               ExceptionUtils.causedBy( exception, SocketTimeoutException.class );
    }

    private String describeNetworkException( final Exception exception ) {
        String description = "connection error";

        if ( ExceptionUtils.causedBy( exception, UnknownHostException.class ) ) {
            description = "unknown host '"+ExceptionUtils.getCauseIfCausedBy( exception, UnknownHostException.class ).getMessage()+"'";
        } else if ( ExceptionUtils.causedBy( exception, SocketTimeoutException.class ) ) {
            description = "network read timed out";
        }

        return description;
    }

    private void mergePolicyUrlToInfo(final String policyKey,
                                      final String policyUrl,
                                      final List<UDDINamedEntity> uddiNamedEntity) {
        for (int i=0; i<uddiNamedEntity.size(); i++) {
            UDDINamedEntity current = uddiNamedEntity.get(i);

            if ( policyKey.equals(current.getKey()) ) {
                UDDINamedEntity merged = new UDDINamedEntityImpl(
                        current.getKey(),
                        current.getName(),
                        policyUrl, 
                        current.getWsdlUrl());
                uddiNamedEntity.remove(i);
                uddiNamedEntity.add(i, merged);
                break;
            }
        }
    }

    private void validateName(String name) throws UDDIDataException {
        validateLength("name", name, MAX_LENGTH_NAME);
    }

    private void validateDescription(String description) throws UDDIDataException {
        validateLength("description", description, MAX_LENGTH_DESC);
    }

    private void validateKey(String key) throws UDDIDataException {
        validateLength("key", key, MAX_LENGTH_KEY);
    }

    private void validateKeyValue(String value) throws UDDIDataException {
        validateLength("keyValue", value, MAX_LENGTH_KEYVALUE);
    }

    private void validateUrl(String url) throws UDDIDataException {
        validateLength("url", url, MAX_LENGTH_URL);
    }

    private void validateLength(String description, String value, int maxLength) throws UDDIDataException {
        if ( value != null ) {
            if ( value.length() > maxLength ) {
                throw new UDDIDataException("Value for " + description + " exceeds maximum length of " + maxLength + " characters.");
            }
        }
    }

    private FindBinding buildFindBinding( final ServiceInfo serviceInfo ) throws UDDIException {
        FindBinding findBinding = new FindBinding();
        findBinding.setAuthInfo(getAuthToken());
        findBinding.setServiceKey(serviceInfo.getServiceKey());

        // We want them all, but setting limit at 50 //todo accecpt this value as a param from cluster variable
        findBinding.setMaxRows(50);

        // TODO This does not work with CentraSite
        //CategoryBag bindingCategoryBag = new CategoryBag();
        //KeyedReference bindingKeyedReference = new KeyedReference();
        //bindingKeyedReference.setKeyValue(WSDL_TYPES_PORT);
        //bindingKeyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
        //bindingCategoryBag.getKeyedReference().add(bindingKeyedReference);
        //findBinding.setCategoryBag(bindingCategoryBag);
        return findBinding;
    }

    private FindService buildFindService( final String servicePattern,
                                          final boolean caseSensitive,
                                          final int offset,
                                          final int maxRows) throws UDDIException {
        String authToken = getAuthToken();
        Name[] names = buildNames(servicePattern);
        FindService findService = new FindService();
        findService.setAuthInfo(authToken);
        if (maxRows > 0)
            findService.setMaxRows(maxRows);
        if (offset > 0)
            findService.setListHead(offset);
        findService.setFindQualifiers(buildFindQualifiers(servicePattern, caseSensitive));
        if (names != null)
            findService.getName().addAll(Arrays.asList(names));

        KeyedReference keyedReference = new KeyedReference();
        keyedReference.setKeyValue(WSDL_TYPES_SERVICE);
        keyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
        CategoryBag categoryBag = new CategoryBag();
        categoryBag.getKeyedReference().add(keyedReference);
        findService.setCategoryBag(categoryBag);

        return findService;
    }

    private ServiceDetail getServiceDetail( final String serviceKey, final String authToken ) throws UDDIException {
        ServiceDetail serviceDetail;
        try {
            UDDIInquiryPortType inquiryPort = getInquirePort();

            GetServiceDetail getServiceDetail = new GetServiceDetail();
            getServiceDetail.setAuthInfo(authToken);
            getServiceDetail.getServiceKey().add(serviceKey);
            serviceDetail = inquiryPort.getServiceDetail(getServiceDetail);

            if (serviceDetail.getBusinessService().size() != 1) {
                String msg = "UDDI registry returned either empty serviceDetail or " +
                             "more than one business services";
                throw new UDDIException(msg);
            }

        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error getting service details", drfm);
        } catch (RuntimeException e) {
            throw buildErrorException("Error getting service details", e);
        }
        return serviceDetail;
    }

    private SaveService buildSaveService( final String authToken, final BusinessService toUpdate ) {
        SaveService saveService = new SaveService();
        saveService.setAuthInfo(authToken);
        saveService.getBusinessService().add(toUpdate);
        return saveService;
    }

    private FindQualifiers buildFindQualifiers(String searchStr, boolean caseSensitive) {
        logger.log(Level.FINE, "- Search string : " + searchStr + " isCaseSensitive : " + caseSensitive + " containsWildCards : " + containWildCards(searchStr));
        FindQualifiers findQualifiers = new FindQualifiers();
        List<String> qualifiers = findQualifiers.getFindQualifier();

        //determine if contains wild cards
        if (containWildCards(searchStr)) {
            qualifiers.add(FINDQUALIFIER_APPROXIMATE);
            if (!caseSensitive) {
                qualifiers.add(FINDQUALIFIER_CASEINSENSITIVE);
            }
        } else {
            if (!caseSensitive) {
                qualifiers.add(FINDQUALIFIER_CASEINSENSITIVE);
            } else {
                return null;
            }
        }

        return findQualifiers;
    }

    private KeyedReference buildKeyedReference(String key, String name, String value) {
        KeyedReference keyedReference = new KeyedReference();
        keyedReference.setTModelKey(key);
        keyedReference.setKeyName(name);
        keyedReference.setKeyValue(value);
        return keyedReference;
    }

    private Name[] buildNames( final String namePattern ) {
        Name[] names = null;
        if (namePattern != null && namePattern.length() > 0) {
            names = new Name[]{buildName(namePattern)};
        }
        return names;
    }

    private Name buildName(String value) {
        Name name = new Name();
        name.setValue(value);
        return name;
    }

    private OverviewDoc buildOverviewDoc(String url) {
        OverviewDoc overviewDoc = new OverviewDoc();
        OverviewURL overviewURL = new OverviewURL();
        overviewURL.setValue(url);
        overviewDoc.setOverviewURL(overviewURL);                                                   
        return overviewDoc;
    }

    private Description buildDescription(String value) {
        Description description = new Description();
        description.setValue(value);
        return description;
    }

    private AccessPoint buildAccessPoint(String protocol, String uri) {
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setUseType(protocol);
        accessPoint.setValue(uri);
        return accessPoint;
    }

    /**
     * Attempt to extracts the wsdl location URL from the TModelInstance.  It searches for the .wsdl
     * extension, if it is not found, then null is returned.
     *
     * @param tmInst the TModelInstance to extract the URL from
     * @return the string for the WSDL location, null if it cannot be found in the TModel
     */
    private String extractOverviewUrl(TModelInstanceInfo tmInst) {

        String result = null;
        try {
            if (tmInst != null && !tmInst.getInstanceDetails().getOverviewDoc().isEmpty()) {

                for (OverviewDoc overview : tmInst.getInstanceDetails().getOverviewDoc()) {
                    if (overview.getOverviewURL() != null) {
                        result = overview.getOverviewURL().getValue();
                    }
                }
            }
        } catch (Throwable ex) {
          // ignore any exceptions
        }

        if (result != null && result.contains(".wsdl")) {
            result = result.substring(0, result.indexOf(".wsdl")+5);

        } else if (result != null && result.contains(".WSDL")) {
            result = result.substring(0, result.indexOf(".WSDL")+5);

        } else {
            result = null;

        }

        return result;
    }

    private void setMoreAvailable(final ListDescription listDescription) {
        if(listDescription == null) return;

        moreAvailable = (listDescription.getActualCount() > listDescription.getListHead() + (listDescription.getIncludeCount()-1))
                && listDescription.getActualCount() > 0;
    }

    /**
     * @param searchPattern Search pattern string
     * @return  TRUE if search pattern contains wild cards, otherwise FALSE.
     */
    private boolean containWildCards(String searchPattern) {
        logger.log(Level.FINEST, "String before :" + searchPattern);
        boolean result = false;

        //remove any known escape characters
        String temp = searchPattern.replace("\\%", "").replace("\\_", "");

        logger.log(Level.FINEST, "String after : " + temp);
        if (temp.contains("%") || temp.contains("_")) result = true;
        return result;
    }

//    private FindTModel getFindTModel(TModel tModelToFind, boolean approxomiateMatch) throws UDDIException {
//        FindTModel findTModel = new FindTModel();
//        findTModel.setAuthInfo(getAuthToken());
//
//        FindQualifiers findQualifiers = new FindQualifiers();
//        List<String> qualifiers = findQualifiers.getFindQualifier();
//        qualifiers.add(FINDQUALIFIER_CASEINSENSITIVE);
//        if(approxomiateMatch) qualifiers.add(FINDQUALIFIER_APPROXIMATE);
//
//        Name name= new Name();
//        name.setValue(tModelToFind.getName().getValue());
//
//        findTModel.setName(name);
//        findTModel.setFindQualifiers(findQualifiers);
//        if(tModelToFind.getCategoryBag() != null){
//            CategoryBag searchCategoryBag = new CategoryBag();
//            searchCategoryBag.getKeyedReference().addAll(tModelToFind.getCategoryBag().getKeyedReference());
//            findTModel.setCategoryBag(searchCategoryBag);
//        }
//
//        if(tModelToFind.getIdentifierBag() != null){
//            IdentifierBag searchIdentifierBag = new IdentifierBag();
//            searchIdentifierBag.getKeyedReference().addAll(tModelToFind.getIdentifierBag().getKeyedReference());
//            findTModel.setIdentifierBag(searchIdentifierBag);
//        }
//        return findTModel;
//    }

    private static class UDDIOperationalInfoImpl implements UDDIOperationalInfo {
        private final OperationalInfo info;
        private Calendar createdCalendar;
        private Calendar modifiedCalendar;
        private Calendar modifiedIncludingChildrenCalendar;

        private UDDIOperationalInfoImpl( final OperationalInfo info ) {
            this.info = info;            
        }

        @Override
        public String getEntityKey() {
            return info.getEntityKey();
        }

        @Override
        public String getAuthorizedName() {
            return info.getAuthorizedName();
        }

        @Override
        public long getCreatedTime() {
            if ( createdCalendar == null ) {
                createdCalendar = toCalendar(info.getCreated());
            }
            return createdCalendar.getTimeInMillis();
        }

        @Override
        public long getModifiedTime() {
            if ( modifiedCalendar == null ) {
                modifiedCalendar = toCalendar(info.getModified());
            }
            return modifiedCalendar.getTimeInMillis();
        }

        @Override
        public long getModifiedIncludingChildrenTime() {
            if ( modifiedIncludingChildrenCalendar == null ) {
                modifiedIncludingChildrenCalendar = toCalendar(info.getModifiedIncludingChildren());
            }
            return modifiedIncludingChildrenCalendar.getTimeInMillis();
        }

        private Calendar toCalendar( final XMLGregorianCalendar xmlGregorianCalendar ) {
            Calendar calendar;

            if ( xmlGregorianCalendar == null ) {
                calendar = Calendar.getInstance();
                calendar.setTimeInMillis( 0 );
            } else {
                calendar = xmlGregorianCalendar.toGregorianCalendar();
            }

            return calendar;
        }
    }

    private static final class SunJaxWsTLSConfigAdapter implements UDDIClientTLSConfig.TLSConfigAdapter {
        private static final SecureRandom random = new SecureRandom();
        private static final String PROP_SSL_SESSION_TIMEOUT = "com.l7tech.server.uddi.sslSessionTimeoutSeconds";
        private static final int DEFAULT_SSL_SESSION_TIMEOUT = 10 * 60;

        @Override
        public boolean configure( final Object target, final UDDIClientTLSConfig config, final boolean configureTLS ) {
            boolean processed = false;

            if ( target instanceof BindingProvider &&
                 Proxy.isProxyClass( target.getClass() ) &&
                 Proxy.getInvocationHandler( target ).getClass().getName().equals("com.sun.xml.ws.client.sei.SEIStub") )  {
                processed = true;
                BindingProvider bindingProvider = (BindingProvider) target;
                Map<String,Object> context = bindingProvider.getRequestContext();

                context.put("com.sun.xml.ws.connect.timeout", (int)config.getConnectionTimeout());
                context.put("com.sun.xml.ws.request.timeout", (int)config.getReadTimeout());

                if ( configureTLS ) {
                    // Hostname verifier
                    context.put("com.sun.xml.ws.transport.https.client.hostname.verifier", config.getHostnameVerifier());

                    // SSL socket factory
                    int timeout = SyspropUtil.getInteger(PROP_SSL_SESSION_TIMEOUT, DEFAULT_SSL_SESSION_TIMEOUT);
                    try {
                        final SSLContext ctx = SSLContext.getInstance("SSL");
                        ctx.init( config.getKeyManagers(),  config.getTrustManagers(), random );
                        ctx.getClientSessionContext().setSessionTimeout(timeout);
                        context.put("com.sun.xml.ws.transport.https.client.SSLSocketFactory", ctx.getSocketFactory());
                    } catch (NoSuchAlgorithmException e) {
                        logger.log( Level.WARNING, "Error configuring TLS for UDDI client.", e );
                    } catch (KeyManagementException e) {
                        logger.log( Level.WARNING, "Error configuring TLS for UDDI client.", e );
                    }
                }
            }

            return processed;
        }
    }
}
