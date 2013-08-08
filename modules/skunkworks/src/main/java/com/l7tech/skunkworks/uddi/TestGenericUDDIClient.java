package com.l7tech.skunkworks.uddi;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.uddi.guddiv3.*;
import com.l7tech.example.manager.apidemo.SsgAdminSession;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.uddi.UDDIProxiedServiceInfo;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.uddi.*;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import com.l7tech.wsdl.Wsdl;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.security.auth.login.LoginException;
import javax.wsdl.WSDLException;
import java.io.*;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.LogManager;

/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 *
 * Any offline tests for testing with a real UDDI Registry
 *
 * @author darmstrong
 */
public class TestGenericUDDIClient {

    private List<String> tModelKeys = new ArrayList<String>();
    private List<String> serviceKeys = new ArrayList<String>();
    private UDDIClient uddiClient = getUDDIClient();

    @Before
    public void setUp(){
        initializeLogging();
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
            uddiClient.deleteBusinessServiceByKey(key);
        }
    }

    @Test
    public void testPublishProxyBusinessService() throws Exception {
        Wsdl wsdl = Wsdl.newInstance(null, getWsdlReader("Warehouse.wsdl"));

        Goid serviceGoid = new Goid(0,Calendar.getInstance().getTimeInMillis());
        System.out.println("Service GOID is: " + serviceGoid);
        final String gatewayWsdlUrl = "http://localhost:8080/" + serviceGoid + "?wsdl";
        final String gatewayURL = "http://localhost:8080/" + serviceGoid;

        EndpointPair endpointPair = new EndpointPair(gatewayURL, gatewayWsdlUrl);

        final String businessKey = "uddi:c4f2cbdd-beab-11de-8126-f78857d54072";//this exists in my local uddi registry
        WsdlToUDDIModelConverter wsdlToUDDIModelConverter = new WsdlToUDDIModelConverter(wsdl, businessKey);
        wsdlToUDDIModelConverter.convertWsdlToUDDIModel(Arrays.asList(endpointPair), "Layer7", Goid.toString(serviceGoid));

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

    @Test
    public void testDeleteBindingTemplates() throws UDDIException {
        final String bindingKey = "uddi:1e0d7f34-d8a9-11de-bd69-fab156656beb";
        uddiClient.deleteBindingTemplate(bindingKey);
    }

    @Test
    public void testDeleteProxiedBusinessService() throws UDDIException {
        final String serviceKey = "uddi:9259c9d0-cfe2-11de-a2bb-8068e9b1817d";
        uddiClient.deleteBusinessServiceByKey(serviceKey);
    }

    @Test
    public void updateBindingTemplateToCauseNotification() throws UDDIException{
//        String bindingKey = "uddi:847b0264-ea9a-11de-84ef-f026cfc018c3";
        String bindingKey = "uddi:74aace36-37a7-11df-a4ff-b77727d60711";
        GenericUDDIClient genericUDDIClient = (GenericUDDIClient) uddiClient;

//        final BusinessService businessService = genericUDDIClient.getBusinessService("uddi:84789164-ea9a-11de-84ef-ddc3284465e0");
        final BusinessService businessService = genericUDDIClient.getBusinessService("uddi:6aecf1a1-37a7-11df-a4ff-8a4c4c35872b");
        BindingTemplate foundTemplate = null;
        for(BindingTemplate bt: businessService.getBindingTemplates().getBindingTemplate()){
            if(bt.getBindingKey().equals(bindingKey)){
                foundTemplate = bt;
                break;
            }
        }

        Assert.assertNotNull(foundTemplate);

//        foundTemplate.getAccessPoint().setValue("http://thehostwedontwanttorouteto.com:1610/SpaceOrderofBattle1.asmx");
        foundTemplate.getAccessPoint().setValue("http://www.company.com/companyInfo_updated_7");
//        foundTemplate.getAccessPoint().setValue("http://hugh:8081/axis/services/PlayerStats/11");
//        foundTemplate.getAccessPoint().setValue("http://hugh/ACMEWarehouseWS/12/Service.asmx");
//        foundTemplate.getAccessPoint().setValue("http://www50.brinkster.com/vbfacileinpt/np2.asmx");
//        foundTemplate.getAccessPoint().setValue("http://ThisHostIsFromTheImportedDocument1.asmx");
        genericUDDIClient.publishBusinessService(businessService);
        //genericUDDIClient.publishBindingTemplate(foundTemplate);
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

        SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );

        SsgAdminSession ssgAdminSession = new SsgAdminSession("localhost", "admin", "password");
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

        uddiRegistryAdmin.publishGatewayWsdl(serviceToPublish, activeSoa.getGoid(), businessKey, "Skunkworks Organization", false, null);

        SyspropUtil.clearProperty( "com.l7tech.console.suppressVersionCheck" );
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

        SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );

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
        final String serviceGoid = new Goid(0,70615040).toHexString();
        PublishedService serviceToPublish = serviceAdmin.findServiceByID(serviceGoid);
        //this service has already had it's WSDL published
        Assert.assertNotNull("Service with id not found: " + serviceGoid, serviceToPublish);

        //update the contents of the wsdl's xml to simulate it's WSDL having changed
        InputStream inputStream = this.getClass().getResourceAsStream("PlayerStats.wsdl"); //completely change the wsdl - a different namespace!
        Document dom = XmlUtil.parse(inputStream);
        serviceToPublish.setWsdlXml(XmlUtil.nodeToString(dom));
        serviceAdmin.savePublishedService(serviceToPublish);

        UDDIProxiedServiceInfo uddiProxiedServiceInfo = uddiRegistryAdmin.findProxiedServiceInfoForPublishedService(serviceToPublish.getGoid());
        if(uddiProxiedServiceInfo == null) throw new IllegalStateException("UDDIProxiedService not found");
        uddiRegistryAdmin.updatePublishedGatewayWsdl(uddiProxiedServiceInfo.getGoid());

        SyspropUtil.clearProperty( "com.l7tech.console.suppressVersionCheck" );
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

        SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );

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

        UDDIProxiedServiceInfo uddiProxiedServiceInfo = uddiRegistryAdmin.findProxiedServiceInfoForPublishedService(serviceToPublish.getGoid());
        if(uddiProxiedServiceInfo == null) throw new IllegalStateException("UDDIProxiedServiceInfo not found");
        uddiRegistryAdmin.updatePublishedGatewayWsdl(uddiProxiedServiceInfo.getGoid());

        SyspropUtil.clearProperty( "com.l7tech.console.suppressVersionCheck" );
    }

    @Test
    public void testDeleteUDDIProxiedService()
            throws MalformedURLException, LoginException, RemoteException, FindException, DeleteException, UDDIException, UDDIRegistryAdmin.UDDIRegistryNotEnabledException {
        SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

//        UDDIProxiedService proxiedService = uddiRegistryAdmin.findProxiedServiceInfoForPublishedService(70615040);
//        uddiRegistryAdmin.deleteGatewayWsdlFromUDDI(proxiedService);

        SyspropUtil.clearProperty( "com.l7tech.console.suppressVersionCheck" );
    }

    /**
     * Test that a service under uddi control cannot have it's wsdl xml changed
     * @throws Exception
     */
    @Test
    public void testUpdatePublishedServiceUnderUDDIControl() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );

        InputStream inputStream = this.getClass().getResourceAsStream("Warehouse.wsdl"); //completely change the wsdl - a different namespace!
        Document dom = XmlUtil.parse(inputStream);

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        final PublishedService service = ssgAdminSession.getServiceAdmin().findServiceByID("1540099");

        service.setWsdlXml(XmlUtil.nodeToString(dom));

        ssgAdminSession.getServiceAdmin().savePublishedService(service);

        SyspropUtil.clearProperty( "com.l7tech.console.suppressVersionCheck" );
    }

    /**
     * Test that a service under uddi control can propeties other than it's wsdl xml changed successfully
     * @throws Exception
     */
    @Test
    public void testUpdatePublishedServiceUnderUDDIControlOk() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        final PublishedService service = ssgAdminSession.getServiceAdmin().findServiceByID("1540099");
        service.setRoutingUri("testroutinguri");

        ssgAdminSession.getServiceAdmin().savePublishedService(service);

        SyspropUtil.clearProperty( "com.l7tech.console.suppressVersionCheck" );
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

        uddiClient.deleteTModel("uddi:647f6ea0-d08e-11de-a2bb-f4a2db84c921");
        uddiClient.deleteTModel("uddi:24bd4210-d08e-11de-a2bb-d7a960a62d33");

    }

    @Test
    public void testGenericBusinessServiceDelete() throws UDDIException {
        uddiClient.deleteBusinessServiceByKey("uddi:03c6fc80-c02c-11de-8342-cc4d177a16de");
    }

    @Test
    public void testGetTModel() throws UDDIException {
        String tModelKey = "uuid:aa254698-93de-3870-8df3-a5c075d64a0e";
        GenericUDDIClient genericUDDIClient = (GenericUDDIClient) uddiClient;
        TModel tModel = genericUDDIClient.getTModel(tModelKey);
        Assert.assertNotNull(tModel);
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
                        PolicyAttachmentVersion.v1_2/*not important here*/,
                        null,
                        true){};


        uddiClient.authenticate();

        Collection<UDDINamedEntity> uddiNamedEntities = uddiClient.listServices("%", false, 169401, 100);
        System.out.println("There were this many results: " + uddiNamedEntities.size());
        boolean moreAvail = uddiClient.listMoreAvailable();
        System.out.println("More are available: " + moreAvail);

    }

    @Test
    public void testDeleteUDDIServiceControl() throws Exception {
        SyspropUtil.setProperty( "com.l7tech.console.suppressVersionCheck", "true" );

        SsgAdminSession ssgAdminSession = new SsgAdminSession("irishman.l7tech.com", "admin", "password");
        UDDIRegistryAdmin uddiRegistryAdmin = ssgAdminSession.getUDDIRegistryAdmin();

        uddiRegistryAdmin.deleteUDDIServiceControl(new Goid(0,70615040));

        SyspropUtil.clearProperty( "com.l7tech.console.suppressVersionCheck" );
    }

    @Test
    public void testListWsdls() throws UDDIException {

        final Collection<WsdlPortInfo> infoCollection = uddiClient.listServiceWsdls("%", false, 0, 1000, false);
        Assert.assertNotNull(infoCollection);
        for(WsdlPortInfo wi: infoCollection) {
            System.out.println(wi.getBusinessServiceName() + "\t" + wi.getWsdlUrl());
        }
    }

    @Test
    public void testListWsdlsNoWslds() throws UDDIException {

        final Collection<WsdlPortInfo> infoCollection = uddiClient.listServiceWsdls("%", false, 0, 1000, true);
        Assert.assertNotNull(infoCollection);
        for(WsdlPortInfo wi: infoCollection) {
            System.out.println(wi.getBusinessServiceName() + "\t" + wi.getWsdlUrl());
        }
    }

    @Test
    public void testSearchCaseSensitive() throws Exception, DispositionReportFaultMessage {

//        final Collection<WsdlPortInfo> infoCollection = uddiClient.listServiceWsdls("PlayerstatsService", false, 0, 1000, false);
//        Assert.assertFalse("Results should be found", infoCollection.isEmpty());

        UDDIPolicyTool uddiSupport = new UDDIPolicyTool();

        FindService findService = new FindService();
        Name name = new Name();
        name.setValue("PlayerstatsService");
        findService.getName().add(name);
        FindQualifiers findQualifiers = new FindQualifiers();
        findQualifiers.getFindQualifier().add("caseInsensitiveMatch");
        findQualifiers.getFindQualifier().add("approximateMatch");

        findService.setFindQualifiers(findQualifiers);

        findService.setAuthInfo(uddiSupport.authToken());
        final ServiceList service = uddiSupport.getInquirePort().findService(findService);
        Assert.assertFalse("Service should have results", service.getServiceInfos().getServiceInfo().isEmpty());


    }

    @Test
    public void testClientCaseInsensitiveSearch() throws Exception{
        final Collection<WsdlPortInfo> infoCollection = uddiClient.listServiceWsdls("PlayerstatsService", false, 0, 1000, true);
        Assert.assertFalse("Results should be found", infoCollection.isEmpty());

    }

    /**
     * Publish according to GIF. In this test the proxied bindingTemplate reuses the tModels already published.
     *
     * @throws Exception
     */
    @Test
    public void testCorrectPublishOfGifProxyBindingTemplate() throws Exception{
        UDDIPolicyTool uddiSupport = new UDDIPolicyTool();

        final BusinessService origService = getBusinessService(uddiSupport, "uddi:9cfc3550-e44e-11de-a428-25a7e540a3f8");
        final BusinessService backup = getBusinessService(uddiSupport, "uddi:9cfc3550-e44e-11de-a428-25a7e540a3f8");

        Throwable t = null;
        try {
            publishGifTemplate(origService, false, null, true);

            SaveService saveService = new SaveService();
            saveService.setAuthInfo(uddiSupport.authToken());
            saveService.getBusinessService().add(origService);
            uddiSupport.getPublishPort().saveService(saveService);

        } catch (DispositionReportFaultMessage e) {
            e.printStackTrace();
            final List<Result> results = e.getFaultInfo().getResult();
            for (Result result : results) {
                System.out.println("Error: " + result.getErrInfo().getValue());
            }

            t = e;
        } finally {
//            Rollback changes
            if(t == null){
                SaveService saveService = new SaveService();
                saveService.setAuthInfo(uddiSupport.authToken());
                saveService.getBusinessService().add(backup);
                uddiSupport.getPublishPort().saveService(saveService);
            }
        }
    }

    /**
     * Publish according to GIF. In this test the proxied bindingTemplate publisheds new tModels, as this is what
     * the SSG would do
     *
     * @throws Exception
     */
    @Test
    public void testCorrectPublishOfGifProxyBindingTemplateNewTModels() throws Exception{
        UDDIPolicyTool uddiSupport = new UDDIPolicyTool();

        final BusinessService origService = getBusinessService(uddiSupport, "uddi:9cfc3550-e44e-11de-a428-25a7e540a3f8");
        final BusinessService backup = getBusinessService(uddiSupport, "uddi:9cfc3550-e44e-11de-a428-25a7e540a3f8");

        Throwable t = null;
        try {
            publishGifTemplate(origService, true, uddiSupport, true);

            SaveService saveService = new SaveService();
            saveService.setAuthInfo(uddiSupport.authToken());
            saveService.getBusinessService().add(origService);
            uddiSupport.getPublishPort().saveService(saveService);

        } catch (DispositionReportFaultMessage e) {
            e.printStackTrace();
            final List<Result> results = e.getFaultInfo().getResult();
            for (Result result : results) {
                System.out.println("Error: " + result.getErrInfo().getValue());
            }

            t = e;
        } finally {
//            Rollback changes
            if(t == null){
                SaveService saveService = new SaveService();
                saveService.setAuthInfo(uddiSupport.authToken());
                saveService.getBusinessService().add(backup);
                uddiSupport.getPublishPort().saveService(saveService);
            }
        }
    }

    /**
     * Publishes according to GIF but omits references to a WSMS as that requires an out of band publish and knowledge
     * of the resulting serviceKey.
     * 
     * @throws Exception
     */
    @Test
    public void testSystinetNoOutOfBandInformation() throws Exception{
        UDDIPolicyTool uddiSupport = new UDDIPolicyTool();

        final BusinessService origService = getBusinessService(uddiSupport, "uddi:9cfc3550-e44e-11de-a428-25a7e540a3f8");
        final BusinessService backup = getBusinessService(uddiSupport, "uddi:9cfc3550-e44e-11de-a428-25a7e540a3f8");

        Throwable t = null;
        try {
            publishGifTemplate(origService, true, uddiSupport, false);

            SaveService saveService = new SaveService();
            saveService.setAuthInfo(uddiSupport.authToken());
            saveService.getBusinessService().add(origService);
            uddiSupport.getPublishPort().saveService(saveService);

        } catch (DispositionReportFaultMessage e) {
            e.printStackTrace();
            final List<Result> results = e.getFaultInfo().getResult();
            for (Result result : results) {
                System.out.println("Error: " + result.getErrInfo().getValue());
            }

            t = e;
        } finally {
//            Rollback changes
            if(t == null){
                SaveService saveService = new SaveService();
                saveService.setAuthInfo(uddiSupport.authToken());
                saveService.getBusinessService().add(backup);
                uddiSupport.getPublishPort().saveService(saveService);
            }
        }
    }

    @Test
    public void testDeleteGifEndpoint() throws Exception{
        UDDIPolicyTool uddiSupport = new UDDIPolicyTool();

        final BusinessService origService = getBusinessService(uddiSupport, "uddi:b89ebbe0-d19f-11df-a330-7bda57c0a325");
        final BusinessService backup = getBusinessService(uddiSupport, "uddi:b89ebbe0-d19f-11df-a330-7bda57c0a325");

        Map<String, BindingTemplate> keyToTemplate = new HashMap<String, BindingTemplate>();
        final List<BindingTemplate> templates = origService.getBindingTemplates().getBindingTemplate();
        for (BindingTemplate template : templates) {
            keyToTemplate.put(template.getBindingKey(), template);
        }

        Throwable t = null;
        try {

            final BindingTemplate proxyTemplate = keyToTemplate.get("uddi:b89f7f30-d19f-11df-a330-7bda57c0a325");
            final BindingTemplate functionalTemplate = keyToTemplate.get("uddi:166bf2b0-d1a0-11df-a331-7bda57c0a325");

            final String proxyKey = proxyTemplate.getBindingKey();

            //give the functional back it's original key
            functionalTemplate.setBindingKey(proxyKey);
            //remove the added meta data
            final List<KeyedReference> funcRefs = functionalTemplate.getCategoryBag().getKeyedReference();
            for (int i = funcRefs.size() - 1; i >= 0; i--) {
                KeyedReference funcRef = funcRefs.get(i);
                final String modelKey = funcRef.getTModelKey();
                if(modelKey.equals(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_TYPE) ||
                        modelKey.equals(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_PROXY_REFERENCE)){
                    funcRefs.remove(i);
                }
            }

            //Save the functional key and see how UDDI looks

            SaveBinding saveBinding = new SaveBinding();
            saveBinding.setAuthInfo(uddiSupport.authToken());
            saveBinding.getBindingTemplate().add(functionalTemplate);
            uddiSupport.getPublishPort().saveBinding(saveBinding);

            DeleteBinding deleteBinding = new DeleteBinding();
            deleteBinding.setAuthInfo(uddiSupport.authToken());
            deleteBinding.getBindingKey().add("uddi:166bf2b0-d1a0-11df-a331-7bda57c0a325");


        } catch (DispositionReportFaultMessage e) {
            e.printStackTrace();
            final List<Result> results = e.getFaultInfo().getResult();
            for (Result result : results) {
                System.out.println("Error: " + result.getErrInfo().getValue());
            }

            t = e;
        } finally {
//            Rollback changes
            if(t == null){
                SaveService saveService = new SaveService();
                saveService.setAuthInfo(uddiSupport.authToken());
                saveService.getBusinessService().add(backup);
                uddiSupport.getPublishPort().saveService(saveService);
            }
        }
    }

    private void publishGifTemplate(BusinessService origService, boolean publishNewTModels, UDDISupport uddiSupport, boolean publishWsmsServiceMetaData) throws Exception {
        Assert.assertNotNull(origService);

        //Get the binding to take over
        final BindingTemplates bts = origService.getBindingTemplates();
        BindingTemplate origTemplate = null;
        for (BindingTemplate bt : bts.getBindingTemplate()) {
            if(bt.getBindingKey().equals("uddi:9cfc8370-e44e-11de-a428-25a7e540a3f8")){
                origTemplate = bt;
                break;
            }
        }

        //Create the new BindingTemplate
        BindingTemplate proxy = new BindingTemplate();
        //swap keys
        final String origBindingKey = origTemplate.getBindingKey();
        proxy.setBindingKey(origBindingKey);
        proxy.setServiceKey(origTemplate.getServiceKey());
        origTemplate.setBindingKey(null);
        origService.getBindingTemplates().getBindingTemplate().add(proxy);

        //Reuse tmodel instance info - as the related tModels can be reused
        final String ssgURl = "https://irishman.l7tech.com:8443/service/28246017";
        AccessPoint accessPoint = new AccessPoint();
        accessPoint.setValue(ssgURl);
        accessPoint.setUseType("endPoint");
        proxy.setAccessPoint(accessPoint);

        final TModelInstanceDetails modelInstanceDetails = origTemplate.getTModelInstanceDetails();
        if (publishNewTModels) {
            final List<TModelInstanceInfo> tModelInstanceInfo = modelInstanceDetails.getTModelInstanceInfo();
            TModel portType = null;
            TModel bindingType = null;

            for (TModelInstanceInfo modelInstanceInfo : tModelInstanceInfo) {
                final String tModelKey = modelInstanceInfo.getTModelKey();
                GetTModelDetail getTModelDetail = new GetTModelDetail();
                getTModelDetail.getTModelKey().add(tModelKey);
                try {
                    final TModelDetail modelDetail = uddiSupport.getInquirePort().getTModelDetail(getTModelDetail);
                    final TModel model = modelDetail.getTModel().get(0);
                    final UDDIUtilities.TMODEL_TYPE tModelType = UDDIUtilities.getTModelType(model, true);
                    switch (tModelType) {
                        case WSDL_BINDING:
                            bindingType = model;
                            break;
                        case WSDL_PORT_TYPE:
                            portType = model;
                            break;
                    }
                } catch (DispositionReportFaultMessage dispositionReportFaultMessage) {
                    final List<Result> result = dispositionReportFaultMessage.getFaultInfo().getResult();
                    for (Result result1 : result) {
                        System.out.println("Error Info: " + result1.getErrInfo().getValue());
                    }
                    throw dispositionReportFaultMessage;
                }
            }

            if (bindingType == null || portType == null) {
                throw new IllegalStateException("bindingType and portType tModels must be found");
            }

            Map<String, TModel> oldKeyToModel = new HashMap<String, TModel>();
            oldKeyToModel.put(portType.getTModelKey(), portType);
            oldKeyToModel.put(bindingType.getTModelKey(), bindingType);

            //publish the portType
            portType.setTModelKey(null);
            final String portTypeKey = saveTModel(uddiSupport, portType);
            portType.setTModelKey(portTypeKey);

            final List<KeyedReference> references = bindingType.getCategoryBag().getKeyedReference();
            boolean found = false;
            for (KeyedReference reference : references) {
                if (reference.getTModelKey().equals("uddi:uddi.org:wsdl:portTypeReference")) {
                    reference.setKeyValue(portTypeKey);
                    found = true;
                }
            }
            if (!found) throw new IllegalStateException("portTypeReference keyedReference not found");

            bindingType.setTModelKey(null);
            final String bindingKey = saveTModel(uddiSupport, bindingType);
            bindingType.setTModelKey(bindingKey);

            //Update the tmodel instance info's
            for (TModelInstanceInfo modelInstanceInfo : tModelInstanceInfo) {
                final String modelKey = modelInstanceInfo.getTModelKey();
                final TModel model = oldKeyToModel.get(modelKey);
                modelInstanceInfo.setTModelKey(model.getTModelKey());
            }
        }

        proxy.setTModelInstanceDetails(modelInstanceDetails);
        //Proxy required steps
        final CategoryBag categoryBag = new CategoryBag();
        proxy.setCategoryBag(categoryBag);
        //add all existing keyed references
        final List<KeyedReference> keyedReferences = categoryBag.getKeyedReference();
        final List<KeyedReference> origReferences = origTemplate.getCategoryBag().getKeyedReference();
        keyedReferences.addAll(origReferences);

        KeyedReference managedEndpoint = new KeyedReference();
        managedEndpoint.setTModelKey(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_TYPE);
        managedEndpoint.setKeyName("Management entity type");
        managedEndpoint.setKeyValue(BusinessServicePublisher.MANAGED_ENDPOINT);
        keyedReferences.add(managedEndpoint);

        KeyedReference managementSystem = new KeyedReference();
        managementSystem.setTModelKey(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_SYSTEM);
        managementSystem.setKeyName("Management System");
        managementSystem.setKeyValue("Layer7 WSMS");
        keyedReferences.add(managementSystem);

        KeyedReference managementState = new KeyedReference();
        managementState.setTModelKey(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_STATE);
        managementState.setKeyName("Governance state");
        managementState.setKeyValue("managed");
        keyedReferences.add(managementState);

        if(publishWsmsServiceMetaData){
            KeyedReference serverReference = new KeyedReference();
            serverReference.setTModelKey("uddi:systinet.com:management:server-reference");
            serverReference.setKeyName("Reference to management server");
            serverReference.setKeyValue("uddi:923c2ea0-c4d5-11df-a326-7bda57c0a325");
            keyedReferences.add(serverReference);
        }

        KeyedReference endPoint = new KeyedReference();
        endPoint.setTModelKey("uddi:systinet.com:management:url");
        endPoint.setKeyName("URL from AccessPoint");
        endPoint.setKeyValue(ssgURl);
        keyedReferences.add(endPoint);

        //functional endpoint required steps
        KeyedReference funcEndpoint = new KeyedReference();
        funcEndpoint.setTModelKey(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_TYPE);
        funcEndpoint.setKeyName("Management entity type");
        funcEndpoint.setKeyValue(BusinessServicePublisher.FUNCTIONAL_ENDPOINT);
        origReferences.add(funcEndpoint);

        KeyedReference proxyRef = new KeyedReference();
        proxyRef.setTModelKey(BusinessServicePublisher.UDDI_SYSTINET_COM_MANAGEMENT_PROXY_REFERENCE);
        proxyRef.setKeyName("Proxy reference");
        proxyRef.setKeyValue(origBindingKey);
        origReferences.add(proxyRef);
    }

    private String saveTModel(UDDISupport uddiSupport, TModel tModelToSave) throws Exception{
        SaveTModel saveTModel = new SaveTModel();
        saveTModel.setAuthInfo(uddiSupport.authToken());
        saveTModel.getTModel().add(tModelToSave);
        try {
            final TModelDetail modelDetail = uddiSupport.getPublishPort().saveTModel(saveTModel);
            final String portTypeKey = modelDetail.getTModel().get(0).getTModelKey();
            return portTypeKey;
        } catch (DispositionReportFaultMessage dispositionReportFaultMessage) {
            final List<Result> results = dispositionReportFaultMessage.getFaultInfo().getResult();
            for (Result result : results) {
                System.out.println("Error: " + result.getErrInfo().getValue());
            }
            throw new Exception("Could not publish tModel");
        }

    }

    private BusinessService getBusinessService(UDDIPolicyTool uddiSupport, String serviceKey) throws Exception{
        GetServiceDetail getServiceDetail = new GetServiceDetail();
        getServiceDetail.getServiceKey().add(serviceKey);
        try {
            final ServiceDetail serviceDetail = uddiSupport.getInquirePort().getServiceDetail(getServiceDetail);
            return serviceDetail.getBusinessService().get(0);
        } catch (DispositionReportFaultMessage dispositionReportFaultMessage) {
            final List<Result> result = dispositionReportFaultMessage.getFaultInfo().getResult();
            for (Result result1 : result) {
                System.out.println("Result: " + result1.getErrInfo().getValue());
            }
            throw dispositionReportFaultMessage;
        }
    }

    private static void initializeLogging() {
        final LogManager logManager = LogManager.getLogManager();

        final File file = new File("logging.properties");
        if (file.exists()) {
            InputStream in = null;
            try {
                in = file.toURI().toURL().openStream();
                if (in != null) {
                    logManager.readConfiguration(in);
                }
            } catch (IOException e) {
                System.err.println("Cannot initialize logging " + e.getMessage());
            } finally {
                try {
                    if (in != null) in.close();
                } catch (IOException e) { // should not happen
                    System.err.println("Cannot close logging properties input stream " + e.getMessage());
                }
            }
        } else {
            System.err.println("Cannot initialize logging");
        }
    }

    private UDDIClient getUDDIClient(){
//        return new GenericUDDIClient("http://DONALWINXP:53307/UddiRegistry/inquiry",
//                        "http://DONALWINXP:53307/UddiRegistry/publish",
//                        null,
//                        "http://DONALWINXP:53307/UddiRegistry/security",
//                        "administrator",
//                        "7layer",
//                        PolicyAttachmentVersion.v1_2/*not important here*/, null, false){};

//        return new GenericUDDIClient("http://activesoa:53307/UddiRegistry/inquiry",
//                        "http://activesoa:53307/UddiRegistry/publish",
//                        null,
//                        "http://activesoa:53307/UddiRegistry/security",
//                        "administrator",
//                        "7layer",
//                        PolicyAttachmentVersion.v1_2/*not important here*/, null, false){};

        return new GenericUDDIClient("http://systinet.l7tech.com:8080/uddi/inquiry",
                        "http://systinet.l7tech.com:8080/uddi/publishing",
                        null,
                        "http://systinet.l7tech.com:8080/uddi/security",
                        "admin",
                        "7layer",
                        PolicyAttachmentVersion.v1_2/*not important here*/,
                        null,
                        true){};

//        return new GenericUDDIClient("http://centrasiteuddi:53307/UddiRegistry/inquiry",
//                        "http://centrasiteuddi:53307/UddiRegistry/publish",
//                        null,
//                        "http://centrasiteuddi:53307/UddiRegistry/security",
//                        "administrator",
//                        "7layer",
//                        PolicyAttachmentVersion.v1_2/*not important here*/, null){};
    }

    public Reader getWsdlReader(String resourcetoread) {
        InputStream inputStream = this.getClass().getResourceAsStream(resourcetoread);
        return new InputStreamReader(inputStream);
    }

}
