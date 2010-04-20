/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 19, 2009
 * Time: 5:54:09 PM
 *
 * This class is used to test the behaviour of it's clients by reporting with 'get' methods what methods were called
 */
package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.*;

import java.util.*;
import java.io.IOException;

public class TestUddiClient implements UDDIClient, JaxWsUDDIClient{

    private List<BusinessService> dataStructureForDownloaderTest;
    private BusinessService businessServiceForTest;
    private List<TModel> tModelsForTest;
    private boolean dontCreateFakeServices;
    private int numBindingsDeleted = 0;
    private int numServicesDeleted = 0;
    private int numBindingTemplatesWithNoKey = 0;
    private int numPublishedBindingTemplates = 0;
    private int numTModelsWithNoKey = 0;
    private int numServicesWithNoKey = 0;
    private int numTModelsDeleted = 0;
    private int numTModelsPublished = 0;
    private Set<String> uniqueUrls = new HashSet<String>();
    private Set<String> uniqueWsdlUrls = new HashSet<String>();
    private Set<String> publishedBindingTemplateKeys = new HashSet<String>();
    private boolean dontDeleteBindingTempaltes = false;
    private boolean isOverwrite = false;

    public TestUddiClient(List<BusinessService> dataStructureForDownloaderTest) {
        this.dataStructureForDownloaderTest = dataStructureForDownloaderTest;
    }

    public TestUddiClient(BusinessService businessServiceForTest, List<TModel> tModelsForTest) {
        this.businessServiceForTest = businessServiceForTest;
        this.tModelsForTest = new ArrayList<TModel>(tModelsForTest);
    }

    public TestUddiClient(BusinessService businessServiceForTest, List<TModel> tModelsForTest, boolean dontDeleteBindingTemplates) {
        this.businessServiceForTest = businessServiceForTest;
        this.tModelsForTest = (tModelsForTest != null)? new ArrayList<TModel>(tModelsForTest): null;
        this.dontDeleteBindingTempaltes = dontDeleteBindingTemplates;
    }

    public TestUddiClient(BusinessService businessServiceForTest, boolean isOverwrite, List<TModel> tModelsForTest) {
        this.businessServiceForTest = businessServiceForTest;
        this.tModelsForTest = new ArrayList<TModel>(tModelsForTest);
        this.isOverwrite = isOverwrite;
    }

    public TestUddiClient() {
    }

    public TestUddiClient(boolean dontCreateFakeServices) {
        this.dontCreateFakeServices = dontCreateFakeServices;
    }

    @Override
    public UDDIBusinessService getUDDIBusinessServiceInvalidKeyOk(String serviceKey) throws UDDIException {
        return null;
    }

    @Override
    public void deleteBusinessServiceByKey(String serviceKey) throws UDDIException {
        numServicesDeleted++;
    }

    @Override
    public Collection<WsdlPortInfo> listWsdlPortsForService(String serviceKey, boolean getFirstOnly) throws UDDIException {
        return null;
    }

    @Override
    public Collection<TModel> getTModels(Set<String> tModelKeys) throws UDDIException {
        return tModelsForTest;
    }

    @Override
    public void publishTModel(TModel tModelToPublish) throws UDDIException {
        if(tModelToPublish.getTModelKey() == null || tModelToPublish.getTModelKey().trim().isEmpty()){
            numTModelsWithNoKey++;
            final String key = "uddi:"+UUID.randomUUID();
            tModelToPublish.setTModelKey(key);
            if(tModelsForTest != null) tModelsForTest.add(tModelToPublish);
        }

        final List<OverviewDoc> overviewDoc = tModelToPublish.getOverviewDoc();
        for (OverviewDoc doc : overviewDoc) {
            if(doc.getOverviewURL().getUseType().equals("wsdlInterface")){
                uniqueWsdlUrls.add(doc.getOverviewURL().getValue());
            }
        }

        numTModelsPublished++;
    }

    @Override
    public TModel getTModel(String tModelKey) throws UDDIException {

        if(tModelsForTest != null && !tModelsForTest.isEmpty()){
            for (TModel tModel : tModelsForTest) {
                if(tModel.getTModelKey().equals(tModelKey)) return tModel;
            }

            throw new RuntimeException("tModel not found. tModelKey: " + tModelKey);
        }

        TModel testModel = new TModel();
        testModel.setTModelKey(tModelKey);
        return testModel;
    }

    @Override
    public BusinessService getBusinessService(String serviceKey) throws UDDIException {
        return businessServiceForTest;
    }

    @Override
    public void updateBusinessService(BusinessService businessService) throws UDDIException {

    }

    @Override
    public void publishBindingTemplate(BindingTemplate bindingTemplate) throws UDDIException {
        if(bindingTemplate.getBindingKey() == null || bindingTemplate.getBindingKey().trim().isEmpty()){
            numBindingTemplatesWithNoKey++;
            final String key = "uddi:"+UUID.randomUUID();
            bindingTemplate.setBindingKey(key);
        }
        numPublishedBindingTemplates++;
        publishedBindingTemplateKeys.add(bindingTemplate.getBindingKey());
        uniqueUrls.add(bindingTemplate.getAccessPoint().getValue());
    }

    @Override
    public void deleteBindingTemplateFromSingleService(Set<String> bindingKeys) throws UDDIException {

    }

    @Override
    public void authenticate() throws UDDIException {

    }

    @Override
    public String publishTModel(String tModelKey, String name, String description, Collection<UDDIKeyedReference> keyedReferences) throws UDDIException {
        return null;
    }

    @Override
    public String getBusinessEntityName(String businessKey) throws UDDIException {
        return null;
    }

    //used to collect results after a test
    private List<BusinessService> publishedServices = new ArrayList<BusinessService>();

    public List<BusinessService> getPublishedServices() {
        return publishedServices;
    }

    @Override
    public boolean publishBusinessService(BusinessService businessService) throws UDDIException {
        String key;
        if(businessService.getServiceKey() == null || businessService.getServiceKey().trim().isEmpty()){
            numServicesWithNoKey++;
            key = "uddi:"+UUID.randomUUID();
            businessService.setServiceKey(key);
        }else{
            key = businessService.getServiceKey();
        }

        BindingTemplates bindingTemplates = businessService.getBindingTemplates();
        List<BindingTemplate> allTemplates = bindingTemplates.getBindingTemplate();
        for(BindingTemplate bindingTemplate: allTemplates){
            if(bindingTemplate.getBindingKey() == null || bindingTemplate.getBindingKey().trim().isEmpty()){
                final String templateKey = "uddi:"+UUID.randomUUID();
                bindingTemplate.setBindingKey(templateKey);
                numBindingTemplatesWithNoKey++;
            }
            bindingTemplate.setServiceKey(key);
            uniqueUrls.add(bindingTemplate.getAccessPoint().getValue());
        }
        publishedServices.add(businessService);

        if(isOverwrite){
            businessServiceForTest = businessService;
        }
            
        return true;
    }

    @Override
    public void deleteTModel(Set<String> tModelKeys) throws UDDIException {
        numTModelsDeleted += tModelKeys.size();
    }

    @Override
    public void deleteTModel(String tModelKey) throws UDDIException {

    }

    @Override
    public void deleteBindingTemplate(String bindingKey) throws UDDIException {
        //remove this bindingTemplate from the businessServiceForTest
        final List<BindingTemplate> templates = businessServiceForTest.getBindingTemplates().getBindingTemplate();
        BindingTemplate toRemove = null;
        for (BindingTemplate template : templates) {
            if(template.getBindingKey() == bindingKey){
                toRemove = template;
            }
        }
        
        if(toRemove == null) throw new IllegalStateException("bindingKey not found in test business service");
        if(!dontDeleteBindingTempaltes) templates.remove(toRemove);
        numBindingsDeleted++;
    }

    public int getNumBindingsDeleted() {
        return numBindingsDeleted;
    }

    @Override
    public Collection<UDDINamedEntity> listBusinessEntities(String businessName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        return null;
    }

    @Override
    public List<BusinessService> getBusinessServices(Set<String> serviceKeys, boolean allowInvalidKeys) throws UDDIException {
        if (dataStructureForDownloaderTest != null) return dataStructureForDownloaderTest;

        List<BusinessService> returnColl = new ArrayList<BusinessService>();
        if(dontCreateFakeServices) return returnColl;

        if(businessServiceForTest != null) return Arrays.asList(businessServiceForTest);

        for(String s: serviceKeys){
            BusinessService service = new BusinessService();
            service.setServiceKey(s);
            returnColl.add(service);
        }

        return returnColl;

    }

    @Override
    public List<UDDIBusinessService> getUDDIBusinessServices(Set<String> serviceKeys, boolean allowInvalidKeys) throws UDDIException {
        return null;
    }

    // Not really interested in these for Bondo testing

    @Override
    public String getPolicyUrl(String policyKey) throws UDDIException {
        return null;
    }

    @Override
    public Collection<String> listPolicyUrlsByOrganization(String key) throws UDDIException {
        return null;
    }

    @Override
    public Collection<String> listPolicyUrlsByService(String key) throws UDDIException {
        return null;
    }

    @Override
    public Collection<String> listPolicyUrlsByEndpoint(String key) throws UDDIException {
        return null;
    }

    @Override
    public void referencePolicy(String serviceKey, String serviceUrl, String policyKey, String policyUrl, String description, Boolean force) throws UDDIException {

    }

    @Override
    public boolean removePolicyReference( final String serviceKey, final String policyKey, final String policyUrl ) {
        return false;
    }

    @Override
    public void addKeyedReference( final String serviceKey, final String keyedReferenceKey, final String keyedReferenceName, final String keyedReferenceValue ) {
    }

    @Override
    public boolean removeKeyedReference( final String serviceKey, final String keyedReferenceKey, final String keyedReferenceName, final String keyedReferenceValue ) {
        return false;
    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public Collection<UDDINamedEntity> listServices(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        return null;
    }

    @Override
    public Collection<UDDINamedEntity> listEndpoints(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        return null;
    }

    @Override
    public Collection<UDDINamedEntity> listOrganizations(String orgName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        return null;
    }

    @Override
    public Collection<WsdlPortInfo> listServiceWsdls(String serviceName, boolean caseSensitive, int offset, int maxRows, boolean noWsdlInfo) throws UDDIException {
        return null;
    }

    @Override
    public Collection<UDDINamedEntity> listPolicies(String policyName, String policyUrl) throws UDDIException {
        return null;
    }

    @Override
    public boolean listMoreAvailable() {
        return false;
    }

    @Override
    public String getBindingKeyForService( final String uddiServiceKey, final Collection<String> schemePreferences ) {
        return null; 
    }

    @Override
    public String publishPolicy(String tModelKey, String name, String description, String url) throws UDDIException {
        return null;
    }

    @Override
    public UDDIOperationalInfo getOperationalInfo( final String entityKey ) throws UDDIException {
        return null;
    }

    @Override
    public Collection<UDDIOperationalInfo> getOperationalInfos( final String... entityKey ) throws UDDIException {
        return null;
    }

    @Override
    public String subscribe( final long expiryTime, final long notificationInterval, final String bindingKey ) throws UDDIException {
        return null;
    }

    @Override
    public void deleteSubscription( final String subscriptionKey ) throws UDDIException {
    }

    @Override
    public UDDISubscriptionResults pollSubscription( final long startTime, final long endTime, final String subscriptionKey ) throws UDDIException {
        return null;
    }

    public int getNumServicesDeleted() {
        return numServicesDeleted;
    }

    public int getNumBindingTemplatesWithNoKey() {
        return numBindingTemplatesWithNoKey;
    }

    public int getNumTModelsWithNoKey() {
        return numTModelsWithNoKey;
    }

    public int getNumServicesWithNoKey() {
        return numServicesWithNoKey;
    }

    public Set<String> getUniqueUrls() {
        return uniqueUrls;
    }

    public Set<String> getUniqueWsdlUrls() {
        return uniqueWsdlUrls;
    }

    public int getNumTModelsDeleted() {
        return numTModelsDeleted;
    }

    public int getNumPublishedBindingTemplates() {
        return numPublishedBindingTemplates;
    }

    public Set<String> getPublishedBindingTemplateKeys() {
        return publishedBindingTemplateKeys;
    }

    public int getNumTModelsPublished() {
        return numTModelsPublished;
    }
}
