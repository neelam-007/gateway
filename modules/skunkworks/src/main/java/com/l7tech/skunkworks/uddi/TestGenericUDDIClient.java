package com.l7tech.skunkworks.uddi;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.uddi.*;
import com.l7tech.util.Pair;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.example.manager.apidemo.SsgAdminSession;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.util.*;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

import junit.framework.Assert;

import javax.wsdl.WSDLException;
import javax.security.auth.login.LoginException;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Test the GenericUDDIClient implementation of UDDIClient with respect to publishing information to a UDDI Registry
 *
 * These tests requies a working UDDI Registry
 *
 * @author darmstrong
 */
public class TestGenericUDDIClient {

    private List<String> tModelKeys = new ArrayList<String>();
    private List<String> serviceKeys = new ArrayList<String>();
    private UDDIClient uddiClient = getUDDIClient();

    @Before
    public void setUp(){
        tModelKeys.clear();
        serviceKeys.clear();
    }

    @After
    public void tearDown() throws UDDIException {
        //Delete all published info
        for(String key: tModelKeys){
            System.out.println("Deleting tModel: " + key);
            uddiClient.deleteTModel(key);                        
        }

        for(String key: serviceKeys){
            System.out.println("Deleting service: " + key);
            uddiClient.deleteBusinessService(key);
        }
    }

    /**
     * Test the successful publish of a tModel to UDDI.
     *
     * Also test that once published successfully, a second publish attempt will return false, as the previously
     * published tModel should have been found
     */
    @Test
    public void testPublishTModel() throws UDDIException {
        //Create a tModel representing a wsdl:portType

        TModel tModel = new TModel();
        Name name = new Name();
        name.setLang("en-US");
        name.setValue("Skunkworks portType tModel " + Calendar.getInstance().getTimeInMillis());
        tModel.setName(name);

        CategoryBag categoryBag = new CategoryBag();
        tModel.setCategoryBag(categoryBag);
        List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();

        //portType type ref
        KeyedReference typeRef = new KeyedReference();
        typeRef.setKeyValue("portType");
        typeRef.setTModelKey(WsdlToUDDIModelConverter.UDDI_WSDL_TYPES);
        keyedReferences.add(typeRef);

        //namespace ref
        KeyedReference nameSpaceRef = new KeyedReference();
        nameSpaceRef.setKeyValue("http://skunkworks.com/porttypenamespace");
        nameSpaceRef.setKeyName("portType namespace ");
        nameSpaceRef.setTModelKey(WsdlToUDDIModelConverter.UDDI_XML_NAMESPACE);
        keyedReferences.add(nameSpaceRef);

        //overview docs etc are considered in the logic in TestGenericUDDIClient, but not by UDDI find code
        OverviewDoc overviewDoc = new OverviewDoc();
        OverviewURL overviewURL = new OverviewURL();
        overviewURL.setUseType("wsdlInterface");
        overviewURL.setValue("http://skunkworks.ssg.com/serviceoid=23423432");
        overviewDoc.setOverviewURL(overviewURL);

        tModel.getOverviewDoc().add(overviewDoc);

        boolean published = uddiClient.publishTModel(tModel);
        Assert.assertTrue("TModel should have been published", published);
        tModelKeys.add(tModel.getTModelKey());

        published = uddiClient.publishTModel(tModel);
        Assert.assertFalse("TModel should not have been published", published);
        
    }

    @Test
    public void testPublishProxyBusinessService() throws UDDIException, WSDLException {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader("Warehouse.wsdl"));

        long serviceOid = Calendar.getInstance().getTimeInMillis();
        System.out.println("Service OID is: " + serviceOid);
        final String gatewayWsdlUrl = "http://localhost:8080/" + serviceOid + "?wsdl";
        final String gatewayURL = "http://localhost:8080/" + serviceOid;

        final String businessKey = "uddi:c4f2cbdd-beab-11de-8126-f78857d54072";//this exists in my local uddi registry
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, gatewayWsdlUrl, gatewayURL, businessKey, serviceOid, null);
        Pair<List<BusinessService>, Map<String, TModel>> servicesAndTModels = wsdlToUDDIModelConverter.convertWsdlToUDDIModel();

        BusinessServicePublisher servicePublisher = new BusinessServicePublisher();
        servicePublisher.publishServicesToUDDIRegistry(uddiClient, servicesAndTModels.left, servicesAndTModels.right, null);

        //Place a break point here, and examine the UDDI Registry. The code below will delete everything published
        for(BusinessService businessService: servicesAndTModels.left){
            serviceKeys.add(businessService.getServiceKey());
        }
    }

    /**
     * Test the ServiceAdmin api for creating a UDDIProxiedService and corresponding BusinessService in UDDI Registry
     *
     * This test requies a configured UDDI Registry and a running gateway
     */
    @Test
    public void testUDDIProxyEntityPublish()
            throws MalformedURLException, LoginException, RemoteException, FindException,
            UDDIRegistryAdmin.PublishProxiedServiceException, VersionException, SaveException, UpdateException {

        System.setProperty("com.l7tech.console.suppressVersionCheck", "true");

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

        Collection<UDDIRegistry> uddiRegistries = uddiRegistryAdmin.findAllUDDIRegistries();
        com.l7tech.gateway.common.uddi.UDDIRegistry activeSoa = null;
        for(com.l7tech.gateway.common.uddi.UDDIRegistry uddiRegistry: uddiRegistries){
            if(UDDIRegistry.UDDIRegistryType.findType(uddiRegistry.getUddiRegistryType()) == UDDIRegistry.UDDIRegistryType.CENTRASITE_ACTIVE_SOA){
                activeSoa = uddiRegistry;
                break;
            }
        }

        if(activeSoa == null) throw new IllegalStateException("Gateway does not have any ActiveSOA UDDI registries configured");

        ServiceAdmin serviceAdmin = ssgAdminSession.getServiceAdmin();
        final String serviceOid = "70615040";
        PublishedService serviceToPublish = serviceAdmin.findServiceByID(serviceOid);
        Assert.assertNotNull("Service with id not found: " + serviceOid, serviceToPublish);

        final String businessKey = "uddi:c4f2cbdd-beab-11de-8126-f78857d54072";//this exists in my local uddi registry
        UDDIProxiedService uddiProxiedService = new UDDIProxiedService(serviceToPublish.getOid(),
                activeSoa.getOid(),
                businessKey,
                "Skunkworks Organization", false);

        uddiRegistryAdmin.publishGatewayWsdl(uddiProxiedService);

        System.clearProperty("com.l7tech.console.suppressVersionCheck");
    }

    /**
     * Test the ServiceAdmin api for updating a UDDIProxiedService and corresponding BusinessService in UDDI Registry
     *
     * This test completely overwrites the WSDL, which results in the complete removal of the initial WSDL in UDDI
     * and is replaced by a brand new set of BusinessServices and tModels
     *
     * This test requies a configured UDDI Registry, with a previously published BusinessService and a running gateway
     */
    @Test
    public void testUDDIProxyEntityUpdate()
            throws IOException, LoginException, FindException,
            UDDIRegistryAdmin.PublishProxiedServiceException, VersionException, SaveException, UpdateException, WSDLException, SAXException, PolicyAssertionException {

        System.setProperty("com.l7tech.console.suppressVersionCheck", "true");

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

        Collection<UDDIRegistry> uddiRegistries = uddiRegistryAdmin.findAllUDDIRegistries();
        com.l7tech.gateway.common.uddi.UDDIRegistry activeSoa = null;
        for(com.l7tech.gateway.common.uddi.UDDIRegistry uddiRegistry: uddiRegistries){
            if(UDDIRegistry.UDDIRegistryType.findType(uddiRegistry.getUddiRegistryType()) == UDDIRegistry.UDDIRegistryType.CENTRASITE_ACTIVE_SOA){
                activeSoa = uddiRegistry;
                break;
            }
        }

        if(activeSoa == null) throw new IllegalStateException("Gateway does not have any ActiveSOA UDDI registries configured");

        ServiceAdmin serviceAdmin = ssgAdminSession.getServiceAdmin();
        final String serviceOid = "70615040";
        PublishedService serviceToPublish = serviceAdmin.findServiceByID(serviceOid);
        //this service has already had it's WSDL published
        Assert.assertNotNull("Service with id not found: " + serviceOid, serviceToPublish);

        //update the contents of the wsdl's xml to simulate it's WSDL having changed
        InputStream inputStream = this.getClass().getResourceAsStream("PlayerStats.wsdl"); //completely change the wsdl - a different namespace!
        Document dom = XmlUtil.parse(inputStream);
        serviceToPublish.setWsdlXml(XmlUtil.nodeToString(dom));
        serviceAdmin.savePublishedService(serviceToPublish);

        UDDIProxiedService uddiProxiedService = uddiRegistryAdmin.getUDDIProxiedService(serviceToPublish.getOid());
        if(uddiProxiedService == null) throw new IllegalStateException("UDDIProxiedService not found");
        uddiRegistryAdmin.publishGatewayWsdl(uddiProxiedService);

        System.clearProperty("com.l7tech.console.suppressVersionCheck");
    }

    /**
     * Test the ServiceAdmin api for updating a UDDIProxiedService and corresponding BusinessService in UDDI Registry
     *
     * This test only updates the wsdl:binding, so the existing BusinessService in UDDI should be resused.
     *
     * This test requies a configured UDDI Registry, with a previously published BusinessService and a running gateway
     */
    @Test
    public void testUDDIProxyEntityPartialUpdate()
            throws IOException, LoginException, FindException,
            UDDIRegistryAdmin.PublishProxiedServiceException, VersionException, SaveException, UpdateException, WSDLException, SAXException, PolicyAssertionException {

        System.setProperty("com.l7tech.console.suppressVersionCheck", "true");

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

        Collection<UDDIRegistry> uddiRegistries = uddiRegistryAdmin.findAllUDDIRegistries();
        com.l7tech.gateway.common.uddi.UDDIRegistry activeSoa = null;
        for(com.l7tech.gateway.common.uddi.UDDIRegistry uddiRegistry: uddiRegistries){
            if(UDDIRegistry.UDDIRegistryType.findType(uddiRegistry.getUddiRegistryType()) == UDDIRegistry.UDDIRegistryType.CENTRASITE_ACTIVE_SOA){
                activeSoa = uddiRegistry;
                break;
            }
        }

        if(activeSoa == null) throw new IllegalStateException("Gateway does not have any ActiveSOA UDDI registries configured");

        ServiceAdmin serviceAdmin = ssgAdminSession.getServiceAdmin();
        final String serviceOid = "70615040";
        PublishedService serviceToPublish = serviceAdmin.findServiceByID(serviceOid);
        //this service has already had it's WSDL published
        Assert.assertNotNull("Service with id not found: " + serviceOid, serviceToPublish);

        //update the contents of the wsdl's xml to simulate it's WSDL having changed
        InputStream inputStream = this.getClass().getResourceAsStream("Warehouse_modified.wsdl"); //completely change the wsdl - a different namespace!
        Document dom = XmlUtil.parse(inputStream);
        serviceToPublish.setWsdlXml(XmlUtil.nodeToString(dom));
        serviceAdmin.savePublishedService(serviceToPublish);

        UDDIProxiedService uddiProxiedService = uddiRegistryAdmin.getUDDIProxiedService(serviceToPublish.getOid());
        if(uddiProxiedService == null) throw new IllegalStateException("UDDIProxiedService not found");
        uddiRegistryAdmin.publishGatewayWsdl(uddiProxiedService);

        System.clearProperty("com.l7tech.console.suppressVersionCheck");
    }

    @Test
    public void testGetUDDIProxiedService() throws MalformedURLException, LoginException, RemoteException, FindException {
        System.setProperty("com.l7tech.console.suppressVersionCheck", "true");

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

        UDDIProxiedService service = uddiRegistryAdmin.getUDDIProxiedService(70615040);
        System.out.println(service.getGeneralKeywordServiceIdentifier());

        System.clearProperty("com.l7tech.console.suppressVersionCheck");
    }

    @Test
    public void testDeleteUDDIProxiedService()
            throws MalformedURLException, LoginException, RemoteException, FindException, DeleteException, UDDIException {
        System.setProperty("com.l7tech.console.suppressVersionCheck", "true");

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

        UDDIProxiedService proxiedService = uddiRegistryAdmin.getUDDIProxiedService(70615040);
        uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(proxiedService);

        System.clearProperty("com.l7tech.console.suppressVersionCheck");
    }

    @Test
    public void testUDDIClientDeleteServiceByKeyword() throws UDDIException {
        final String keyWord = "70615040";

        uddiClient.deleteAllBusinessServicesForGatewayWsdl(keyWord);
    }

    @Test
    public void testFindBusinessEntity() throws UDDIException {
        final String keyWord = "70615040";

        uddiClient.deleteAllBusinessServicesForGatewayWsdl(keyWord);
    }

    @Test
    public void testGenericTModelDelete() throws UDDIException {
        TModel tModel = new TModel();
        Name name = new Name();
        name.setValue("%Warehouse%");
//        name.setValue("%Skunk%");
        tModel.setName(name);

        uddiClient.deleteMatchingTModels(tModel);
    }

    @Test
    public void testGenericBusinessServiceDelete() throws UDDIException {
        uddiClient.deleteBusinessService("uddi:03c6fc80-c02c-11de-8342-cc4d177a16de");
    }

    @Test
    public void testTModelWithGeneralKeyword() throws UDDIException {
        KeyedReference keyWordRef = new KeyedReference();
//        keyWordRef.setKeyName("Snoopy");
        keyWordRef.setKeyName(WsdlToUDDIModelConverter.LAYER7_PROXY_SERVICE_GENERAL_KEYWORD_URN);
        keyWordRef.setKeyValue("Snoopy1");
        keyWordRef.setTModelKey(WsdlToUDDIModelConverter.UDDI_GENERAL_KEYWORDS);

        CategoryBag categoryBag = new CategoryBag();
        categoryBag.getKeyedReference().add(keyWordRef);

        TModel testModel = new TModel();
        Name name = new Name();
        name.setValue("Test Model");
        testModel.setName(name);

        testModel.setCategoryBag(categoryBag);

        uddiClient.publishTModel(testModel);

        tModelKeys.add(testModel.getTModelKey());

    }
    /**
     * Bug number 7898
     * @throws UDDIException
     */
    @Test
    public void testUDDIBug() throws UDDIException {
        UDDIClient uddiClient = new GenericUDDIClient("http://rsbcentos.l7tech.com:8080/juddiv3/services/inquiry",
                        "http://rsbcentos.l7tech.com:8080/juddiv3/services/publish",
                        null,
                        "http://rsbcentos.l7tech.com:8080/juddiv3/services/security",
                        "root",
                        "root",
                        PolicyAttachmentVersion.v1_2/*not important here*/);


        uddiClient.authenticate();

        Collection<UDDINamedEntity> uddiNamedEntities = uddiClient.listServices("%", false, 169401, 100);
        System.out.println("There were this many results: " + uddiNamedEntities.size());
        boolean moreAvail = uddiClient.listMoreAvailable();
        System.out.println("More are available: " + moreAvail);

    }

    private UDDIClient getUDDIClient(){
        return new GenericUDDIClient("http://DONALWINXP:53307/UddiRegistry/inquiry",
                        "http://DONALWINXP:53307/UddiRegistry/publish",
                        null,
                        "http://DONALWINXP:53307/UddiRegistry/security",
                        "administrator",
                        "7layer",
                        PolicyAttachmentVersion.v1_2/*not important here*/);
    }

    public Reader getWsdlReader(String resourcetoread) {
        InputStream inputStream = this.getClass().getResourceAsStream(resourcetoread);
        return new InputStreamReader(inputStream);
    }

}
