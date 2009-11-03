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

public class TestUddiClient implements UDDIClient{

    private List<BusinessService> dataStructureForDownloaderTest;

    public TestUddiClient(List<BusinessService> dataStructureForDownloaderTest) {
        this.dataStructureForDownloaderTest = dataStructureForDownloaderTest;
    }

    public TestUddiClient() {
    }

    @Override
    public void authenticate() throws UDDIException {

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
    public boolean publishTModel(TModel tModel) throws UDDIException {
        final String key = "uddi:"+UUID.randomUUID();
        tModel.setTModelKey(key);
        return true;
    }

    @Override
    public TModel getTModel(String tModelKey) throws UDDIException {
        TModel testModel = new TModel();
        testModel.setTModelKey(tModelKey);
        return testModel;
    }

    @Override
    public void deleteTModel(String tModelKey) throws UDDIException {

    }

    @Override
    public Set<String> deleteBusinessService(String serviceKey) throws UDDIException {
        return null;
    }

    @Override
    public void deleteBusinessServices(Collection<BusinessService> businessServices) throws UDDIException {

    }

    @Override
    public void deleteBusinessServicesByKey(Collection<String> serviceKeys) throws UDDIException {

    }

    @Override
    public void deleteMatchingTModels(TModel prototype) throws UDDIException {

    }

    @Override
    public BusinessService getBusinessService(String serviceKey) throws UDDIException {
        return null;
    }

    @Override
    public Collection<UDDINamedEntity> listBusinessEntities(String businessName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
        return null;
    }

    @Override
    public List<BusinessService> getBusinessServices(Set<String> serviceKeys) throws UDDIException {
        return (dataStructureForDownloaderTest != null)? dataStructureForDownloaderTest: Collections.<BusinessService>emptyList();
    }

    @Override
    public void deleteTModel(TModel tModel) throws UDDIException {

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
    public String publishPolicy(String name, String description, String url) throws UDDIException {
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
