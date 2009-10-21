package com.l7tech.skunkworks.uddi;

import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.uddi.*;
import com.l7tech.util.SyspropUtil;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.junit.Test;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Offline tests for working with UDDI.
 *
 * @author darmstrong
 */
public class UddiTests {

    private static final String UDDIV3_NAMESPACE = "urn:uddi-org:api_v3_service";

    @Test
    public void testGetAuthToken() throws DispositionReportFaultMessage, MalformedURLException, UDDIException {
        final String authToken = getAuthToken();
        System.out.println(authToken);
    }

    private String getAuthToken() throws DispositionReportFaultMessage {
        UDDISecurityPortType securityPort = getSecurityPort();

        GetAuthToken getAuthToken = new GetAuthToken();
        getAuthToken.setUserID("administrator");
        getAuthToken.setCred("7layer");
        return securityPort.getAuthToken(getAuthToken).getAuthInfo();
    }

    @Test
    public void testPublishProxiedBusinessService() {

        BindingTemplate bindingTemplate = new BindingTemplate();
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setUseType("endPoint");
        accessPoint.setValue("http://irishman:8080/testservice");
        bindingTemplate.setAccessPoint(accessPoint);

        InstanceDetails instanceDetails = new InstanceDetails();
        Description desc = new Description();
        desc.setLang("en-US");
        desc.setValue("the wsdl:binding that this wsdl:port implements");
        instanceDetails.getDescription().add(desc);
        instanceDetails.setInstanceParms("WarehouseSoap");
        
        TModelInstanceInfo tModelInstanceInfo = new TModelInstanceInfo();
        tModelInstanceInfo.setTModelKey("uddi:602e38c2-b9b2-11de-8126-b25b792da72e");
        tModelInstanceInfo.setInstanceDetails(instanceDetails);

        TModelInstanceInfo tModelInstanceInfo1 = new TModelInstanceInfo();
        tModelInstanceInfo1.setTModelKey("uddi:5fc2f3d2-b9b2-11de-8126-cd00b5e8a015");

        TModelInstanceDetails tModelInstanceDetails = new TModelInstanceDetails();
        tModelInstanceDetails.getTModelInstanceInfo().add(tModelInstanceInfo);
        tModelInstanceDetails.getTModelInstanceInfo().add(tModelInstanceInfo1);
        bindingTemplate.setTModelInstanceDetails(tModelInstanceDetails);

        CategoryBag categoryBag = new CategoryBag();
        KeyedReference svcNameSpace = new KeyedReference();
        svcNameSpace.setKeyName("service namespace");
        svcNameSpace.setKeyValue("http://warehouse.acme.com/ws");
        svcNameSpace.setTModelKey("uddi:uddi.org:xml:namespace");
        categoryBag.getKeyedReference().add(svcNameSpace);

        KeyedReference svcLocalName = new KeyedReference();
        svcLocalName.setKeyName("service local name");
        svcLocalName.setKeyValue("Proxied Warehouse");
        svcLocalName.setTModelKey("uddi:uddi.org:xml:localname");
        categoryBag.getKeyedReference().add(svcLocalName);

        KeyedReference svcRef = new KeyedReference();
        svcRef.setKeyValue("service");
        svcRef.setTModelKey("uddi:uddi.org:wsdl:types");
        categoryBag.getKeyedReference().add(svcRef);

        bindingTemplate.setCategoryBag(categoryBag);

        BusinessService proxiedService = new BusinessService();
        proxiedService.setBusinessKey("uddi:207ff1cc-25c5-544c-415c-5d98ea91060c");
        Name name = new Name();
        name.setLang("en-US");
        name.setValue("Proxied Warehouse 1");
        proxiedService.getName().add(name);

        BindingTemplates bindingTemplates = new BindingTemplates();
        bindingTemplates.getBindingTemplate().add(bindingTemplate);

        proxiedService.setBindingTemplates(bindingTemplates);

        final String authToken;
        try {
            authToken = getAuthToken();
            SaveService saveService = new SaveService();
            saveService.setAuthInfo(authToken);
            saveService.getBusinessService().add(proxiedService);

            UDDIPublicationPortType publicationPort = getPublishPort();
            ServiceDetail svcDetail = publicationPort.saveService(saveService);

            System.out.println("Service Key: " +svcDetail.getBusinessService().get(0).getServiceKey());

        } catch (DispositionReportFaultMessage dispositionReportFaultMessage) {
            List<Result> results = dispositionReportFaultMessage.getFaultInfo().getResult();
            for(Result r: results){
                System.out.println(r.getErrInfo().getValue());
            }

            dispositionReportFaultMessage.printStackTrace();            
        }
    }

    private <T> T get(List<T> list, String description, boolean onlyOne) throws UDDIException {
        if (list == null || list.isEmpty()) {
            throw new UDDIException("Missing " + description);
        } else if (onlyOne && list.size()!=1) {
            throw new UDDIException("Duplicate " + description);
        }

        return list.get(0);
    }

    private UDDISecurityPortType getSecurityPort() {
        UDDISecurity security = new UDDISecurity(buildUrl("resources/uddi_v3_service_s.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDISecurity"));
        UDDISecurityPortType securityPort = security.getUDDISecurityPort();
        stubConfig(securityPort, "http://DONALWINXP:53307/UddiRegistry/security");
        return securityPort;
    }

    protected UDDIPublicationPortType getPublishPort() {
        UDDIPublication publication = new UDDIPublication(buildUrl("resources/uddi_v3_service_p.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDIPublication"));
        UDDIPublicationPortType publicationPort = publication.getUDDIPublicationPort();
        stubConfig(publicationPort, "http://DONALWINXP:53307/UddiRegistry/publish");
        return publicationPort;
    }

    protected URL buildUrl(String relativeUrl) {
        return UDDIInquiry.class.getResource(relativeUrl);
    }

    private void stubConfig(Object proxy, String url) {
        BindingProvider bindingProvider = (BindingProvider) proxy;
        Binding binding = bindingProvider.getBinding();
        Map<String,Object> context = bindingProvider.getRequestContext();
        List<Handler> handlerChain = new ArrayList();

        // Add handler to fix any issues with invalid faults
        handlerChain.add(new FaultRepairSOAPHandler());

        // Add handler to fix namespace in on Java 5 / SSM
        if ( "1.5".equals(SyspropUtil.getProperty("java.specification.version")) ) {
            handlerChain.add(new NamespaceRepairSOAPHandler());
        }

        // Set handlers
        binding.setHandlerChain(handlerChain);

        // Set endpoint
        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
    }

}
