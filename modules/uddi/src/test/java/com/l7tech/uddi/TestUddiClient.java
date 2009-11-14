/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 19, 2009
 * Time: 5:54:09 PM
 */
package com.l7tech.uddi;

import com.l7tech.common.uddi.guddiv3.BusinessService;
import com.l7tech.common.uddi.guddiv3.TModel;
import com.l7tech.common.uddi.guddiv3.BindingTemplate;
import com.l7tech.common.uddi.guddiv3.BindingTemplates;

import java.util.*;
import java.io.IOException;

public class TestUddiClient implements UDDIClient, JaxWsUDDIClient{

    private List<BusinessService> dataStructureForDownloaderTest;
    private BusinessService businessServiceForTest;
    private List<TModel> tModelsForTest;

    public TestUddiClient(List<BusinessService> dataStructureForDownloaderTest) {
        this.dataStructureForDownloaderTest = dataStructureForDownloaderTest;
    }


    public TestUddiClient(BusinessService businessServiceForTest, List<TModel> tModelsForTest) {
        this.businessServiceForTest = businessServiceForTest;
        this.tModelsForTest = tModelsForTest;
    }

    public TestUddiClient() {
    }

    @Override
    public UDDIBusinessService getUDDIBusinessServiceInvalidKeyOk(String serviceKey) throws UDDIException {
        return null;
    }

    @Override
    public Collection<TModel> getTModels(Set<String> tModelKeys) throws UDDIException {
        return tModelsForTest;
    }

    @Override
    public boolean publishTModel(TModel tModelToPublish) throws UDDIException {
        final String key = "uddi:"+UUID.randomUUID();
        tModelToPublish.setTModelKey(key);
        return true;
    }

    @Override
    public TModel getTModel(String tModelKey) throws UDDIException {
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
        final String key = "uddi:"+UUID.randomUUID();
        bindingTemplate.setBindingKey(key);
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
    public void deleteUDDIBusinessServices(Collection<UDDIBusinessService> businessServices) throws UDDIException {

    }

    @Override
    public String getBusinessEntityName(String businessKey) throws UDDIException {
        return null;
    }

    @Override
    public boolean publishBusinessService(BusinessService businessService) throws UDDIException {
        final String key = "uddi:"+UUID.randomUUID();
        businessService.setServiceKey(key);

        BindingTemplates bindingTemplates = businessService.getBindingTemplates();
        List<BindingTemplate> allTemplates = bindingTemplates.getBindingTemplate();
        for(BindingTemplate bindingTemplate: allTemplates){
            final String templateKey = "uddi:"+UUID.randomUUID();
            bindingTemplate.setServiceKey(key);
            bindingTemplate.setBindingKey(templateKey);
        }
        return true;
    }

    @Override
    public void deleteTModel(Set<String> tModelKeys) throws UDDIException {

    }

    @Override
    public void deleteTModel(String tModelKey) throws UDDIException {

    }

    @Override
    public void deleteBindingTemplate(String bindingKey) throws UDDIException {

    }

    @Override
    public Set<String> deleteBusinessService(String serviceKey) throws UDDIException {
        return null;
    }

    @Override
    public void deleteBusinessServicesByKey(Collection<String> serviceKeys) throws UDDIException {

    }

    @Override
    public Collection<UDDINamedEntity> listBusinessEntities(String businessName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        return null;
    }

    @Override
    public List<BusinessService> getBusinessServices(Set<String> serviceKeys, boolean allowInvalidKeys) throws UDDIException {
        if (dataStructureForDownloaderTest != null) return dataStructureForDownloaderTest;

        BusinessService service = new BusinessService();
        service.setServiceKey("serviceKey");
        return Arrays.asList(service);

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
    public Collection<WsdlPortInfo> listServiceWsdls(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
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
    public String getBindingKeyForService( final String uddiServiceKey ) {
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
}
