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

import java.util.Collection;
import java.util.UUID;
import java.util.List;
import java.io.IOException;

public class TestUddiClient implements UDDIClient{
    @Override
    public void authenticate() throws UDDIException {

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
        return null;
    }

    @Override
    public void deleteTModel(String tModelKey) throws UDDIException {

    }

    @Override
    public void deleteBusinessService(String serviceKey) throws UDDIException {

    }

    @Override
    public void deleteMatchingTModels(TModel prototype) throws UDDIException {

    }

    @Override
    public void deleteAllBusinessServicesForGatewayWsdl(String generalKeyword) throws UDDIException {

    }

    @Override
    public BusinessService getBusinessService(String serviceKey) throws UDDIException {
        return null;
    }

    @Override
    public Collection<UDDINamedEntity> listBusinessEntities(String businessName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
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
    public Collection<UDDINamedEntity> listServiceWsdls(String serviceName, boolean caseSensitive, int offset, int maxRows) throws UDDIException {
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
    public String publishPolicy(String name, String description, String url) throws UDDIException {
        return null;
    }

}
