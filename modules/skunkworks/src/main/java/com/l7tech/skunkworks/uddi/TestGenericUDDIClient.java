package com.l7tech.skunkworks.uddi;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.uddi.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.example.manager.apidemo.SsgAdminSession;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.util.Pair;

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

    @Test
    public void testPublishProxyBusinessService() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader("Warehouse.wsdl"));

        long serviceOid = Calendar.getInstance().getTimeInMillis();
        System.out.println("Service OID is: " + serviceOid);
        final String gatewayWsdlUrl = "http://localhost:8080/" + serviceOid + "?wsdl";
        final String gatewayURL = "http://localhost:8080/" + serviceOid;

        final String businessKey = "uddi:c4f2cbdd-beab-11de-8126-f78857d54072";//this exists in my local uddi registry
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, gatewayWsdlUrl, gatewayURL, businessKey, serviceOid);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel();

        final List<Pair<BusinessService, Map<String, TModel>>> serviceToDependentModels = wsdlToUDDIModelConverter.getServicesAndDependentTModels();

//        BusinessServicePublisher servicePublisher = new BusinessServicePublisher(wsdl, serviceOid, uddiClient){};
//        servicePublisher.publishServicesToUDDIRegistry(uddiClient, wsdlToUDDIModelConverter.getBusinessServices(),
//                wsdlToUDDIModelConverter.getKeysToPublishedTModels());

        //Place a break point here, and examine the UDDI Registry. The code below will delete everything published
        for(Pair<BusinessService, Map<String, TModel>> sericeToModel: serviceToDependentModels){
            BusinessService businessService = sericeToModel.left;
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
            UDDIRegistryAdmin.PublishProxiedServiceException, VersionException, SaveException, UpdateException, UDDIRegistryAdmin.UDDIRegistryNotEnabledException {

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
        final String serviceOid = "87293952";
        PublishedService serviceToPublish = serviceAdmin.findServiceByID(serviceOid);
        Assert.assertNotNull("Service with id not found: " + serviceOid, serviceToPublish);

        final String businessKey = "uddi:c4f2cbdd-beab-11de-8126-f78857d54072";//this exists in my local uddi registry

        uddiRegistryAdmin.publishGatewayWsdl(Long.valueOf(serviceOid), activeSoa.getOid(), businessKey, "Skunkworks Organization", false);

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
            UDDIRegistryAdmin.PublishProxiedServiceException, VersionException, SaveException, UpdateException, WSDLException, SAXException, PolicyAssertionException, UDDIRegistryAdmin.UDDIRegistryNotEnabledException, DeleteException {

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

        UDDIProxiedServiceInfo uddiProxiedServiceInfo = uddiRegistryAdmin.getUDDIProxiedServiceInfo(serviceToPublish.getOid());
        if(uddiProxiedServiceInfo == null) throw new IllegalStateException("UDDIProxiedService not found");
        uddiRegistryAdmin.updatePublishedGatewayWsdl(uddiProxiedServiceInfo.getOid());

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
            UDDIRegistryAdmin.PublishProxiedServiceException, VersionException, SaveException, UpdateException, WSDLException, SAXException, PolicyAssertionException, UDDIRegistryAdmin.UDDIRegistryNotEnabledException, DeleteException {

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
        final String serviceOid = "91783168";
        PublishedService serviceToPublish = serviceAdmin.findServiceByID(serviceOid);
        //this service has already had it's WSDL published
        Assert.assertNotNull("Service with id not found: " + serviceOid, serviceToPublish);

        //update the contents of the wsdl's xml to simulate it's WSDL having changed
        InputStream inputStream = this.getClass().getResourceAsStream("Warehouse.wsdl"); //completely change the wsdl - a different namespace!
        Document dom = XmlUtil.parse(inputStream);
        serviceToPublish.setWsdlXml(XmlUtil.nodeToString(dom));
        serviceAdmin.savePublishedService(serviceToPublish);

        UDDIProxiedServiceInfo uddiProxiedServiceInfo = uddiRegistryAdmin.getUDDIProxiedServiceInfo(serviceToPublish.getOid());
        if(uddiProxiedServiceInfo == null) throw new IllegalStateException("UDDIProxiedServiceInfo not found");
        uddiRegistryAdmin.updatePublishedGatewayWsdl(uddiProxiedServiceInfo.getOid());

        System.clearProperty("com.l7tech.console.suppressVersionCheck");
    }

    @Test
    public void testDeleteUDDIProxiedService()
            throws MalformedURLException, LoginException, RemoteException, FindException, DeleteException, UDDIException, UDDIRegistryAdmin.UDDIRegistryNotEnabledException {
        System.setProperty("com.l7tech.console.suppressVersionCheck", "true");

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

//        UDDIProxiedService proxiedService = uddiRegistryAdmin.getUDDIProxiedServiceInfo(70615040);
//        uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(proxiedService);

        System.clearProperty("com.l7tech.console.suppressVersionCheck");
    }

    /**
     * Test that the deleteTModel successfully determines when a tModel is safe to delete
     *
     * Test is for stepping through, Asserts not used here
     *
     * Requirements: Test requires a running UDDI. Gateway is not needed
     * Need a tModelKey for a tModel which is referenced by a Business Service
     *
     * @throws UDDIException
     */
    @Test
    public void testTModelReferencedDelete() throws UDDIException{
uddiClient.deleteTModel("uddi:ee613b50-cb2d-11de-8486-c4344922f660");
uddiClient.deleteTModel("uddi:edfab150-cb2d-11de-8486-fdc8e7430f2c");
    
    }

    @Test
    public void testGenericBusinessServiceDelete() throws UDDIException {
        Set<String> tModelsToDelete = new HashSet<String>();
        tModelsToDelete.addAll(uddiClient.deleteBusinessService("uddi:03c6fc80-c02c-11de-8342-cc4d177a16de"));
        for(String tModelKey: tModelsToDelete){
            uddiClient.deleteTModel(tModelKey);
        }
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
                        PolicyAttachmentVersion.v1_2/*not important here*/, null){};


        uddiClient.authenticate();

        Collection<UDDINamedEntity> uddiNamedEntities = uddiClient.listServices("%", false, 169401, 100);
        System.out.println("There were this many results: " + uddiNamedEntities.size());
        boolean moreAvail = uddiClient.listMoreAvailable();
        System.out.println("More are available: " + moreAvail);

    }

    @Test
    public void testDeleteUDDIServiceControl() throws Exception {
        System.setProperty("com.l7tech.console.suppressVersionCheck", "true");

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

        uddiRegistryAdmin.deleteUDDIServiceControl(70615040);

        System.clearProperty("com.l7tech.console.suppressVersionCheck");
    }

    private UDDIClient getUDDIClient(){
        return new GenericUDDIClient("http://DONALWINXP:53307/UddiRegistry/inquiry",
                        "http://DONALWINXP:53307/UddiRegistry/publish",
                        null,
                        "http://DONALWINXP:53307/UddiRegistry/security",
                        "administrator",
                        "7layer",
                        PolicyAttachmentVersion.v1_2/*not important here*/, null){};
    }

    public Reader getWsdlReader(String resourcetoread) {
        InputStream inputStream = this.getClass().getResourceAsStream(resourcetoread);
        return new InputStreamReader(inputStream);
    }

}
