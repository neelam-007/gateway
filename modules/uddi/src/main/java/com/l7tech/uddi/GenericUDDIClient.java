package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.URL;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.namespace.QName;

/**
 * UDDIv3 client implementation using generated JAX-WS UDDI API
 */
public class GenericUDDIClient implements UDDIClient {

    //- PUBLIC

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
    public GenericUDDIClient(final String inquiryUrl,
                             final String publishUrl,
                             final String subscriptionUrl,
                             final String securityUrl,
                             final String login,
                             final String password,
                             final PolicyAttachmentVersion policyAttachmentVersion) {
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

        // keys for ws-policy versions
        this.tModelKeyPolicyType = policyAttachmentVersion.getTModelKeyPolicyTypes();
        this.tModelKeyLocalPolicyReference = policyAttachmentVersion.getTModelKeyLocalPolicyReference();
        this.tModelKeyRemotePolicyReference = policyAttachmentVersion.getTModelKeyRemotePolicyReference();
    }

    /**
     *
     */
    @Override
    public void authenticate() throws UDDIException {
        getAuthToken();   
    }

    /**
     *
     */
    @Override
    public boolean publishBusinessService(final BusinessService businessService) throws UDDIException {
        SaveService saveService = new SaveService();
        saveService.setAuthInfo(getAuthToken());

        saveService.getBusinessService().add(businessService);
        try {
            ServiceDetail serviceDetail = getPublishPort().saveService(saveService);
            businessService.setServiceKey(serviceDetail.getBusinessService().get(0).getServiceKey());
            logger.log(Level.INFO, "Saved service with key: " + businessService.getServiceKey());
            return true;
        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception saving business service: ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }
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

    @Override
    public boolean publishTModel(TModel tModelToPublish) throws UDDIException {
        TModelList tModelList = findMatchingTModels(tModelToPublish, false);
        TModelInfos tModelInfos = tModelList.getTModelInfos();
        if(tModelInfos != null && !tModelInfos.getTModelInfo().isEmpty()){
            List<TModelInfo> allTModelInfos = tModelInfos.getTModelInfo();

            //tModels published by the gateway will only have a single OverviewDoc with use type wsdlInterface
            String tModelToPublishWsdlUrl = null;
            for(OverviewDoc overviewDoc: tModelToPublish.getOverviewDoc()){
                OverviewURL overviewURL = overviewDoc.getOverviewURL();
                if(!overviewURL.getUseType().equals("wsdlInterface")) continue;
                tModelToPublishWsdlUrl = overviewURL.getValue();
            }
            if(tModelToPublishWsdlUrl == null) throw new IllegalStateException("tModel to publish does not have an OverviewURl of type 'wsdlInterface'");

            List<TModel> matchingTModels = new ArrayList<TModel>();
            for(TModelInfo tModelInfo: allTModelInfos){
                TModel foundModel = getTModel(tModelInfo.getTModelKey());
                if(foundModel == null) continue;
                //compare overviewDocs and overviewUrls
                boolean foundMatchingOverviewUrl = false;
                for(OverviewDoc overviewDoc: foundModel.getOverviewDoc()){
                    OverviewURL overviewURL = overviewDoc.getOverviewURL();
                    if(!overviewURL.getUseType().equals("wsdlInterface")) continue;
                    if(!overviewURL.getValue().equalsIgnoreCase(tModelToPublishWsdlUrl)) continue;
                    foundMatchingOverviewUrl = true;
                }
                if(foundMatchingOverviewUrl) matchingTModels.add(foundModel);
            }

            if(!matchingTModels.isEmpty()){
                if(matchingTModels.size() != 1)
                    logger.log(Level.INFO, "Found " + matchingTModels.size()+" matching TModels. The first will be used");

                tModelToPublish.setTModelKey(allTModelInfos.get(0).getTModelKey());
                logger.log(Level.INFO, "Found matching tModel in UDDI Registry. Not publishing supplied tModel");
                return false;
            }
            //fall through to publish below
        }
        //TModel does not exist yet in registry. Publish it

        UDDIPublicationPortType uddiPublicationPortType = getPublishPort();
        SaveTModel saveTModel = new SaveTModel();
        saveTModel.setAuthInfo(authToken);
        saveTModel.getTModel().add(tModelToPublish);
        try {
            TModelDetail tModelDetail = uddiPublicationPortType.saveTModel(saveTModel);
            TModel saved = tModelDetail.getTModel().get(0);
            tModelToPublish.setTModelKey(saved.getTModelKey());
            logger.log(Level.INFO, "Published tModel to UDDI with key: " + tModelToPublish.getTModelKey());
            return true;
        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception publishing tModel: ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        } catch(RuntimeException e){
            throw new UDDIException(e.getMessage());
        }
    }

    @Override
    public TModel getTModel(String tModelKey) throws UDDIException {
        //Turn the tModelInfo into a TModel
        GetTModelDetail getTModelDetail = new GetTModelDetail();
        getTModelDetail.setAuthInfo(getAuthToken());
        getTModelDetail.getTModelKey().add(tModelKey);

        try {
            TModelDetail tModelDetail = getInquirePort().getTModelDetail(getTModelDetail);
            List<TModel> tModels = tModelDetail.getTModel();
            if(tModels.isEmpty()) return null;
            return tModels.get(0);
        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception geting tModel: ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }
    }

    @Override
    public BusinessService getBusinessService(String serviceKey) throws UDDIException {

        GetServiceDetail getServiceDetail = new GetServiceDetail();
        getServiceDetail.setAuthInfo(getAuthToken());
        getServiceDetail.getServiceKey().add(serviceKey);
        try {
            ServiceDetail serviceDetail = getInquirePort().getServiceDetail(getServiceDetail);
            List<BusinessService> services = serviceDetail.getBusinessService();
            if(services.isEmpty()) return null;
            return services.get(0);
        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception geting BusinessService: ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }
    }

    /**
     * Find a matching tModel based on all searchable attributes of tModelToFind
     * The search is case insensitive, but exact
     *
     * @param tModelToFind TModel to find in the UDDI Registry
     * @param approxomiateMatch
     * @return TModelList containing results, if any. Never null.
     * @throws UDDIException any problems searching the registry
     */
    private TModelList findMatchingTModels(final TModel tModelToFind, boolean approxomiateMatch) throws UDDIException{
        FindTModel findTModel = getFindTModel(tModelToFind, approxomiateMatch);

        UDDIInquiryPortType inquiryPortType = getInquirePort();
        try {
            return inquiryPortType.findTModel(findTModel);

        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception finding matching tModels: ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }

    }

    private FindTModel getFindTModel(TModel tModelToFind, boolean approxomiateMatch) throws UDDIException {
        FindTModel findTModel = new FindTModel();
        findTModel.setAuthInfo(getAuthToken());

        FindQualifiers findQualifiers = new FindQualifiers();
        List<String> qualifiers = findQualifiers.getFindQualifier();
        qualifiers.add(FINDQUALIFIER_CASEINSENSITIVE);
        if(approxomiateMatch) qualifiers.add(FINDQUALIFIER_APPROXIMATE);

        Name name= new Name();
        name.setValue(tModelToFind.getName().getValue());

        findTModel.setName(name);
        findTModel.setFindQualifiers(findQualifiers);
        if(tModelToFind.getCategoryBag() != null){
            CategoryBag searchCategoryBag = new CategoryBag();
            searchCategoryBag.getKeyedReference().addAll(tModelToFind.getCategoryBag().getKeyedReference());
            findTModel.setCategoryBag(searchCategoryBag);
        }

        if(tModelToFind.getIdentifierBag() != null){
            IdentifierBag searchIdentifierBag = new IdentifierBag();
            searchIdentifierBag.getKeyedReference().addAll(tModelToFind.getIdentifierBag().getKeyedReference());
            findTModel.setIdentifierBag(searchIdentifierBag);
        }
        return findTModel;
    }

    @Override
    public void deleteTModel(TModel tModel) throws UDDIException{
        //See if any services reference this tModelKey, is so then don't delete
        final FindService findService = new FindService();
        findService.setAuthInfo(getAuthToken());

        final FindTModel findTModel = getFindTModel(tModel, false);
        findService.setFindTModel(findTModel);

        try {
            final ServiceList serviceList = getInquirePort().findService(findService);
            if(serviceList.getServiceInfos() != null && !serviceList.getServiceInfos().getServiceInfo().isEmpty()){
                logger.log(Level.INFO, "Not deleting tModel as BusinessService found which references tModel with key: " + tModel.getTModelKey());
                return;
            }
        } catch (DispositionReportFaultMessage drfm) {
            final String msg =
                    getExceptionMessage("Exception searching for BusinessServices which reference tModel with key: "
                            + tModel.getTModelKey(), drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }

        final String tModelKey = tModel.getTModelKey();
        //if not found delete
        DeleteTModel deleteTModel = new DeleteTModel();
        deleteTModel.setAuthInfo(getAuthToken());
        deleteTModel.getTModelKey().add(tModelKey);
        try {
            getPublishPort().deleteTModel(deleteTModel);
            logger.log(Level.INFO, "Delete tModel with key: " + tModelKey);
        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception deleting tModel with key: " + tModelKey+ ": ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }
    }

    @Override
    public void deleteTModel(String tModelKey) throws UDDIException {
        final TModel tModel = getTModel(tModelKey);
        deleteTModel(tModel);
    }

    @Override
    public void deleteMatchingTModels(TModel prototype) throws UDDIException {
        TModelList tModelList = findMatchingTModels(prototype, true);
        TModelInfos tModelInfos = tModelList.getTModelInfos();
        for(TModelInfo tModelInfo: tModelInfos.getTModelInfo()){
            deleteTModel(tModelInfo.getTModelKey());
        }
    }

    @Override
    public void deleteAllBusinessServicesForGatewayWsdl(final String generalKeyword) throws UDDIException {
        final FindService findService = getFindQualifiersForServiceKeyword(generalKeyword);

        try {
            ServiceList serviceList = getInquirePort().findService(findService);
            if(serviceList.getServiceInfos() != null){
                for(ServiceInfo serviceInfo: serviceList.getServiceInfos().getServiceInfo()){
                    deleteBusinessService(serviceInfo.getServiceKey());
                }
            }else{
                logger.log(Level.WARNING, "No matching BusinessServices were found. None were deleted");
            }
        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception finding services with keyword: " + generalKeyword+ ": ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }
    }

    @Override
    public List<BusinessService> findMatchingBusinessServices(final String generalKeyword) throws UDDIException {
        final FindService findService = getFindQualifiersForServiceKeyword(generalKeyword);

        final List<BusinessService> businessServices = new ArrayList<BusinessService>();
        try {
            final ServiceList serviceList = getInquirePort().findService(findService);
            if(serviceList.getServiceInfos() != null){
                for(ServiceInfo serviceInfo: serviceList.getServiceInfos().getServiceInfo()){
                    final BusinessService businessService = getBusinessService(serviceInfo.getServiceKey());
                    if(businessService == null)
                        throw new UDDIException("Could not find Business Service with serviceKey: " + serviceInfo.getServiceKey());

                    businessServices.add(businessService);
                }
            }else{
                logger.log(Level.INFO, "No matching BusinessServices were found for keyword: " + generalKeyword);
            }
        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception finding services with keyword: " + generalKeyword+ ": ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }

        return businessServices;
    }

    private FindService getFindQualifiersForServiceKeyword(String generalKeyword) throws UDDIException {
        FindService findService = new FindService();
        findService.setAuthInfo(getAuthToken());

        CategoryBag categoryBag = new CategoryBag();
        KeyedReference generalKeywordRef = new KeyedReference();
        generalKeywordRef.setKeyName(WsdlToUDDIModelConverter.LAYER7_PROXY_SERVICE_GENERAL_KEYWORD_URN);
        generalKeywordRef.setKeyValue("%" + generalKeyword + "%");
        generalKeywordRef.setTModelKey(WsdlToUDDIModelConverter.UDDI_GENERAL_KEYWORDS);

        categoryBag.getKeyedReference().add(generalKeywordRef);

        findService.setCategoryBag(categoryBag);

        FindQualifiers findQualifiers = new FindQualifiers();
        List<String> qualifiers = findQualifiers.getFindQualifier();
        qualifiers.add(FINDQUALIFIER_CASEINSENSITIVE);//in case we change it from an oid to also contain strings
        qualifiers.add(FINDQUALIFIER_APPROXIMATE);

        findService.setFindQualifiers(findQualifiers);
        return findService;
    }

    @Override
    public void deleteBusinessService(String serviceKey) throws UDDIException {

        //We need to delete any tModels it references
        //find the service
        final BusinessService businessService = getBusinessService(serviceKey);
        BindingTemplates bindingTemplates = businessService.getBindingTemplates();
        List<String> tModelsToDelete = new ArrayList<String>();
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
            logger.log(Level.INFO, "Deleted service with key: " + businessService.getServiceKey());
            for(String tModelKey: tModelsToDelete){
                deleteTModel(tModelKey);
            }

        } catch (DispositionReportFaultMessage drfm) {
            final String msg = getExceptionMessage("Exception deleting service with key: " + serviceKey+ ": ", drfm);
            logger.log(Level.WARNING, msg);
            throw new UDDIException(msg, drfm);
        }
    }

    /**
     *
     */
    @Override
    public String publishPolicy(final String name,
                                final String description,
                                final String url) throws UDDIException {
        validateName(name);
        validateDescription(description);
        validateKeyValue(url);

        // create a tmodel to save
        TModel tmodel = new TModel();
        CategoryBag cbag = new CategoryBag();
        cbag.getKeyedReference().add(buildKeyedReference(tModelKeyPolicyType, POLICY_TYPE_KEY_NAME, POLICY_TYPE_KEY_VALUE));
        cbag.getKeyedReference().add(buildKeyedReference(tModelKeyRemotePolicyReference, description, url));

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
            throw buildFaultException("Error publishing model: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error publishing model.", e);
        }
    }

    private void setMoreAvailable(final ListDescription listDescription) {
        if(listDescription == null) return;

        moreAvailable = (listDescription.getActualCount() > listDescription.getListHead() + (listDescription.getIncludeCount()-1))
                && listDescription.getActualCount() > 0;
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
            throw buildFaultException("Error listing businesses: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error listing businesses.", e);
        }
    }

    /**
     *
     */
    @Override
    public Collection<UDDINamedEntity> listServiceWsdls(final String servicePattern,
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

            FindService findService = new FindService();
            findService.setAuthInfo(authToken);
            if (maxRows>0)
                findService.setMaxRows(maxRows);
            if (offset>0)
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

            ServiceList serviceList = inquiryPort.findService(findService);

            // check if any more results
            ListDescription listDescription = serviceList.getListDescription();
            setMoreAvailable(listDescription);

            // process
            if ( serviceList.getServiceInfos() != null ) {
                for (ServiceInfo serviceInfo : serviceList.getServiceInfos().getServiceInfo() ) {
                    FindBinding findBinding = new FindBinding();
                    findBinding.setAuthInfo(authToken);
                    findBinding.setServiceKey(serviceInfo.getServiceKey());

                    // We only need one, since all bindingTemplates will have the same WSDL
                    // but since searching by type is not reliable we'll have to get all the
                    // bindings and search though them
                    findBinding.setMaxRows(50);

                    // This does not work with CentraSite
                    //CategoryBag bindingCategoryBag = new CategoryBag();
                    //KeyedReference bindingKeyedReference = new KeyedReference();
                    //bindingKeyedReference.setKeyValue(WSDL_TYPES_PORT);
                    //bindingKeyedReference.setTModelKey(TMODEL_KEY_WSDL_TYPES);
                    //bindingCategoryBag.getKeyedReference().add(bindingKeyedReference);
                    //findBinding.setCategoryBag(bindingCategoryBag);

                    // Find tModel keys for the WSDL portType/binding
                    List<String> modelKeys = new ArrayList<String>();
                    List<String> modelInstanceUrl = new ArrayList<String>();
                    BindingDetail bd = inquiryPort.findBinding(findBinding);
                    for (BindingTemplate bindingTemplate : bd.getBindingTemplate()) {
                        AccessPoint accessPoint = bindingTemplate.getAccessPoint();
                        if (bindingTemplate.getTModelInstanceDetails() == null ||
                            accessPoint==null ) {
                            continue;
                        }

                        List<TModelInstanceInfo> infos = bindingTemplate.getTModelInstanceDetails().getTModelInstanceInfo();
                        for (TModelInstanceInfo tmii : infos) {
                            modelKeys.add(tmii.getTModelKey());

                            // bug 5330 - workaround for Centrasite problem with fetching wsdl
                            String tmiiWsdlUrl = extractOverviewUrl(tmii);
                            if (tmiiWsdlUrl != null) {
                                modelInstanceUrl.add(tmiiWsdlUrl);
                                break;
                            }
                        }
                    }

                    // Get the WSDL url
                    if ( !modelInstanceUrl.isEmpty() ) {

                        // there should only be one
                        String name = get(serviceInfo.getName(), "service name", false).getValue();
                        services.add(new UDDINamedEntityImpl(serviceInfo.getServiceKey(), name, null, modelInstanceUrl.get(0)));

                    } else if ( !modelKeys.isEmpty() ) {
                        GetTModelDetail getTModels = new GetTModelDetail();
                        getTModels.setAuthInfo(authToken);
                        getTModels.getTModelKey().addAll(modelKeys);
                        TModelDetail tmd = inquiryPort.getTModelDetail(getTModels);

                        modelloop:
                        for(TModel tm : tmd.getTModel()) {
                            if ( isBinding(tm) ) {
                                for ( OverviewDoc doc : tm.getOverviewDoc() ) {
                                    OverviewURL url = doc.getOverviewURL();
                                    if ( url!= null && OVERVIEW_URL_TYPE_WSDL.equals(url.getUseType()) ) {
                                        String name = get(serviceInfo.getName(), "service name", false).getValue();
                                        services.add(new UDDINamedEntityImpl(serviceInfo.getServiceKey(), name, null, url.getValue()));
                                        break modelloop;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return services;
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error listing services: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error listing services.", e);
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
            throw buildFaultException("Error listing services: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error listing services.", e);
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
            throw buildFaultException("Error listing endpoints: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error listing endpoints.", e);
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
            throw buildFaultException("Error listing businesses: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error listing businesses.", e);
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
            throw buildFaultException("Error listing policies: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error listing policies.", e);
        }
    }

    /**
     *
     */
    @Override
    public boolean listMoreAvailable() {
        return moreAvailable;
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
            throw buildFaultException("Error getting policy URL: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error getting policy URL.", e);
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
            throw buildFaultException("Error getting policy URL: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error getting policy URL.", e);
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
            throw buildFaultException("Error getting policy URL: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error getting policy URL.", e);
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
            throw buildFaultException("Error getting policy URL: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error getting policy URL.", e);
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
            throw buildFaultException("Error getting service details: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error getting service details.", e);
        }

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
            SaveService saveService = new SaveService();
            saveService.setAuthInfo(authToken);
            saveService.getBusinessService().add(toUpdate);
            publicationPort.saveService(saveService);
        } catch (DispositionReportFaultMessage drfm) {
            throw buildFaultException("Error updating service details: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error updating service details.", e);
        }
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
                logger.log(Level.INFO, "Error logging out.", buildFaultException("Error logging out: ", drfm));
            } catch (RuntimeException e) {
                logger.log(Level.INFO, "Error logging out.", e);
            } catch (UDDIException e) {
                logger.log(Level.INFO, "Error logging out.", e);
            }
        }
    }

    //- PROTECTED

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
            throw buildFaultException("Error getting authentication token: ", drfm);
        } catch (RuntimeException e) {
            throw new UDDIException("Error getting authentication token.", e);
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
     * @param contextMessage Contextual message for exception
     * @param faultMessage The fault to handle
     * @throws UDDIException always
     */
    protected UDDIException buildFaultException(final String contextMessage,
                                                final DispositionReportFaultMessage faultMessage) {
        UDDIException exception;

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
        } else {
            // handle general exception
            exception = new UDDIException(contextMessage + toString(faultMessage));
        }

        return exception;
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
    private static final String WSDL_TYPES_BINDING = "binding";
    private static final String OVERVIEW_URL_TYPE_WSDL = "wsdlInterface";
    private static final String FINDQUALIFIER_APPROXIMATE = "approximateMatch";
    private static final String FINDQUALIFIER_CASEINSENSITIVE = "caseInsensitiveMatch";

    // From http://www.uddi.org/pubs/uddi_v3.htm#_Toc85908004
    private static final int MAX_LENGTH_KEY = 255;
    private static final int MAX_LENGTH_NAME = 255;
    private static final int MAX_LENGTH_DESC = 255;
    private static final int MAX_LENGTH_KEYVALUE = 255;
    private static final int MAX_LENGTH_URL = 4096;

    private final String inquiryUrl;
    private final String publishUrl;
    private final String subscriptionUrl;
    private final String securityUrl;
    private final String login;
    private final String password;
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

    private boolean isBinding(final TModel model) {
        boolean isBinding = false;

        for (KeyedReference reference : model.getCategoryBag().getKeyedReference()) {
            if (TMODEL_KEY_WSDL_TYPES.equals(reference.getTModelKey()) &&
                WSDL_TYPES_BINDING.equals(reference.getKeyValue())) {
                isBinding = true;
                break;
            }
        }

        return isBinding;
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

    /**
     * @param searchPattern Search pattern string
     * @return  TRUE if search pattern contains wild cards, otherwise FALSE.
     */
    private boolean containWildCards(String searchPattern) {
        logger.log(Level.FINE, "String before :" + searchPattern);
        boolean result = false;

        //remove any known escape characters
        String temp = searchPattern.replace("\\%", "").replace("\\_", "");

        logger.log(Level.FINE, "String after : " + temp);
        if (temp.contains("%") || temp.contains("_")) result = true;
        return result;
    }
}
